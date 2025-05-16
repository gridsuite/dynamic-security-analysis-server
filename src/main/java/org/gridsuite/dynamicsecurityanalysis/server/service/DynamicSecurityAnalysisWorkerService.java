/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.io.FileUtil;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynawoModelsSupplier;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationsResult;
import com.powsybl.security.PostContingencyComputationStatus;
import com.powsybl.security.SecurityAnalysisReport;
import com.powsybl.security.dynamic.DynamicSecurityAnalysis;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisRunParameters;
import com.powsybl.security.results.PostContingencyResult;
import com.powsybl.ws.commons.computation.service.*;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException;
import org.gridsuite.dynamicsecurityanalysis.server.dto.DynamicSecurityAnalysisStatus;
import org.gridsuite.dynamicsecurityanalysis.server.dto.contingency.ContingencyInfos;
import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersInfos;
import org.gridsuite.dynamicsecurityanalysis.server.service.client.ActionsClient;
import org.gridsuite.dynamicsecurityanalysis.server.service.client.DynamicSimulationClient;
import org.gridsuite.dynamicsecurityanalysis.server.service.contexts.DynamicSecurityAnalysisResultContext;
import org.gridsuite.dynamicsecurityanalysis.server.service.contexts.DynamicSecurityAnalysisRunContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException.Type.CONTINGENCIES_NOT_FOUND;
import static org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException.Type.DUMP_FILE_ERROR;
import static org.gridsuite.dynamicsecurityanalysis.server.service.DynamicSecurityAnalysisService.COMPUTATION_TYPE;
import static org.gridsuite.dynamicsecurityanalysis.server.utils.Utils.*;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class, NotificationService.class})
@Service
public class DynamicSecurityAnalysisWorkerService extends AbstractWorkerService<SecurityAnalysisReport, DynamicSecurityAnalysisRunContext, DynamicSecurityAnalysisParametersInfos, DynamicSecurityAnalysisResultService> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSecurityAnalysisWorkerService.class);

    private final DynamicSimulationClient dynamicSimulationClient;
    private final ActionsClient actionsClient;
    private final ParametersService parametersService;

    public DynamicSecurityAnalysisWorkerService(NetworkStoreService networkStoreService,
                                                NotificationService notificationService,
                                                ReportService reportService,
                                                ExecutionService executionService,
                                                DynamicSecurityAnalysisObserver observer,
                                                ObjectMapper objectMapper,
                                                DynamicSecurityAnalysisResultService dynamicSecurityAnalysisResultService,
                                                DynamicSimulationClient dynamicSimulationClient,
                                                ActionsClient actionsClient,
                                                ParametersService parametersService) {
        super(networkStoreService, notificationService, reportService, dynamicSecurityAnalysisResultService, executionService, observer, objectMapper);
        this.dynamicSimulationClient = Objects.requireNonNull(dynamicSimulationClient);
        this.actionsClient = Objects.requireNonNull(actionsClient);
        this.parametersService = Objects.requireNonNull(parametersService);
    }

    /**
     * Use this method to mock with DockerLocalComputationManager in case of integration tests with test container
     *
     * @return a computation manager
     */
    public ComputationManager getComputationManager() {
        return executionService.getComputationManager();
    }

    @Override
    protected DynamicSecurityAnalysisResultContext fromMessage(Message<String> message) {
        return DynamicSecurityAnalysisResultContext.fromMessage(message, objectMapper);
    }

    public void updateResult(UUID resultUuid, SecurityAnalysisReport result) {
        Objects.requireNonNull(resultUuid);
        DynamicSecurityAnalysisStatus status = result.getResult().getPostContingencyResults().stream().anyMatch(postContingencyResult -> postContingencyResult.getStatus() == PostContingencyComputationStatus.FAILED) ?
                DynamicSecurityAnalysisStatus.FAILED :
                DynamicSecurityAnalysisStatus.SUCCEED;

        resultService.updateResult(resultUuid, status);
    }

    @Override
    protected void saveResult(Network network, AbstractResultContext<DynamicSecurityAnalysisRunContext> resultContext, SecurityAnalysisReport result) {
        updateResult(resultContext.getResultUuid(), result);
    }

    @Override
    protected String getComputationType() {
        return COMPUTATION_TYPE;
    }

    /**
     * TODO : open to public for mocking test with enrich report, to revert to protected when powsybl-dynawo implements
     */
    @Override
    public void postRun(DynamicSecurityAnalysisRunContext runContext, AtomicReference<ReportNode> rootReportNode, SecurityAnalysisReport securityAnalysisReport) {
        // TODO remove these reports when powsybl-dynawo implements
        // enrich infos for contingencies timeline report
        if (runContext.getReportInfos().reportUuid() != null) {
            ReportNode dsaReportNode = getReportNode(runContext.getReportNode(), "dsa", null);
            if (dsaReportNode != null) {
                enrichContingenciesTimelineReport(securityAnalysisReport, dsaReportNode);
            }
        }

        super.postRun(runContext, rootReportNode, securityAnalysisReport);
    }

    // open the visibility from protected to public to mock in a test where the stop arrives early
    @Override
    public void preRun(DynamicSecurityAnalysisRunContext runContext) {
        super.preRun(runContext);

        // get contingencies from actions server
        List<ContingencyInfos> contingencyList = actionsClient.getContingencyList(runContext.getParameters().getContingencyListIds(), runContext.getNetworkUuid(), runContext.getVariantId());
        if (CollectionUtils.isEmpty(contingencyList)) {
            throw new DynamicSecurityAnalysisException(CONTINGENCIES_NOT_FOUND, "No contingencies");
        }

        // get dump file from dynamic simulation server
        byte[] dynamicSimulationZippedOutputState = dynamicSimulationClient.getOutputState(runContext.getDynamicSimulationResultUuid());

        // get dynamic model list from dynamic simulation server
        byte[] dynamicSimulationZippedDynamicModel = dynamicSimulationClient.getDynamicModel(runContext.getDynamicSimulationResultUuid());
        List<DynamicModelConfig> dynamicModel = parametersService.unZipDynamicModel(dynamicSimulationZippedDynamicModel, objectMapper);

        // get dynamic simulation parameters from dynamic simulation server
        byte[] dynamicSimulationZippedParameters = dynamicSimulationClient.getDynamicSimulationParameters(runContext.getDynamicSimulationResultUuid());
        DynamicSimulationParameters dynamicSimulationParameters = parametersService.unZipDynamicSimulationParameters(dynamicSimulationZippedParameters, objectMapper);

        DynamicSecurityAnalysisParametersInfos parametersInfos = runContext.getParameters();

        // create a new dynamic security analysis parameters
        DynamicSecurityAnalysisParameters parameters = new DynamicSecurityAnalysisParameters();
        parameters.setDynamicSimulationParameters(dynamicSimulationParameters);

        // set start and stop times
        parameters.getDynamicSimulationParameters().setStartTime(dynamicSimulationParameters.getStopTime());
        parameters.getDynamicSimulationParameters().setStopTime(dynamicSimulationParameters.getStopTime() + parametersInfos.getScenarioDuration());

        // set contingency start time
        parameters.getDynamicContingenciesParameters().setContingenciesStartTime(parametersInfos.getContingenciesStartTime());

        // enrich runContext
        runContext.setContingencies(contingencyList);
        runContext.setDynamicModelContent(dynamicModel);
        runContext.setDynamicSecurityAnalysisParameters(parameters);

        // create a working folder for this run
        Path workDir;
        workDir = createWorkingDirectory();
        runContext.setWorkDir(workDir);

        // enrich dump parameters
        parametersService.setupDumpParameters(workDir, parameters.getDynamicSimulationParameters(), dynamicSimulationZippedOutputState);
    }

    @Override
    public CompletableFuture<SecurityAnalysisReport> getCompletableFuture(DynamicSecurityAnalysisRunContext runContext, String provider, UUID resultUuid) {

        DynamicModelsSupplier dynamicModelsSupplier = new DynawoModelsSupplier(runContext.getDynamicModelContent());

        List<Contingency> contingencies = runContext.getContingencies()
                .stream().map(ContingencyInfos::getContingency)
                .filter(Objects::nonNull).toList();
        ContingenciesProvider contingenciesProvider = network -> contingencies;

        DynamicSecurityAnalysisParameters parameters = runContext.getDynamicSecurityAnalysisParameters();
        LOGGER.info("Run dynamic security analysis on network {}, startTime {}, stopTime {}, contingenciesStartTime {}",
                runContext.getNetworkUuid(), parameters.getDynamicSimulationParameters().getStartTime(),
                parameters.getDynamicSimulationParameters().getStopTime(),
                parameters.getDynamicContingenciesParameters().getContingenciesStartTime());

        DynamicSecurityAnalysisRunParameters runParameters = new DynamicSecurityAnalysisRunParameters()
                .setComputationManager(getComputationManager())
                .setDynamicSecurityAnalysisParameters(parameters)
                .setReportNode(runContext.getReportNode());

        DynamicSecurityAnalysis.Runner runner = DynamicSecurityAnalysis.find(provider);

        return runner.runAsync(runContext.getNetwork(),
            runContext.getVariantId() != null ? runContext.getVariantId() : VariantManagerConstants.INITIAL_VARIANT_ID,
            dynamicModelsSupplier,
            contingenciesProvider,
            runParameters
        );
    }

    @Override
    protected void handleNonCancellationException(AbstractResultContext<DynamicSecurityAnalysisRunContext> resultContext, Exception exception, AtomicReference<ReportNode> rootReporter) {
        super.handleNonCancellationException(resultContext, exception, rootReporter);
        // try to get report nodes at powsybl level
        List<ReportNode> computationReportNodes = Optional.ofNullable(resultContext.getRunContext().getReportNode()).map(ReportNode::getChildren).orElse(null);
        if (CollectionUtils.isNotEmpty(computationReportNodes)) { // means computing has started at powsybl level
            //  re-inject result table since it has been removed by handling exception in the super
            resultService.insertStatus(List.of(resultContext.getResultUuid()), DynamicSecurityAnalysisStatus.FAILED);
            // continue sending report for tracing reason
            super.postRun(resultContext.getRunContext(), rootReporter, null);
        }
    }

    @Bean
    @Override
    public Consumer<Message<String>> consumeRun() {
        return super.consumeRun();
    }

    @Bean
    @Override
    public Consumer<Message<String>> consumeCancel() {
        return super.consumeCancel();
    }

    @Override
    protected void clean(AbstractResultContext<DynamicSecurityAnalysisRunContext> resultContext) {
        super.clean(resultContext);
        // clean working directory
        Path workDir = resultContext.getRunContext().getWorkDir();
        removeWorkingDirectory(workDir);
    }

    private Path createWorkingDirectory() {
        Path workDir;
        Path localDir = getComputationManager().getLocalDir();
        try {
            workDir = Files.createTempDirectory(localDir, "dynamic_security_analysis_");
        } catch (IOException e) {
            throw new DynamicSecurityAnalysisException(DUMP_FILE_ERROR, String.format("Error occurred while creating a working directory inside the local directory %s",
                    localDir.toAbsolutePath()));
        }
        return workDir;
    }

    private void removeWorkingDirectory(Path workDir) {
        if (workDir != null) {
            try {
                FileUtil.removeDir(workDir);
            } catch (IOException e) {
                LOGGER.error(String.format("%s: Error occurred while cleaning working directory at %s", getComputationType(), workDir.toAbsolutePath()), e);
            }
        } else {
            LOGGER.info("{}: No working directory to clean", getComputationType());
        }
    }

    // --- TODO remove these reports when powsybl-dynawo implements --- //
    private static void enrichContingenciesTimelineReport(SecurityAnalysisReport securityAnalysisReport, ReportNode reportNode) {
        for (PostContingencyResult postContingencyResult : securityAnalysisReport.getResult().getPostContingencyResults()) {
            String contingencyId = postContingencyResult.getContingency().getId();
            List<LimitViolation> limitViolations = Optional.ofNullable(postContingencyResult.getLimitViolationsResult())
                    .map(LimitViolationsResult::getLimitViolations).orElse(null);

            ReportNode contingencyReportNode = getReportNode(reportNode, "saContingency", String.format("(.*)%s(.*)", contingencyId));
            if (contingencyReportNode != null) {
                contingencyReportNode.newReportNode()
                        .withSeverity(postContingencyResult.getStatus() == PostContingencyComputationStatus.CONVERGED ? TypedValue.INFO_SEVERITY : TypedValue.ERROR_SEVERITY)
                        .withMessageTemplate("dynamicsecurityanalysis.server.saContingencyStatus")
                        .withUntypedValue("contingencyStatus", postContingencyResult.getStatus().name()).add();
                if (isNotEmpty(limitViolations)) {
                    ReportNode limitViolationsReportNode = contingencyReportNode.newReportNode()
                            .withMessageTemplate("dynamicsecurityanalysis.server.limitViolations")
                            .add();
                    for (LimitViolation limitViolation : limitViolations) {
                        limitViolationsReportNode.newReportNode()
                                .withSeverity(TypedValue.TRACE_SEVERITY)
                                .withMessageTemplate("dynamicsecurityanalysis.server.limitViolation")
                                .withUntypedValue("count", limitViolations.indexOf(limitViolation) + 1)
                                .withUntypedValue("limitViolation", limitViolation.toString())
                                .add();
                    }
                }
            }
        }
    }

}

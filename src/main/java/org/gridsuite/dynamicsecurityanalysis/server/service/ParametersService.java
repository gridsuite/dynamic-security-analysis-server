/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.contingency.Contingency;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationProvider;
import com.powsybl.dynawo.DumpFileParameters;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.computation.dto.ReportInfos;
import org.gridsuite.computation.error.ComputationException;
import org.gridsuite.dynamicsecurityanalysis.server.dto.contingency.ContingencyInfos;
import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersInfos;
import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersValues;
import org.gridsuite.dynamicsecurityanalysis.server.entities.parameters.DynamicSecurityAnalysisParametersEntity;
import org.gridsuite.dynamicsecurityanalysis.server.error.DynamicSecurityAnalysisException;
import org.gridsuite.dynamicsecurityanalysis.server.repositories.DynamicSecurityAnalysisParametersRepository;
import org.gridsuite.dynamicsecurityanalysis.server.service.client.ActionsClient;
import org.gridsuite.dynamicsecurityanalysis.server.service.contexts.DynamicSecurityAnalysisRunContext;
import org.gridsuite.dynamicsecurityanalysis.server.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.gridsuite.computation.error.ComputationBusinessErrorCode.PARAMETERS_NOT_FOUND;
import static org.gridsuite.dynamicsecurityanalysis.server.error.DynamicSecurityAnalysisBusinessErrorCode.CONTINGENCIES_NOT_FOUND;
import static org.gridsuite.dynamicsecurityanalysis.server.error.DynamicSecurityAnalysisBusinessErrorCode.PROVIDER_NOT_FOUND;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class ParametersService {

    public static final String MSG_PARAMETERS_UUID_NOT_FOUND = "Parameters uuid not found: ";

    private final String defaultProvider;

    private final DynamicSecurityAnalysisParametersRepository dynamicSecurityAnalysisParametersRepository;
    private final ActionsClient actionsClient;

    @Autowired
    public ParametersService(@Value("${dynamic-security-analysis.default-provider}") String defaultProvider,
                             DynamicSecurityAnalysisParametersRepository dynamicSecurityAnalysisParametersRepository,
                             ActionsClient actionsClient) {
        this.defaultProvider = defaultProvider;
        this.dynamicSecurityAnalysisParametersRepository = dynamicSecurityAnalysisParametersRepository;
        this.actionsClient = actionsClient;
    }

    @Transactional(readOnly = true)
    public DynamicSecurityAnalysisRunContext createRunContext(UUID networkUuid, String variantId, String receiver,
                                                              String provider, ReportInfos reportInfos, String userId,
                                                              UUID dynamicSimulationResultUuid,
                                                              UUID dynamicSecurityAnalysisParametersUuid,
                                                              boolean debug) {

        // get parameters from the local database
        DynamicSecurityAnalysisParametersInfos dynamicSecurityAnalysisParametersInfos = doGetParameters(dynamicSecurityAnalysisParametersUuid);

        // build run context
        DynamicSecurityAnalysisRunContext runContext = DynamicSecurityAnalysisRunContext.builder()
                .networkUuid(networkUuid)
                .variantId(variantId)
                .receiver(receiver)
                .reportInfos(reportInfos)
                .userId(userId)
                .parameters(dynamicSecurityAnalysisParametersInfos)
                .debug(debug)
                .build();
        runContext.setDynamicSimulationResultUuid(dynamicSimulationResultUuid);

        // set provider for run context
        String providerToUse = provider;
        if (providerToUse == null) {
            providerToUse = Optional.ofNullable(runContext.getParameters().getProvider()).orElse(defaultProvider);
        }

        runContext.setProvider(providerToUse);

        // check provider
        if (DynamicSimulationProvider.findAll().stream()
                .noneMatch(elem -> Objects.equals(elem.getName(), runContext.getProvider()))) {
            throw new DynamicSecurityAnalysisException(PROVIDER_NOT_FOUND, "Dynamic security analysis provider not found: " + runContext.getProvider());
        }

        return runContext;
    }

    // --- Dynamic simulation result related methods --- //

    public void setupDumpParameters(Path workDir, DynamicSimulationParameters dynamicSimulationParameters, byte[] zippedOutputState) {
        Path dumpFile = unZipDumpFile(workDir, zippedOutputState);
        DynawoSimulationParameters dynawoSimulationParameters = dynamicSimulationParameters.getExtension(DynawoSimulationParameters.class);
        dynawoSimulationParameters.setDumpFileParameters(DumpFileParameters.createImportDumpFileParameters(workDir, dumpFile.getFileName().toString()));
    }

    private Path unZipDumpFile(Path dumpDir, byte[] zippedOutputState) {
        Path dumpFile = dumpDir.resolve("outputState.dmp");
        try {
            // UNZIP output state
            Utils.unzip(zippedOutputState, dumpFile);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Error occurred while unzip the output state into a dump file in the directory %s",
                    dumpDir.toAbsolutePath()), e);
        }
        return dumpFile;
    }

    public List<DynamicModelConfig> unZipDynamicModel(byte[] dynamicSimulationZippedDynamicModel, ObjectMapper objectMapper) {
        try {
            // unzip dynamic model
            List<DynamicModelConfig> dynamicModel = Utils.unzip(dynamicSimulationZippedDynamicModel, objectMapper, new TypeReference<>() { });
            return dynamicModel;
        } catch (IOException e) {
            throw new UncheckedIOException("Error occurred while unzip the dynamic model", e);
        }
    }

    public DynamicSimulationParameters unZipDynamicSimulationParameters(byte[] dynamicSimulationZippedParameters, ObjectMapper objectMapper) {
        try {
            // unzip dynamic model
            return Utils.unzip(dynamicSimulationZippedParameters, objectMapper, DynamicSimulationParameters.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Error occurred while unzip the dynamic simulation parameters", e);
        }
    }

    // --- Dynamic security analysis parameters related methods --- //

    @Transactional(readOnly = true)
    public DynamicSecurityAnalysisParametersInfos getParameters(UUID parametersUuid) {
        return doGetParameters(parametersUuid);
    }

    private DynamicSecurityAnalysisParametersInfos doGetParameters(UUID parametersUuid) {
        DynamicSecurityAnalysisParametersEntity entity = dynamicSecurityAnalysisParametersRepository.findById(parametersUuid)
                .orElseThrow(() -> new ComputationException(PARAMETERS_NOT_FOUND, MSG_PARAMETERS_UUID_NOT_FOUND + parametersUuid));

        return new DynamicSecurityAnalysisParametersInfos(parametersUuid, entity.getProvider(), entity.getScenarioDuration(), entity.getContingenciesStartTime(), entity.getContingencyListIds());
    }

    @Transactional
    public UUID createParameters(DynamicSecurityAnalysisParametersInfos parametersInfos) {
        return doCreateParameters(parametersInfos);
    }

    private UUID doCreateParameters(DynamicSecurityAnalysisParametersInfos parametersInfos) {
        return dynamicSecurityAnalysisParametersRepository.save(new DynamicSecurityAnalysisParametersEntity(parametersInfos)).getId();
    }

    @Transactional
    public UUID createDefaultParameters() {
        DynamicSecurityAnalysisParametersInfos defaultParametersInfos = getDefaultParametersValues();
        return doCreateParameters(defaultParametersInfos);
    }

    public DynamicSecurityAnalysisParametersInfos getDefaultParametersValues() {
        DynamicSecurityAnalysisParameters defaultConfigParameters = DynamicSecurityAnalysisParameters.load();
        return DynamicSecurityAnalysisParametersInfos.builder()
                .provider(defaultProvider)
                .scenarioDuration(5.0)
                .contingenciesStartTime(defaultConfigParameters.getDynamicContingenciesParameters().getContingenciesStartTime())
                .contingencyListIds(null)
                .build();
    }

    @Transactional
    public UUID duplicateParameters(UUID sourceParametersUuid) {
        DynamicSecurityAnalysisParametersEntity entity = dynamicSecurityAnalysisParametersRepository.findById(sourceParametersUuid)
                .orElseThrow(() -> new ComputationException(PARAMETERS_NOT_FOUND, MSG_PARAMETERS_UUID_NOT_FOUND + sourceParametersUuid));
        DynamicSecurityAnalysisParametersInfos duplicatedParametersInfos = entity.toDto();
        duplicatedParametersInfos.setId(null);
        return doCreateParameters(duplicatedParametersInfos);
    }

    @Transactional(readOnly = true)
    public List<DynamicSecurityAnalysisParametersInfos> getAllParameters() {
        return dynamicSecurityAnalysisParametersRepository.findAll().stream()
                .map(DynamicSecurityAnalysisParametersEntity::toDto)
                .toList();
    }

    @Transactional
    public void updateParameters(UUID parametersUuid, DynamicSecurityAnalysisParametersInfos parametersInfos) {
        DynamicSecurityAnalysisParametersEntity entity = dynamicSecurityAnalysisParametersRepository.findById(parametersUuid)
                .orElseThrow(() -> new ComputationException(PARAMETERS_NOT_FOUND, MSG_PARAMETERS_UUID_NOT_FOUND + parametersUuid));
        if (parametersInfos == null) {
            //if the parameters is null it means it's a reset to defaultValues
            entity.update(getDefaultParametersValues());
        } else {
            entity.update(parametersInfos);
        }
    }

    @Transactional
    public void deleteParameters(UUID parametersUuid) {
        dynamicSecurityAnalysisParametersRepository.deleteById(parametersUuid);
    }

    // --- Dynamic security analysis evaluated parameters related methods --- //

    public List<Contingency> getContingencies(List<UUID> contingencyListIds, UUID networkUuid, String variantId) {
        List<ContingencyInfos> contingencyList = actionsClient.getContingencyList(contingencyListIds, networkUuid, variantId);
        List<Contingency> contingencies = contingencyList.stream().map(ContingencyInfos::getContingency).filter(Objects::nonNull).toList();
        if (CollectionUtils.isEmpty(contingencies)) {
            throw new DynamicSecurityAnalysisException(CONTINGENCIES_NOT_FOUND, "No contingencies");
        }
        return contingencies;
    }

    @Transactional(readOnly = true)
    public DynamicSecurityAnalysisParametersValues getParametersValues(UUID parametersUuid, UUID networkUuid, String variantId) {
        DynamicSecurityAnalysisParametersEntity entity = dynamicSecurityAnalysisParametersRepository.findById(parametersUuid)
                .orElseThrow(() -> new ComputationException(PARAMETERS_NOT_FOUND, MSG_PARAMETERS_UUID_NOT_FOUND + parametersUuid));
        List<Contingency> contingencyList = getContingencies(entity.getContingencyListIds(), networkUuid, variantId);
        return new DynamicSecurityAnalysisParametersValues(entity.getContingenciesStartTime(), contingencyList);
    }

}

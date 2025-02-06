/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.controller;

import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.datasource.ResourceDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.security.*;
import com.powsybl.security.dynamic.DynamicSecurityAnalysis;
import com.powsybl.security.results.PostContingencyResult;
import com.powsybl.security.results.PreContingencyResult;
import com.powsybl.ws.commons.computation.service.NotificationService;
import org.gridsuite.dynamicsecurityanalysis.server.dto.DynamicSecurityAnalysisStatus;
import org.gridsuite.dynamicsecurityanalysis.server.dto.contingency.ContingencyInfos;
import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersInfos;
import org.gridsuite.dynamicsecurityanalysis.server.entities.parameters.DynamicSecurityAnalysisParametersEntity;
import org.gridsuite.dynamicsecurityanalysis.server.service.contexts.DynamicSecurityAnalysisRunContext;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.messaging.Message;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.powsybl.network.store.model.NetworkStoreApi.VERSION;
import static com.powsybl.ws.commons.computation.service.AbstractResultContext.VARIANT_ID_HEADER;
import static com.powsybl.ws.commons.computation.service.NotificationService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gridsuite.dynamicsecurityanalysis.server.service.DynamicSecurityAnalysisService.COMPUTATION_TYPE;
import static org.gridsuite.dynamicsecurityanalysis.server.utils.Utils.RESOURCE_PATH_DELIMITER;
import static org.gridsuite.dynamicsecurityanalysis.server.utils.Utils.zip;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicSecurityAnalysisControllerTest extends AbstractDynamicSecurityAnalysisControllerTest {
    // mapping names
    public static final String TEST_CASE_01 = "_01";

    // directories
    public static final String DATA_IEEE14_BASE_DIR = RESOURCE_PATH_DELIMITER + "data" + RESOURCE_PATH_DELIMITER + "ieee14";
    public static final String INPUT = "input";
    public static final String OUTPUT_STATE_DUMP_FILE = "outputState.dmp";
    public static final String DYNAMIC_MODEL_DUMP_FILE = "dynamicModel.dmp";
    public static final String DYNAMIC_SIMULATION_PARAMETERS_DUMP_FILE = "dynamicSimulationParameters.dmp";

    private static final UUID NETWORK_UUID = UUID.randomUUID();
    private static final String VARIANT_1_ID = "variant_1";
    private static final String NETWORK_FILE = "IEEE14.iidm";

    private static final UUID DYNAMIC_SIMULATION_RESULT_UUID = UUID.randomUUID();
    private static final UUID PARAMETERS_UUID = UUID.randomUUID();
    private static final UUID CONTINGENCY_UUID = UUID.randomUUID();

    @Autowired
    private OutputDestination output;

    @SpyBean
    private NotificationService notificationService;

    @Override
    public OutputDestination getOutputDestination() {
        return output;
    }

    @Override
    protected void initNetworkStoreServiceMock() {
        ReadOnlyDataSource dataSource = new ResourceDataSource("IEEE14",
                new ResourceSet(DATA_IEEE14_BASE_DIR, NETWORK_FILE));
        Network network = Importers.importData("XIIDM", dataSource, null);
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_1_ID);
        given(networkStoreClient.getNetwork(NETWORK_UUID, PreloadingStrategy.COLLECTION)).willReturn(network);
    }

    @Override
    protected void initDynamicSimulationClientMock() {
        try {
            String inputDir = DATA_IEEE14_BASE_DIR +
                              RESOURCE_PATH_DELIMITER + TEST_CASE_01 +
                              RESOURCE_PATH_DELIMITER + INPUT;

            // load outputState.dmp
            String outputStateFilePath = inputDir + RESOURCE_PATH_DELIMITER + OUTPUT_STATE_DUMP_FILE;
            InputStream outputStateIS = getClass().getResourceAsStream(outputStateFilePath);
            assert outputStateIS != null;
            byte[] zippedOutputState = zip(outputStateIS);

            given(dynamicSimulationClient.getOutputState(DYNAMIC_SIMULATION_RESULT_UUID)).willReturn(zippedOutputState);

            // load dynamicModel.dmp
            String dynamicModelFilePath = inputDir + RESOURCE_PATH_DELIMITER + DYNAMIC_MODEL_DUMP_FILE;
            InputStream dynamicModelIS = getClass().getResourceAsStream(dynamicModelFilePath);
            assert dynamicModelIS != null;
            byte[] zippedDynamicModel = zip(dynamicModelIS);

            given(dynamicSimulationClient.getDynamicModel(DYNAMIC_SIMULATION_RESULT_UUID)).willReturn(zippedDynamicModel);

            // load dynamicSimulationParameters.dmp
            String dynamicSimulationParametersFilePath = inputDir + RESOURCE_PATH_DELIMITER + DYNAMIC_SIMULATION_PARAMETERS_DUMP_FILE;
            InputStream dynamicSimulationParametersIS = getClass().getResourceAsStream(dynamicSimulationParametersFilePath);
            assert dynamicSimulationParametersIS != null;
            byte[] zippedDynamicSimulationParameters = zip(dynamicSimulationParametersIS);

            given(dynamicSimulationClient.getDynamicSimulationParameters(DYNAMIC_SIMULATION_RESULT_UUID)).willReturn(zippedDynamicSimulationParameters);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected void initActionsClientMock() {
        when(actionsClient.getContingencyList(anyList(), eq(NETWORK_UUID), eq(VARIANT_1_ID)))
                .thenReturn(List.of(new ContingencyInfos(Contingency.load("_LOAD__11_EC"))));
    }

    @Override
    protected void initDynamicSecurityAnalysisParametersRepositoryMock() {
        given(dynamicSecurityAnalysisParametersRepository.findById(PARAMETERS_UUID)).willReturn(Optional.of(new DynamicSecurityAnalysisParametersEntity()));
    }

    @Override
    protected void initParametersServiceSpy() {
        DynamicSecurityAnalysisParametersInfos defaultParams = parametersService.getDefaultParametersValues("Dynawo");
        defaultParams.setScenarioDuration(50.0);
        defaultParams.setContingenciesStartTime(5.0);
        defaultParams.setContingencyListIds(List.of(CONTINGENCY_UUID));

        // setup spy bean
        when(parametersService.getParameters(PARAMETERS_UUID)).thenReturn(defaultParams);
    }

    @Test
    void testResult() throws Exception {

        doReturn(CompletableFuture.completedFuture(new SecurityAnalysisReport(SecurityAnalysisResult.empty())))
                .when(dynamicSecurityAnalysisWorkerService).getCompletableFuture(any(), any(), any());

        //run the dynamic security analysis on a specific variant
        MvcResult result = mockMvc.perform(
                        post("/v1/networks/{networkUuid}/run?"
                             + "&" + VARIANT_ID_HEADER + "=" + VARIANT_1_ID
                             + "&dynamicSimulationResultUuid=" + DYNAMIC_SIMULATION_RESULT_UUID
                             + "&parametersUuid=" + PARAMETERS_UUID,
                                NETWORK_UUID.toString())
                                .contentType(APPLICATION_JSON)
                                .header(HEADER_USER_ID, "testUserId"))
                .andExpect(status().isOk())
                .andReturn();
        UUID runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        Message<byte[]> messageSwitch = output.receive(1000 * 10, dsaResultDestination);
        assertThat(messageSwitch.getHeaders()).containsEntry(HEADER_RESULT_UUID, runUuid.toString());

        //run the dynamic security analysis on the implicit default variant
        result = mockMvc.perform(
                        post("/v1/networks/{networkUuid}/run?"
                             + "&dynamicSimulationResultUuid=" + DYNAMIC_SIMULATION_RESULT_UUID
                             + "&parametersUuid=" + PARAMETERS_UUID,
                                NETWORK_UUID.toString())
                                .contentType(APPLICATION_JSON)
                                .header(HEADER_USER_ID, "testUserId"))
                .andExpect(status().isOk())
                .andReturn();
        runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        messageSwitch = output.receive(1000 * 10, dsaResultDestination);
        assertThat(messageSwitch.getHeaders()).containsEntry(HEADER_RESULT_UUID, runUuid.toString());

        //get the calculation status
        result = mockMvc.perform(
                get("/v1/results/{resultUuid}/status", runUuid))
            .andExpect(status().isOk())
            .andReturn();

        DynamicSecurityAnalysisStatus status = objectMapper.readValue(result.getResponse().getContentAsString(), DynamicSecurityAnalysisStatus.class);

        //depending on the execution speed it can be both
        assertThat(status).isIn(DynamicSecurityAnalysisStatus.SUCCEED, DynamicSecurityAnalysisStatus.RUNNING);

        //get the status of a non-existing result and expect null as status
        assertResultStatus(UUID.randomUUID(), null);

        // test invalidate status => i.e. set NOT_DONE
        // set NOT_DONE
        mockMvc.perform(
                put("/v1/results/invalidate-status?resultUuid=" + runUuid))
            .andExpect(status().isOk());

        // check whether NOT_DONE is persisted
        result = mockMvc.perform(
                get("/v1/results/{resultUuid}/status", runUuid))
            .andExpect(status().isOk())
            .andReturn();
        DynamicSecurityAnalysisStatus statusAfterInvalidate = objectMapper.readValue(result.getResponse().getContentAsString(), DynamicSecurityAnalysisStatus.class);

        assertThat(statusAfterInvalidate).isSameAs(DynamicSecurityAnalysisStatus.NOT_DONE);

        // set NOT_DONE for none existing result
        mockMvc.perform(
                        put("/v1/results/invalidate-status?resultUuid=" + UUID.randomUUID()))
                .andExpect(status().isNotFound());

        //delete a result
        mockMvc.perform(
                delete("/v1/results/{resultUuid}", runUuid))
            .andExpect(status().isOk());

        //try to get the removed result and expect a not found
        assertResultStatus(runUuid, null);

        //delete a none existing result
        mockMvc.perform(
                        delete("/v1/results/{resultUuid}", UUID.randomUUID()))
                .andExpect(status().isOk());

        //delete all results and except ok
        mockMvc.perform(
                delete("/v1/results"))
            .andExpect(status().isOk());
    }

    @Test
    void testRunWithSynchronousExceptions() throws Exception {
        //run the dynamic security analysis on a non-exiting provider
        mockMvc.perform(
                        post("/v1/networks/{networkUuid}/run?"
                             + "&provider=notFoundProvider"
                             + "&" + VARIANT_ID_HEADER + "=" + VARIANT_1_ID
                             + "&dynamicSimulationResultUuid=" + DYNAMIC_SIMULATION_RESULT_UUID
                             + "&parametersUuid=" + PARAMETERS_UUID,
                                NETWORK_UUID.toString())
                                .contentType(APPLICATION_JSON)
                                .header(HEADER_USER_ID, "testUserId"))
                .andExpect(status().isNotFound());

        //run the dynamic security analysis on a non-exiting parameters
        mockMvc.perform(
                        post("/v1/networks/{networkUuid}/run?"
                             + "&" + VARIANT_ID_HEADER + "=" + VARIANT_1_ID
                             + "&dynamicSimulationResultUuid=" + DYNAMIC_SIMULATION_RESULT_UUID
                             + "&parametersUuid=" + UUID.randomUUID(),
                                NETWORK_UUID.toString())
                                .contentType(APPLICATION_JSON)
                                .header(HEADER_USER_ID, "testUserId"))
                .andExpect(status().isNotFound());

    }

    @Test
    void testRunWithReport() throws Exception {

        doAnswer(invocation -> null).when(reportService).deleteReport(any());
        doAnswer(invocation -> null).when(reportService).sendReport(any(), any());

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            if (args[0] instanceof DynamicSecurityAnalysisRunContext runContext && runContext.getReportInfos().reportUuid() != null) {
                ReportNode dsaReportNode = runContext.getReportNode().newReportNode().withMessageTemplate("dsa", "").add();
                dsaReportNode.newReportNode().withMessageTemplate("saContingency", "Contingency '${contingencyId}'")
                        .withUntypedValue("contingencyId", "contingencyId01")
                        .add();
            }
            invocation.callRealMethod();
            return null;
        })
            .when(dynamicSecurityAnalysisWorkerService).postRun(any(), any(), any());

        doReturn(CompletableFuture.completedFuture(new SecurityAnalysisReport(
                new SecurityAnalysisResult(
                    new PreContingencyResult(),
                    List.of(new PostContingencyResult(
                            new Contingency("contingencyId01", List.of()),
                            PostContingencyComputationStatus.CONVERGED,
                            new LimitViolationsResult(List.of(
                                new LimitViolation("subjectId01", LimitViolationType.HIGH_SHORT_CIRCUIT_CURRENT, 25.63, 4f, 33.54)
                            )))),
                    List.of()))))
        .when(dynamicSecurityAnalysisWorkerService).getCompletableFuture(any(), any(), any());

        MvcResult result = mockMvc.perform(
                        post("/v1/networks/{networkUuid}/run?"
                             + "&" + VARIANT_ID_HEADER + "=" + VARIANT_1_ID
                             + "&dynamicSimulationResultUuid=" + DYNAMIC_SIMULATION_RESULT_UUID
                             + "&parametersUuid=" + PARAMETERS_UUID
                             + "&reportUuid=" + UUID.randomUUID()
                             + "&reporterId=dsa",
                                NETWORK_UUID.toString())
                                .contentType(APPLICATION_JSON)
                                .header(HEADER_USER_ID, "testUserId"))
                .andExpect(status().isOk())
                .andReturn();

        UUID runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        Message<byte[]> messageSwitch = output.receive(1000, dsaResultDestination);
        assertThat(messageSwitch.getHeaders()).containsEntry(HEADER_RESULT_UUID, runUuid.toString());
    }

    // --- BEGIN Test cancelling a running computation ---//
    private void mockSendRunMessage(Supplier<CompletableFuture<?>> runAsyncMock) {
        // In test environment, the test binder calls consumers directly in the caller thread, i.e. the controller thread.
        // By consequence, a real asynchronous Producer/Consumer can not be simulated like prod
        // So mocking producer in a separated thread differing from the controller thread
        doAnswer(invocation -> CompletableFuture.runAsync(() -> {
            // static mock must be in the same thread of the consumer
            // see : https://stackoverflow.com/questions/76406935/mock-static-method-in-spring-boot-integration-test
            try (MockedStatic<DynamicSecurityAnalysis> dynamicSecurityAnalysisMockedStatic = mockStatic(DynamicSecurityAnalysis.class)) {
                DynamicSecurityAnalysis.Runner runner = mock(DynamicSecurityAnalysis.Runner.class);
                dynamicSecurityAnalysisMockedStatic.when(() -> DynamicSecurityAnalysis.find(any())).thenReturn(runner);

                // mock the computation
                doAnswer(invocation2 -> runAsyncMock.get())
                        .when(runner).runAsync(any(), any(), any(), any(), any());

                // call real method sendRunMessage
                try {
                    invocation.callRealMethod();
                } catch (Throwable e) {
                    throw new RuntimeException("Error while wrapping sendRunMessage in a separated thread", e);
                }
            }
        }))
        .when(notificationService).sendRunMessage(any());
    }

    private UUID runAndCancel(CountDownLatch cancelLatch, int cancelDelay) throws Exception {
        //run the dynamic simulation on a specific variant
        MvcResult result = mockMvc.perform(
                        post("/v1/networks/{networkUuid}/run?"
                             + "&" + VARIANT_ID_HEADER + "=" + VARIANT_1_ID
                             + "&dynamicSimulationResultUuid=" + DYNAMIC_SIMULATION_RESULT_UUID
                             + "&parametersUuid=" + PARAMETERS_UUID,
                                NETWORK_UUID.toString())
                                .contentType(APPLICATION_JSON)
                                .header(HEADER_USER_ID, "testUserId"))
                .andExpect(status().isOk())
                .andReturn();
        UUID runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        assertResultStatus(runUuid, DynamicSecurityAnalysisStatus.RUNNING);

        // stop dynamic simulation
        cancelLatch.await();
        // custom additional wait
        await().pollDelay(cancelDelay, TimeUnit.MILLISECONDS).until(() -> true);

        mockMvc.perform(put("/" + VERSION + "/results/{resultUuid}/stop", runUuid))
                .andExpect(status().isOk());

        return runUuid;
    }

    @Test
    void testStopOnTime() throws Exception {
        CountDownLatch cancelLatch = new CountDownLatch(1);
        // Emit messages in separate threads, like in production.
        mockSendRunMessage(() -> {
            // using latch to trigger stop dynamic simulation at the beginning of computation
            cancelLatch.countDown();

            // fake a long computation 1s
            return CompletableFuture.supplyAsync(() ->
                            new SecurityAnalysisReport(SecurityAnalysisResult.empty()),
                    CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS)
            );
        });

        // test run then cancel
        UUID runUuid = runAndCancel(cancelLatch, 0);

        // check result
        // Must have a cancel message in the stop queue
        Message<byte[]> message = output.receive(1000, dsaStoppedDestination);
        assertThat(message.getHeaders())
                .containsEntry(HEADER_RESULT_UUID, runUuid.toString())
                .containsEntry(HEADER_MESSAGE, getCancelMessage(COMPUTATION_TYPE));
        // result has been deleted by cancel so not found
        assertResultStatus(runUuid, null);

    }

    @Test
    void testStopEarly() throws Exception {
        CountDownLatch cancelLatch = new CountDownLatch(1);
        // Emit messages in separate threads, like in production.
        mockSendRunMessage(() -> CompletableFuture.supplyAsync(() -> new SecurityAnalysisReport(SecurityAnalysisResult.empty())));

        doAnswer(invocation -> {
            Object object = invocation.callRealMethod();

            // using latch to trigger stop dynamic simulation at the beginning of computation
            cancelLatch.countDown();

            // fake a long process 1s before run computation
            await().pollDelay(1000, TimeUnit.MILLISECONDS).until(() -> true);

            return object;
        })
        .when(dynamicSecurityAnalysisWorkerService).preRun(any());

        // test run then cancel
        UUID runUuid = runAndCancel(cancelLatch, 0);

        // check result
        // Must have a cancel failed message in the queue
        Message<byte[]> message = output.receive(1000, dsaCancelFailedDestination);
        assertThat(message.getHeaders())
                .containsEntry(HEADER_RESULT_UUID, runUuid.toString())
                .containsEntry(HEADER_MESSAGE, getCancelFailedMessage(COMPUTATION_TYPE));
        // cancel failed so result still exist but status is still RUNNING
        // TODO need to revisit the implementation in ws-commons, status must be NOT_DONE
        assertResultStatus(runUuid, DynamicSecurityAnalysisStatus.RUNNING);
    }

    @Test
    void testStopLately() throws Exception {
        CountDownLatch cancelLatch = new CountDownLatch(1);
        // Emit messages in separate threads, like in production.
        mockSendRunMessage(() -> {
            // using latch to trigger stop dynamic simulation at the beginning of computation
            cancelLatch.countDown();

            // fake a short computation
            return CompletableFuture.supplyAsync(() -> new SecurityAnalysisReport(SecurityAnalysisResult.empty())
            );
        });

        // test run then cancel
        UUID runUuid = runAndCancel(cancelLatch, 1000);

        // check result
        // Must have a result message in the result queue since the computation finished so quickly in the mock
        Message<byte[]> message = output.receive(1000, dsaResultDestination);
        assertThat(message.getHeaders())
                .containsEntry(HEADER_RESULT_UUID, runUuid.toString());

        // Must have a cancel failed message in the queue
        message = output.receive(1000, dsaCancelFailedDestination);
        assertThat(message.getHeaders())
                .containsEntry(HEADER_RESULT_UUID, runUuid.toString())
                .containsEntry(HEADER_MESSAGE, getCancelFailedMessage(COMPUTATION_TYPE));
        // cancel failed so results are not deleted
        assertResultStatus(runUuid, DynamicSecurityAnalysisStatus.SUCCEED);
    }
    // --- END Test cancelling a running computation ---//

}

/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.datasource.ResourceDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.security.SecurityAnalysisReport;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.dynamic.DynamicSecurityAnalysis;
import com.powsybl.ws.commons.computation.service.NotificationService;
import lombok.SneakyThrows;
import org.gridsuite.dynamicsecurityanalysis.server.dto.DynamicSecurityAnalysisStatus;
import org.gridsuite.dynamicsecurityanalysis.server.dto.contingency.ContingencyInfos;
import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersInfos;
import org.gridsuite.dynamicsecurityanalysis.server.entities.parameters.DynamicSecurityAnalysisParametersEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.messaging.Message;
import org.springframework.test.web.servlet.MockMvc;
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
    public static final String OUTPUT = "output";
    public static final String OUTPUT_STATE_DUMP_FILE = "outputState.dmp";
    public static final String DYNAMIC_MODEL_DUMP_FILE = "dynamicModel.dmp";
    public static final String DYNAMIC_SIMULATION_PARAMETERS_DUMP_FILE = "dynamicSimulationParameters.dmp";

    private static final UUID NETWORK_UUID = UUID.randomUUID();
    private static final UUID NETWORK_UUID_NOT_FOUND = UUID.randomUUID();
    private static final String VARIANT_1_ID = "variant_1";
    private static final String NETWORK_FILE = "IEEE14.iidm";

    private static final UUID DYNAMIC_SIMULATION_RESULT_UUID = UUID.randomUUID();
    private static final UUID PARAMETERS_UUID = UUID.randomUUID();
    private static final UUID CONTINGENCY_UUID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private OutputDestination output;

    @Autowired
    private InputDestination input;

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

    @BeforeEach
    @Override
    public void setUp() throws IOException {
        super.setUp();
    }

    @SneakyThrows
    @Override
    public void tearDown() {
        super.tearDown();

        // delete all results
        mockMvc.perform(
                        delete("/v1/results"))
                .andExpect(status().isOk());
    }

//    @Test
//    public void testGivenTimeSeriesAndTimeLine() throws Exception {
//
//        // mock DynamicSimulationWorkerService with time-series and timeline
//        Map<String, DoubleTimeSeries> curves = new HashMap<>();
//        TimeSeriesIndex index = new IrregularTimeSeriesIndex(new long[]{32, 64, 128, 256});
//        curves.put("NETWORK__BUS____2-BUS____5-1_AC_iSide2", TimeSeries.createDouble("NETWORK__BUS____2-BUS____5-1_AC_iSide2", index, 333.847331, 333.847321, 333.847300, 333.847259));
//        curves.put("NETWORK__BUS____1_TN_Upu_value", TimeSeries.createDouble("NETWORK__BUS____1_TN_Upu_value", index, 1.059970, 1.059970, 1.059970, 1.059970));
//
//        List<TimelineEvent> timeLine = List.of(
//                new TimelineEvent(102479, "CLA_2_5 - CLA", "order to change topology"),
//                new TimelineEvent(102479, "_BUS____2-BUS____5-1_AC - LINE", "opening both sides"),
//                new TimelineEvent(102479, "CLA_2_5 - CLA", "order to change topology"),
//                new TimelineEvent(104396, "CLA_2_4 - CLA", "arming by over-current constraint")
//        );
//
//        Map<String, Double> finalStateValues = new HashMap<>();
//
//        doReturn(CompletableFuture.completedFuture(new DynamicSimulationResultImpl(DynamicSimulationResult.Status.SUCCESS, "", curves, finalStateValues, timeLine)))
//                .when(dynamicSimulationWorkerService).getCompletableFuture(any(), any(), any());
//        doReturn(CompletableFuture.completedFuture(new DynamicSimulationResultImpl(DynamicSimulationResult.Status.SUCCESS, "", curves, finalStateValues, timeLine)))
//                .when(dynamicSimulationWorkerService).getCompletableFuture(any(), any(), isNull());
//
//        // prepare parameters
//        DynamicSimulationParametersInfos parameters = ParameterUtils.getDefaultDynamicSimulationParameters();
//
//        //run the dynamic simulation on a specific variant
//        MvcResult result = mockMvc.perform(
//                post("/v1/networks/{networkUuid}/run?variantId=" +
//                     VARIANT_1_ID + "&mappingName=" + MAPPING_NAME, NETWORK_UUID_STRING)
//                    .contentType(APPLICATION_JSON)
//                    .header(HEADER_USER_ID, "testUserId")
//                    .content(objectMapper.writeValueAsString(parameters)))
//            .andExpect(status().isOk())
//            .andReturn();
//        UUID runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);
//
//        Message<byte[]> messageSwitch = output.receive(1000 * 10, dsResultDestination);
//        assertThat(messageSwitch.getHeaders()).containsEntry(HEADER_RESULT_UUID, runUuid.toString());
//
//        //run the dynamic simulation on the implicit default variant
//        result = mockMvc.perform(
//                post("/v1/networks/{networkUuid}/run?" + "&mappingName=" + MAPPING_NAME, NETWORK_UUID_STRING)
//                    .contentType(APPLICATION_JSON)
//                    .header(HEADER_USER_ID, "testUserId")
//                    .content(objectMapper.writeValueAsString(parameters)))
//            .andExpect(status().isOk())
//            .andReturn();
//
//        runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);
//
//        messageSwitch = output.receive(1000 * 10, dsResultDestination);
//        assertThat(messageSwitch.getHeaders()).containsEntry(HEADER_RESULT_UUID, runUuid.toString());
//
//        //get the calculation status
//        result = mockMvc.perform(
//                get("/v1/results/{resultUuid}/status", runUuid))
//            .andExpect(status().isOk())
//            .andReturn();
//
//        DynamicSimulationStatus status = objectMapper.readValue(result.getResponse().getContentAsString(), DynamicSimulationStatus.class);
//
//        //depending on the execution speed it can be both
//        assertThat(status).isIn(DynamicSimulationStatus.CONVERGED, DynamicSimulationStatus.RUNNING);
//
//        //get the status of a non-existing simulation and expect a not found
//        mockMvc.perform(
//                get("/v1/results/{resultUuid}/status", UUID.randomUUID()))
//            .andExpect(status().isNotFound());
//
//        //get the time-series uuid of a non-existing simulation and expect a not found
//        mockMvc.perform(
//                get("/v1/results/{resultUuid}/timeseries", UUID.randomUUID()))
//            .andExpect(status().isNotFound());
//
//        //get the timeline uuid of a non-existing simulation and expect a not found
//        mockMvc.perform(
//                        get("/v1/results/{resultUuid}/timeline", UUID.randomUUID()))
//                .andExpect(status().isNotFound());
//
//        //get the result time-series uuid of the calculation
//        result = mockMvc.perform(
//                get("/v1/results/{resultUuid}/timeseries", runUuid))
//            .andExpect(status().isOk())
//            .andReturn();
//
//        // the return content must be a UUID class
//        assertType(result.getResponse().getContentAsString(), UUID.class, objectMapper);
//
//        //get the result timeline uuid of the calculation
//        result = mockMvc.perform(
//                get("/v1/results/{resultUuid}/timeline", runUuid))
//            .andExpect(status().isOk())
//            .andReturn();
//
//        // the return content must be a UUID class
//        assertType(result.getResponse().getContentAsString(), UUID.class, objectMapper);
//
//        // get the ending status of the calculation which must be is converged
//        result = mockMvc.perform(
//                get("/v1/results/{resultUuid}/status", runUuid))
//            .andExpect(status().isOk())
//            .andReturn();
//
//        status = objectMapper.readValue(result.getResponse().getContentAsString(), DynamicSimulationStatus.class);
//
//        assertThat(status).isSameAs(DynamicSimulationStatus.CONVERGED);
//
//        // test invalidate status => i.e. set NOT_DONE
//        // set NOT_DONE
//        mockMvc.perform(
//                put("/v1/results/invalidate-status?resultUuid=" + runUuid))
//            .andExpect(status().isOk());
//
//        // check whether NOT_DONE is persisted
//        result = mockMvc.perform(
//                get("/v1/results/{resultUuid}/status", runUuid))
//            .andExpect(status().isOk())
//            .andReturn();
//        DynamicSimulationStatus statusAfterInvalidate = objectMapper.readValue(result.getResponse().getContentAsString(), DynamicSimulationStatus.class);
//
//        assertThat(statusAfterInvalidate).isSameAs(DynamicSimulationStatus.NOT_DONE);
//
//        // set NOT_DONE for none existing result
//        mockMvc.perform(
//                        put("/v1/results/invalidate-status?resultUuid=" + UUID.randomUUID()))
//                .andExpect(status().isNotFound());
//
//        //delete a result
//        mockMvc.perform(
//                delete("/v1/results/{resultUuid}", runUuid))
//            .andExpect(status().isOk());
//
//        //try to get the removed result and except a not found
//        mockMvc.perform(
//                get("/v1/results/{resultUuid}/timeseries", runUuid))
//            .andExpect(status().isNotFound());
//
//        //delete a none existing result
//        mockMvc.perform(
//                        delete("/v1/results/{resultUuid}", UUID.randomUUID()))
//                .andExpect(status().isOk());
//
//        //delete all results and except ok
//        mockMvc.perform(
//                delete("/v1/results"))
//            .andExpect(status().isOk());
//    }

//    @Test
//    public void testGivenEmptyTimeSeriesAndTimeLine() throws Exception {
//        // mock DynamicSimulationWorkerService without time-series and timeline
//        Map<String, DoubleTimeSeries> curves = new HashMap<>();
//        List<TimelineEvent> timeLine = List.of();
//        Map<String, Double> finalStateValues = new HashMap<>();
//
//        doReturn(CompletableFuture.completedFuture(new DynamicSimulationResultImpl(DynamicSimulationResult.Status.SUCCESS, "", curves, finalStateValues, timeLine)))
//                .when(dynamicSimulationWorkerService).getCompletableFuture(any(), any(), any());
//
//        // prepare parameters
//        DynamicSimulationParametersInfos parameters = ParameterUtils.getDefaultDynamicSimulationParameters();
//
//        //run the dynamic simulation on a specific variant
//        MvcResult result = mockMvc.perform(
//                        post("/v1/networks/{networkUuid}/run?variantId=" +
//                             VARIANT_1_ID + "&mappingName=" + MAPPING_NAME, NETWORK_UUID_STRING)
//                                .contentType(APPLICATION_JSON)
//                                .header(HEADER_USER_ID, "testUserId")
//                                .content(objectMapper.writeValueAsString(parameters)))
//                .andExpect(status().isOk())
//                .andReturn();
//        UUID runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);
//
//        Message<byte[]> messageSwitch = output.receive(1000, dsResultDestination);
//        assertThat(messageSwitch.getHeaders()).containsEntry(HEADER_RESULT_UUID, runUuid.toString());
//
//        // get the ending status of the calculation which must be is converged
//        result = mockMvc.perform(
//                        get("/v1/results/{resultUuid}/status", runUuid))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        DynamicSimulationStatus status = objectMapper.readValue(result.getResponse().getContentAsString(), DynamicSimulationStatus.class);
//
//        assertThat(status).isSameAs(DynamicSimulationStatus.CONVERGED);
//
//        //get time-series uuid of the calculation
//        mockMvc.perform(
//                        get("/v1/results/{resultUuid}/timeseries", runUuid))
//                .andExpect(status().isNoContent());
//
//        //get timeline uuid of the calculation
//        mockMvc.perform(
//                        get("/v1/results/{resultUuid}/timeline", runUuid))
//                .andExpect(status().isNoContent());
//
//    }

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

    private void assertResultStatus(UUID runUuid, DynamicSecurityAnalysisStatus expectedStatus) throws Exception {

        MvcResult result = mockMvc.perform(
                        get("/v1/results/{resultUuid}/status", runUuid))
                .andExpect(status().isOk()).andReturn();

        DynamicSecurityAnalysisStatus status = null;
        if (!result.getResponse().getContentAsString().isEmpty()) {
            status = objectMapper.readValue(result.getResponse().getContentAsString(), DynamicSecurityAnalysisStatus.class);
        }

        assertThat(status).isSameAs(expectedStatus);
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
        // cancel failed so result still exist
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

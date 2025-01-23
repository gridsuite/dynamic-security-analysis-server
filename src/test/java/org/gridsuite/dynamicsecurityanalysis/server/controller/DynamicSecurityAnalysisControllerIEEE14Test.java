/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.datasource.ResourceDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.PreloadingStrategy;
import org.gridsuite.dynamicsecurityanalysis.server.dto.contingency.ContingencyInfos;
import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersInfos;
import org.gridsuite.dynamicsecurityanalysis.server.entities.parameters.DynamicSecurityAnalysisParametersEntity;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

import static com.powsybl.ws.commons.computation.service.AbstractResultContext.VARIANT_ID_HEADER;
import static com.powsybl.ws.commons.computation.service.NotificationService.HEADER_RESULT_UUID;
import static com.powsybl.ws.commons.computation.service.NotificationService.HEADER_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gridsuite.dynamicsecurityanalysis.server.utils.Utils.RESOURCE_PATH_DELIMITER;
import static org.gridsuite.dynamicsecurityanalysis.server.utils.Utils.zip;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicSecurityAnalysisControllerIEEE14Test extends AbstractDynamicSecurityAnalysisControllerTest {
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
    private OutputDestination output;

    @Autowired
    private InputDestination input;

    @Autowired
    private ObjectMapper objectMapper;

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
        given(networkStoreClient.getNetwork(NETWORK_UUID_NOT_FOUND, PreloadingStrategy.COLLECTION)).willThrow(new PowsyblException());
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

    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        // delete all results
        mockMvc.perform(
                delete("/v1/results"))
            .andExpect(status().isOk());
    }

    @Test
    public void test01GivenCurvesAndEvents() throws Exception {
        String testBaseDir = TEST_CASE_01;

        //run the dynamic security analysis (on a specific variant with variantId=" + VARIANT_1_ID + ")
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

        //TODO maybe find a more reliable way to test this : failed with 1000 * 30 timeout
        Message<byte[]> messageSwitch = output.receive(1000 * 40, dsaResultDestination);
        assertThat(messageSwitch.getHeaders()).containsEntry(HEADER_RESULT_UUID, runUuid.toString());

//        // --- CHECK result at abstract level --- //
//        // expected seriesNames
//        List<String> expectedSeriesNames = curveInfosList.stream().map(curveInfos -> curveInfos.getEquipmentId() + "_" + curveInfos.getVariableId()).toList();
//
//        // get timeseries from mock timeseries db
//        UUID timeSeriesUuid = TimeSeriesClientTest.TIME_SERIES_UUID;
//        List<TimeSeries<?, ?>> resultTimeSeries = timeSeriesMockBd.get(timeSeriesUuid);
//        // result seriesNames
//        List<String> seriesNames = resultTimeSeries.stream().map(TimeSeries::getMetadata).map(TimeSeriesMetadata::getName).toList();
//
//        // compare result only series' names
//        expectedSeriesNames.forEach(expectedSeriesName -> {
//            logger.info(String.format("Check time series %s exists or not : %b", expectedSeriesName, seriesNames.contains(expectedSeriesName)));
//            assertThat(seriesNames).contains(expectedSeriesName);
//        });
//
//        // --- CHECK result at detail level --- //
//        // prepare expected result to compare
//        String outputDir = DATA_IEEE14_BASE_DIR +
//                           RESOURCE_PATH_DELIMITER + testBaseDir +
//                           RESOURCE_PATH_DELIMITER + OUTPUT;
//        DynamicSimulationResult expectedResult = DynamicSimulationResultDeserializer.read(getClass().getResourceAsStream(outputDir + RESOURCE_PATH_DELIMITER + RESULT_SIM_JSON));
//        String jsonExpectedTimeSeries = TimeSeries.toJson(new ArrayList<>(expectedResult.getCurves().values()));
//
//        // convert result time series to json
//        String jsonResultTimeSeries = TimeSeries.toJson(resultTimeSeries);
//
//        // export result to file
//        FileUtils.writeBytesToFile(this, outputDir + RESOURCE_PATH_DELIMITER + "exported_" + RESULT_SIM_JSON, jsonResultTimeSeries.getBytes());
//
//        // compare result only timeseries
//        assertThat(objectMapper.readTree(jsonResultTimeSeries)).isEqualTo(objectMapper.readTree(jsonExpectedTimeSeries));
//
//        // check dump file not empty
//        result = mockMvc.perform(
//                        get("/v1/results/{resultUuid}/output-state", runUuid))
//                .andExpect(status().isOk())
//                .andReturn();
//        byte[] zippedOutputState = result.getResponse().getContentAsByteArray();
//
//        assertThat(zippedOutputState)
//                .withFailMessage("Expecting Output state of dynamic simulation to be not empty but was empty.")
//                .isNotEmpty();
//        logger.info("Size of zipped output state = {} KB ", zippedOutputState.length / 1024);
//
//        // export dump file content to manual check
//        File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource(".")).getFile() +
//                             outputDir + RESOURCE_PATH_DELIMITER + "outputState.dmp");
//        Utils.unzip(zippedOutputState, file.toPath());
//
//        // check dynamic model persisted in result in gzip format not empty
//        result = mockMvc.perform(
//                        get("/v1/results/{resultUuid}/dynamic-model", runUuid))
//                .andExpect(status().isOk())
//                .andReturn();
//        byte[] zippedDynamicModel = result.getResponse().getContentAsByteArray();
//
//        assertThat(zippedDynamicModel)
//                .withFailMessage("Expecting dynamic model of dynamic simulation to be not empty but was empty.")
//                .isNotEmpty();
//        logger.info("Size of zipped dynamic model = {} B ", zippedDynamicModel.length);
//
//        // export dynamic model in json and dump files to manual check
//        List<DynamicModelConfig> dynamicModel = Utils.unzip(zippedDynamicModel, objectMapper, new TypeReference<>() { });
//        String jsonDynamicModel = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dynamicModel);
//        FileUtils.writeBytesToFile(this, outputDir + RESOURCE_PATH_DELIMITER + "dynamicModel.json", jsonDynamicModel.getBytes());
//
//        file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource(".")).getFile() +
//                 outputDir + RESOURCE_PATH_DELIMITER + "dynamicModel.dmp");
//        Utils.unzip(zippedDynamicModel, file.toPath());
//
//        // check parameters persisted in result in gzip format not empty
//        result = mockMvc.perform(
//                        get("/v1/results/{resultUuid}/parameters", runUuid))
//                .andExpect(status().isOk())
//                .andReturn();
//        byte[] zippedDynamicSimulationParameters = result.getResponse().getContentAsByteArray();
//
//        assertThat(zippedDynamicSimulationParameters)
//                .withFailMessage("Expecting parameters of dynamic simulation to be not empty but was empty.")
//                .isNotEmpty();
//        logger.info("Size of zipped parameters = {} KB ", zippedDynamicSimulationParameters.length / 1024);
//
//        // export dynamic model in json and dump files to manual check
//        DynamicSimulationParameters dynamicSimulationParameters = Utils.unzip(zippedDynamicSimulationParameters, objectMapper, DynamicSimulationParameters.class);
//        String jsonDynamicSimulationParameters = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dynamicSimulationParameters);
//        FileUtils.writeBytesToFile(this, outputDir + RESOURCE_PATH_DELIMITER + "dynamicSimulationParameters.json", jsonDynamicSimulationParameters.getBytes());
//
//        file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource(".")).getFile() +
//                        outputDir + RESOURCE_PATH_DELIMITER + "dynamicSimulationParameters.dmp");
//        Utils.unzip(zippedDynamicSimulationParameters, file.toPath());

    }
}

/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.controller;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.datasource.ResourceDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.PreloadingStrategy;
import org.gridsuite.dynamicsecurityanalysis.server.dto.DynamicSecurityAnalysisStatus;
import org.gridsuite.dynamicsecurityanalysis.server.dto.contingency.ContingencyInfos;
import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersInfos;
import org.gridsuite.dynamicsecurityanalysis.server.entities.parameters.DynamicSecurityAnalysisParametersEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.messaging.Message;
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
    public static final String OUTPUT_STATE_DUMP_GZIP_FILE = "outputState.dmp.gz";
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
    private OutputDestination output;

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

            // load outputState.dmp.gz
            String outputStateFilePath = inputDir + RESOURCE_PATH_DELIMITER + OUTPUT_STATE_DUMP_GZIP_FILE;
            InputStream outputStateIS = getClass().getResourceAsStream(outputStateFilePath);
            assert outputStateIS != null;
            byte[] zippedOutputState = outputStateIS.readAllBytes();

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
    void test01IEEE14() throws Exception {

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

        Message<byte[]> messageSwitch = output.receive(1000, dsaResultDestination);
        assertThat(messageSwitch.getHeaders()).containsEntry(HEADER_RESULT_UUID, runUuid.toString());

        // --- CHECK result --- //
        assertResultStatus(runUuid, DynamicSecurityAnalysisStatus.SUCCEED);

    }
}

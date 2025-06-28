/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.ws.commons.computation.service.ReportService;
import lombok.SneakyThrows;
import org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisApplication;
import org.gridsuite.dynamicsecurityanalysis.server.controller.utils.TestUtils;
import org.gridsuite.dynamicsecurityanalysis.server.dto.DynamicSecurityAnalysisStatus;
import org.gridsuite.dynamicsecurityanalysis.server.repositories.DynamicSecurityAnalysisParametersRepository;
import org.gridsuite.dynamicsecurityanalysis.server.service.DynamicSecurityAnalysisWorkerService;
import org.gridsuite.dynamicsecurityanalysis.server.service.ParametersService;
import org.gridsuite.dynamicsecurityanalysis.server.service.client.ActionsClient;
import org.gridsuite.dynamicsecurityanalysis.server.service.client.DynamicSimulationClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest
@ContextConfiguration(classes = {DynamicSecurityAnalysisApplication.class, TestChannelBinderConfiguration.class})
public abstract class AbstractDynamicSecurityAnalysisControllerTest extends AbstractDynawoTest {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final String dsaDebugDestination = "dsa.debug.destination";
    protected final String dsaResultDestination = "dsa.result.destination";
    protected final String dsaStoppedDestination = "dsa.stopped.destination";
    protected final String dsaCancelFailedDestination = "dsa.cancelfailed.destination";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    protected ReportService reportService;

    @MockBean
    protected DynamicSimulationClient dynamicSimulationClient;

    @MockBean
    protected ActionsClient actionsClient;

    @MockBean
    protected NetworkStoreService networkStoreClient;

    @MockBean
    protected DynamicSecurityAnalysisParametersRepository dynamicSecurityAnalysisParametersRepository;

    @SpyBean
    protected ParametersService parametersService;

    @SpyBean
    protected DynamicSecurityAnalysisWorkerService dynamicSecurityAnalysisWorkerService;

    @BeforeEach
    @Override
    public void setUp() throws IOException {
        super.setUp();

        // NetworkStoreService mock
        initNetworkStoreServiceMock();

        // DynamicSimulationClient mock
        initDynamicSimulationClientMock();

        // ActionsClient mock
        initActionsClientMock();

        // DynamicSecurityAnalysisParametersRepository mock
        initDynamicSecurityAnalysisParametersRepositoryMock();

        // ParametersService spy
        initParametersServiceSpy();

        // DynamicSecurityAnalysisWorkerService spy
        initDynamicSecurityAnalysisWorkerServiceSpy();
    }

    @SneakyThrows
    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();

        // delete all results
        mockMvc.perform(
                        delete("/v1/results"))
                .andExpect(status().isOk());

        // check messages in rabbitmq
        OutputDestination output = getOutputDestination();
        List<String> destinations = List.of(dsaDebugDestination, dsaResultDestination, dsaStoppedDestination, dsaCancelFailedDestination);

        TestUtils.assertQueuesEmptyThenClear(destinations, output);
    }

    protected abstract OutputDestination getOutputDestination();

    protected abstract void initNetworkStoreServiceMock();

    protected abstract void initDynamicSimulationClientMock();

    protected abstract void initActionsClientMock();

    protected abstract void initDynamicSecurityAnalysisParametersRepositoryMock();

    protected abstract void initParametersServiceSpy();

    private void initDynamicSecurityAnalysisWorkerServiceSpy() {
        // setup spy bean
        when(dynamicSecurityAnalysisWorkerService.getComputationManager()).thenReturn(computationManager);
    }

    // --- utility methods --- //
    protected void assertResultStatus(UUID runUuid, DynamicSecurityAnalysisStatus expectedStatus) throws Exception {

        MvcResult result = mockMvc.perform(
                        get("/v1/results/{resultUuid}/status", runUuid))
                .andExpect(status().isOk()).andReturn();

        DynamicSecurityAnalysisStatus status = null;
        if (!result.getResponse().getContentAsString().isEmpty()) {
            status = objectMapper.readValue(result.getResponse().getContentAsString(), DynamicSecurityAnalysisStatus.class);
        }

        assertThat(status).isSameAs(expectedStatus);
    }
}

/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.controller;

import com.powsybl.network.store.client.NetworkStoreService;
import org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisApplication;
import org.gridsuite.dynamicsecurityanalysis.server.controller.utils.TestUtils;
import org.gridsuite.dynamicsecurityanalysis.server.repositories.DynamicSecurityAnalysisParametersRepository;
import org.gridsuite.dynamicsecurityanalysis.server.service.DynamicSecurityAnalysisWorkerService;
import org.gridsuite.dynamicsecurityanalysis.server.service.ParametersService;
import org.gridsuite.dynamicsecurityanalysis.server.service.client.ActionsClient;
import org.gridsuite.dynamicsecurityanalysis.server.service.client.DynamicSimulationClient;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
@ContextConfiguration(classes = {DynamicSecurityAnalysisApplication.class, TestChannelBinderConfiguration.class})
public abstract class AbstractDynamicSecurityAnalysisControllerTest extends AbstractDynawoTest {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final String dsaResultDestination = "dsa.result.destination";
    protected final String dsaStoppedDestination = "dsa.stopped.destination";
    protected final String dsaCancelFailedDestination = "dsa.cancelfailed.destination";

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

    @Before
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

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        OutputDestination output = getOutputDestination();
        List<String> destinations = List.of(dsaResultDestination, dsaStoppedDestination, dsaCancelFailedDestination);

        try {
            TestUtils.assertQueuesEmptyThenClear(destinations, output);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error while checking message queues empty", e);
        }
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

}

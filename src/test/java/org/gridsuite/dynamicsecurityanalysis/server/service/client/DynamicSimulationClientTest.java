/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.gridsuite.dynamicsecurityanalysis.server.service.client.ActionsClient.API_VERSION;
import static org.gridsuite.dynamicsecurityanalysis.server.service.client.DynamicSimulationClient.DYNAMIC_MODEL;
import static org.gridsuite.dynamicsecurityanalysis.server.service.client.DynamicSimulationClient.DYNAMIC_SIMULATION_END_POINT_RESULT;
import static org.gridsuite.dynamicsecurityanalysis.server.service.client.DynamicSimulationClient.OUTPUT_STATE;
import static org.gridsuite.dynamicsecurityanalysis.server.service.client.DynamicSimulationClient.PARAMETERS;
import static org.gridsuite.dynamicsecurityanalysis.server.service.client.utils.UrlUtils.URL_DELIMITER;
import static org.gridsuite.dynamicsecurityanalysis.server.service.client.utils.UrlUtils.buildEndPointUrl;
import static org.gridsuite.dynamicsecurityanalysis.server.utils.assertions.Assertions.assertThat;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicSimulationClientTest extends AbstractWireMockRestClientTest {

    public static final UUID DYNAMIC_SIMULATION_RESULT_UUID = UUID.randomUUID();

    private DynamicSimulationClient dynamicSimulationClient;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String getEndpointUrl() {
        return buildEndPointUrl("", API_VERSION,
                DYNAMIC_SIMULATION_END_POINT_RESULT);
    }

    @BeforeEach
    public void setup() {
        dynamicSimulationClient = new DynamicSimulationClient(
                // use new WireMockServer(DYNAMIC_SIMULATION_PORT) to test with local server if needed
                initMockWebServer(new WireMockServer(wireMockConfig().dynamicPort())),
                restTemplate,
                objectMapper);
    }

    private void setupWireMockServerResponse(String resultElementEndpoint, byte[] response) {
        String baseUrl = getEndpointUrl() + URL_DELIMITER + DYNAMIC_SIMULATION_RESULT_UUID + URL_DELIMITER + resultElementEndpoint;
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathTemplate(baseUrl))
                .willReturn(WireMock.ok()
                        .withBody(response)
                        .withHeader("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE)
                )
        );
    }

    private void setupWireMockServerResponseNotFound(String resultElementEndpoint) {
        String baseUrl = getEndpointUrl() + URL_DELIMITER + DYNAMIC_SIMULATION_RESULT_UUID + URL_DELIMITER + resultElementEndpoint;
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathTemplate(baseUrl))
                .willReturn(WireMock.notFound())
        );
    }

    private void setupWireMockServerResponseGivenException(String resultElementEndpoint) {
        String baseUrl = getEndpointUrl() + URL_DELIMITER + DYNAMIC_SIMULATION_RESULT_UUID + URL_DELIMITER + resultElementEndpoint;
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathTemplate(baseUrl))
                .willReturn(WireMock.serverError()
                        .withBody(ERROR_MESSAGE))
        );
    }

    @Test
    void testGetOutputState() {
        // --- test normal case --- //
        setupWireMockServerResponse(OUTPUT_STATE, OUTPUT_STATE.getBytes());

        byte[] outputState = dynamicSimulationClient.getOutputState(DYNAMIC_SIMULATION_RESULT_UUID);

        assertThat(new String(outputState)).isEqualTo(OUTPUT_STATE);

        // --- test not found --- //
        setupWireMockServerResponseNotFound(OUTPUT_STATE);

        HttpClientErrorException httpClientErrorException = catchThrowableOfType(HttpClientErrorException.class,
                () -> dynamicSimulationClient.getOutputState(DYNAMIC_SIMULATION_RESULT_UUID));

        assertThat(httpClientErrorException.getMessage())
                .contains(NOT_FOUND_ERROR_MESSAGE);

        // --- test error exception --- //
        setupWireMockServerResponseGivenException(OUTPUT_STATE);

        HttpServerErrorException httpServerErrorException = catchThrowableOfType(HttpServerErrorException.class,
                () -> dynamicSimulationClient.getOutputState(DYNAMIC_SIMULATION_RESULT_UUID));

        assertThat(httpServerErrorException.getMessage())
                .contains(ERROR_MESSAGE);
    }

    @Test
    void testGetDynamicModel() {
        // --- test normal case --- //
        setupWireMockServerResponse(DYNAMIC_MODEL, DYNAMIC_MODEL.getBytes());

        byte[] dynamicModel = dynamicSimulationClient.getDynamicModel(DYNAMIC_SIMULATION_RESULT_UUID);

        assertThat(new String(dynamicModel)).isEqualTo(DYNAMIC_MODEL);

        // --- test not found --- //
        setupWireMockServerResponseNotFound(DYNAMIC_MODEL);

        HttpClientErrorException httpClientErrorException = catchThrowableOfType(HttpClientErrorException.class,
                () -> dynamicSimulationClient.getDynamicModel(DYNAMIC_SIMULATION_RESULT_UUID));
        assertThat(httpClientErrorException.getMessage()).contains(NOT_FOUND_ERROR_MESSAGE);

        // --- test error exception --- //
        setupWireMockServerResponseGivenException(DYNAMIC_MODEL);

        HttpServerErrorException serverErrorException = catchThrowableOfType(HttpServerErrorException.class,
                () -> dynamicSimulationClient.getDynamicModel(DYNAMIC_SIMULATION_RESULT_UUID));
        assertThat(serverErrorException.getMessage()).contains(ERROR_MESSAGE);
    }

    @Test
    void testGetDynamicSimulationParameters() {
        // --- test normal case --- //
        setupWireMockServerResponse(PARAMETERS, PARAMETERS.getBytes());

        byte[] parameters = dynamicSimulationClient.getDynamicSimulationParameters(DYNAMIC_SIMULATION_RESULT_UUID);

        assertThat(new String(parameters)).isEqualTo(PARAMETERS);

        // --- test not found --- //
        setupWireMockServerResponseNotFound(PARAMETERS);
        HttpClientErrorException httpClientErrorException = catchThrowableOfType(HttpClientErrorException.class,
                () -> dynamicSimulationClient.getDynamicSimulationParameters(DYNAMIC_SIMULATION_RESULT_UUID));

        assertThat(httpClientErrorException.getMessage())
                .contains(NOT_FOUND_ERROR_MESSAGE);
        // --- test error exception --- //
        setupWireMockServerResponseGivenException(PARAMETERS);
        HttpServerErrorException httpServerErrorException = catchThrowableOfType(HttpServerErrorException.class,
                () -> dynamicSimulationClient.getDynamicSimulationParameters(DYNAMIC_SIMULATION_RESULT_UUID));

        assertThat(httpServerErrorException.getMessage())
                .contains(ERROR_MESSAGE);
    }
}

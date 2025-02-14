/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.powsybl.contingency.Contingency;
import org.assertj.core.api.Assertions;
import org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException;
import org.gridsuite.dynamicsecurityanalysis.server.dto.contingency.ContingencyInfos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.havingExactly;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException.Type.CONTINGENCIES_GET_ERROR;
import static org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException.Type.CONTINGENCIES_NOT_FOUND;
import static org.gridsuite.dynamicsecurityanalysis.server.service.client.ActionsClient.ACTIONS_END_POINT_CONTINGENCY;
import static org.gridsuite.dynamicsecurityanalysis.server.service.client.ActionsClient.API_VERSION;
import static org.gridsuite.dynamicsecurityanalysis.server.service.client.utils.UrlUtils.buildEndPointUrl;
import static org.gridsuite.dynamicsecurityanalysis.server.utils.assertions.Assertions.assertThat;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class ActionsClientTest extends AbstractWireMockRestClientTest {

    public static final UUID NETWORK_UUID = UUID.randomUUID();
    public static final String VARIANT_1_ID = "variant_1";
    private static final UUID CONTINGENCY_UUID = UUID.randomUUID();

    private ActionsClient actionsClient;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String getEndpointUrl() {
        return buildEndPointUrl("", API_VERSION,
                ACTIONS_END_POINT_CONTINGENCY);
    }

    @BeforeEach
    public void setup() {
        actionsClient = new ActionsClient(
            // use new WireMockServer(ACTIONS_PORT) to test with local server if needed
            initMockWebServer(new WireMockServer(wireMockConfig().dynamicPort())),
            restTemplate,
            objectMapper);
    }

    @Test
    void testGetContingencyList() throws Exception {
        String baseUrl = getEndpointUrl() + "/contingency-infos/export";

        List<ContingencyInfos> contingencyInfosList = List.of(new ContingencyInfos(Contingency.load("_LOAD__11_EC")));
        ObjectWriter ow = objectMapper.writer().withDefaultPrettyPrinter();
        String contingencyInfosListJson = ow.writeValueAsString(contingencyInfosList);

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathTemplate(baseUrl))
                .withQueryParam("networkUuid", equalTo(NETWORK_UUID.toString()))
                .withQueryParam("variantId", equalTo(VARIANT_1_ID))
                .withQueryParam("ids", havingExactly(CONTINGENCY_UUID.toString()))
                .willReturn(WireMock.ok()
                    .withBody(contingencyInfosListJson)
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                )
        );

        List<ContingencyInfos> resultContingencyInfosList = actionsClient.getContingencyList(List.of(CONTINGENCY_UUID), NETWORK_UUID, VARIANT_1_ID);

        assertThat(resultContingencyInfosList).hasSize(1);
        assertThat(resultContingencyInfosList.get(0)).recursivelyEquals(contingencyInfosList.get(0));

    }

    @Test
    void testGetContingencyListNotFound() {
        String baseUrl = getEndpointUrl() + "/contingency-infos/export";

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathTemplate(baseUrl))
                .withQueryParam("networkUuid", equalTo(NETWORK_UUID.toString()))
                .withQueryParam("variantId", equalTo(VARIANT_1_ID))
                .withQueryParam("ids", havingExactly(CONTINGENCY_UUID.toString()))
                .willReturn(WireMock.notFound())
        );

        List<UUID> contingencyList = List.of(CONTINGENCY_UUID);
        DynamicSecurityAnalysisException dynamicSecurityAnalysisException = catchThrowableOfType(
                () -> actionsClient.getContingencyList(contingencyList, NETWORK_UUID, VARIANT_1_ID),
                DynamicSecurityAnalysisException.class);

        Assertions.assertThat(dynamicSecurityAnalysisException.getType())
                .isEqualTo(CONTINGENCIES_NOT_FOUND);
    }

    @Test
    void testGetContingencyListGivenException() {
        String baseUrl = getEndpointUrl() + "/contingency-infos/export";

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathTemplate(baseUrl))
                .withQueryParam("networkUuid", equalTo(NETWORK_UUID.toString()))
                .withQueryParam("variantId", equalTo(VARIANT_1_ID))
                .withQueryParam("ids", havingExactly(CONTINGENCY_UUID.toString()))
                .willReturn(WireMock.serverError()
                        .withBody(ERROR_MESSAGE_JSON))
        );

        List<UUID> contingencyList = List.of(CONTINGENCY_UUID);
        DynamicSecurityAnalysisException dynamicSecurityAnalysisException = catchThrowableOfType(
                () -> actionsClient.getContingencyList(contingencyList, NETWORK_UUID, VARIANT_1_ID),
                DynamicSecurityAnalysisException.class);

        Assertions.assertThat(dynamicSecurityAnalysisException.getType())
                .isEqualTo(CONTINGENCIES_GET_ERROR);
        Assertions.assertThat(dynamicSecurityAnalysisException.getMessage())
                .isEqualTo(ERROR_MESSAGE);
    }

}

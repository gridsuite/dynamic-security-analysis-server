/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

import static org.gridsuite.dynamicsecurityanalysis.server.service.client.utils.UrlUtils.buildEndPointUrl;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class DynamicSimulationClient extends AbstractRestClient {

    public static final String API_VERSION = "v1";
    public static final String DYNAMIC_SIMULATION_REST_API_CALLED_SUCCESSFULLY_MESSAGE = "Dynamic simulation REST API called successfully {}";

    public static final String DYNAMIC_SIMULATION_END_POINT_RESULT = "results";
    public static final String OUTPUT_STATE = "output-state";
    public static final String DYNAMIC_MODEL = "dynamic-model";
    public static final String PARAMETERS = "parameters";

    @Autowired
    public DynamicSimulationClient(@Value("${gridsuite.services.dynamic-simulation-server.base-uri:http://dynamic-simulation-server/}") String baseUri, RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(baseUri, restTemplate, objectMapper);
    }

    private byte[] getDynamicSimulationResultElement(@NonNull UUID dynamicSimulationResultUuid, @NonNull String resultElementEndpoint) {
        String endPointUrl = buildEndPointUrl(getBaseUri(), API_VERSION, DYNAMIC_SIMULATION_END_POINT_RESULT);

        UriComponents uriComponents = UriComponentsBuilder.fromUriString(endPointUrl + "/{resultUuid}/{resultElementEndpoint}")
                .buildAndExpand(dynamicSimulationResultUuid, resultElementEndpoint);

        // call dynamic-simulation REST API
        String url = uriComponents.toUriString();
        byte[] resultElement = getRestTemplate().getForObject(url, byte[].class);
        logger.debug(DYNAMIC_SIMULATION_REST_API_CALLED_SUCCESSFULLY_MESSAGE, url);
        return resultElement;
    }

    public byte[] getOutputState(UUID dynamicSimulationResultUuid) {
        return getDynamicSimulationResultElement(dynamicSimulationResultUuid, OUTPUT_STATE);
    }

    public byte[] getDynamicModel(UUID dynamicSimulationResultUuid) {
        return getDynamicSimulationResultElement(dynamicSimulationResultUuid, DYNAMIC_MODEL);
    }

    public byte[] getDynamicSimulationParameters(UUID dynamicSimulationResultUuid) {
        return getDynamicSimulationResultElement(dynamicSimulationResultUuid, PARAMETERS);
    }
}

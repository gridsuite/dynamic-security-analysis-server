/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException.Type.DYNAMIC_SIMULATION_RESULT_GET_ERROR;
import static org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException.Type.DYNAMIC_SIMULATION_RESULT_UUID_NOT_FOUND;
import static org.gridsuite.dynamicsecurityanalysis.server.service.client.utils.ExceptionUtils.handleHttpError;
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

    private byte[] getDynamicSimulationResultElement(UUID dynamicSimulationResultUuid, String resultElementEndpoint) {
        Objects.requireNonNull(dynamicSimulationResultUuid);
        String endPointUrl = buildEndPointUrl(getBaseUri(), API_VERSION, DYNAMIC_SIMULATION_END_POINT_RESULT);

        UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(endPointUrl + "/{resultUuid}/{resultElementEndpoint}")
                .buildAndExpand(dynamicSimulationResultUuid, resultElementEndpoint);

        // call dynamic-simulation REST API
        try {
            String url = uriComponents.toUriString();
            byte[] resultElement = getRestTemplate().getForObject(url, byte[].class);
            if (logger.isDebugEnabled()) {
                logger.debug(DYNAMIC_SIMULATION_REST_API_CALLED_SUCCESSFULLY_MESSAGE, url);
            }
            return resultElement;
        } catch (HttpStatusCodeException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                throw new DynamicSecurityAnalysisException(DYNAMIC_SIMULATION_RESULT_UUID_NOT_FOUND, "Dynamic simulation result not found");
            } else {
                throw handleHttpError(e, DYNAMIC_SIMULATION_RESULT_GET_ERROR, getObjectMapper());
            }
        }
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

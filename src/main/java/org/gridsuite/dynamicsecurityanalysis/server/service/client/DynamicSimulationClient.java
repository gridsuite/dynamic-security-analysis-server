/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException.Type.CONTINGENCIES_NOT_FOUND;
import static org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException.Type.DYNAMIC_SIMULATION_RESULT_UUID_NOT_FOUND;
import static org.gridsuite.dynamicsecurityanalysis.server.service.client.utils.UrlUtils.buildEndPointUrl;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class DynamicSimulationClient extends AbstractRestClient {

    public static final String API_VERSION = "v1";
    public static final String DYNAMIC_SIMULATION_REST_API_CALLED_SUCCESSFULLY_MESSAGE = "Dynamic simulation REST API called successfully {}";

    public static final String DYNAMIC_SIMULATION_END_POINT_RESULT = "results";

    @Autowired
    public DynamicSimulationClient(@Value("${gridsuite.services.dynamic-simulation-server.base-uri:http://dynamic-simulation-server/}") String baseUri, RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(baseUri, restTemplate, objectMapper);
    }

    public byte[] getOutputState(UUID dynamicSimulationResultUuid) {
        Objects.requireNonNull(dynamicSimulationResultUuid);
        String endPointUrl = buildEndPointUrl(getBaseUri(), API_VERSION, DYNAMIC_SIMULATION_END_POINT_RESULT);

        UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(endPointUrl + "{resultUuid}/output-state")
                .buildAndExpand(dynamicSimulationResultUuid);

        // call dynamic-simulation REST API
        try {
            String url = uriComponents.toUriString();
            byte[] outputState = getRestTemplate().getForObject(url, byte[].class);
            if (logger.isDebugEnabled()) {
                logger.debug(DYNAMIC_SIMULATION_REST_API_CALLED_SUCCESSFULLY_MESSAGE, url);
            }
            return outputState;
        } catch (HttpStatusCodeException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                throw new DynamicSecurityAnalysisException(DYNAMIC_SIMULATION_RESULT_UUID_NOT_FOUND, "Dynamic simulation result not found");
            }
            throw e;
        }
    }

    public List<DynamicModelConfig> getDynamicModel(UUID dynamicSimulationResultUuid) {
        Objects.requireNonNull(dynamicSimulationResultUuid);
        String endPointUrl = buildEndPointUrl(getBaseUri(), API_VERSION, DYNAMIC_SIMULATION_END_POINT_RESULT);

        UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(endPointUrl + "{resultUuid}/dynamic-model-config")
                .buildAndExpand(dynamicSimulationResultUuid);

        try {
            String url = uriComponents.toUriString();
            ResponseEntity<List<DynamicModelConfig>> responseEntity = getRestTemplate()
                    .exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<DynamicModelConfig>>() { });
            if (logger.isDebugEnabled()) {
                logger.debug(DYNAMIC_SIMULATION_REST_API_CALLED_SUCCESSFULLY_MESSAGE, url);
            }
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                throw new DynamicSecurityAnalysisException(CONTINGENCIES_NOT_FOUND, "Contingencies not found");
            }
            throw e;
        }
    }
}

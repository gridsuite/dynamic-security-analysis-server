/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException;
import org.gridsuite.dynamicsecurityanalysis.server.dto.contingency.ContingencyInfos;
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
import java.util.Optional;
import java.util.UUID;

import static org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException.Type.CONTINGENCIES_NOT_FOUND;
import static org.gridsuite.dynamicsecurityanalysis.server.service.client.utils.UrlUtils.buildEndPointUrl;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@Service
public class ActionsClient extends AbstractRestClient {

    static final String API_VERSION = "v1";

    public static final String ACTIONS_END_POINT_CONTINGENCY = "contingency-lists";

    @Autowired
    public ActionsClient(
            @Value("${gridsuite.services.actions-server.base-uri:http://actions-server/}") String baseUri,
            RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(baseUri, restTemplate, objectMapper);
    }

    public List<ContingencyInfos> getContingencyList(List<String> ids, UUID networkUuid, String variantId) {
        Objects.requireNonNull(ids);
        Objects.requireNonNull(networkUuid);
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("List 'ids' must not be null or empty");
        }
        String endPointUrl = buildEndPointUrl(getBaseUri(), API_VERSION, ACTIONS_END_POINT_CONTINGENCY);
        UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(endPointUrl + "contingency-infos/export")
                .queryParam("networkUuid", networkUuid.toString())
                .queryParamIfPresent("variantId", Optional.ofNullable(variantId))
                .queryParam("ids", ids)
                .build();

        try {
            String url = uriComponents.toUriString();
            ResponseEntity<List<ContingencyInfos>> responseEntity = getRestTemplate()
                .exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<List<ContingencyInfos>>() { });
            if (logger.isDebugEnabled()) {
                logger.debug("Actions REST API called successfully {}", url);
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

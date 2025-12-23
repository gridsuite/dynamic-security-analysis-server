/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.dynamicsecurityanalysis.server.dto.contingency.ContingencyInfos;
import org.gridsuite.dynamicsecurityanalysis.server.error.DynamicSecurityAnalysisException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.gridsuite.dynamicsecurityanalysis.server.error.DynamicSecurityAnalysisBusinessErrorCode.CONTINGENCY_LIST_EMPTY;
import static org.gridsuite.dynamicsecurityanalysis.server.service.client.utils.UrlUtils.buildEndPointUrl;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
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

    public List<ContingencyInfos> getContingencyList(List<UUID> ids, @NonNull UUID networkUuid, String variantId) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new DynamicSecurityAnalysisException(CONTINGENCY_LIST_EMPTY, "Contingency list parameter must not be null or empty");
        }
        String endPointUrl = buildEndPointUrl(getBaseUri(), API_VERSION, ACTIONS_END_POINT_CONTINGENCY);
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(endPointUrl + "/contingency-infos/export")
                .queryParam("networkUuid", networkUuid.toString())
                .queryParamIfPresent("variantId", Optional.ofNullable(variantId))
                .queryParam("ids", ids)
                .build();

        String url = uriComponents.toUriString();
        ResponseEntity<List<ContingencyInfos>> responseEntity = getRestTemplate()
                .exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {
                });
        logger.debug("Actions REST API called successfully {}", url);
        return responseEntity.getBody();
    }
}

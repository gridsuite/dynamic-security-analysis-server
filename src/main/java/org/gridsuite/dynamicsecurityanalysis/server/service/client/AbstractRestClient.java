/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public abstract class AbstractRestClient {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Getter
    private final RestTemplate restTemplate;

    @Getter
    private final String baseUri;

    @Getter
    private final ObjectMapper objectMapper;

    protected AbstractRestClient(String baseUri, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.baseUri = baseUri;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

}

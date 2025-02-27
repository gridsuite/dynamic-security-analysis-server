/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.service.client.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * The implementation of class {@link ExceptionUtils} is taken from class {@code StudyUtils} in study-server
 *
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class ExceptionUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionUtils.class);

    private ExceptionUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static DynamicSecurityAnalysisException handleHttpError(HttpStatusCodeException httpException, DynamicSecurityAnalysisException.Type type, ObjectMapper objectMapper) {
        String responseBody = httpException.getResponseBodyAsString();

        String errorMessage = responseBody.isEmpty() ? httpException.getStatusCode().toString() : parseHttpError(responseBody, objectMapper);

        LOGGER.error(errorMessage, httpException);

        return new DynamicSecurityAnalysisException(type, errorMessage);
    }

    private static String parseHttpError(String responseBody, ObjectMapper objectMapper) {
        try {
            JsonNode node = objectMapper.readTree(responseBody).path("message");
            if (!node.isMissingNode()) {
                return node.asText();
            }
        } catch (JsonProcessingException e) {
            // status code or responseBody by default
        }

        return responseBody;
    }
}

/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestResponseEntityExceptionHandler.class);

    @ExceptionHandler(DynamicSecurityAnalysisException.class)
    protected ResponseEntity<Object> handleDynamicSecurityAnalysisException(DynamicSecurityAnalysisException exception) {
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error(exception.getMessage(), exception);
        }

        DynamicSecurityAnalysisException.Type type = exception.getType();
        return switch (type) {
            case RESULT_UUID_NOT_FOUND,
                 DYNAMIC_SIMULATION_RESULT_UUID_NOT_FOUND,
                 PROVIDER_NOT_FOUND,
                 CONTINGENCIES_NOT_FOUND,
                 PARAMETERS_UUID_NOT_FOUND
                    -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
            case URI_SYNTAX,
                 DUMP_FILE_ERROR,
                 DYNAMIC_MODEL_ERROR,
                 CONTINGENCY_LIST_EMPTY,
                 CONTINGENCIES_GET_ERROR,
                 DYNAMIC_SIMULATION_PARAMETERS_ERROR,
                 DYNAMIC_SIMULATION_RESULT_GET_ERROR
                    -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
        };
    }

}

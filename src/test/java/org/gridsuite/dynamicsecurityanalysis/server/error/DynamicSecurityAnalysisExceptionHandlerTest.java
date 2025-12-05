/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.error;

import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gridsuite.dynamicsecurityanalysis.server.error.DynamicSecurityAnalysisBusinessErrorCode.CONTINGENCIES_NOT_FOUND;
import static org.gridsuite.dynamicsecurityanalysis.server.error.DynamicSecurityAnalysisBusinessErrorCode.CONTINGENCY_LIST_EMPTY;
import static org.gridsuite.dynamicsecurityanalysis.server.error.DynamicSecurityAnalysisBusinessErrorCode.PROVIDER_NOT_FOUND;

/**
 * @author Hugo Marcellin <hugo.marcelin at rte-france.com>
 */
class DynamicSecurityAnalysisExceptionHandlerTest {

    private DynamicSecurityAnalysisExceptionHandler exceptionHandler;

    @BeforeEach
    void setup() {
        exceptionHandler = new DynamicSecurityAnalysisExceptionHandler(() -> "dynamic-security-analysis-server");
    }

    @Test
    void testHandleProviderNotFound() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");

        DynamicSecurityAnalysisException exception = new DynamicSecurityAnalysisException(
                PROVIDER_NOT_FOUND,
                "Provider not found"
        );

        ResponseEntity<PowsyblWsProblemDetail> response =
                exceptionHandler.handleDynamicSecurityAnalysisException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Provider not found");
        assertThat(response.getBody().getBusinessErrorCode()).isEqualTo(PROVIDER_NOT_FOUND.value());
    }

    @Test
    void testHandleContingenciesNotFound() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/contingencies");

        DynamicSecurityAnalysisException exception = new DynamicSecurityAnalysisException(
                CONTINGENCIES_NOT_FOUND,
                "Contingencies not found"
        );

        ResponseEntity<PowsyblWsProblemDetail> response =
                exceptionHandler.handleDynamicSecurityAnalysisException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Contingencies not found");
    }

    @Test
    void testHandleContingencyListEmpty() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/contingencies");

        DynamicSecurityAnalysisException exception = new DynamicSecurityAnalysisException(
                CONTINGENCY_LIST_EMPTY,
                "Contingency list is empty"
        );

        ResponseEntity<PowsyblWsProblemDetail> response =
                exceptionHandler.handleDynamicSecurityAnalysisException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Contingency list is empty");
    }
}


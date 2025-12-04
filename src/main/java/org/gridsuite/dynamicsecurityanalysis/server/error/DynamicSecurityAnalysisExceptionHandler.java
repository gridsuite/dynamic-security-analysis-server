/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.error;

import com.powsybl.ws.commons.error.AbstractBusinessExceptionHandler;
import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;
import com.powsybl.ws.commons.error.ServerNameProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@ControllerAdvice
public class DynamicSecurityAnalysisExceptionHandler extends AbstractBusinessExceptionHandler<DynamicSecurityAnalysisException, DynamicSecurityAnalysisBusinessErrorCode> {

    protected DynamicSecurityAnalysisExceptionHandler(ServerNameProvider serverNameProvider) {
        super(serverNameProvider);
    }

    @Override
    protected @NonNull DynamicSecurityAnalysisBusinessErrorCode getBusinessCode(DynamicSecurityAnalysisException e) {
        return e.getBusinessErrorCode();
    }

    protected HttpStatus mapStatus(DynamicSecurityAnalysisBusinessErrorCode businessErrorCode) {
        return switch (businessErrorCode) {
            case PROVIDER_NOT_FOUND,
                 CONTINGENCIES_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONTINGENCY_LIST_EMPTY -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    @ExceptionHandler(DynamicSecurityAnalysisException.class)
    public ResponseEntity<PowsyblWsProblemDetail> handleDynamicSecurityAnalysisException(DynamicSecurityAnalysisException exception, HttpServletRequest request) {
        return super.handleDomainException(exception, request);
    }

}

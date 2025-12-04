/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.error;

import com.powsybl.ws.commons.error.BusinessErrorCode;

/**
 * @author Hugo Marcellin <hugo.marcelin at rte-france.com>
 */
public enum DynamicSecurityAnalysisBusinessErrorCode implements BusinessErrorCode {
    PROVIDER_NOT_FOUND("dynamicSecurityAnalysis.providerNotFound"),
    CONTINGENCIES_NOT_FOUND("dynamicSecurityAnalysis.contingenciesNotFound"),
    CONTINGENCY_LIST_EMPTY("dynamicSecurityAnalysis.contingencyListEmpty");

    private final String code;

    DynamicSecurityAnalysisBusinessErrorCode(String code) {
        this.code = code;
    }

    public String value() {
        return code;
    }
}

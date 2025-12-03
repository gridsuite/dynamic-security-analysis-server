package org.gridsuite.dynamicsecurityanalysis.server.error;

import com.powsybl.ws.commons.error.BusinessErrorCode;

public enum DynamicSecurityAnalysisBusinessErrorCode implements BusinessErrorCode {
    PROVIDER_NOT_FOUND("dynamicSecurityAnalysis.providerNotFound"),
    CONTINGENCIES_NOT_FOUND("dynamicSecurityAnalysis.contingenciesNotFound"),
    CONTINGENCY_LIST_EMPTY("dynamicSecurityAnalysis.contingenciesListEmpty");

    private final String code;

    DynamicSecurityAnalysisBusinessErrorCode(String code) {
        this.code = code;
    }

    public String value() {
        return code;
    }
}

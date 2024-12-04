/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.service;

import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.security.SecurityAnalysisReport;
import com.powsybl.ws.commons.computation.service.AbstractComputationObserver;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import lombok.NonNull;
import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersInfos;
import org.springframework.stereotype.Service;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class DynamicSecurityAnalysisObserver extends AbstractComputationObserver<SecurityAnalysisReport, DynamicSecurityAnalysisParametersInfos> {

    private static final String COMPUTATION_TYPE = "dynamicsecurityanalysis";

    public DynamicSecurityAnalysisObserver(@NonNull ObservationRegistry observationRegistry, @NonNull MeterRegistry meterRegistry) {
        super(observationRegistry, meterRegistry);
    }

    @Override
    protected String getComputationType() {
        return COMPUTATION_TYPE;
    }

    @Override
    protected String getResultStatus(SecurityAnalysisReport res) {
        return res != null && res.getResult().getPreContingencyResult().getStatus() == LoadFlowResult.ComponentResult.Status.CONVERGED ? "OK" : "NOK";
    }
}

/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.service.contexts;

import com.powsybl.contingency.Contingency;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.gridsuite.computation.dto.ReportInfos;
import org.gridsuite.computation.service.AbstractComputationRunContext;
import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersInfos;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Getter
@Setter
public class DynamicSecurityAnalysisRunContext extends AbstractComputationRunContext<DynamicSecurityAnalysisParametersInfos> {

    private UUID dynamicSimulationResultUuid;

    // --- Fields which are enriched in worker service --- //

    private Path workDir;
    private List<Contingency> contingencies;
    private List<DynamicModelConfig> dynamicModelContent;
    private DynamicSecurityAnalysisParameters dynamicSecurityAnalysisParameters;

    @Builder
    public DynamicSecurityAnalysisRunContext(UUID networkUuid, String variantId, String receiver, String provider,
                                             ReportInfos reportInfos, String userId, DynamicSecurityAnalysisParametersInfos parameters, Boolean debug) {
        super(networkUuid, variantId, receiver, reportInfos, userId, provider, parameters, debug);
    }
}


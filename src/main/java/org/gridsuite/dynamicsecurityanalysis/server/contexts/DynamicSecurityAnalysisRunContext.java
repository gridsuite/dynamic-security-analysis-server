/*
 * Copyright (c) 2021-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.contexts;

import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters;
import com.powsybl.ws.commons.computation.dto.ReportInfos;
import com.powsybl.ws.commons.computation.service.AbstractComputationRunContext;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.gridsuite.dynamicsecurityanalysis.server.dto.DynamicSecurityAnalysisParametersInfos;

import java.nio.file.Path;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Getter
@Setter
public class DynamicSecurityAnalysisRunContext extends AbstractComputationRunContext<DynamicSecurityAnalysisParametersInfos> {

    // fields which are enriched in worker service
    private Path workDir;

    private DynamicSecurityAnalysisParameters dynamicSecurityAnalysisParameters;

    @Builder
    public DynamicSecurityAnalysisRunContext(UUID networkUuid, String variantId, String receiver, String provider,
                                             ReportInfos reportInfos, String userId, DynamicSecurityAnalysisParametersInfos parameters) {
        super(networkUuid, variantId, receiver, reportInfos, userId, provider, parameters);
    }
}


/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.controller.utils;

import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersInfos;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class ParameterUtils {
    private ParameterUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * get default dynamic security analysis parameters
     * @return a default dynamic security analysis parameters
     */
    public static DynamicSecurityAnalysisParametersInfos getDefaultDynamicSecurityAnalysisParameters() {
        return new DynamicSecurityAnalysisParametersInfos(null, 50.0, 5.0, null);
    }
}

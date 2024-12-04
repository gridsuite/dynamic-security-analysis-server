/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server;

import lombok.Getter;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Getter
public class DynamicSecurityAnalysisException extends RuntimeException {

    public enum Type {
        URI_SYNTAX,
        PROVIDER_NOT_FOUND,
        RESULT_UUID_NOT_FOUND,
        DYNAMIC_SIMULATION_RESULT_UUID_NOT_FOUND,
        CONTINGENCIES_NOT_FOUND,
        PARAMETERS_UUID_NOT_FOUND,
        DUMP_FILE_ERROR,
        DYNAMIC_MODEL_ERROR,
        DYNAMIC_SIMULATION_PARAMETERS_ERROR,
    }

    private final Type type;

    public DynamicSecurityAnalysisException(Type type, String message) {
        super(message);
        this.type = type;
    }
}

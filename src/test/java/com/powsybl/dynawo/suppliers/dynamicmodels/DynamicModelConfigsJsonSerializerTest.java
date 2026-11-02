/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.dynawo.suppliers.dynamicmodels;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
class DynamicModelConfigsJsonSerializerTest {

    @Test
    void testModelConfigDeserializerSerializer() throws IOException {

        ObjectMapper objectMapper = DynamicModelConfigJsonUtils.createObjectMapper();
        DynamicModelConfigs dynamicModelConfigs = objectMapper.readValue(
                getClass().getResourceAsStream("/data/dynamicModels.json"), DynamicModelConfigs.class);
        assertEquals(2, dynamicModelConfigs.dynamicModelConfigList().size());

        String dynamicModelConfigsJson = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(dynamicModelConfigs.dynamicModelConfigList());
        DynamicModelConfigs dynamicModelConfigs2 = objectMapper.readValue(dynamicModelConfigsJson, DynamicModelConfigs.class);

        Assertions.assertThat(dynamicModelConfigs2.dynamicModelConfigList())
                .usingRecursiveComparison().isEqualTo(dynamicModelConfigs.dynamicModelConfigList());
    }
}

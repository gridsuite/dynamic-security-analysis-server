/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.utils;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynawo.suppliers.Property;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.iidm.network.TwoSides;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class Utils {

    public static final String RESOURCE_PATH_DELIMITER = "/";

    public static final String INDENT = " ";

    public static final String INDENT_2 = INDENT.repeat(2);

    public static final String INDENT_4 = INDENT_2.repeat(2);

    public static final String INDENT_8 = INDENT_4.repeat(2);

    private Utils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static void postDeserializerDynamicModel(List<DynamicModelConfig> dynamicModelConfigList) {
        // enum TwoSide have been serialized as a string => when deserialize the value is a string and not enum TwoSide
        // so need convert from a string to enum
        dynamicModelConfigList.forEach(dynamicModelConfig ->
            dynamicModelConfig.properties().forEach(property -> {
                if (property.propertyClass() == TwoSides.class) {
                    Property replaceProperty = new Property(property.name(), TwoSides.valueOf(String.valueOf(property.value())), property.propertyClass());
                    int currIdx = dynamicModelConfig.properties().indexOf(property);
                    dynamicModelConfig.properties().set(currIdx, replaceProperty);
                }
            }));
    }

    /**
     * lookup the first report node in the report hierarchy which is matched a given key and a given message
     * @param rootNode a given node at whatever level
     * @param keyRegex a given key
     * @param messageRegex a given matcher for message
     * @return the first matched node
     */
    public static ReportNode getReportNode(@NotNull ReportNode rootNode, @Nullable String keyRegex, @Nullable String messageRegex) {
        Deque<ReportNode> deque = new ArrayDeque<>();
        deque.push(rootNode);
        while (!deque.isEmpty()) {
            ReportNode reportNode = deque.pop();
            String key = reportNode.getMessageKey();
            String message = reportNode.getMessage();
            if ((keyRegex == null || key != null && key.matches(keyRegex)) &&
                (messageRegex == null || message != null && message.matches(messageRegex))) {
                return reportNode;
            }

            if (reportNode.getChildren() != null) {
                deque.addAll(reportNode.getChildren());
            }
        }
        return null;
    }
}

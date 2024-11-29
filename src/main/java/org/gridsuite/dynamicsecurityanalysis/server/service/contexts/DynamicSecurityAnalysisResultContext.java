/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.service.contexts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.ws.commons.computation.dto.ReportInfos;
import com.powsybl.ws.commons.computation.service.AbstractResultContext;
import org.gridsuite.dynamicsecurityanalysis.server.dto.dynamicsimulation.DynamicSimulationParametersInfos;
import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersInfos;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.io.UncheckedIOException;
import java.util.*;

import static com.powsybl.ws.commons.computation.service.NotificationService.*;
import static com.powsybl.ws.commons.computation.utils.MessageUtils.getNonNullHeader;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicSecurityAnalysisResultContext extends AbstractResultContext<DynamicSecurityAnalysisRunContext> {

    private static final String HEADER_DYNAMIC_SIMULATION_RESULT_UUID = "dynamicSimulationResultUuid";
    private static final String HEADER_DYNAMIC_SIMULATION_PARAMETERS_INFOS = "dynamicSimulationParametersInfos";
    private static final String HEADER_CONTINGENCY_LIST_NAMES = "contingencyListNames";

    public DynamicSecurityAnalysisResultContext(UUID resultUuid, DynamicSecurityAnalysisRunContext runContext) {
        super(resultUuid, runContext);
    }

    private static List<String> getHeaderList(MessageHeaders headers, String name) {
        String header = (String) headers.get(name);
        if (header == null || header.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(header.split(","));
    }

    public static DynamicSecurityAnalysisResultContext fromMessage(Message<String> message, ObjectMapper objectMapper) {
        Objects.requireNonNull(message);

        // decode the parameters values
        DynamicSecurityAnalysisParametersInfos parametersInfos;
        try {
            parametersInfos = objectMapper.readValue(message.getPayload(), DynamicSecurityAnalysisParametersInfos.class);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }

        MessageHeaders headers = message.getHeaders();
        UUID resultUuid = UUID.fromString(getNonNullHeader(headers, RESULT_UUID_HEADER));
        String provider = getNonNullHeader(headers, HEADER_PROVIDER);
        String receiver = (String) headers.get(HEADER_RECEIVER);
        UUID networkUuid = UUID.fromString(getNonNullHeader(headers, NETWORK_UUID_HEADER));
        String variantId = (String) headers.get(VARIANT_ID_HEADER);
        String reportUuidStr = (String) headers.get(REPORT_UUID_HEADER);
        UUID reportUuid = reportUuidStr != null ? UUID.fromString(reportUuidStr) : null;
        String reporterId = (String) headers.get(REPORTER_ID_HEADER);
        String reportType = (String) headers.get(REPORT_TYPE_HEADER);
        String userId = (String) headers.get(HEADER_USER_ID);

        DynamicSecurityAnalysisRunContext runContext = DynamicSecurityAnalysisRunContext.builder()
            .networkUuid(networkUuid)
            .variantId(variantId)
            .receiver(receiver)
            .provider(provider)
            .reportInfos(ReportInfos.builder().reportUuid(reportUuid).reporterId(reporterId).computationType(reportType).build())
            .userId(userId)
            .parameters(parametersInfos)
            .build();

        // specific headers for dynamic simulation
        UUID dynamicSimulationResultUuid = UUID.fromString(getNonNullHeader(headers, HEADER_DYNAMIC_SIMULATION_RESULT_UUID));
        List<String> contingencyListNames = getHeaderList(headers, HEADER_CONTINGENCY_LIST_NAMES);

        DynamicSimulationParametersInfos dynamicSimulationParametersInfos;
        try {
            dynamicSimulationParametersInfos = objectMapper.readValue(getNonNullHeader(headers, HEADER_DYNAMIC_SIMULATION_PARAMETERS_INFOS),
                    DynamicSimulationParametersInfos.class);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }

        runContext.setDynamicSimulationResultUuid(dynamicSimulationResultUuid);
        runContext.setDynamicSimulationParametersInfos(dynamicSimulationParametersInfos);
        runContext.setContingencyListNames(contingencyListNames);

        return new DynamicSecurityAnalysisResultContext(resultUuid, runContext);
    }

    @Override
    public Map<String, String> getSpecificMsgHeaders(ObjectMapper objectMapper) {

        String jsonDynamicSimulationParametersInfos;
        try {
            jsonDynamicSimulationParametersInfos = objectMapper.writeValueAsString(getRunContext().getDynamicSimulationParametersInfos());
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }

        return Map.of(HEADER_DYNAMIC_SIMULATION_RESULT_UUID, getRunContext().getDynamicSimulationResultUuid().toString(),
                HEADER_DYNAMIC_SIMULATION_PARAMETERS_INFOS, jsonDynamicSimulationParametersInfos,
                HEADER_CONTINGENCY_LIST_NAMES, String.join(",", getRunContext().getContingencyListNames()));
    }
}

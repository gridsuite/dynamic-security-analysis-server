/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisProvider;
import com.powsybl.ws.commons.computation.service.AbstractComputationService;
import com.powsybl.ws.commons.computation.service.NotificationService;
import com.powsybl.ws.commons.computation.service.UuidGeneratorService;
import org.gridsuite.dynamicsecurityanalysis.server.dto.DynamicSecurityAnalysisStatus;
import org.gridsuite.dynamicsecurityanalysis.server.service.contexts.DynamicSecurityAnalysisResultContext;
import org.gridsuite.dynamicsecurityanalysis.server.service.contexts.DynamicSecurityAnalysisRunContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
@ComponentScan(basePackageClasses = {NetworkStoreService.class, NotificationService.class})
public class DynamicSecurityAnalysisService extends AbstractComputationService<DynamicSecurityAnalysisRunContext, DynamicSecurityAnalysisResultService, DynamicSecurityAnalysisStatus> {
    public static final String COMPUTATION_TYPE = "dynamic security analysis";

    public DynamicSecurityAnalysisService(
            NotificationService notificationService,
            ObjectMapper objectMapper,
            UuidGeneratorService uuidGeneratorService,
            DynamicSecurityAnalysisResultService dynamicSecurityAnalysisResultService,
            @Value("${dynamic-security-analysis.default-provider}") String defaultProvider) {
        super(notificationService, dynamicSecurityAnalysisResultService, objectMapper, uuidGeneratorService, defaultProvider);
    }

    @Override
    public UUID runAndSaveResult(DynamicSecurityAnalysisRunContext runContext) {
        // insert a new result entity with running status
        UUID resultUuid = uuidGeneratorService.generate();
        resultService.insertStatus(List.of(resultUuid), DynamicSecurityAnalysisStatus.RUNNING);

        // emit a message to launch the dynamic security analysis by the worker service
        Message<String> message = new DynamicSecurityAnalysisResultContext(resultUuid, runContext).toMessage(objectMapper);
        notificationService.sendRunMessage(message);
        return resultUuid;
    }

    public List<String> getProviders() {
        return DynamicSecurityAnalysisProvider.findAll().stream()
                .map(DynamicSecurityAnalysisProvider::getName)
                .toList();
    }
}

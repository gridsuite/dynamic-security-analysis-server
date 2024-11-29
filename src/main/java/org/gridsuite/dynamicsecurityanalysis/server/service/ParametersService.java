/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.service;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationProvider;
import com.powsybl.dynawo.DynawoSimulationProvider;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters;
import com.powsybl.ws.commons.computation.dto.ReportInfos;
import org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException;
import org.gridsuite.dynamicsecurityanalysis.server.dto.dynamicsimulation.DynamicSimulationParametersInfos;
import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersInfos;
import org.gridsuite.dynamicsecurityanalysis.server.entities.parameters.DynamicSecurityAnalysisParametersEntity;
import org.gridsuite.dynamicsecurityanalysis.server.repositories.DynamicSecurityAnalysisParametersRepository;
import org.gridsuite.dynamicsecurityanalysis.server.service.contexts.DynamicSecurityAnalysisRunContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException.Type.PARAMETERS_UUID_NOT_FOUND;
import static org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException.Type.PROVIDER_NOT_FOUND;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class ParametersService {

    public static final String MSG_PARAMETERS_UUID_NOT_FOUND = "Parameters uuid not found: ";

    private final String defaultProvider;

    private final DynamicSecurityAnalysisParametersRepository dynamicSecurityAnalysisParametersRepository;

    @Autowired
    public ParametersService(@Value("${dynamic-security-analysis.default-provider}") String defaultProvider, DynamicSecurityAnalysisParametersRepository dynamicSecurityAnalysisParametersRepository) {
        this.defaultProvider = defaultProvider;
        this.dynamicSecurityAnalysisParametersRepository = dynamicSecurityAnalysisParametersRepository;
    }

    public DynamicSecurityAnalysisParameters getDynamicSecurityAnalysisParameters(String provider, DynamicSecurityAnalysisParametersInfos inputParameters) {
        DynamicSecurityAnalysisParameters parameters = new DynamicSecurityAnalysisParameters();

        // TODO: Powsybl side - create an explicit dependency to Dynawo class and keep dynamic security analysis abstraction all over this micro service
        if (DynawoSimulationProvider.NAME.equals(provider)) {
            // dynamic simulation parameters
            DynamicSimulationParameters dynamicSimulationParameters = new DynamicSimulationParameters();
            parameters.setDynamicSimulationParameters(dynamicSimulationParameters);
        }

        return parameters;
    }

    public DynamicSecurityAnalysisRunContext createRunContext(UUID networkUuid, String variantId, String receiver,
                                                              String provider, UUID dynamicSimulationResultUuid, List<String> contingencyListNames, ReportInfos reportInfos, String userId,
                                                              DynamicSimulationParametersInfos dynamicSimulationParametersInfos, UUID dynamicSecurityAnalysisParametersUuid) {

        // get parameters from the local database
        DynamicSecurityAnalysisParametersInfos dynamicSecurityAnalysisParametersInfos = getDynamicSecurityAnalysisParameters(dynamicSecurityAnalysisParametersUuid);

        // build run context
        DynamicSecurityAnalysisRunContext runContext = DynamicSecurityAnalysisRunContext.builder()
                .networkUuid(networkUuid)
                .variantId(variantId)
                .receiver(receiver)
                .dynamicSimulationResultUuid(dynamicSimulationResultUuid)
                .dynamicSimulationParametersInfos(dynamicSimulationParametersInfos)
                .contingencyListNames(contingencyListNames)
                .reportInfos(reportInfos)
                .userId(userId)
                .parameters(dynamicSecurityAnalysisParametersInfos)
                .build();

        // set provider for run context
        String providerToUse = provider;
        if (providerToUse == null) {
            providerToUse = runContext.getParameters().getProvider();
        }
        if (providerToUse == null) {
            providerToUse = defaultProvider;
        }
        runContext.setProvider(providerToUse);

        // check provider
        if (DynamicSimulationProvider.findAll().stream()
                .noneMatch(elem -> Objects.equals(elem.getName(), runContext.getProvider()))) {
            throw new DynamicSecurityAnalysisException(PROVIDER_NOT_FOUND, "Dynamic security analysis provider not found: " + runContext.getProvider());
        }

        return runContext;
    }

    public DynamicSecurityAnalysisParametersInfos getDynamicSecurityAnalysisParameters(UUID parametersUuid) {
        Objects.requireNonNull(parametersUuid, "Parameters uuid must not be empty");
        DynamicSecurityAnalysisParametersEntity entity = dynamicSecurityAnalysisParametersRepository.findById(parametersUuid)
                .orElseThrow(() -> new DynamicSecurityAnalysisException(PARAMETERS_UUID_NOT_FOUND, MSG_PARAMETERS_UUID_NOT_FOUND + parametersUuid ));

        return new DynamicSecurityAnalysisParametersInfos(entity.getProvider(), entity.getScenarioDuration(), entity.getContingenciesStartTime());
    }

}

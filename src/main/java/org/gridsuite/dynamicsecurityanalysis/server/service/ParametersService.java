/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.io.FileUtil;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationProvider;
import com.powsybl.dynawo.DumpFileParameters;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.ws.commons.computation.dto.ReportInfos;
import org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException;
import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersInfos;
import org.gridsuite.dynamicsecurityanalysis.server.entities.parameters.DynamicSecurityAnalysisParametersEntity;
import org.gridsuite.dynamicsecurityanalysis.server.repositories.DynamicSecurityAnalysisParametersRepository;
import org.gridsuite.dynamicsecurityanalysis.server.service.contexts.DynamicSecurityAnalysisRunContext;
import org.gridsuite.dynamicsecurityanalysis.server.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException.Type.*;

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

    public DynamicSecurityAnalysisRunContext createRunContext(UUID networkUuid, String variantId, String receiver,
                                                              String provider, ReportInfos reportInfos, String userId,
                                                              List<String> contingencyListNames, UUID dynamicSimulationResultUuid,
                                                              UUID dynamicSecurityAnalysisParametersUuid) {

        // get parameters from the local database
        DynamicSecurityAnalysisParametersInfos dynamicSecurityAnalysisParametersInfos = getDynamicSecurityAnalysisParameters(dynamicSecurityAnalysisParametersUuid);

        // build run context
        DynamicSecurityAnalysisRunContext runContext = DynamicSecurityAnalysisRunContext.builder()
                .networkUuid(networkUuid)
                .variantId(variantId)
                .receiver(receiver)
                .dynamicSimulationResultUuid(dynamicSimulationResultUuid)
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
                .orElseThrow(() -> new DynamicSecurityAnalysisException(PARAMETERS_UUID_NOT_FOUND, MSG_PARAMETERS_UUID_NOT_FOUND + parametersUuid));

        return new DynamicSecurityAnalysisParametersInfos(entity.getProvider(), entity.getScenarioDuration(), entity.getContingenciesStartTime());
    }

    // --- Dynamic simulation result related methods --- //

    public void setupDumpParameters(Path workDir, DynamicSimulationParameters dynamicSimulationParameters, byte[] zippedOutputState) {
        Path dumpDir = workDir.resolve("dump");
        FileUtil.createDirectory(dumpDir);
        Path dumpFile = unZipDumpFile(dumpDir, zippedOutputState);
        DynawoSimulationParameters dynawoSimulationParameters = dynamicSimulationParameters.getExtension(DynawoSimulationParameters.class);
        dynawoSimulationParameters.setDumpFileParameters(DumpFileParameters.createImportDumpFileParameters(dumpDir, dumpFile.getFileName().toString()));
    }

    private Path unZipDumpFile(Path dumpDir, byte[] zippedOutputState) {
        Path dumpFile = dumpDir.resolve("outputState.dmp");
        try {
            // UNZIP output state
            Utils.unzip(zippedOutputState, dumpFile);
        } catch (IOException e) {
            throw new DynamicSecurityAnalysisException(DUMP_FILE_ERROR, String.format("Error occurred while unzip the output state into a dump file in the directory %s",
                    dumpDir.toAbsolutePath()));
        }
        return dumpFile;
    }

    public List<DynamicModelConfig> unZipDynamicModel(byte[] dynamicSimulationZippedDynamicModel, ObjectMapper objectMapper) {
        try {
            // unzip dynamic model
            return Utils.unzip(dynamicSimulationZippedDynamicModel, objectMapper, new TypeReference<>() { });
        } catch (IOException e) {
            throw new DynamicSecurityAnalysisException(DYNAMIC_MODEL_ERROR, "Error occurred while unzip the dynamic model");
        }
    }

    public DynamicSimulationParameters unZipDynamicSimulationParameters(byte[] dynamicSimulationZippedParameters, ObjectMapper objectMapper) {
        try {
            // unzip dynamic model
            return Utils.unzip(dynamicSimulationZippedParameters, objectMapper, DynamicSimulationParameters.class);
        } catch (IOException e) {
            throw new DynamicSecurityAnalysisException(DYNAMIC_SIMULATION_PARAMETERS_ERROR, "Error occurred while unzip the dynamic simulation parameters");
        }
    }

}

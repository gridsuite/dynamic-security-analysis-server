/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
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
import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters;
import com.powsybl.ws.commons.computation.dto.ReportInfos;
import com.powsybl.ws.commons.utils.GZipUtils;
import jakarta.transaction.Transactional;
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
import java.util.Optional;
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
                                                              UUID dynamicSimulationResultUuid,
                                                              UUID dynamicSecurityAnalysisParametersUuid) {

        // get parameters from the local database
        DynamicSecurityAnalysisParametersInfos dynamicSecurityAnalysisParametersInfos = getParameters(dynamicSecurityAnalysisParametersUuid);

        // build run context
        DynamicSecurityAnalysisRunContext runContext = DynamicSecurityAnalysisRunContext.builder()
                .networkUuid(networkUuid)
                .variantId(variantId)
                .receiver(receiver)
                .reportInfos(reportInfos)
                .userId(userId)
                .parameters(dynamicSecurityAnalysisParametersInfos)
                .build();
        runContext.setDynamicSimulationResultUuid(dynamicSimulationResultUuid);

        // set provider for run context
        String providerToUse = provider;
        if (providerToUse == null) {
            providerToUse = Optional.ofNullable(runContext.getParameters().getProvider()).orElse(defaultProvider);
        }

        runContext.setProvider(providerToUse);

        // check provider
        if (DynamicSimulationProvider.findAll().stream()
                .noneMatch(elem -> Objects.equals(elem.getName(), runContext.getProvider()))) {
            throw new DynamicSecurityAnalysisException(PROVIDER_NOT_FOUND, "Dynamic security analysis provider not found: " + runContext.getProvider());
        }

        return runContext;
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
            GZipUtils.unzip(zippedOutputState, dumpFile);
        } catch (IOException e) {
            throw new DynamicSecurityAnalysisException(DUMP_FILE_ERROR, String.format("Error occurred while unzip the output state into a dump file in the directory %s",
                    dumpDir.toAbsolutePath()));
        }
        return dumpFile;
    }

    public List<DynamicModelConfig> unZipDynamicModel(byte[] dynamicSimulationZippedDynamicModel, ObjectMapper objectMapper) {
        try {
            // unzip dynamic model
            List<DynamicModelConfig> dynamicModel = GZipUtils.unzip(dynamicSimulationZippedDynamicModel, objectMapper, new TypeReference<>() { });
            Utils.postDeserializerDynamicModel(dynamicModel);
            return dynamicModel;
        } catch (IOException e) {
            throw new DynamicSecurityAnalysisException(DYNAMIC_MODEL_ERROR, "Error occurred while unzip the dynamic model");
        }
    }

    public DynamicSimulationParameters unZipDynamicSimulationParameters(byte[] dynamicSimulationZippedParameters, ObjectMapper objectMapper) {
        try {
            // unzip dynamic model
            return GZipUtils.unzip(dynamicSimulationZippedParameters, objectMapper, DynamicSimulationParameters.class);
        } catch (IOException e) {
            throw new DynamicSecurityAnalysisException(DYNAMIC_SIMULATION_PARAMETERS_ERROR, "Error occurred while unzip the dynamic simulation parameters");
        }
    }

    // --- Dynamic security analysis parameters related methods --- //

    public DynamicSecurityAnalysisParametersInfos getParameters(UUID parametersUuid) {
        DynamicSecurityAnalysisParametersEntity entity = dynamicSecurityAnalysisParametersRepository.findById(parametersUuid)
                .orElseThrow(() -> new DynamicSecurityAnalysisException(PARAMETERS_UUID_NOT_FOUND, MSG_PARAMETERS_UUID_NOT_FOUND + parametersUuid));

        return new DynamicSecurityAnalysisParametersInfos(parametersUuid, entity.getProvider(), entity.getScenarioDuration(), entity.getContingenciesStartTime(), entity.getContingencyListIds());
    }

    public UUID createParameters(DynamicSecurityAnalysisParametersInfos parametersInfos) {
        return dynamicSecurityAnalysisParametersRepository.save(new DynamicSecurityAnalysisParametersEntity(parametersInfos)).getId();
    }

    public UUID createDefaultParameters() {
        DynamicSecurityAnalysisParametersInfos defaultParametersInfos = getDefaultParametersValues(defaultProvider);
        return createParameters(defaultParametersInfos);
    }

    public DynamicSecurityAnalysisParametersInfos getDefaultParametersValues(String provider) {
        DynamicSecurityAnalysisParameters defaultConfigParameters = DynamicSecurityAnalysisParameters.load();
        return DynamicSecurityAnalysisParametersInfos.builder()
                .provider(provider)
                .scenarioDuration(5.0)
                .contingenciesStartTime(defaultConfigParameters.getDynamicContingenciesParameters().getContingenciesStartTime())
                .contingencyListIds(null)
                .build();
    }

    @Transactional
    public UUID duplicateParameters(UUID sourceParametersUuid) {
        DynamicSecurityAnalysisParametersEntity entity = dynamicSecurityAnalysisParametersRepository.findById(sourceParametersUuid)
                .orElseThrow(() -> new DynamicSecurityAnalysisException(PARAMETERS_UUID_NOT_FOUND, MSG_PARAMETERS_UUID_NOT_FOUND + sourceParametersUuid));
        DynamicSecurityAnalysisParametersInfos duplicatedParametersInfos = entity.toDto();
        duplicatedParametersInfos.setId(null);
        return createParameters(duplicatedParametersInfos);
    }

    public List<DynamicSecurityAnalysisParametersInfos> getAllParameters() {
        return dynamicSecurityAnalysisParametersRepository.findAll().stream()
                .map(DynamicSecurityAnalysisParametersEntity::toDto)
                .toList();
    }

    @Transactional
    public void updateParameters(UUID parametersUuid, DynamicSecurityAnalysisParametersInfos parametersInfos) {
        DynamicSecurityAnalysisParametersEntity entity = dynamicSecurityAnalysisParametersRepository.findById(parametersUuid)
                .orElseThrow(() -> new DynamicSecurityAnalysisException(PARAMETERS_UUID_NOT_FOUND, MSG_PARAMETERS_UUID_NOT_FOUND + parametersUuid));
        if (parametersInfos == null) {
            //if the parameters is null it means it's a reset to defaultValues, but we need to keep the provider because it's updated separately
            entity.update(getDefaultParametersValues(Optional.ofNullable(entity.getProvider()).orElse(defaultProvider)));
        } else {
            entity.update(parametersInfos);
        }
    }

    public void deleteParameters(UUID parametersUuid) {
        dynamicSecurityAnalysisParametersRepository.deleteById(parametersUuid);
    }

    @Transactional
    public void updateProvider(UUID parametersUuid, String provider) {
        DynamicSecurityAnalysisParametersEntity entity = dynamicSecurityAnalysisParametersRepository.findById(parametersUuid)
                .orElseThrow(() -> new DynamicSecurityAnalysisException(PARAMETERS_UUID_NOT_FOUND, MSG_PARAMETERS_UUID_NOT_FOUND + parametersUuid));
        entity.setProvider(provider != null ? provider : defaultProvider);
    }

}

/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.service;

import com.powsybl.ws.commons.computation.service.AbstractComputationResultService;
import jakarta.transaction.Transactional;
import org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException;
import org.gridsuite.dynamicsecurityanalysis.server.dto.DynamicSecurityAnalysisStatus;
import org.gridsuite.dynamicsecurityanalysis.server.entities.DynamicSecurityAnalysisResultEntity;
import org.gridsuite.dynamicsecurityanalysis.server.repositories.DynamicSecurityAnalysisResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisException.Type.RESULT_UUID_NOT_FOUND;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class DynamicSecurityAnalysisResultService extends AbstractComputationResultService<DynamicSecurityAnalysisStatus> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSecurityAnalysisResultService.class);

    public static final String MSG_RESULT_UUID_NOT_FOUND = "Result uuid not found: ";

    private final DynamicSecurityAnalysisResultRepository resultRepository;

    public DynamicSecurityAnalysisResultService(DynamicSecurityAnalysisResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    @Override
    @Transactional
    public void insertStatus(List<UUID> resultUuids, DynamicSecurityAnalysisStatus status) {
        Objects.requireNonNull(resultUuids);
        resultRepository.saveAll(resultUuids.stream()
            .map(uuid -> new DynamicSecurityAnalysisResultEntity(uuid, status, null)).toList());
    }

    @Transactional
    public List<UUID> updateStatus(List<UUID> resultUuids, DynamicSecurityAnalysisStatus status) {
        // find result entities
        List<DynamicSecurityAnalysisResultEntity> resultEntities = resultRepository.findAllById(resultUuids);
        // set entity with new values
        resultEntities.forEach(resultEntity -> resultEntity.setStatus(status));
        // save entities into database
        return resultRepository.saveAllAndFlush(resultEntities).stream().map(DynamicSecurityAnalysisResultEntity::getId).toList();
    }

    @Transactional
    public void updateResult(UUID resultUuid, DynamicSecurityAnalysisStatus status) {
        LOGGER.debug("Update dynamic simulation [resultUuid={}, status={}", resultUuid, status);
        DynamicSecurityAnalysisResultEntity resultEntity = resultRepository.findById(resultUuid)
               .orElseThrow(() -> new DynamicSecurityAnalysisException(RESULT_UUID_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid));
        resultEntity.setStatus(status);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void updateDebugFileLocation(UUID resultUuid, String debugFilePath) {
        resultRepository.findById(resultUuid).ifPresentOrElse(
                (var resultEntity) -> resultRepository.updateDebugFileLocation(resultUuid, debugFilePath),
                () -> resultRepository.save(new DynamicSecurityAnalysisResultEntity(resultUuid, DynamicSecurityAnalysisStatus.NOT_DONE, debugFilePath))
        );
    }

    @Override
    public void delete(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        resultRepository.deleteById(resultUuid);
    }

    @Override
    @Transactional
    public void deleteAll() {
        resultRepository.deleteAll();
    }

    @Override
    public DynamicSecurityAnalysisStatus findStatus(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid)
            .map(DynamicSecurityAnalysisResultEntity::getStatus)
            .orElse(null);
    }

    @Override
    public String findDebugFileLocation(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid)
                .map(DynamicSecurityAnalysisResultEntity::getDebugFileLocation)
                .orElse(null);
    }
}

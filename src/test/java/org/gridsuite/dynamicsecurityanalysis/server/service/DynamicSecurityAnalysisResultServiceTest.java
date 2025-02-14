/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.service;

import org.gridsuite.dynamicsecurityanalysis.server.dto.DynamicSecurityAnalysisStatus;
import org.gridsuite.dynamicsecurityanalysis.server.entities.DynamicSecurityAnalysisResultEntity;
import org.gridsuite.dynamicsecurityanalysis.server.repositories.DynamicSecurityAnalysisResultRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@SpringBootTest
class DynamicSecurityAnalysisResultServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSecurityAnalysisResultServiceTest.class);

    @Autowired
    DynamicSecurityAnalysisResultRepository resultRepository;

    @Autowired
    DynamicSecurityAnalysisResultService dynamicSecurityAnalysisResultService;

    @AfterEach
    public void cleanDB() {
        resultRepository.deleteAll();
    }

    @Test
    void testCrud() {
        // --- insert an entity in the db --- //
        LOGGER.info("Test insert status");
        UUID resultUuid = UUID.randomUUID();
        dynamicSecurityAnalysisResultService.insertStatus(List.of(resultUuid), DynamicSecurityAnalysisStatus.SUCCEED);

        Optional<DynamicSecurityAnalysisResultEntity> insertedResultEntityOpt = resultRepository.findById(resultUuid);
        assertThat(insertedResultEntityOpt).isPresent();
        LOGGER.info("Expected result status = {}", DynamicSecurityAnalysisStatus.SUCCEED);
        LOGGER.info("Actual inserted result status = {}", insertedResultEntityOpt.get().getStatus());
        assertThat(insertedResultEntityOpt.get().getStatus()).isSameAs(DynamicSecurityAnalysisStatus.SUCCEED);

        // --- get status of the entity -- //
        LOGGER.info("Test find status");
        DynamicSecurityAnalysisStatus status = dynamicSecurityAnalysisResultService.findStatus(resultUuid);

        LOGGER.info("Expected result status = {}", DynamicSecurityAnalysisStatus.SUCCEED);
        LOGGER.info("Actual get result status = {}", insertedResultEntityOpt.get().getStatus());
        assertThat(status).isEqualTo(DynamicSecurityAnalysisStatus.SUCCEED);

        // --- update the entity --- //
        LOGGER.info("Test update status");
        List<UUID> updatedResultUuids = dynamicSecurityAnalysisResultService.updateStatus(List.of(resultUuid), DynamicSecurityAnalysisStatus.NOT_DONE);

        Optional<DynamicSecurityAnalysisResultEntity> updatedResultEntityOpt = resultRepository.findById(updatedResultUuids.get(0));

        // status must be changed
        assertThat(updatedResultEntityOpt).isPresent();
        LOGGER.info("Expected result status = {}", DynamicSecurityAnalysisStatus.NOT_DONE);
        LOGGER.info("Actual updated result status = {}", updatedResultEntityOpt.get().getStatus());
        assertThat(updatedResultEntityOpt.get().getStatus()).isSameAs(DynamicSecurityAnalysisStatus.NOT_DONE);

        // --- delete result --- //
        LOGGER.info("Test delete a result");
        dynamicSecurityAnalysisResultService.delete(resultUuid);

        Optional<DynamicSecurityAnalysisResultEntity> foundResultEntity = resultRepository.findById(resultUuid);
        assertThat(foundResultEntity).isNotPresent();

        // --- get status of a deleted entity --- //
        status = dynamicSecurityAnalysisResultService.findStatus(resultUuid);
        assertThat(status).isNull();

        // --- delete all --- //
        LOGGER.info("Test delete all results");
        resultRepository.saveAllAndFlush(List.of(
                new DynamicSecurityAnalysisResultEntity(UUID.randomUUID(), DynamicSecurityAnalysisStatus.RUNNING),
                new DynamicSecurityAnalysisResultEntity(UUID.randomUUID(), DynamicSecurityAnalysisStatus.RUNNING)
        ));

        dynamicSecurityAnalysisResultService.deleteAll();
        assertThat(resultRepository.findAll()).isEmpty();
    }
}

/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.repositories;

import org.gridsuite.dynamicsecurityanalysis.server.entities.DynamicSecurityAnalysisResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Repository
public interface DynamicSecurityAnalysisResultRepository extends JpaRepository<DynamicSecurityAnalysisResultEntity, UUID> {
    @Modifying
    @Query("UPDATE DynamicSecurityAnalysisResultEntity r SET r.debugFileLocation = :debugFileLocation WHERE r.id = :resultUuid")
    int updateDebugFileLocation(@Param("resultUuid") UUID resultUuid, @Param("debugFileLocation") String debugFileLocation);

}

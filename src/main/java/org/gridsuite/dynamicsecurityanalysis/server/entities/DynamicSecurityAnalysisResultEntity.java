/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.dynamicsecurityanalysis.server.dto.DynamicSecurityAnalysisStatus;

import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Getter
@Setter
@Table(name = "dynamic_security_analysis_result")
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class DynamicSecurityAnalysisResultEntity {

    @Id
    @Column(name = "result_uuid")
    private UUID id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DynamicSecurityAnalysisStatus status;

    @Column(name = "debugFileLocation")
    private String debugFileLocation;

}

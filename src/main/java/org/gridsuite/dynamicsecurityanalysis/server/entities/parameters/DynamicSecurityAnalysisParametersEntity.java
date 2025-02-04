/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.entities.parameters;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersInfos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "dynamicSecurityAnalysisParameters")
public class DynamicSecurityAnalysisParametersEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "provider")
    private String provider;

    @Column(name = "scenarioDuration")
    private Double scenarioDuration;

    @Column(name = "contingenciesStartTime")
    private Double contingenciesStartTime;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "dynamic_security_analysis_parameters_contingency_list",
        joinColumns = @JoinColumn(name = "dynamic_security_analysis_parameters_id"),
        foreignKey = @ForeignKey(name = "dynamic_security_analysis_parameters_id_fk")
    )
    @Column(name = "contingency_list_id", nullable = false)
    private List<UUID> contingencyListIds = new ArrayList<>();

    public DynamicSecurityAnalysisParametersEntity(DynamicSecurityAnalysisParametersInfos parametersInfos) {
        assignAttributes(parametersInfos);
    }

    public void assignAttributes(DynamicSecurityAnalysisParametersInfos parametersInfos) {
        provider = parametersInfos.getProvider();
        scenarioDuration = parametersInfos.getScenarioDuration();
        contingenciesStartTime = parametersInfos.getContingenciesStartTime();
        contingencyListIds = parametersInfos.getContingencyListIds();
    }

    public void update(DynamicSecurityAnalysisParametersInfos parametersInfos) {
        assignAttributes(parametersInfos);
    }

    public DynamicSecurityAnalysisParametersInfos toDto() {
        return DynamicSecurityAnalysisParametersInfos.builder()
                .id(id)
                .provider(provider)
                .scenarioDuration(scenarioDuration)
                .contingenciesStartTime(contingenciesStartTime)
                .contingencyListIds(new ArrayList<>(contingencyListIds))
                .build();
    }

}

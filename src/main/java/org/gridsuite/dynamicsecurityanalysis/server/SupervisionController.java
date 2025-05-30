/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.dynamicsecurityanalysis.server.service.SupervisionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + DynamicSecurityAnalysisApi.API_VERSION + "/supervision")
@Tag(name = "Dynamic security analysis server - Supervision")
public class SupervisionController {
    private final SupervisionService supervisionService;

    public SupervisionController(SupervisionService supervisionService) {
        this.supervisionService = supervisionService;
    }

    @GetMapping(value = "/results-count")
    @Operation(summary = "Get results count")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The count of all results")})
    public ResponseEntity<Integer> getResultsCount() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(supervisionService.getResultsCount());
    }
}

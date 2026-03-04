/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisApi;
import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersInfos;
import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersValues;
import org.gridsuite.dynamicsecurityanalysis.server.service.ParametersService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.gridsuite.computation.service.AbstractResultContext.VARIANT_ID_HEADER;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + DynamicSecurityAnalysisApi.API_VERSION + "/parameters")
@Tag(name = "Dynamic security analysis server - Parameters")
public class DynamicSecurityAnalysisParametersController {

    private final ParametersService parametersService;

    public DynamicSecurityAnalysisParametersController(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create parameters")
    @ApiResponse(responseCode = "200", description = "parameters were created")
    public ResponseEntity<UUID> createParameters(
            @RequestBody DynamicSecurityAnalysisParametersInfos parametersInfos) {
        return ResponseEntity.ok(parametersService.createParameters(parametersInfos));
    }

    @PostMapping(value = "/default")
    @Operation(summary = "Create default parameters")
    @ApiResponse(responseCode = "200", description = "Default parameters were created")
    public ResponseEntity<UUID> createDefaultParameters() {
        return ResponseEntity.ok(parametersService.createDefaultParameters());
    }

    @PostMapping(value = "", params = "duplicateFrom", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Duplicate parameters")
    @ApiResponse(responseCode = "200", description = "parameters were duplicated")
    public ResponseEntity<UUID> duplicateParameters(
            @Parameter(description = "source parameters UUID") @RequestParam("duplicateFrom") UUID sourceParametersUuid) {
        return ResponseEntity.of(Optional.of(parametersService.duplicateParameters(sourceParametersUuid)));
    }

    @GetMapping(value = "/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get parameters")
    @ApiResponse(responseCode = "200", description = "parameters were returned")
    @ApiResponse(responseCode = "404", description = "parameters were not found")
    public ResponseEntity<DynamicSecurityAnalysisParametersInfos> getParameters(
            @Parameter(description = "parameters UUID") @PathVariable("uuid") UUID parametersUuid) {
        return ResponseEntity.of(Optional.of(parametersService.getParameters(parametersUuid)));
    }

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all parameters")
    @ApiResponse(responseCode = "200", description = "The list of all parameters was returned")
    public ResponseEntity<List<DynamicSecurityAnalysisParametersInfos>> getAllParameters() {
        return ResponseEntity.ok().body(parametersService.getAllParameters());
    }

    @PutMapping(value = "/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update parameters")
    @ApiResponse(responseCode = "200", description = "parameters were updated")
    public ResponseEntity<Void> updateParameters(
            @Parameter(description = "parameters UUID") @PathVariable("uuid") UUID parametersUuid,
            @RequestBody(required = false) DynamicSecurityAnalysisParametersInfos parametersInfos) {
        parametersService.updateParameters(parametersUuid, parametersInfos);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{uuid}")
    @Operation(summary = "Delete parameters")
    @ApiResponse(responseCode = "200", description = "parameters were deleted")
    public ResponseEntity<Void> deleteParameters(
            @Parameter(description = "parameters UUID") @PathVariable("uuid") UUID parametersUuid) {
        parametersService.deleteParameters(parametersUuid);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{uuid}/provider", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get provider")
    @ApiResponse(responseCode = "200", description = "provider were returned")
    @ApiResponse(responseCode = "404", description = "provider were not found")
    public ResponseEntity<String> getProvider(
            @Parameter(description = "parameters UUID") @PathVariable("uuid") UUID parametersUuid) {
        return ResponseEntity.of(Optional.of(parametersService.getParameters(parametersUuid).getProvider()));
    }

    @GetMapping(value = "/{uuid}/values", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get parameters values")
    @ApiResponse(responseCode = "200", description = "parameters were returned")
    @ApiResponse(responseCode = "404", description = "parameters were not found")
    public ResponseEntity<DynamicSecurityAnalysisParametersValues> getParametersValues(
            @Parameter(description = "parameters UUID") @PathVariable("uuid") UUID parametersUuid,
            @RequestParam(name = "networkUuid") UUID networkUuid,
            @RequestParam(name = VARIANT_ID_HEADER, required = false) String variantId) {
        return ResponseEntity.of(Optional.of(parametersService.getParametersValues(parametersUuid, networkUuid, variantId)));
    }

}

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
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.computation.dto.ReportInfos;
import org.gridsuite.dynamicsecurityanalysis.server.dto.DynamicSecurityAnalysisStatus;
import org.gridsuite.dynamicsecurityanalysis.server.service.DynamicSecurityAnalysisResultService;
import org.gridsuite.dynamicsecurityanalysis.server.service.DynamicSecurityAnalysisService;
import org.gridsuite.dynamicsecurityanalysis.server.service.ParametersService;
import org.gridsuite.dynamicsecurityanalysis.server.service.contexts.DynamicSecurityAnalysisRunContext;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.gridsuite.computation.service.AbstractResultContext.*;
import static org.gridsuite.computation.service.NotificationService.*;
import static org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisApi.API_VERSION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + API_VERSION)
@Tag(name = "Dynamic security analysis server")
public class DynamicSecurityAnalysisController {

    private final DynamicSecurityAnalysisService dynamicSecurityAnalysisService;
    private final DynamicSecurityAnalysisResultService dynamicSecurityAnalysisResultService;
    private final ParametersService parametersService;

    public DynamicSecurityAnalysisController(DynamicSecurityAnalysisService dynamicSecurityAnalysisService,
                                             DynamicSecurityAnalysisResultService dynamicSecurityAnalysisResultService,
                                             ParametersService parametersService) {
        this.dynamicSecurityAnalysisService = dynamicSecurityAnalysisService;
        this.dynamicSecurityAnalysisResultService = dynamicSecurityAnalysisResultService;
        this.parametersService = parametersService;
    }

    @PostMapping(value = "/networks/{networkUuid}/run", produces = "application/json")
    @Operation(summary = "run the dynamic security analysis")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Run dynamic security analysis")})
    public ResponseEntity<UUID> run(@PathVariable("networkUuid") UUID networkUuid,
                                          @RequestParam(name = VARIANT_ID_HEADER, required = false) String variantId,
                                          @RequestParam(name = HEADER_RECEIVER, required = false) String receiver,
                                          @RequestParam(name = "reportUuid", required = false) UUID reportId,
                                          @RequestParam(name = REPORTER_ID_HEADER, required = false) String reportName,
                                          @RequestParam(name = REPORT_TYPE_HEADER, required = false, defaultValue = "DynamicSecurityAnalysis") String reportType,
                                          @RequestParam(name = HEADER_PROVIDER, required = false) String provider,
                                          @RequestParam(name = "debug", required = false, defaultValue = "false") boolean debug,
                                          @RequestParam(name = "dynamicSimulationResultUuid") UUID dynamicSimulationResultUuid,
                                          @RequestParam(name = "parametersUuid") UUID parametersUuid,
                                          @RequestHeader(HEADER_USER_ID) String userId) {

        DynamicSecurityAnalysisRunContext dynamicSecurityAnalysisRunContext = parametersService.createRunContext(
            networkUuid,
            variantId,
            receiver,
            provider,
            ReportInfos.builder().reportUuid(reportId).reporterId(reportName).computationType(reportType).build(),
            userId,
            dynamicSimulationResultUuid,
            parametersUuid,
            debug);

        UUID resultUuid = dynamicSecurityAnalysisService.runAndSaveResult(dynamicSecurityAnalysisRunContext);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resultUuid);
    }

    @GetMapping(value = "/results/{resultUuid}/status", produces = "application/json")
    @Operation(summary = "Get the dynamic security analysis status from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic security analysis status"),
        @ApiResponse(responseCode = "204", description = "Dynamic security analysis status is empty"),
        @ApiResponse(responseCode = "404", description = "Dynamic security analysis result uuid has not been found")})
    public ResponseEntity<DynamicSecurityAnalysisStatus> getStatus(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        DynamicSecurityAnalysisStatus result = dynamicSecurityAnalysisService.getStatus(resultUuid);
        return ResponseEntity.ok().body(result);
    }

    @PutMapping(value = "/results/invalidate-status", produces = "application/json")
    @Operation(summary = "Invalidate the dynamic security analysis status from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic security analysis result uuids have been invalidated"),
        @ApiResponse(responseCode = "404", description = "Dynamic security analysis result has not been found")})
    public ResponseEntity<List<UUID>> invalidateStatus(@Parameter(description = "Result UUIDs") @RequestParam("resultUuid") List<UUID> resultUuids) {
        List<UUID> result = dynamicSecurityAnalysisResultService.updateStatus(resultUuids, DynamicSecurityAnalysisStatus.NOT_DONE);
        return CollectionUtils.isEmpty(result) ? ResponseEntity.notFound().build() :
                ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
    }

    @DeleteMapping(value = "/results/{resultUuid}")
    @Operation(summary = "Delete a dynamic security analysis result from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic security analysis result has been deleted")})
    public ResponseEntity<Void> deleteResult(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        dynamicSecurityAnalysisResultService.delete(resultUuid);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/results", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete dynamic security analysis results from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Dynamic security analysis results have been deleted")})
    public ResponseEntity<Void> deleteResults(@Parameter(description = "Results UUID") @RequestParam(value = "resultsUuids", required = false) List<UUID> resultsUuids) {
        dynamicSecurityAnalysisService.deleteResults(resultsUuids);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/results/{resultUuid}/stop", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Stop a dynamic security analysis computation")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic security analysis has been stopped")})
    public ResponseEntity<Void> stop(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid,
                                   @Parameter(description = "Result receiver") @RequestParam(name = "receiver", required = false, defaultValue = "") String receiver) {
        dynamicSecurityAnalysisService.stop(resultUuid, receiver);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/providers", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all security analysis providers")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Dynamic security analysis providers have been found")})
    public ResponseEntity<List<String>> getProviders() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(dynamicSecurityAnalysisService.getProviders());
    }

    @GetMapping(value = "/default-provider", produces = TEXT_PLAIN_VALUE)
    @Operation(summary = "Get dynamic security analysis default provider")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "The dynamic security analysis default provider has been found"))
    public ResponseEntity<String> getDefaultProvider() {
        return ResponseEntity.ok().body(dynamicSecurityAnalysisService.getDefaultProvider());
    }

    @GetMapping(value = "/results/{resultUuid}/download-debug-file", produces = "application/json")
    @Operation(summary = "Download a dynamic security analysis debug file")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Dynamic security analysis debug file"),
        @ApiResponse(responseCode = "404", description = "Dynamic security analysis debug file has not been found")})
    public ResponseEntity<Resource> downloadDebugFile(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        return dynamicSecurityAnalysisService.downloadDebugFile(resultUuid);
    }

}

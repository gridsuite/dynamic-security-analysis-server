/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisApplication;
import org.gridsuite.dynamicsecurityanalysis.server.dto.parameters.DynamicSecurityAnalysisParametersInfos;
import org.gridsuite.dynamicsecurityanalysis.server.entities.parameters.DynamicSecurityAnalysisParametersEntity;
import org.gridsuite.dynamicsecurityanalysis.server.repositories.DynamicSecurityAnalysisParametersRepository;
import org.gridsuite.dynamicsecurityanalysis.server.service.ParametersService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.gridsuite.dynamicsecurityanalysis.server.utils.assertions.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest
@ContextConfiguration(classes = {DynamicSecurityAnalysisApplication.class})
class DynamicSecurityAnalysisParametersControllerTest {

    private static final UUID CONTINGENCY_UUID = UUID.randomUUID();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ParametersService parametersService;

    @Autowired
    DynamicSecurityAnalysisParametersRepository parametersRepository;

    @AfterEach
    void tearDown() {
        // delete all parameters
        parametersRepository.deleteAll();
    }

    private DynamicSecurityAnalysisParametersInfos getParametersInfos() {
        DynamicSecurityAnalysisParametersInfos defaultParams = parametersService.getDefaultParametersValues("Dynawo");
        defaultParams.setScenarioDuration(50.0);
        defaultParams.setContingenciesStartTime(5.0);
        defaultParams.setContingencyListIds(List.of(CONTINGENCY_UUID));
        return defaultParams;
    }

    @Test
    void testCreateParameters() throws Exception {
        DynamicSecurityAnalysisParametersInfos parametersInfos = getParametersInfos();
        MvcResult result = mockMvc.perform(post("/v1/parameters")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(parametersInfos)))
                .andExpect(status().isOk())
                .andReturn();

        UUID parametersUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        Optional<DynamicSecurityAnalysisParametersEntity> parametersEntityOpt = parametersRepository.findById(parametersUuid);
        assertThat(parametersEntityOpt).isPresent();
        DynamicSecurityAnalysisParametersInfos expectedParametersInfos = parametersEntityOpt.get().toDto();
        expectedParametersInfos.setId(null); // to avoid compare null id

        // check parameters
        assertThat(parametersInfos).recursivelyEquals(expectedParametersInfos);
    }

    @Test
    void testCreateDefaultParameters() throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/parameters/default"))
                .andExpect(status().isOk())
                .andReturn();
        UUID parametersUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        // must same as in the database
        Optional<DynamicSecurityAnalysisParametersEntity> parametersEntityOpt = parametersRepository.findById(parametersUuid);
        assertThat(parametersEntityOpt).isPresent();
        DynamicSecurityAnalysisParametersInfos expectedParametersInfos = parametersEntityOpt.get().toDto();

        // check parameters
        assertThat(expectedParametersInfos.getProvider()).isEqualTo("Dynawo");
    }

    @Test
    void testDuplicateParameters() throws Exception {
        DynamicSecurityAnalysisParametersInfos parametersInfos = getParametersInfos();
        DynamicSecurityAnalysisParametersEntity originalParametersEntity = new DynamicSecurityAnalysisParametersEntity(parametersInfos);
        UUID originalParametersUuid = parametersRepository.save(originalParametersEntity).getId();

        MvcResult result = mockMvc.perform(post("/v1/parameters?duplicateFrom=" + originalParametersUuid))
                .andExpect(status().isOk())
                .andReturn();
        UUID parametersUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        // must same as in the database
        Optional<DynamicSecurityAnalysisParametersEntity> duplicatedParametersEntityOpt = parametersRepository.findById(parametersUuid);
        assertThat(duplicatedParametersEntityOpt).isPresent();
        DynamicSecurityAnalysisParametersInfos duplicatedParametersInfos = duplicatedParametersEntityOpt.get().toDto();
        duplicatedParametersInfos.setId(null); // to avoid compare null id

        // check parameters
        assertThat(parametersInfos).recursivelyEquals(duplicatedParametersInfos);
    }

    @Test
    void testGetParameters() throws Exception {
        DynamicSecurityAnalysisParametersInfos parametersInfos = getParametersInfos();
        DynamicSecurityAnalysisParametersEntity parametersEntity = new DynamicSecurityAnalysisParametersEntity(parametersInfos);
        UUID parametersUuid = parametersRepository.save(parametersEntity).getId();

        MvcResult result = mockMvc.perform(get("/v1/parameters/" + parametersUuid))
                .andExpect(status().isOk())
                .andReturn();
        DynamicSecurityAnalysisParametersInfos resultParametersInfos = objectMapper.readValue(result.getResponse().getContentAsString(), DynamicSecurityAnalysisParametersInfos.class);

        // check parameters
        assertThat(resultParametersInfos.getId()).isEqualTo(parametersUuid);
        resultParametersInfos.setId(null); // to avoid compare null id
        assertThat(resultParametersInfos).recursivelyEquals(parametersInfos);
    }

    @Test
    void testGetAllParameters() throws Exception {

        DynamicSecurityAnalysisParametersInfos parametersInfos = getParametersInfos();
        DynamicSecurityAnalysisParametersEntity parametersEntity1 = new DynamicSecurityAnalysisParametersEntity(parametersInfos);
        DynamicSecurityAnalysisParametersEntity parametersEntity2 = new DynamicSecurityAnalysisParametersEntity(parametersInfos);
        parametersRepository.saveAll(List.of(parametersEntity1, parametersEntity2));

        MvcResult result = mockMvc.perform(get("/v1/parameters"))
                .andExpect(status().isOk())
                .andReturn();
        List<DynamicSecurityAnalysisParametersInfos> resultListParametersInfos = objectMapper
                .readValue(result.getResponse().getContentAsString(), new TypeReference<>() { });

        // check parameters
        assertThat(resultListParametersInfos).hasSize(2);
        resultListParametersInfos.get(0).setId(null); // to avoid compare null id
        assertThat(resultListParametersInfos.get(0)).recursivelyEquals(parametersInfos);
        resultListParametersInfos.get(1).setId(null); // to avoid compare null id
        assertThat(resultListParametersInfos.get(1)).recursivelyEquals(parametersInfos);
    }

    @Test
    void testUpdateParameters() throws Exception {
        DynamicSecurityAnalysisParametersInfos parametersInfos = getParametersInfos();
        DynamicSecurityAnalysisParametersEntity parametersEntity = new DynamicSecurityAnalysisParametersEntity(parametersInfos);
        UUID parametersUuid = parametersRepository.save(parametersEntity).getId();

        // create a new parameters
        DynamicSecurityAnalysisParametersInfos newParametersInfos = getParametersInfos();
        parametersInfos.setProvider("Dynawo2");
        parametersInfos.setScenarioDuration(20.0);
        parametersInfos.setContingenciesStartTime(2.0);
        parametersInfos.setContingencyListIds(List.of());

        mockMvc.perform(put("/v1/parameters/" + parametersUuid)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newParametersInfos)))
                .andExpect(status().isOk());

        Optional<DynamicSecurityAnalysisParametersEntity> updatedParametersEntityOpt = parametersRepository.findById(parametersUuid);
        assertThat(updatedParametersEntityOpt).isPresent();
        DynamicSecurityAnalysisParametersInfos expectedParametersInfos = updatedParametersEntityOpt.get().toDto();
        expectedParametersInfos.setId(null); // to avoid compare null id

        // check parameters
        assertThat(newParametersInfos).recursivelyEquals(expectedParametersInfos);

    }

    @Test
    void testDeleteParameters() throws Exception {
        DynamicSecurityAnalysisParametersInfos parametersInfos = getParametersInfos();
        DynamicSecurityAnalysisParametersEntity parametersEntity = new DynamicSecurityAnalysisParametersEntity(parametersInfos);
        UUID parametersUuid = parametersRepository.save(parametersEntity).getId();

        mockMvc.perform(delete("/v1/parameters/" + parametersUuid))
                .andExpect(status().isOk());

        Optional<DynamicSecurityAnalysisParametersEntity> parametersEntityOpt = parametersRepository.findById(parametersUuid);
        assertThat(parametersEntityOpt).isEmpty();
    }

    @Test
    void testGetProvider() throws Exception {
        DynamicSecurityAnalysisParametersInfos parametersInfos = getParametersInfos();
        DynamicSecurityAnalysisParametersEntity parametersEntity = new DynamicSecurityAnalysisParametersEntity(parametersInfos);
        UUID parametersUuid = parametersRepository.save(parametersEntity).getId();

        MvcResult result = mockMvc.perform(get("/v1/parameters/" + parametersUuid + "/provider"))
                .andExpect(status().isOk())
                .andReturn();
        String provider = result.getResponse().getContentAsString();

        // check provider
        assertThat(provider).isEqualTo(parametersInfos.getProvider());
    }

    @Test
    void testUpdateProvider() throws Exception {
        DynamicSecurityAnalysisParametersInfos parametersInfos = getParametersInfos();
        DynamicSecurityAnalysisParametersEntity parametersEntity = new DynamicSecurityAnalysisParametersEntity(parametersInfos);
        UUID parametersUuid = parametersRepository.save(parametersEntity).getId();

        String newProvider = "Dynawo2";

        mockMvc.perform(put("/v1/parameters/" + parametersUuid + "/provider")
                        .content(newProvider))
                .andExpect(status().isOk());

        Optional<DynamicSecurityAnalysisParametersEntity> updatedParametersEntityOpt = parametersRepository.findById(parametersUuid);
        assertThat(updatedParametersEntityOpt).isPresent();

        // check provider
        assertThat(updatedParametersEntityOpt.get().getProvider()).isEqualTo(newProvider);
    }
}

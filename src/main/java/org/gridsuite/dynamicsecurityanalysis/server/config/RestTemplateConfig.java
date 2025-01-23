/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.config;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.powsybl.commons.report.ReportNodeJsonModule;
import com.powsybl.dynamicsimulation.json.DynamicSimulationParametersJsonModule;
import com.powsybl.security.dynamic.json.DynamicSecurityAnalysisJsonModule;
import com.powsybl.ws.commons.computation.ComputationConfig;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Configuration
@Import(ComputationConfig.class)
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter(objectMapper);

        return restTemplateBuilder
                .additionalMessageConverters(messageConverter)
                .build();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer appJsonCustomizer() {
        return builder -> builder
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .modulesToInstall(new DynamicSecurityAnalysisJsonModule(),
                        new DynamicSimulationParametersJsonModule(), new ReportNodeJsonModule());
    }
}

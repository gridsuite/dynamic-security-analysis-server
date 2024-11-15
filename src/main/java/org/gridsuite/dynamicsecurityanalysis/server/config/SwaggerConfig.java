/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.gridsuite.dynamicsecurityanalysis.server.DynamicSecurityAnalysisApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI createOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dynamic Security Analysis Server API")
                        .description("This is the documentation of the Dynamic-Security-Analysis REST API")
                        .version(DynamicSecurityAnalysisApi.API_VERSION));
    }
}

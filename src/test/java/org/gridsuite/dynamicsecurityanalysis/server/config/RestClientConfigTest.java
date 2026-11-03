/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.dynamicsecurityanalysis.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestClientConfigTest {
    enum State { READY }

    @Test
    void configuredMapperTakesPrecedence() {
        RestClientConfig config = new RestClientConfig();
        Jackson2ObjectMapperBuilder mapperBuilder = Jackson2ObjectMapperBuilder.json();
        config.appJsonCustomizer().customize(mapperBuilder);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = config.restClient(builder, mapperBuilder.build());
        server.expect(requestTo("/state")).andRespond(withSuccess("\"ready\"", MediaType.APPLICATION_JSON));
        assertEquals(State.READY, client.get().uri("/state").retrieve().body(State.class));
        server.verify();
    }
}

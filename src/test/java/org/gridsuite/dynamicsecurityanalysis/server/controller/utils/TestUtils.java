/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.controller.utils;

import org.springframework.cloud.stream.binder.test.OutputDestination;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class TestUtils {
    private static final long TIMEOUT = 100;

    private TestUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static void assertQueuesEmptyThenClear(List<String> destinations, OutputDestination output) {
        await().pollDelay(TIMEOUT, TimeUnit.MILLISECONDS).until(() -> {
            try {
                destinations.forEach(destination -> assertThat(output.receive(0, destination)).as("Should not be any messages in queue " + destination + " : ").isNull());
            } catch (NullPointerException e) {
                // Ignoring
            } finally {
                output.clear(); // purge in order to not fail the other tests
            }
            return true;
        });
    }
}

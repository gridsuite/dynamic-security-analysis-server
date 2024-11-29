/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class Utils {

    public static final String RESOURCE_PATH_DELIMITER = "/";

    private Utils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static byte[] zip(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath);
             ByteArrayOutputStream os = new ByteArrayOutputStream();
             GZIPOutputStream zipOs = new GZIPOutputStream(os)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                zipOs.write(buffer, 0, length);
            }
            zipOs.finish();
            return os.toByteArray();
        }
    }

    public static void unzip(byte[] zippedBytes, Path filePath) throws IOException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(zippedBytes);
             FileOutputStream fos = new FileOutputStream(new File(filePath.toUri()));
             GZIPInputStream zipIs = new GZIPInputStream(is)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = zipIs.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
    }
}

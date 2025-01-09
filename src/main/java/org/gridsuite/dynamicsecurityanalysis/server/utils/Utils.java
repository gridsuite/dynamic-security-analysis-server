/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicsecurityanalysis.server.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.dynawo.suppliers.Property;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.iidm.network.TwoSides;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

    public static void postDeserializerDynamicModel(List<DynamicModelConfig> dynamicModelConfigList) {
        // enum TwoSide have been serialized as a string => when deserialize the value is a string and not enum TwoSide
        // so need convert from a string to enum
        dynamicModelConfigList.forEach(dynamicModelConfig -> {
            dynamicModelConfig.properties().forEach(property -> {
                if (property.propertyClass() == TwoSides.class) {
                    Property replaceProperty = new Property(property.name(), TwoSides.valueOf(String.valueOf(property.value())), property.propertyClass());
                    int currIdx = dynamicModelConfig.properties().indexOf(property);
                    dynamicModelConfig.properties().set(currIdx, replaceProperty);
                }
            });
        });
    }

    public static byte[] zip(InputStream is) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
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

    public static byte[] zip(String content) throws IOException {
        try (InputStream is = new ByteArrayInputStream(content.getBytes())) {
            return zip(is);
        }
    }

    public static byte[] zip(Path filePath) {
        try (InputStream is = Files.newInputStream(filePath)) {
            return zip(is);
        } catch (IOException e) {
            throw new UncheckedIOException("Error occurred while zipping the file " + filePath.toAbsolutePath(), e);
        }
    }

    private static void unzipToStream(byte[] zippedBytes, OutputStream outputStream) throws IOException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(zippedBytes);
             GZIPInputStream zipIs = new GZIPInputStream(is);
             BufferedOutputStream bufferedOut = new BufferedOutputStream(outputStream)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = zipIs.read(buffer)) > 0) {
                bufferedOut.write(buffer, 0, length);
            }
        }
    }

    public static void unzip(byte[] zippedBytes, Path filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(new File(filePath.toUri()))) {
            unzipToStream(zippedBytes, fos);
        }
    }

    public static <T> T unzip(byte[] zippedBytes, ObjectMapper objectMapper, TypeReference<T> valueTypeRef) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            unzipToStream(zippedBytes, bos);
            return objectMapper.readValue(bos.toByteArray(), valueTypeRef);
        }
    }

    public static <T> T unzip(byte[] zippedBytes, ObjectMapper objectMapper, Class<T> valueType) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            unzipToStream(zippedBytes, bos);
            return objectMapper.readValue(bos.toByteArray(), valueType);
        }
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2024 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.microprofile.config.extensions.toml;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import fish.payara.nucleus.microprofile.config.source.extension.ConfiguredExtensionConfigSource;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Service(name = "toml-config-source")
public class TOMLConfigSource extends ConfiguredExtensionConfigSource<TOMLConfigSourceConfiguration> {

    private static final Logger LOGGER = Logger.getLogger(TOMLConfigSource.class.getName());
    private final Map<String, String> properties = new ConcurrentHashMap<>();
    private long lastModified;

    @Inject
    MicroprofileConfigConfiguration mpConfig;

    @Override
    public String getValue(String s) {
        return getProperties().get(s);
    }

    @Override
    public String getName() {
        return "toml";
    }

    @Override
    public int getOrdinal() {
        return Integer.parseInt(mpConfig.getTomlOrdinality());
    }

    @Override
    public Map<String, String> getProperties() {
        Path tomlFilePath = getFilePath();
        if (tomlFilePath == null) {
            return properties;
        }
        long tomlFileLastModified = tomlFilePath.toFile().lastModified();
        if (lastModified == tomlFileLastModified) {
            return properties;
        }

        try {
            properties.clear();
            lastModified = tomlFileLastModified;
            TomlMapper tomlMapper = new TomlMapper();
            Map<?, ?> config = tomlMapper.readValue(tomlFilePath.toFile(), Map.class);

            flattenToml(config, "", properties, 0, Integer.parseInt(configuration.getDepth()));

        } catch (Exception e) {
            LOGGER.warning(e.getMessage());
        }
        return properties;
    }

    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }

    @Override
    public boolean setValue(String name, String value) {
        return false;
    }

    @Override
    public boolean deleteValue(String name) {
        return false;
    }

    @Override
    public String getSource() {
        return "toml";
    }

    private Path getFilePath() {
        String path = configuration.getPath();
        if (path == null || path.isEmpty()) {
            return null;
        }

        Path tomlPath = Paths.get(path);

        if (!tomlPath.isAbsolute()) {
            tomlPath = Paths.get(System.getProperty("com.sun.aas.instanceRoot"), tomlPath.toString()).normalize();
        }

        File file = tomlPath.toFile();
        return file.exists() ? tomlPath : null;
    }

    private void flattenToml(Map<?,?> tomlTable, String prefix, Map<String, String> resultMap, int depth, int maxDepth) {
        if (depth > maxDepth) {
            throw new IllegalArgumentException("Exceeded maximum depth of " + maxDepth);
        }
        for (Object key : tomlTable.keySet()) {
            Object value;
            try {
                value = tomlTable.get(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            String fullKey = prefix.isEmpty() ? key.toString() : prefix + "." + key.toString();

            if (value instanceof Map) {
                // If the value is a TomlTable (nested table), recurse into it
                flattenToml((Map<?, ?>) value, fullKey, resultMap, depth + 1, maxDepth);
            } else if (value instanceof ArrayList<?>) {
                // If the value is a TomlArray, iterate through the array
                flattenTomlArray((ArrayList<?>) value, fullKey, resultMap, depth + 1, maxDepth);
            } else {
                // Otherwise, add the key-value pair to the result map
                if (value == null) {
                    continue;
                }
                resultMap.put(fullKey, value.toString());
            }
        }
    }

    private void flattenTomlArray(ArrayList<?> array, String prefix, Map<String, String> resultMap, int depth, int maxDepth) {
        if (depth > maxDepth) {
            throw new IllegalArgumentException(String.format("Exceeded maximum depth of %s with depth of %s", maxDepth, depth));
        }
        for (int i = 0; i < array.size(); i++) {
            Object arrayValue = array.get(i);
            String arrayKey = prefix + "[" + i + "]";
            if (arrayValue instanceof Map) {
                flattenToml((Map<?, ?>) arrayValue, arrayKey, resultMap, depth + 1, maxDepth);
            } else if (arrayValue instanceof ArrayList) {
                // Recursively flatten the nested array
                flattenTomlArray((ArrayList<?>) arrayValue, arrayKey, resultMap, depth + 1, maxDepth);
            } else {
                resultMap.put(arrayKey, arrayValue.toString());
            }
        }
    }
}

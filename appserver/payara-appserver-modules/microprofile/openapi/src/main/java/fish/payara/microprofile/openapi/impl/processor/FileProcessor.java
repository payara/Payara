/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.impl.processor;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.merge;
import static java.util.logging.Level.WARNING;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import fish.payara.microprofile.openapi.activation.OpenApiSniffer;
import fish.payara.microprofile.openapi.api.processor.OASProcessor;
import fish.payara.microprofile.openapi.impl.config.OpenApiConfiguration;
import fish.payara.microprofile.openapi.impl.model.OpenAPIImpl;
import fish.payara.microprofile.openapi.impl.rest.app.provider.ObjectMapperFactory;

/**
 * A processor to process a static document in the <code>META-INF</code>
 * directory of the application, and merge it into the provided model.
 */
public class FileProcessor implements OASProcessor {

    private static final Logger LOGGER = Logger.getLogger(FileProcessor.class.getName());

    /**
     * The static file provided by the application.
     */
    private File file;

    /**
     * A mapper object to create an OpenAPI model from the document.
     */
    private ObjectMapper mapper;

    public FileProcessor(ClassLoader appClassLoader) {
        try {
            // Search for a valid static file
            // WebAppClassLoader root is found in WEB-INF/classes, so paths need relativising
            URL fileUrl = getFirstValidOpenApiResource(appClassLoader, "../../");

            // If the file is found, configure the public variables
            if (fileUrl != null) {
                file = new File(fileUrl.toURI());
                if (file.getPath().endsWith(".json")) {
                    mapper = ObjectMapperFactory.createJson();
                } else {
                    mapper = ObjectMapperFactory.createYaml();
                }
            } else {
                LOGGER.fine("No static OpenAPI document provided.");
            }
        } catch (URISyntaxException ex) {
            LOGGER.log(WARNING, "Invalid URI syntax", ex);
        }
    }

    @Override
    public OpenAPI process(OpenAPI api, OpenApiConfiguration config) {
        if (file != null) {
            OpenAPI readResult = null;
            try {
                readResult = mapper.readValue(file, OpenAPIImpl.class);
            } catch (IOException ex) {
                LOGGER.log(WARNING, "Error when reading static OpenAPI document.", ex);
            }
            if (readResult != null) {
                merge(readResult, api, true);
            }
        }
        return api;
    }

    private static final URL getFirstValidOpenApiResource(ClassLoader classLoader, String prefix) {
        for (String path : OpenApiSniffer.OPENAPI_YAML_FILE_PATHS) {
            final String resourceName = (prefix + "/" + path).replace("//", "/");
            final URL resource = classLoader.getResource(resourceName);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

}
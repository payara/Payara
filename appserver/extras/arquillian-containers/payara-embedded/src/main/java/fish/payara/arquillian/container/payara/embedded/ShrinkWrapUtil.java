/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Portions Copyright [2016-2017] [Payara Foundation]
package fish.payara.arquillian.container.payara.embedded;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

import static java.io.File.createTempFile;

import java.io.File;
import java.net.URL;

/**
 * ShrinkWrapUtil
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
final class ShrinkWrapUtil {
    
    private ShrinkWrapUtil() {
    }

    /**
     * Creates a tmp folder and exports the file. Returns the URL for that file location.
     *
     * @param archive Archive to export
     */
    public static URL toURL(final Archive<?> archive) {
        // create a random named temp file, then delete and use it as a directory
        try {
            File root = createTempFile("arquillian", archive.getName());
            
            root.delete();
            root.mkdirs();
            root.deleteOnExit();
            File deployment = new File(root, archive.getName());
            deployment.deleteOnExit();
            archive.as(ZipExporter.class).exportTo(deployment, true);
            
            return deployment.toURI().toURL();
        } catch (Exception e) {
            throw new RuntimeException("Could not export deployment to temp", e);
        }
    }
}

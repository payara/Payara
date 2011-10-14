/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
package org.glassfish.virtualization.runtime;

import org.glassfish.virtualization.config.Template;
import org.glassfish.virtualization.os.FileOperations;
import org.glassfish.virtualization.spi.Machine;
import org.glassfish.virtualization.spi.MachineOperations;
import org.glassfish.virtualization.spi.TemplateInstance;
import org.glassfish.virtualization.util.RuntimeContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defines a local template, installed on *this* machine.
 *
 * @author Jerome Dochez
 */
public class LocalTemplate extends VMTemplate {

    public LocalTemplate(TemplateInstance config) {
        super(config);
    }

    @Override
    public synchronized void copyTo(final Machine destination, final String destDir) throws IOException {


        final String destinationDirectory = destination.getConfig().getTemplatesLocation() + "/" + getDefinition().getName();
        final File sourceDirectory = templateInstance.getLocation();
        destination.execute(new MachineOperations<Void>() {
            @Override
            public Void run(FileOperations fileOperations) throws IOException {
                if (!sourceDirectory.exists()) {
                    RuntimeContext.logger.severe("Cannot find template directory " + sourceDirectory.getAbsolutePath());
                    return null;
                }
                for (File file : sourceDirectory.listFiles()) {
                    String destPath = destinationDirectory + "/" + file.getName();

                    try {
                        boolean needRefresh = false;
                        if (!fileOperations.exists(destPath)) {
                            needRefresh = true;
                        } else {
                            Date lastModification = fileOperations.mod(destPath);
                            RuntimeContext.logger.info(file.getName() + " last modified on " + destination.getName() + " is " + lastModification.toString());
                            Date sourceLastModification = new Date(file.lastModified());
                            RuntimeContext.logger.info("while here, the last mod for " + file.getName() + " is " + sourceLastModification.toString());
                            if (lastModification.compareTo(sourceLastModification) > 0) {
                                RuntimeContext.logger.info("There is no need to copy " + file.getName());
                            } else {
                                RuntimeContext.logger.info(file.getName() + " need to be updated ");
                                needRefresh = true;
                            }
                        }
                        if (needRefresh) {
                            fileOperations.mkdir(destinationDirectory);
                            if (fileOperations.exists(destPath)) {
                                fileOperations.delete(destPath);
                            }
                            RuntimeContext.logger.info("Copying template " + getDefinition().getName() + " file "
                                    + file.getName() + " on " + destination.getName());
                            fileOperations.copy(file, new File(destinationDirectory));
                            RuntimeContext.logger.info("Finished copying template " + getDefinition().getName() + " file "
                                    + file.getName() + " on " + destination.getName());
                        }
                    } catch (IOException e) {
                        RuntimeContext.logger.log(Level.SEVERE, "Cannot copy template on " + getDefinition().getName(), e);
                        throw e;
                    }
                }
                return null;
            }
        });

    }

    @Override
    public long getSize() throws IOException {
        return templateInstance.getFileByExtension(".img").length();
    }

}
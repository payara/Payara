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
import org.glassfish.virtualization.spi.Machine;
import org.glassfish.virtualization.spi.TemplateInstance;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Defines a remote template, installed on a remote machine.
 * @author Jerome Dochez
 */
public class RemoteTemplate extends VMTemplate {
    private final Machine machine;

    /**
     * Creates a new template instance for a remotely installed template
     * @param location the machine where the template is installed.
     * @param config  the template characteristics
     */
    public RemoteTemplate(Machine location, TemplateInstance config) {
        super(config);
        this.machine = location;
    }

    @Override
    public void copyTo(Machine destination, String destDir) throws IOException {

        destination.getFileOperations().localCopy(remotePath(), destDir);

    }

    private String remotePath() {
        try {
            String fileName = templateInstance.getFileByExtension(".img").getName();
            return machine.getConfig().getTemplatesLocation()+"/"+getDefinition().getName()+"/" + fileName;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public long getSize() throws IOException {
        return machine.getFileOperations().length(remotePath());
    }
}
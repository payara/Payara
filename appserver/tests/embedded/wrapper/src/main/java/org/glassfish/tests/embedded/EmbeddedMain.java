/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tests.embedded;

import org.glassfish.internal.embedded.*;

import java.io.File;


/** A 'wrapper' class that is used by QA to run tests on embedded. This class is used to start the domain
 * in embedded mode from asadmin cli
 *
 */

public class EmbeddedMain {
    public static void main(String[] args) {

       String installRoot = System.getenv("S1AS_HOME");
       if (installRoot == null) {
           System.out.println("Environment variable S1AS_HOME not defined - it must point to the glassfish install root");
           return;
       }
       String instanceRoot = installRoot + "/domains/domain1";
       String domainXml = instanceRoot + "/config/domain.xml";

        System.setProperty("com.sun.aas.instanceRootURI", "file:" + instanceRoot);
        //System.setProperty("com.sun.aas.installRoot", installRoot );
//        System.setProperty("com.sun.aas.instanceRoot", instanceRoot );

       EmbeddedFileSystem efs =
          new EmbeddedFileSystem.Builder().
          installRoot(new File(installRoot), true).
          instanceRoot(new File(instanceRoot)).
          configurationFile(new File(domainXml), false).autoDelete(false).build();

        Server.Builder builder = new Server.Builder("asadmin");

        Server server = builder.embeddedFileSystem(efs).build();
        server.addContainer(ContainerBuilder.Type.all);
    }
}



/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.kernel.embedded;

import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.v3.server.GFDomainXml;
import org.glassfish.server.ServerEnvironmentImpl;
import javax.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

/**
 * Embedded domain.xml, can use externally pointed domain.xml
 *
 * @author Jerome Dochez
 * @author bhavanishankar@dev.java.net
 */
public class EmbeddedDomainXml extends GFDomainXml {

    @Inject
    StartupContext startupContext;

    @Override
    protected URL getDomainXml(ServerEnvironmentImpl env) throws IOException {
        return getDomainXml(startupContext);
    }

    static URL getDomainXml(StartupContext startupContext) throws IOException {
        String configFileURI = startupContext.getArguments().getProperty(
                "org.glassfish.embeddable.configFileURI");
        if (configFileURI != null) { // user specified domain.xml
            return URI.create(configFileURI).toURL();
        }
        String instanceRoot = startupContext.getArguments().getProperty(
                "com.sun.aas.instanceRoot");
        File domainXml = new File(instanceRoot, "config/domain.xml");
        if (domainXml.exists()) { // domain/config/domain.xml, if exists.
            return domainXml.toURI().toURL();
        }
        return EmbeddedDomainXml.class.getClassLoader().getResource(
                "org/glassfish/embed/domain.xml");
    }

    @Override
    protected void upgrade() {
        // for now, we don't upgrade in embedded mode...
    }

}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.server.core.jws;

import com.sun.enterprise.module.bootstrap.StartupContext;
import java.io.File;
import java.util.List;
import javax.inject.Inject;
import org.glassfish.api.admin.ServerEnvironment;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import javax.inject.Singleton;

/**
 * Maintains the in-memory representation for the customization points of
 * the Java Web Start JNLP that a developer might provide inside his or her
 * app client.  During deployment, if such custom JNLP exists in the
 * app client being deployed then the server combines that developer-provided
 * JNLP with the server-generated JNLP to produce the JNLP document that
 * is used for launching the app client.
 * <p>
 * There are two aspects to this combination: merging the JNLP XML data itself
 * and detecting and recording places in the developer-provided JNLP which
 * refer to other resources - JARs, native libraries, other JNLP documents.
 * <p>
 * The on-disk config file contains property settings for both types of
 * XPath information.  This service makes sure that the in-memory data is
 * up-to-date with respect to the on-disk file.  The server installation
 * includes the config file in the installations's config directory.  But for
 * unusual circumstances, this class looks for the config file
 * first in the domain's config directory and, if it is not there or is there
 * but is not readable, then tries to read the file from the installations's
 * config directory.  Note that even if the installation's config is more
 * recent than the domain's config this class will always prefer the domain's
 * config file if it exists and is readable.
 *
 * @author tjquinn
 */
@Service
@Singleton
public class DeveloperContentService implements PostConstruct {


    /** for locating the installation's client JNLP config file */
    @Inject
    private StartupContext startupContext;

    /** for locating the domain's client JNLP config file (if any) */
    @Inject
    private ServerEnvironment serverEnv;

    /** the always-current data reflecting what is in the on-disk file */
    private ClientJNLPConfigData configData = null;

    public void postConstruct() {
        configData = new ClientJNLPConfigData(installConfigDir(), domainConfigDir());
    }

    private File installConfigDir() {
        return new File(startupContext.getArguments().getProperty("com.sun.aas.installRoot"), "config");
    }

    private File domainConfigDir() {
        return serverEnv.getConfigDirPath();
    }

    /**
     * Returns the XPath-related objects for references to other resources
     * that might exist in the developer's JNLP.
     *
     * @return XPathToDeveloperProvidedContentRefs objects for reference sites
     */
    List<XPathToDeveloperProvidedContentRefs> xPathsToDevContentRefs() {
        return configData.xPathsToDevContentRefs();
    }

    /**
     * Returns the XPath-related objects for combining parts of the JNLP from
     * the developer with the JNLP generated by the server.
     *
     * @return CombinedXPath objects for JNLP sites to be combined
     */
    List<CombinedXPath> xPathsToCombinedContent() {
        return configData.xPathsToCombinedContent();
    }
}

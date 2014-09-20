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

package org.glassfish.kernel.embedded;

import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.v3.server.DomainXmlPersistence;
import javax.inject.Inject;
import org.jvnet.hk2.config.DomDocument;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

/**
 * Configuration file persistence handler for embedded
 *
 * @author Jerome Dochez
 * @author bhavanishankar@dev.java.net
 */
public class EmbeddedDomainPersistence extends DomainXmlPersistence {

    @Inject
    StartupContext startupContext;

    final static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(DomainXmlPersistence.class);

    /**
     * Returns the destination file for saving the embedded configuration file,
     * when set.
     *
     * @return the embedded configuration file if set in read-write mode.
     * @throws IOException
     */
    @Override
    protected File getDestination() throws IOException {
        String configFileReadOnly = startupContext.getArguments().getProperty(
                "org.glassfish.embeddable.configFileReadOnly");
        if (configFileReadOnly != null &&
                !Boolean.valueOf(configFileReadOnly).booleanValue()) {
            try {
                URI uri = EmbeddedDomainXml.getDomainXml(startupContext).toURI();
                if ("file".equalsIgnoreCase(uri.getScheme())) {
                    return new File(uri);
                } else {
                    // TODO :: localize the message.
                    throw new IOException("configurationFile is writable but is not a file");
                }
            } catch (URISyntaxException ex) {
                throw new IOException(ex);
            }
        }
        return null; // Don't persist domain.xml anywhere.
    }

    @Override
    public void save(DomDocument doc) throws IOException {
        File destination = getDestination();
        if (destination == null) {
            String msg = localStrings.getLocalString("NoLocation",
                    "domain.xml cannot be persisted, null destination");
            logger.finer(msg);
            return;
        }
        super.save(doc);
    }

    @Override
    protected void saved(File destination) {
        logger.log(Level.INFO, "Configuration saved at {0}", destination);
    }
}

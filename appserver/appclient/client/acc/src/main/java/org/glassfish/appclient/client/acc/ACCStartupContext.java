/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2017-2021] [Payara Foundation and/or its affiliates.]

package org.glassfish.appclient.client.acc;

import com.sun.enterprise.glassfish.bootstrap.MainHelper;
import com.sun.enterprise.module.bootstrap.StartupContext;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import jakarta.inject.Singleton;
import org.jvnet.hk2.annotations.Service;

import static com.sun.enterprise.util.SystemPropertyConstants.INSTALL_ROOT_PROPERTY;

/**
 * Start-up context for the ACC.  Note that this context is used also for
 * Java Web Start launches.
 * 
 * @author tjquinn
 */
@Service
@Singleton
public class ACCStartupContext extends StartupContext {
    
    private static final String H2_ROOT_PROPERTY = "AS_H2_INSTALL";

    public ACCStartupContext() {
        super(accEnvironment());
    }

    /**
     * Creates a Properties object containing setting for the definitions
     * in the asenv[.bat|.conf] file.
     *
     * @return
     */
    private static Properties accEnvironment() {
        final Properties result = MainHelper.parseAsEnv(getRootDirectory());
        result.setProperty(INSTALL_ROOT_PROPERTY, getRootDirectory().getAbsolutePath());
        final File h2dbDir = new File(getRootDirectory().getParentFile(), "h2db");
        if (h2dbDir.isDirectory()) {
            result.setProperty(H2_ROOT_PROPERTY, h2dbDir.getAbsolutePath());
        }
        return result;
    }

    private static File getRootDirectory() {
        /*
         * During launches not using Java Web Start the root directory
         * is important; it is used in setting some system properties.
         */
        URI jarURI = null;
        try {
            jarURI = ACCClassLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
        if (jarURI.getScheme().startsWith("http")) {
            /*
             * We do not really rely on the root directory during Java
             * Web Start launches but we must return something.
             */
            return new File(System.getProperty("user.home"));
        }
        File jarFile = new File(jarURI);
        File dirFile = jarFile.getParentFile().getParentFile();
        return dirFile;
    }
}

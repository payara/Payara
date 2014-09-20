/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.server.core.jws.servedcontent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.glassfish.appclient.server.core.jws.Util;

/**
 * Abstracts the content of several server-side config files so the current
 * values can be served to the Java Web Start client.
 *
 * @author tjquinn
 */
public class ACCConfigContent {

    private final SunACCPairedFiles sunACC;
    private final PairedFiles appClientLogin;
    
    /* match the security.config property and capture the value */
    private final static Pattern SECURITY_CONFIG_VALUE_PATTERN = Pattern.compile(
            "<property name=\"security.config\"\\s*value=\"([^\"]*)\"\\s*/\\s*>");

    private final static String SECURITY_CONFIG_REPLACEMENT =
            "<property name=\"security.config\" value=\"\\${security.config.path}\"/>";

    public ACCConfigContent(File domainConfig, File installLibAppclient) throws FileNotFoundException, IOException {

        sunACC = SunACCPairedFiles.newSunACCPairedFiles(
                new File(domainConfig, "glassfish-acc.xml"),
                new File(domainConfig, "glassfish-acc.jws.xml"));

        appClientLogin = PairedFiles.newPairedFiles(
                new File(installLibAppclient, "appclientlogin.conf"),
                new File(installLibAppclient, "appclientlogin.jws.conf"));

    }

    public String sunACC() throws FileNotFoundException, IOException {
        return sunACC.content();
    }

    public String appClientLogin() throws FileNotFoundException, IOException {
        return appClientLogin.content();
    }

    public String securityConfig() throws FileNotFoundException, IOException {
        return sunACC.securityConfigContent();
    }


    private static class PairedFiles {
        private final File normalFile;
        private final File jwsFile;

        private String currentContent;

        private long lastModified = 0;

        private static PairedFiles newPairedFiles(final File normalFile, final File jwsFile) throws FileNotFoundException, IOException {
            final PairedFiles result = new PairedFiles(normalFile, jwsFile);
            result.setCurrentContent();
            return result;
        }

        private PairedFiles(final File normalFile, final File jwsFile)
                throws FileNotFoundException, IOException {
            this.normalFile = normalFile;
            this.jwsFile = jwsFile;
        }

        protected long lastModified() {
            return lastModified;
        }

        protected void setCurrentContent() throws FileNotFoundException, IOException {
            setCurrentContent(loadContent(fileToCheck()));
        }

        protected void setCurrentContent(final String content) {
            currentContent = content;
            lastModified = fileToCheck().lastModified();
        }

        protected boolean isContentCurrent() {
            return lastModified >= fileToCheck().lastModified();
        }
        
        protected File fileToCheck() {
            return (jwsFile.exists() ? jwsFile : normalFile);
        }

        protected String loadContent(final File f) throws FileNotFoundException, IOException {
            FileReader fr = new FileReader(f);
            StringBuilder sb = new StringBuilder();
            int charsRead;

            final char[] buffer = new char[1024];
            try {
                while ( (charsRead = fr.read(buffer)) != -1) {
                    sb.append(buffer, 0, charsRead);
                }
                return Util.replaceTokens(sb.toString(), System.getProperties());
            } finally {
                fr.close();
            }
        }

        String content() throws FileNotFoundException, IOException {
            if ( ! isContentCurrent()) {
                loadContent(fileToCheck());
            }
            return currentContent;
        }

    }

    private static class SunACCPairedFiles extends PairedFiles {

        private String configFilePath = null;
        
        private File securityConfigFile = null;

        private String securityConfigContent = null;

        private static SunACCPairedFiles newSunACCPairedFiles(
                final File normalFile, final File jwsFile) throws FileNotFoundException, IOException {
            final SunACCPairedFiles result = new SunACCPairedFiles(normalFile, jwsFile);
            result.setCurrentContent();
            return result;
        }

        public SunACCPairedFiles(File normalFile, File jwsFile) throws FileNotFoundException, IOException {
            super(normalFile, jwsFile);
        }

        @Override
        protected boolean isContentCurrent() {
            return super.isContentCurrent() &&
                    (securityConfigFile.lastModified() <= lastModified());
        }

        @Override
        protected String loadContent(File f) throws FileNotFoundException, IOException {
            String origContent = super.loadContent(f);
            /*
             * Replace the value in the glassfish-acc.xml content for the
             * security.config property with a placeholder that the client
             * will recognize and replace with a temp file constructed on
             * the client.
             */
            final Matcher m = SECURITY_CONFIG_VALUE_PATTERN.matcher(origContent);
            final StringBuffer sb = new StringBuffer();
            final String origConfigFilePath = configFilePath;
            while (m.find()) {
                /*
                 * This should match only once.
                 */
                configFilePath = m.group(1);
                m.appendReplacement(sb, SECURITY_CONFIG_REPLACEMENT);
            }
            m.appendTail(sb);

            if ( ! configFilePath.equals(origConfigFilePath)) {
                securityConfigFile = new File(configFilePath);
                securityConfigContent = super.loadContent(securityConfigFile);
            }
            return sb.toString();
        }

        String securityConfigContent() throws FileNotFoundException, IOException {
            if ( ! isContentCurrent()) {
                setCurrentContent(loadContent(fileToCheck()));
            }
            return securityConfigContent;
        }
    }

}

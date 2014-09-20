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

import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import java.net.URI;
import java.util.Properties;
import org.glassfish.appclient.server.core.AppClientDeployerHelper;
import org.glassfish.appclient.server.core.NestedAppClientDeployerHelper;
import org.glassfish.appclient.server.core.StandaloneAppClientDeployerHelper;
import org.glassfish.appclient.server.core.jws.JWSAdapterManager;
import org.glassfish.appclient.server.core.jws.JavaWebStartInfo;
import org.glassfish.appclient.server.core.jws.JavaWebStartInfo.VendorInfo;
import org.glassfish.appclient.server.core.jws.NamingConventions;

/**
 *
 * @author tjquinn
 */
public abstract class TokenHelper {

    private static final String AGENT_JAR = "gf-client.jar";
    private static final String DYN_PREFIX = "___dyn/";
    private static final String GROUP_JAR_ELEMENT_PROPERTY_NAME = "group.facade.jar.element";

    private Properties tokens;
    protected final AppClientDeployerHelper dHelper;

    private final LocalStringsImpl localStrings = new LocalStringsImpl(TokenHelper.class);

    private VendorInfo vendorInfo = null;

    private String signingAlias;

    public static TokenHelper newInstance(final AppClientDeployerHelper dHelper,
            final VendorInfo vendorInfo) {
        TokenHelper tHelper;
        if (dHelper instanceof StandaloneAppClientDeployerHelper) {
            tHelper = new StandAloneClientTokenHelper(dHelper);
        } else {
            if (dHelper instanceof NestedAppClientDeployerHelper) {
                tHelper = new NestedClientTokenHelper((NestedAppClientDeployerHelper)dHelper);
            } else {
                throw new RuntimeException("dHelper.getClass() = " + dHelper.getClass().getName() + " != NestedAppClientDeployerHelper");
            }
        }
        tHelper.vendorInfo = vendorInfo;

        tHelper.signingAlias = JWSAdapterManager.signingAlias(dHelper.dc());
        tHelper.tokens = tHelper.buildTokens();
        return tHelper;
    }

    public Properties tokens() {
        return tokens;
    }

    public Object setProperty(final String propName, final String propValue) {
        return tokens.setProperty(propName, propValue);
    }

    public String imageURIFromDescriptor() {
        return vendorInfo.getImageURI();
    }

    public String splashScreenURIFromDescriptor() {
        return vendorInfo.getSplashImageURI();
    }

    protected TokenHelper(final AppClientDeployerHelper dHelper) {
        this.dHelper = dHelper;
    }

    public String appCodebasePath() {
        return NamingConventions.contextRootForAppAdapter(dHelper.appName());
    }

    public String systemContextRoot() {
        return NamingConventions.JWSAPPCLIENT_SYSTEM_PREFIX;
    }

    public String agentJar() {
        return AGENT_JAR;
    }

    public String systemJNLP() {
        return NamingConventions.systemJNLPURI(signingAlias);
    }

    public abstract String appLibraryExtensions();

    /**
     * Returns the relative path from the app's context root to its
     * anchor.  For example, for a stand-alone client the anchor is
     * the same place as the context root; that is where its facade and
     * client JAR reside.  For a nested app client, the
     * anchor is the subdirectory ${clientName}Client.
     * 
     * @return
     */
    protected abstract String anchorSubpath();

    public String mainJNLP() {
        return dyn() + anchorSubpath() + "___main.jnlp";
    }

    public String clientJNLP() {
        return dyn() + anchorSubpath() + "___client.jnlp";
    }

    public String clientFacadeJNLP() {
        return dyn() + anchorSubpath() + "___clientFacade.jnlp";
    }

    public String dyn() {
        return DYN_PREFIX;
    }

    protected AppClientDeployerHelper dHelper() {
        return dHelper;
    }

    public String clientFacadeJARPath() {
        return anchorSubpath() + dHelper.clientName();
    }

    private Properties buildTokens() {
        final Properties t = new Properties();

        t.setProperty("app.codebase.path", appCodebasePath());
        t.setProperty("main.jnlp.path", mainJNLP());
        t.setProperty("system.context.root", systemContextRoot());
        t.setProperty("agent.jar", agentJar());
        t.setProperty("system.jnlp", systemJNLP());
//        t.setProperty("client.facade.jnlp.path", clientFacadeJNLP());
        t.setProperty("client.jnlp.path", clientJNLP());
        t.setProperty(JavaWebStartInfo.APP_LIBRARY_EXTENSION_PROPERTY_NAME, appLibraryExtensions());
        t.setProperty("anchor.subpath", anchorSubpath());
        t.setProperty("dyn", dyn());

        t.setProperty("client.facade.jar.path", clientFacadeJARPath());

        t.setProperty("client.security", "<all-permissions/>");

        final ApplicationClientDescriptor acDesc = dHelper.appClientDesc();
        /*
         * Set the JNLP information title to the app client module's display name,
         * if one is present.
         */
        String displayName = acDesc.getDisplayName();
        String jnlpInformationTitle =
                (displayName != null && displayName.length() > 0) ?
                    displayName : localStrings.get("jws.information.title.prefix") + " " + dHelper.appName();
        t.setProperty("appclient.main.information.title", jnlpInformationTitle);
        t.setProperty("appclient.client.information.title", jnlpInformationTitle);

        /*
         * Set the one-line description the same as the title for now.
         */
        t.setProperty("appclient.main.information.description.one-line", jnlpInformationTitle);
        t.setProperty("appclient.client.information.description.one-line", jnlpInformationTitle);

        /*
         *Set the short description to the description from the descriptor, if any.
         */
        String description = acDesc.getDescription();
        String jnlpInformationShortDescription =
                (description != null && description.length() > 0) ?
                    description : jnlpInformationTitle;
        t.setProperty("appclient.main.information.description.short", jnlpInformationShortDescription);
        t.setProperty("appclient.client.information.description.short", jnlpInformationShortDescription);

        t.setProperty("appclient.vendor", vendorInfo.getVendor());

        /*
         * Construct the icon elements, if the user specified any in the
         * optional descriptor element.
         */
        t.setProperty("appclient.main.information.images", iconElements(vendorInfo));

        /*
         * For clients in an EAR there will be an EAR-level generated group facade
         * JAR to include in the downloaded files.
         */
        final URI groupFacadeUserURI = dHelper.groupFacadeUserURI(dHelper.dc());
        t.setProperty(GROUP_JAR_ELEMENT_PROPERTY_NAME,
                (groupFacadeUserURI == null ? "" : "<jar href=\"" + groupFacadeUserURI.toASCIIString() + "\"/>"));
        setSystemJNLPTokens(t);
        return t;

    }

    private String iconElements(final VendorInfo vendorInfo) {

        StringBuilder result = new StringBuilder();
        String imageURI = vendorInfo.JNLPImageURI();
        if (imageURI.length() > 0) {
            result.append("<icon href=\"" + imageURI + "\"/>");
//            addImageContent(origin, location, imageURI);
        }
        String splashImageURI = vendorInfo.JNLPSplashImageURI();
        if (splashImageURI.length() > 0) {
            result.append("<icon kind=\"splash\" href=\"" + splashImageURI + "\"/>");
//            addImageContent(origin, location, splashImageURI);
        }
        return result.toString();
    }

    private void setSystemJNLPTokens(final Properties props) {
        final String[] tokenNames = new String[] {
            "jws.appserver.information.title",
            "jws.appserver.information.vendor",
            "jws.appserver.information.description.one-line",
            "jws.appserver.information.description.short"
        };

        for (String tokenName : tokenNames) {
            final String value = localStrings.get(tokenName);
            props.setProperty(tokenName, value);
        }
    }
}

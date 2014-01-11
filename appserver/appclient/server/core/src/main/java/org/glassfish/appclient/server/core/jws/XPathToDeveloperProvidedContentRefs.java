/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2014 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.logging.LogDomains;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.appclient.server.core.AppClientDeployerHelper;
import org.glassfish.appclient.server.core.ApplicationSignedJARManager;
import org.glassfish.appclient.server.core.NestedAppClientDeployerHelper;
import org.glassfish.appclient.server.core.StandaloneAppClientDeployerHelper;
import org.glassfish.appclient.server.core.jws.servedcontent.Content;
import org.glassfish.appclient.server.core.jws.servedcontent.DynamicContent;
import org.glassfish.appclient.server.core.jws.servedcontent.FixedContent;
import org.glassfish.appclient.server.core.jws.servedcontent.SimpleDynamicContentImpl;
import org.glassfish.appclient.server.core.jws.servedcontent.StaticContent;
import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * Abstracts the XPath information for developer-provided references to
 * other resources, whether static content (such as JARs or native libraries)
 * or dynamic content (such as other JNLP documents).  (Note that these
 * are dynamic content because the server adjusts them - even the
 * developer-provided ones - at HTTP request time with, for example, the
 * code base.)
 *
 * @param <T> either StaticContent or DynamicContent
 */
abstract class XPathToDeveloperProvidedContentRefs<T extends Content> {

    private static final String STATIC_REFS_PROPERTY_NAME = "static.refs";
    private static final String DYNAMIC_REFS_PROPERTY_NAME = "dynamic.refs";

    private final static XPathFactory xPathFactory = XPathFactory.newInstance();

    private final static XPath xPath = xPathFactory.newXPath();

    private static final Logger logger = Logger.getLogger(JavaWebStartInfo.APPCLIENT_SERVER_MAIN_LOGGER, JavaWebStartInfo.APPCLIENT_SERVER_LOGMESSAGE_RESOURCE);
    
    @LogMessageInfo (
            message = "Client JNLP document {0} refers to the static resource {1} that does not exist or is not readable.",
            cause = "The developer-provided JNLP content refers to a file as if the file is in the application but the server could not find the file.",
            action = "Make sure the file is packaged in the application and that the reference to the file is correct.  Then rebuild and redeploy the application.")
    public static final String BAD_STATIC_CONTENT = "AS-ACDEPL-00111";

    @LogMessageInfo (
            message = "The ApplicationSignedJARManager for a nested app client deployer helper is unexpectedly null.",
            cause = "During deployment of nested app clients (those inside EARs), the system should use an ApplicationSignedJARManager but it is null.",
            action = "This is a system error.  Please report this as a bug.")
    public static final String SIGNED_JAR_MGR_NULL = "AS-ACDEPL-00114";
    
    @LogMessageInfo(
            message = "Tbe custom JNLP document {0} in a stand-alone app client incorrectly refers to a JAR {1}",
            cause = "The app client includes a custom JNLP document which refers to a JAR.  Stand-alone app clients cannot refer to other JARs because they are self-contained deployment units.",
            action = "Remove references to JAR from the custom JNLP document or package the app client inside an EAR that also contains the referenced JAR.")
    public static final String USER_REFERENCED_JAR = "AS-ACDEPL-00115";

    private enum Type {

        STATIC(STATIC_REFS_PROPERTY_NAME), DYNAMIC(DYNAMIC_REFS_PROPERTY_NAME);
        private String propertyName;

        Type(final String propName) {
            this.propertyName = propName;
        }
    }
    private final XPathExpression xPathExpr;

    private XPathToDeveloperProvidedContentRefs(final String path) {
        super();
        try {
            xPathExpr = xPath.compile(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    static List<XPathToDeveloperProvidedContentRefs> parse(final Properties p) {
        List<XPathToDeveloperProvidedContentRefs> result = new ArrayList<XPathToDeveloperProvidedContentRefs>();
        result.addAll(XPathToDeveloperProvidedContentRefs.
                parse(p, XPathToDeveloperProvidedContentRefs.Type.STATIC));
        result.addAll(XPathToDeveloperProvidedContentRefs.
                parse(p, XPathToDeveloperProvidedContentRefs.Type.DYNAMIC));
        return result;
    }
    
    /**
     * Extracts the relevant information from the Properties object and
     * creates the correct set of content objects depending on which type
     * of xpath reference the caller requested (static or dynamic).
     * @param p Properties read from the on-disk config file
     * @param type static or dynamic
     * @return xpath-related objects for the selected type of reference points
     */
    private static List<XPathToDeveloperProvidedContentRefs> parse(final Properties p, Type type) {
        final List<XPathToDeveloperProvidedContentRefs> result = new ArrayList<XPathToDeveloperProvidedContentRefs>();
        final String refs = p.getProperty(type.propertyName);
        for (String ref : refs.split(",")) {
            result.add((type == Type.STATIC) ? new XPathToStaticContent(ref) : new XPathToDynamicContent(ref));
        }
        return result;
    }

    XPathExpression xPathExpr() {
        return xPathExpr;
    }

    /**
     * Adds the referenced data for this object to either the static
     * or dynamic content, depending on whether this object is for
     * static or dynamic content.
     * <p>
     * The concrete implementation in the subclasses will actually
     * update either staticContent or dynamicContent but not both.
     * But providing both as arguments lets the caller not worry about
     * which type of xpath content this object is.
     *
     * @param codebase the code base from the containing document
     * @param pathToContent location of the content (relative or absolute)
     * @param loader class loader which could be used to locate referenced content
     * @param staticContent static content map
     * @param dynamicContent dynamic content map
     * @param appRootURI root URI for the application
     * @throws URISyntaxException
     * @throws IOException
     */
    abstract void addToContentIfInApp(
            DeveloperContentHandler dch,
            final AppClientDeployerHelper helper,
            String referringDocument,
            URI codebase, String pathToContent,
            ClassLoader loader, Map<String, StaticContent> staticContent,
            Map<String, DynamicContent> dynamicContent, final URI appRootURI,
            final ReadableArchive appClientArchive)
                throws URISyntaxException, IOException;

/**
     * Models XPath-related information for a developer-provided reference to
     * static content (such as to a JAR, a native library, or an image).
     */
    private static class XPathToStaticContent extends XPathToDeveloperProvidedContentRefs<StaticContent> {

        XPathToStaticContent(final String path) {
            super(path);
        }

        @Override
        void addToContentIfInApp(
                final DeveloperContentHandler dch,
                final AppClientDeployerHelper helper,
                final String referringDocument,
                final URI codebase,
                final String pathToContent,
                final ClassLoader loader,
                final Map<String,StaticContent> staticContent,
                final Map<String,DynamicContent> dynamicContent,
                final URI appRootURI,
                final ReadableArchive appClientArchive) throws URISyntaxException {
            final URI uriToContent = new URI(pathToContent);
            final URI absURI = codebase.resolve(uriToContent);
            if (absURI.equals(uriToContent)) {
                return;
            }
            final URI fileURI = appRootURI.resolve(pathToContent);
            final File f = new File(fileURI);
            /*
             * The developer might have referred to a JAR or other static file
             * that is not actually in the app.  If so, log a warning.
             */
            if ( ! f.exists() || ! f.canRead()) {
                logger.log(Level.WARNING,
                        BAD_STATIC_CONTENT,
                        new Object[] {referringDocument, pathToContent});
            } else {
                final ApplicationSignedJARManager signedJARManager = helper.signedJARManager();
                if (signedJARManager == null && helper instanceof NestedAppClientDeployerHelper) {
                    /*
                     * The signed JAR manager should not be null when we deploy
                     * an app client nested inside an EAR.  This is a system error.
                     */
                    logger.log(Level.SEVERE, SIGNED_JAR_MGR_NULL);
                } else if (helper instanceof StandaloneAppClientDeployerHelper) {
                    logger.log(Level.WARNING, USER_REFERENCED_JAR,
                            new Object[] {referringDocument, pathToContent});
                } else {
                    try {
                        final URI signedURI = signedJARManager.addJAR(fileURI);
                        staticContent.put(pathToContent, signedJARManager.staticContent(signedURI));
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    /**
     * Models Xpath-related information for a developer-provided reference to
     * dynamic content (such as another JNLP document).
     */
    private static class XPathToDynamicContent extends XPathToDeveloperProvidedContentRefs<DynamicContent>{

        XPathToDynamicContent(final String path) {
            super(path);
        }

        @Override
        void addToContentIfInApp(
                final DeveloperContentHandler dch,
                final AppClientDeployerHelper helper,
                final String referringDocument,
                final URI codebase,
                final String pathToContent,
                final ClassLoader loader,
                final Map<String,StaticContent> staticContent,
                final Map<String,DynamicContent> dynamicContent,
                final URI appRootURI,
                final ReadableArchive appClientArchive) throws URISyntaxException, IOException {
            final URI uriToContent = new URI(pathToContent);
            final URI absURI = codebase.resolve(uriToContent);
            if (absURI.equals(uriToContent)) {
                return;
            }
            /*
             * Find the developer-provided content.
             */

//            InputStream is = JavaWebStartInfo.openEntry(appClientArchive, pathToContent);
//            if (is == null) {
//                return;
//            }
//
//            final byte[] buffer = new byte[1024];
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            int bytesRead;
//
//            while ((bytesRead = is.read(buffer)) != -1) {
//                baos.write(buffer, 0, bytesRead);
//            }
//            is.close();

            /*
             * Combine the developer's extension JNLP with the template so we
             * can set the parts of the resulting document that we need to
             * control.
             */
            final String combinedContent = dch.combineJNLP(
                    JavaWebStartInfo.textFromURL(
                        JavaWebStartInfo.DEVELOPER_EXTENSION_DOCUMENT_TEMPLATE),
                    pathToContent);
            dynamicContent.put(pathToContent,
                    new SimpleDynamicContentImpl(
                        combinedContent,
                        URLConnection.guessContentTypeFromName(pathToContent)));

            /*
             * Currently the only dynamic content processed from the developer's
             * JNLP is an <extension> element which refers to another
             * JNLP document.  So we need to recursively process that
             * document now also.
             */
            dch.addDeveloperContent(pathToContent, combinedContent);
        }

    }
}

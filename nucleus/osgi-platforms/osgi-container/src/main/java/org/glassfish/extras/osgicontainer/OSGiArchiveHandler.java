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

package org.glassfish.extras.osgicontainer;

import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.CompositeHandler;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.internal.deployment.GenericHandler;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 * Archive Handler for OSGi modules.
 * This understands a special deployment property called UriScheme.
 * The value of this property must be a url scheme for which there is a URL handler currently registered in the JVM.
 * Any other deployment properties are treated as query parameters.
 * The rules are pretty much same as what's the case for webbundle url handler
 * as defined in OSGi Web Application spec except that the solution here is not limited to webbundle scheme.
 * Since the deployment properties are used as query parameters, they must be encoded such that they
 * conform to URL RFC 1738.
 *
 * @author Jerome Dochez
 * @author TangYong(tangyong@cn.fujitsu.com)
 * @author sanjeeb.sahoo@oracle.com
 */
@Service(name = OSGiArchiveDetector.OSGI_ARCHIVE_TYPE)
@Singleton
public class OSGiArchiveHandler extends GenericHandler implements CompositeHandler {

    @LoggerInfo(subsystem = "OSGI", description="OSGI container logger", publish=true)
    private static final String LOGGER_NAME = "javax.enterprise.osgi.container";

    @LogMessagesResourceBundle()
    public static final String RB_NAME = "org.glassfish.extras.osgicontainer.LogMessages";

    private static Logger logger = Logger.getLogger(LOGGER_NAME, RB_NAME);

    @LogMessageInfo(message = "Decorated url = {0}", level="INFO")
    public static final String DECORATED_URL = "NCLS-OSGI-00001";


    @Inject
    private OSGiArchiveDetector detector;
    private String URI_SCHEME_PROP_NAME = "UriScheme";
    private char QUERY_PARAM_SEP = '&';
    private String QUERY_DELIM = "?";
    private String SCHEME_SEP = ":";


    public String getArchiveType() {
        return OSGiArchiveDetector.OSGI_ARCHIVE_TYPE;
    }

    public boolean accept(ReadableArchive source, String entryName) {
        // we hide everything so far.
        return false;
    }

    public void initCompositeMetaData(DeploymentContext context) {
        // nothing to initialize
    }

    public boolean handles(ReadableArchive archive) throws IOException {
        return detector.handles(archive);
    }

    public ClassLoader getClassLoader(ClassLoader parent, DeploymentContext context) {
        return parent;
    }

    public String getDefaultApplicationName(ReadableArchive archive,
                                            DeploymentContext context) {
        return getDefaultApplicationNameFromArchiveName(archive);
    }

    /**
     * Overriding the expand method of base class(GenericHandler) in order to
     * support allowing wrapping of non-OSGi bundles when --type=osgi option is
     * used in deploy command or GUI. Pl. see [GLASSFISH-16651]
     *
     * @param source  of the expanding
     * @param target  of the expanding
     * @param context deployment context
     * @throws IOException when the archive is corrupted
     */
    @Override
    public void expand(ReadableArchive source, WritableArchive target,
                       DeploymentContext context) throws IOException {
        Properties props = context
                .getCommandParameters(DeployCommandParameters.class).properties;
        if ((props != null) && (props.containsKey(URI_SCHEME_PROP_NAME))) {
            // if UriScheme is specified, we need to construct a new URL based on user's input
            // and souce parameter and call openConnection() and getInputStream() on it.
            URL url = prepareUrl(context, props);
            logger.log(Level.INFO, DECORATED_URL, new Object[]{url});
            final JarInputStream jis = new JarInputStream(url.openStream());
            expandJar(jis, target);
        } else {
            super.expand(source, target, context);
        }
    }


    /**
     * This method creates a new URL based on user's input. The new URL is expected to be backed by a URL stream handler
     * that decorates the input stream. The general syntax to create the decoarated URI is:
     * newScheme:embeddedUri?query
     * e.g., when user's input is:
     * deploy --type osgi --properties
     *     UriScheme=webbundle:Bundle-SymbolicName=foo:Import-Package=javax.servlet:Web-ContextPath=/foo /tmp/foo.war
     * we create a new URI like this:
     * webbundle:file:/tmp/foo.war?Bundle-SymbolicName=foo&Import-Package=javax.servlet&Web-ContextPath=/foo
     *
     * Please note two things here:
     * a) We add the URI Scheme provided by user as a prefix.
     * b) We always add a ? at the end of embeddedUrl even when user has not provided any query params.
     * This strategy works really well as is proven by OSGi Web Applications spec.
     *
     * We expect the input to be already encoded.
     *
     * @param context DeploymentContext
     * @param props   properties passed in --properties argument of deploy command
     * @return a new URL which can be used to read the decorated content
     * @throws MalformedURLException
     */
    private URL prepareUrl(DeploymentContext context, Properties props)
            throws MalformedURLException {
        logger.logp(Level.FINE, "OSGiArchiveHandler", "prepareUrl", "Deployment properties = {0}", new Object[]{props});
        final String uriScheme = props.getProperty(URI_SCHEME_PROP_NAME);
        final URI embeddedUri = context.getOriginalSource().getURI();
        StringBuilder query = new StringBuilder();
        Enumeration<?> p = props.propertyNames();
        while (p.hasMoreElements()) {
            String key = (String) p.nextElement();
            if (URI_SCHEME_PROP_NAME.equalsIgnoreCase(key)) {
                continue; // separately taken care of
            }
            query.append(key);
            query.append("=");
            query.append(props.getProperty(key));
            query.append(QUERY_PARAM_SEP);
        }
        final int lastIdx = query.length() - 1;
        if (query.charAt(lastIdx) == QUERY_PARAM_SEP) {
            // Remove the trailing &
            query.deleteCharAt(lastIdx);
        }
        // We always add ? at the end of embeddedUri to indicate that's the end of embeddedUri
        String decoratedUriStr = uriScheme + SCHEME_SEP + embeddedUri + QUERY_DELIM + query;
        logger.logp(Level.FINE, "OSGiArchiveHandler", "prepareUrl", "Constructing a new URL from string [{0}]",
                new Object[]{decoratedUriStr});
        try {
            return new URI(decoratedUriStr).toURL(); // Calling new URI().toURL() performs appropriate decoding/encoding
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Populates a writable archive by reading the input JarInputStream.
     * It closes both the input and output at the end.
     * @param jis
     * @param target
     * @throws IOException
     */
    private void expandJar(JarInputStream jis, WritableArchive target) throws IOException {
        try {
            JarEntry je;
            while ((je = jis.getNextJarEntry()) != null) {
                OutputStream os = null;
                try {
                    if (je.isDirectory()) {
                        logger.logp(Level.FINER, "OSGiArchiveHandler", "expandJar",
                                "Skipping jar entry = {0} since this is of directiry type", new Object[]{je});
                        continue;
                    }
                    final String entryName = je.getName();
                    final long entrySize = je.getSize();
                    logger.logp(Level.FINER, "OSGiArchiveHandler", "expandJar", "Writing jar entry name = {0}, size = {1}",
                            new Object[]{entryName, entrySize});
                    os = target.putNextEntry(entryName);
                    FileUtils.copy(jis, os, entrySize < 0 ? 0 : entrySize); // passing 0 will force it to read until EOS
                } finally {
                    if (os != null) {
                        target.closeEntry();
                    }
                    jis.closeEntry();
                }
            }

            //Add MANIFEST File To Target and Write the MANIFEST File To Target
            Manifest m = jis.getManifest();
            if (m != null) {
                logger.logp(Level.FINER, "OSGiArchiveHandler", "expandJar", "Writing manifest entry");
                OutputStream os = null;
                try {
                    os = target.putNextEntry(JarFile.MANIFEST_NAME);
                    m.write(os);
                } finally {
                    if (os != null) {
                        target.closeEntry();
                    }
                }
            }
        } finally {
            if (jis != null)
                jis.close();
            target.close();
        }
    }

    /**
     * Returns whether this archive requires annotation scanning.
     *
     * @param archive file
     * @return whether this archive requires annotation scanning
     */
    public boolean requiresAnnotationScanning(ReadableArchive archive) {
        return false;
    }
}

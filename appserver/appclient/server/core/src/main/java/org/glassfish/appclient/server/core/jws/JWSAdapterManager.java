/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Config;
import org.glassfish.orb.admin.config.IiopService;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.logging.LogDomains;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.container.EndpointRegistrationException;
import org.glassfish.api.container.RequestDispatcher;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.appclient.server.core.AppClientDeployer;
import org.glassfish.appclient.server.core.AppClientServerApplication;
import org.glassfish.appclient.server.core.jws.ExtensionFileManager.Extension;
import org.glassfish.appclient.server.core.jws.servedcontent.ASJarSigner;
import org.glassfish.appclient.server.core.jws.servedcontent.AutoSignedContent;
import org.glassfish.appclient.server.core.jws.servedcontent.DynamicContent;
import org.glassfish.appclient.server.core.jws.servedcontent.SimpleDynamicContentImpl;
import org.glassfish.appclient.server.core.jws.servedcontent.StaticContent;
import org.glassfish.enterprise.iiop.api.GlassFishORBFactory;
import org.glassfish.internal.api.ServerContext;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import javax.inject.Singleton;
import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * Handles all management of the HTTP adapters created to support Java Web
 * Start launches of app clients.
 * 
 * @author tjquinn
 */
@Service
@Singleton
public class JWSAdapterManager implements PostConstruct {
    
    private final static String SIGNING_ALIAS_PROPERTY_NAME = "jar-signing-alias";
    
    private final static String DEFAULT_SIGNING_ALIAS = "s1as";
    
    private final static String MANIFEST_APP_NAME_FOR_SYSTEM_FILES = "GlassFish";

    @Inject
    private ServerEnvironment serverEnv;
    
    @Inject
    private ServerContext serverContext;

    @Inject
    private RequestDispatcher requestDispatcher;

    @Inject
    private ASJarSigner jarSigner;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    @Inject 
    AppClientDeployer appClientDeployer;

    @Inject
    private GlassFishORBFactory orbFactory;

    private static final String LINE_SEP = System.getProperty("line.separator");

    private static final List<String> DO_NOT_SERVE_LIST =
            Collections.EMPTY_LIST; //Arrays.asList("glassfish/modules/jaxb-osgi.jar");

    private static final String JWS_SIGNED_SYSTEM_JARS_ROOT = "java-web-start/___system";
    private static final String JWS_SIGNED_DOMAIN_JARS_ROOT = "java-web-start/___domain";

    private static final String JAVA_WEB_START_CONTEXT_ROOT_PROPERTY_NAME =
            "javaWebStartContextRoot";
    /**
     * maps "(aliasName)/(systemJarRelativePath)" to AutoSignedContent for
     * the system JAR as signed by the cert linked to the alias
     */
    private final Map<String,AutoSignedContent> appLevelSignedSystemContent =
            new HashMap<String,AutoSignedContent>();

    private URI installRootURI = null;

    private AppClientHTTPAdapter systemAdapter = null;

    private Logger logger = null;

    private IiopService iiopService;

    private final HashMap<String,Set<AppClientServerApplication>> contributingAppClients =
            new HashMap<String,Set<AppClientServerApplication>>();

    private final ConcurrentHashMap<String,AppClientHTTPAdapter> httpAdapters = new
            ConcurrentHashMap<String, AppClientHTTPAdapter>();

    private URI umbrellaRootURI = null;
    private File umbrellaRoot = null;
    private File systemLevelSignedJARsRoot = null;
    private File domainLevelSignedJARsRoot = null;
    
    @LogMessageInfo(
            message = "Error starting the adapter to serve static system-level content",
            cause = "An unexpected internal system error occurred",
            action = "Please consult the exception stack trace")
    public static final String ERROR_STARTING_SYSTEM_ADAPTER = "AS-ACDEPL-00105";

    @Override
    public synchronized void postConstruct() {
        installRootURI = serverContext.getInstallRoot().toURI();
        logger = Logger.getLogger(JavaWebStartInfo.APPCLIENT_SERVER_MAIN_LOGGER, JavaWebStartInfo.APPCLIENT_SERVER_LOGMESSAGE_RESOURCE);
        iiopService = config.getExtensionByType(IiopService.class);
        umbrellaRoot = new File(installRootURI).getParentFile();
        umbrellaRootURI = umbrellaRoot.toURI();
        systemLevelSignedJARsRoot = new File(serverEnv.getDomainRoot(), JWS_SIGNED_SYSTEM_JARS_ROOT);
        domainLevelSignedJARsRoot = new File(serverEnv.getDomainRoot(), JWS_SIGNED_DOMAIN_JARS_ROOT);
    }

    public static String signingAlias(final DeploymentContext dc) {
        return chooseAlias(dc);
    }

    synchronized File rootForSignedFilesInDomain() {
        return domainLevelSignedJARsRoot;
    }
    
    /**
     * Adds more static content to the content served by the system adapter.
     * @param lookupURI
     * @param content
     */
    void addStaticSystemContent(final String lookupURI, StaticContent newContent) throws IOException {
        systemAdapter().addContentIfAbsent(lookupURI, newContent);
    }

    private static String chooseAlias(final DeploymentContext dc) {
        final String userSpecifiedAlias;
        return ((userSpecifiedAlias = extractUserProvidedAlias(dc)) != null)
                ? userSpecifiedAlias : DEFAULT_SIGNING_ALIAS;
    }

    private static String extractUserProvidedAlias(final DeploymentContext dc) {
        return dc.getAppProps().getProperty(SIGNING_ALIAS_PROPERTY_NAME);
    }

    private synchronized AppClientHTTPAdapter startSystemContentAdapter() {


        try {
            AppClientHTTPAdapter sysAdapter = new AppClientHTTPAdapter(
                    NamingConventions.JWSAPPCLIENT_SYSTEM_PREFIX,
                    new Properties(),
                    serverEnv.getDomainRoot(), 
                    new File(installRootURI),
                    iiopService,
                    orbFactory);

            requestDispatcher.registerEndpoint(
                    NamingConventions.JWSAPPCLIENT_SYSTEM_PREFIX,
                    sysAdapter,
                    null);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Registered system content adapter serving {0}", sysAdapter);
            }
            return sysAdapter;

        } catch (Exception e) {
            logger.log(Level.SEVERE, ERROR_STARTING_SYSTEM_ADAPTER, e);
            return null;
        }
    }

    void addContentIfAbsent(final Map<String,StaticContent> staticContent,
            final Map<String,DynamicContent> dynamicContent) throws IOException {
        systemAdapter().addContentIfAbsent(staticContent, dynamicContent);
    }
    
    /**
     * Records the need for signed copies of the GlassFish system JARs for the
     * specified signing alias.
     *
     * @param systemJARRelativeURIs populated with the relative URIs for the added content
     * @param signingAlias alias to use in signing the JARs
     * @return map from the key by which the content will be stored to the content for that key
     * @throws IOException
     */
    Map<String,StaticContent> addStaticSystemContent(
            final List<String> systemJARRelativeURIs,
            final String signingAlias) throws IOException {
        /*
         * This method builds the static content for a given signing alias to
         * be served by the "system" Grizzly
         * adapter which serves files from the installation, as opposed to
         * files from the domain or files from a specific app.
         */
        Map<String,StaticContent> result = new HashMap<String,StaticContent>();
        File gfClientJAR = gfClientJAR();

        final String classPathExpr = getGFClientModuleClassPath(gfClientJAR);
        final URI gfClientJARURI = gfClientJAR.toURI();

        result.put(systemPath(gfClientJARURI, signingAlias),
                systemJarSignedContent(gfClientJAR, signingAlias));

        if (classPathExpr != null) {
            for (String classPathElement : classPathExpr.split(" ")) {
                final URI uri = gfClientJARURI.resolve(classPathElement);
                final String systemPath = systemPath(uri, signingAlias);
                /*
                * There may be elements in the class path which do not exist
                * on some platforms.  So make sure the file exists before we offer
                * to serve it.
                */
                final File candidateFile = new File(uri);
                final String relativeSystemPath = relativeSystemPath(uri);
                if (candidateFile.exists() && 
                    ( ! candidateFile.isDirectory()) && ( ! DO_NOT_SERVE_LIST.contains(relativeSystemPath))) {
                    result.put(systemPath,
                            systemJarSignedContent(candidateFile, signingAlias));
                    systemJARRelativeURIs.add(relativeSystemPath(uri));
                }
            }
        }

        /*
         * Add the endorsed JARs to the system content.
         */
        final File endorsedDir = new File(modulesDir(), "endorsed");
        for (File endorsedJar : endorsedDir.listFiles(new FileFilter(){

                    @Override
                    public boolean accept(File pathname) {
                        return (pathname.isFile() && pathname.getName().endsWith(".jar"));
                    }
            })) {
            result.put(systemPath(endorsedJar.toURI()),
                    systemJarSignedContent(endorsedJar, signingAlias));
            systemJARRelativeURIs.add(relativeSystemPath(endorsedJar.toURI()));
        }
        return result;
    }

    File gfClientJAR() {
        return new File(
            libDir(),
            "gf-client.jar");
    }

    File gfClientModuleJAR() {
        return new File(
            modulesDir(),
            "gf-client-module.jar");
    }

    private synchronized File modulesDir() {
        return new File(new File(installRootURI), "modules");
    }

    private synchronized File libDir() {
        return new File(new File(installRootURI), "lib");
    }

    static String publicExtensionHref(final Extension ext) {
        return NamingConventions.JWSAPPCLIENT_SYSTEM_PREFIX + "/" + publicExtensionLookupURIText(ext);
    }

    static String publicExtensionLookupURIText(final Extension ext) {
        return NamingConventions.JWSAPPCLIENT_EXT_INTRODUCER + "/" +
                ext.getExtDirectoryNumber() + "/" +
                ext.getFile().getName();
    }

    private AutoSignedContent systemJarSignedContent (
            final File unsignedFile,
            final String signingAlias) throws FileNotFoundException {
        final String relativeURI = relativeSystemPath(unsignedFile.toURI());
        final File signedFile = new File(signedSystemContentAliasDir(signingAlias),
                relativeURI);
        return new AutoSignedContent(unsignedFile, signedFile, signingAlias, jarSigner, relativeURI,
                MANIFEST_APP_NAME_FOR_SYSTEM_FILES);
    }

    Map<String,DynamicContent> addDynamicSystemContent(final List<String> systemJARRelativeURIs,
            final String signingAlias) throws IOException {
        final Map<String,DynamicContent> result = new HashMap<String,DynamicContent>();
        final String template = JavaWebStartInfo.textFromURL(
                "/org/glassfish/appclient/server/core/jws/templates/systemJarsDocumentTemplate.jnlp");
        final StringBuilder sb = new StringBuilder();
        for (String relativeURIString : systemJARRelativeURIs) {
            sb.append("<jar href=\"").append(signingAlias).append("/")
                    .append(relativeURIString).append("\"/>").append(LINE_SEP);
        }
        
        final Properties p = new Properties();
        p.setProperty("system.jars", sb.toString());
        final String replacedText = Util.replaceTokens(template, p);
        result.put(NamingConventions.systemJNLPURI(signingAlias),
                new SimpleDynamicContentImpl(replacedText, "jnlp"));

        return result;
    }

    private String systemPath(final URI systemFileURI) {
        return //NamingConventions.JWSAPPCLIENT_SYSTEM_PREFIX + "/" +
                relativeSystemPath(systemFileURI);
    }

    String systemPath(final URI systemFileURI, final String signingAlias) {
        return signingAlias + "/" + relativeSystemPath(systemFileURI);
    }

    String systemPathInClientJNLP(final URI systemFileURI, final String signingAlias) {
        return "${request.scheme}://${request.host}:${request.port}" +
                NamingConventions.JWSAPPCLIENT_SYSTEM_PREFIX + "/" + 
                signingAlias + "/" + relativeSystemPath(systemFileURI);
    }
    
    private synchronized String relativeSystemPath(final URI systemFileURI) {
        return umbrellaRootURI.relativize(systemFileURI).getPath();
    }

    /**
     * Adds content on behalf of a single app client to the HTTP adapters
     * for the client and/or the containing EAR.
     * <p>
     * This method always creates at least one adapter for the client - one to
     * receive requests for the user-friendly context path.  (This is the
     * path either assigned by the developer or defaulted by the server based
     * on the application name and, for clients nested within an EAR, the
     * URI to the app client within the EAR.)  This adapter will only serve the
     * main generated JNLP document.  All other accesses to files that can be
     * downloaded will use the "user-unfriendly" context path.
     * <p>
     * For stand-alone
     * app clients it also creates an adapter for the "user-unfriendly" context
     * path.  This serves all content other than the main JNLP document.
     * <p>
     * For nested app clients this method will either create or reuse an
     * adapter which serves content for the EAR.  This includes content specific
     * to the app client being added as well as for library JARs, etc. that
     * could be common to more than one app client.
     *
     * @param appName
     * @param contributor
     * @param tokens
     * @param staticContent
     * @param dynamicContent
     * @throws EndpointRegistrationException
     * @throws IOException
     */
    public synchronized void addContentForAppClient(
            final String appName,
            final String clientURIWithinEAR,
            final AppClientServerApplication contributor, final Properties tokens,
            final Map<String,StaticContent> staticContent,
            final Map<String,DynamicContent> dynamicContent) throws EndpointRegistrationException, IOException {
        AppClientHTTPAdapter appAdapter = httpAdapters.get(appName);
        if (appAdapter == null) {
            /*
             * For a stand-alone app client, this is the adapter that will
             * serve all the content (except for the main JNLP docuemnt which the
             * user-friendly adapter will typically serve).  For a nested
             * app client, the app adapter is the EAR-level adapter.
             */
            appAdapter = addAppAdapter(appName, staticContent, dynamicContent,
                    tokens, contributor);
        } else {
            /*
             * This must be the 2nd through n-th nested app client in an EAR
             * because the app adapter already exists.
             */
            appAdapter.addContentIfAbsent(staticContent, dynamicContent);
        }

        /*
         * Add a new adapter for this client's user-friendly context root.
         */
        AppClientHTTPAdapter userFriendlyAppAdapter =
                addAdapterForUserFriendlyContextRoot(staticContent, dynamicContent,
                tokens, contributor);
        appClientDeployer.recordContextRoot(appName, clientURIWithinEAR, userFriendlyAppAdapter.contextRoot());
        logger.log(Level.FINE, "Registered at context roots {0},{1}",
                new Object[]{appAdapter.contextRoot(), userFriendlyAppAdapter.contextRoot()});
        
        addContributorToAppLevelAdapter(appName, contributor);
    }

    private synchronized AppClientHTTPAdapter createAndRegisterAdapter(
            final String contextRoot,
            final Map<String,StaticContent> staticContent,
            final Map<String,DynamicContent> dynamicContent,
            final Properties tokens,
            final AppClientServerApplication contributor) throws IOException, EndpointRegistrationException {

        final AppClientHTTPAdapter adapter = new AppClientHTTPAdapter(
                contextRoot, staticContent,
                dynamicContent, tokens,
                serverEnv.getDomainRoot(), 
                new File(installRootURI),
                iiopService,
                orbFactory);
        requestDispatcher.registerEndpoint(
                contextRoot,
                adapter,
                null);
        return adapter;
    }

    private synchronized AppClientHTTPAdapter addAdapterForUserFriendlyContextRoot(
            final Map<String,StaticContent> staticContent,
            final Map<String,DynamicContent> dynamicContent,
            final Properties tokens,
            final AppClientServerApplication contributor) throws IOException, EndpointRegistrationException {
        final String ufContextRoot = userFriendlyContextRoot(contributor);
        return createAndRegisterAdapter(ufContextRoot, staticContent, dynamicContent, tokens, contributor);
    }

    private synchronized AppClientHTTPAdapter systemAdapter() {
        if (systemAdapter == null) {
            systemAdapter = startSystemContentAdapter();
        }
        return systemAdapter;
    }

    private synchronized AppClientHTTPAdapter addAppAdapter(
            final String appName, 
            final Map<String,StaticContent> staticContent,
            final Map<String,DynamicContent> dynamicContent,
            final Properties tokens,
            final AppClientServerApplication contributor) throws IOException, EndpointRegistrationException {

        systemAdapter(); // Make sure it is started
        final String contextRoot = NamingConventions.contextRootForAppAdapter(appName);

        final AppClientHTTPAdapter adapter = createAndRegisterAdapter(
                contextRoot, staticContent,
                dynamicContent, tokens, contributor);
        httpAdapters.put(appName, adapter);
        return adapter;
    }

    public static String userFriendlyContextRoot(final AppClientServerApplication contributor) {
        return userFriendlyContextRoot(contributor.getDescriptor(), 
                contributor.dc().getAppProps());
    }

    public static String userFriendlyContextRoot(
            final ApplicationClientDescriptor acDesc, final Properties p) {

        String ufContextRoot = NamingConventions.defaultUserFriendlyContextRoot(
                acDesc);

        /*
         * See if the context root setting has one for this client.  The
         * format of the property setting is:
         *
         * Stand-alone client deployment: javaWebStartContextRoot=contextRoot
         * Nested in EAR: javaWebStartContextRoot.uri-to-client-without-.jar=contextRoot
         *
         * There can be multiple such javaWebStartContextRoot.xxx properties, one for
         * each nested app client in the EAR.
         */
        String overridingContextRoot = null;
        if (acDesc.getApplication().isVirtual()) {
            /*
             * Stand-alone case.
             */
            overridingContextRoot = p.getProperty(JAVA_WEB_START_CONTEXT_ROOT_PROPERTY_NAME);
        } else {
            /*
             * Nested app clients case.
             */
            final String uriToNestedClient = NamingConventions.uriToNestedClient(
                    acDesc);
            overridingContextRoot = p.getProperty(
                    JAVA_WEB_START_CONTEXT_ROOT_PROPERTY_NAME + "." + uriToNestedClient);
        }
        if (overridingContextRoot != null) {
            ufContextRoot = overridingContextRoot;
        }
        
        /*
         * Grizzly wants the context root to start with a slash.
         */
        if ( ! ufContextRoot.startsWith("/")) {
            ufContextRoot = "/" + ufContextRoot;
        }
        return ufContextRoot;
    }

    public synchronized AutoSignedContent appLevelSignedSystemContent(
            final String relativePathToSystemJar,
            final String alias) throws FileNotFoundException {
        /*
         * The key to the map is also the subpath to the file within the
         * domain's repository which holds signed system JARs.
         */
        final String key = keyToAppLevelSignedSystemContentMap(relativePathToSystemJar, alias);
        AutoSignedContent result = appLevelSignedSystemContent.get(key);
        if (result == null) {
            final File unsignedFile = new File(umbrellaRoot, relativePathToSystemJar);
            final File signedFile = new File(systemLevelSignedJARsRoot, key);
            result = new AutoSignedContent(unsignedFile, signedFile, alias, jarSigner, relativePathToSystemJar,
                    MANIFEST_APP_NAME_FOR_SYSTEM_FILES);
            appLevelSignedSystemContent.put(key, result);
        }
        return result;
    }

    private static String keyToAppLevelSignedSystemContentMap(
            final String relativePathToSystemJar,
            final String alias) {
        return alias + "/" + relativePathToSystemJar;
    }

    synchronized File signedSystemContentAliasDir(final String alias) {
        return new File(systemLevelSignedJARsRoot, alias);
    }

    public String contextRootForAppAdapter(final String appName) {
        final AppClientHTTPAdapter adapter = httpAdapters.get(appName);
        if (adapter != null) {
            return adapter.contextRoot();
        } else {
            return null;
        }
    }

    private synchronized void addContributorToAppLevelAdapter(
            final String appName,
            final AppClientServerApplication contributor) {
        /*
         * Record that the calling app client server app has contributed content
         * to the Grizzly adapter.
         */
        Set<AppClientServerApplication> contributorsToAppLevelAdapter = contributingAppClients.get(appName);
        if (contributorsToAppLevelAdapter == null) {
            contributorsToAppLevelAdapter = new HashSet<AppClientServerApplication>();
            contributingAppClients.put(appName, contributorsToAppLevelAdapter);
        }
        contributorsToAppLevelAdapter.add(contributor);
    }

    public synchronized void removeContentForAppClient(final String appName,
            final String clientURIWithinEAR,
            final AppClientServerApplication contributor) throws EndpointRegistrationException {

        /*
         * Remove the adapter for the user-friendly context root.
         */
        removeAdapter(userFriendlyContextRoot(contributor));

        removeContributorToAppLevelAdapter(appName, contributor);

        appClientDeployer.removeContextRoot(appName, clientURIWithinEAR);
    }
    
    private synchronized void removeContributorToAppLevelAdapter(
            final String appName,
            final AppClientServerApplication contributor) throws EndpointRegistrationException {

        /*
         * If this is the last contributor for this app-level adapter then
         * remove the app-level adapter also.
         */
        final Set<AppClientServerApplication> contributorsToAppLevelAdapter = contributingAppClients.get(appName);
        if (contributorsToAppLevelAdapter == null) {
            return;
        }
        contributorsToAppLevelAdapter.remove(contributor);
        if (contributorsToAppLevelAdapter.isEmpty()) {
            contributingAppClients.remove(appName);
            removeAdapter(NamingConventions.contextRootForAppAdapter(appName));
            httpAdapters.remove(appName);
        }
    }

    private synchronized void removeAdapter(final String contextRoot)
            throws EndpointRegistrationException {
        requestDispatcher.unregisterEndpoint(contextRoot);
    }

    private String getGFClientModuleClassPath(final File gfClientJAR) throws IOException {
        final JarFile jf = new JarFile(gfClientJAR);

        try {
          final Manifest mf = jf.getManifest();
          Attributes mainAttrs = mf.getMainAttributes();
          return mainAttrs.getValue(Attributes.Name.CLASS_PATH);
        } finally 
        {
          jf.close();
        }
    }

}

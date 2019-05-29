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

package org.glassfish.appclient.client.acc;

import org.glassfish.appclient.common.ACCAppClientArchivist;
import com.sun.enterprise.deployment.deploy.shared.MultiReadableArchive;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deploy.shared.FileArchive;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.archivist.AppClientArchivist;
import com.sun.enterprise.module.bootstrap.BootException;
import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.hk2.api.ServiceLocator;
import org.xml.sax.SAXParseException;

/**
 * Represents a generated JAR created during deployment corresponding to
 * the developer's original app client JAR or EAR.  Even if the facade object
 * represents an EAR facade, it uses the caller-supplied main class name
 * and/or caller-supplied app client name to select one of the app client
 * facades listed in the group facade.  That is, once fully initialized,
 * a given Facade instance represents the single app client to be launched.
 */
public class FacadeLaunchable implements Launchable {

    /** name of a manifest entry in an EAR facade listing the URIs of the individual app client facades in the group */
    public static final Attributes.Name GLASSFISH_APPCLIENT_GROUP = new Attributes.Name("GlassFish-AppClient-Group");

    /** name of a manifest entry in an app client facade indicating the app client's main class */
    public static final Attributes.Name GLASSFISH_APPCLIENT_MAIN_CLASS = new Attributes.Name("Glassfish-AppClient-Main-Class");

    /** name of a manifest entry in an app client facade listing the URI of the developer's original app client JAR */
    public static final Attributes.Name GLASSFISH_APPCLIENT = new Attributes.Name("GlassFish-AppClient");

    /** name of manifest entry in facade conveying the app name */
    public static final Attributes.Name GLASSFISH_APP_NAME = new Attributes.Name("GlassFish-App-Name");

    public static final ArchiveFactory archiveFactory = ACCModulesManager.getService(ArchiveFactory.class);
    private static final Logger logger = LogDomains.getLogger(FacadeLaunchable.class,
            LogDomains.ACC_LOGGER);

    private static final LocalStringManager localStrings = new LocalStringManagerImpl(FacadeLaunchable.class);
    private static final boolean isJWSLaunch = Boolean.getBoolean("appclient.is.jws");

    private final String mainClassNameToLaunch;
    private final URI[] classPathURIs;
    private ReadableArchive facadeClientRA;
    private MultiReadableArchive combinedRA;
    private static AppClientArchivist facadeArchivist = null;
    private ApplicationClientDescriptor acDesc = null;
    private ClassLoader classLoader = null;
    private final ServiceLocator habitat;
    private final String anchorDir;

    FacadeLaunchable(
            final ServiceLocator habitat,
            final Attributes mainAttrs, final ReadableArchive facadeRA,
            final String anchorDir) throws IOException, URISyntaxException {
        this(habitat,
                facadeRA, mainAttrs,
                openOriginalArchive(facadeRA, mainAttrs.getValue(GLASSFISH_APPCLIENT)),
                mainAttrs.getValue(GLASSFISH_APPCLIENT_MAIN_CLASS),
                anchorDir);
    }

    private static ReadableArchive openOriginalArchive(final ReadableArchive facadeArchive, final String relativeURIToOriginalJar) throws IOException, URISyntaxException {
        URI uriToOriginal = facadeArchive.getURI().resolve(relativeURIToOriginalJar);
        return archiveFactory.openArchive(uriToOriginal);
    }

    FacadeLaunchable(
            final ServiceLocator habitat,
            final ReadableArchive facadeClientRA,
            final Attributes mainAttrs,
            final ReadableArchive clientRA,
            final String mainClassNameToLaunch,
            final String anchorDir) throws IOException {
        super();
        this.facadeClientRA = facadeClientRA;
        this.combinedRA = openCombinedReadableArchive(habitat, facadeClientRA, clientRA);
        this.mainClassNameToLaunch = mainClassNameToLaunch;
        this.classPathURIs = toURIs(mainAttrs.getValue(Name.CLASS_PATH));
        this.habitat = habitat;
        this.anchorDir = anchorDir;
    }

    public URI getURI() {
        return facadeClientRA.getURI();
    }

    public String getAnchorDir() {
        return anchorDir;
    }

    private static MultiReadableArchive openCombinedReadableArchive(
            final ServiceLocator habitat,
            final ReadableArchive facadeRA,
            final ReadableArchive clientRA) throws IOException {
        final MultiReadableArchive combinedRA = habitat.getService(MultiReadableArchive.class);
        combinedRA.open(facadeRA.getURI(), clientRA.getURI());
        return combinedRA;
    }

    protected URI[] toURIs(final String uriList) {
        String[] uris = uriList.split(" ");
        URI[] result = new URI[uris.length];
        for (int i = 0; i < uris.length; i++) {
            result[i] = URI.create(uris[i]);
        }
        return result;
    }

    protected AppClientArchivist getArchivist() {
        return getArchivist(habitat);
    }

    protected synchronized static AppClientArchivist getArchivist(final ServiceLocator habitat) {
        if (facadeArchivist == null) {
            facadeArchivist = habitat.getService(ACCAppClientArchivist.class);
        }
        return facadeArchivist;
    }

    public void validateDescriptor() {
        getArchivist().validate(classLoader);
    }

    /**
     * Returns a Facade object for the specified app client group facade.
     * <p>
     * The caller-supplied information is used to select the first app client
     * facade in the app client group that matches either the main class or
     * the app client name.  If the caller-supplied values are both null then
     * the method returns the first app client facade in the group.  If the
     * caller passes at least one non-null selector (main class or app client
     * name) but no app client matches, the method returns null.
     *
     * @param groupFacadeURI URI to the app client group facade
     * @param callerSuppliedMainClassName main class name to find; null if
     * the caller does not require selection based on the main class name
     * @param callerSuppliedAppName (display) nane of the app client to find; null
     * if the caller does not require selection based on display name
     * @return a Facade object representing the selected app client facade;
     * null if at least one of callerSuppliedMainClasName and callerSuppliedAppName
     * is not null and no app client matched the selection criteria
     * @throws java.io.IOException
     * @throws com.sun.enterprise.module.bootstrap.BootException
     * @throws java.net.URISyntaxException
     * @throws javax.xml.stream.XMLStreamException
     */
    static FacadeLaunchable newFacade(
            final ServiceLocator habitat,
            final ReadableArchive facadeRA,
            final String callerSuppliedMainClassName,
            final String callerSuppliedAppName)
                throws IOException, BootException, URISyntaxException,
                XMLStreamException, SAXParseException, UserError {
        Manifest mf = facadeRA.getManifest();
        if (mf == null) {
            throw new UserError(MessageFormat.format(
                    logger.getResourceBundle().getString("appclient.noMFInFacade"),
                    facadeRA instanceof FileArchive ? 1 : 0,
                    new File(facadeRA.getURI().getPath()).getAbsolutePath()));
        }
        final Attributes mainAttrs = mf.getMainAttributes();
        FacadeLaunchable result = null;
        if (mainAttrs.containsKey(GLASSFISH_APPCLIENT)) {
            if ( ! (facadeRA instanceof HTTPInputArchive)) {
                result = new FacadeLaunchable(habitat, mainAttrs, facadeRA,
                        dirContainingStandAloneFacade(facadeRA));
            } else {
                result = new JWSFacadeLaunchable(habitat, mainAttrs, facadeRA);
            }
        } else {
            /*
             * The facade does not contain GlassFish-AppClient so if it is
             * a facade it must be an app client group facade.  Select
             * which app client facade within the group, if any, matches
             * the caller's selection criteria.
             */
            final String facadeGroupURIs = mainAttrs.getValue(GLASSFISH_APPCLIENT_GROUP);
            if (facadeGroupURIs != null) {
                result = selectFacadeFromGroup(
                        habitat, facadeRA.getURI(), archiveFactory,
                        facadeGroupURIs, callerSuppliedMainClassName,
                        callerSuppliedAppName, dirContainingClientFacadeInGroup(facadeRA));
            } else {
                return null;
            }
        }
        return result;
    }

    private static String dirContainingStandAloneFacade(final ReadableArchive facadeRA) throws URISyntaxException {
        final URI fileURI = new URI("file", facadeRA.getURI().getRawSchemeSpecificPart(), null);
        return new File(fileURI).getParent();
    }

    private static String dirContainingClientFacadeInGroup(final ReadableArchive groupFacadeRA) throws URISyntaxException {
        final String ssp = groupFacadeRA.getURI().getRawSchemeSpecificPart();
        final URI fileURI = new URI("file", ssp.substring(0, ssp.length() - ".jar".length()) + "/", null);
        return new File(fileURI).getAbsolutePath();
    }

    public Class getMainClass() throws ClassNotFoundException {
        return Class.forName(mainClassNameToLaunch, true, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Return the augmented descriptor constructed during deployment and
     * stored in the facade client JAR.
     * @param loader
     * @return
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXParseException
     */
    public ApplicationClientDescriptor getDescriptor(final URLClassLoader loader) throws IOException, SAXParseException {
        if (acDesc == null) {
            /*
             * To support managed beans, perform anno processing which requires
             * a class loader.  But we don't want to load the application
             * classes using the main class loader yet because we need to find
             * set up transformation of application classes by transformers from
             * persistence (perhaps).  So create a temporary loader just to
             * load the descriptor.
             */
            final AppClientArchivist archivist = getArchivist();
            /*
             * Anno processing is currently file-based.  But during Java Web
             * Start launches, the JARs which Java Web Start has downloaded are
             * not accessible as File objects.  Until the anno processing is
             * generalized we suppress the anno processing during Java Web
             * Start launches.
             */
            archivist.setAnnotationProcessingRequested( ! isJWSLaunch);

            final ACCClassLoader tempLoader = AccessController.doPrivileged(new PrivilegedAction<ACCClassLoader>() {

                @Override
                public ACCClassLoader run() {
                    return new ACCClassLoader(loader.getURLs(), loader.getParent());
                }
                
            });
            
            
            archivist.setClassLoader(tempLoader);

            acDesc = archivist.open(combinedRA, mainClassNameToLaunch);
            archivist.setDescriptor(acDesc);

//            acDesc = LaunchableUtil.openWithAnnoProcessingAndTempLoader(
//                    arch, loader, facadeClientRA, clientRA);
            
            Application.createVirtualApplication(null, acDesc.getModuleDescriptor());

            final Manifest facadeMF = combinedRA.getManifest();
            final Attributes mainAttrs = facadeMF.getMainAttributes();
            final String appName = mainAttrs.getValue(GLASSFISH_APP_NAME);
            acDesc.getApplication().setAppName(appName);

            /*
             * Save the class loader for later use.
             */
            this.classLoader = loader;
        }
        return acDesc;
    }

    public URI[] getClassPathURIs() {
        return classPathURIs;
    }

    private static FacadeLaunchable selectFacadeFromGroup(
            final ServiceLocator habitat,
            final URI groupFacadeURI, final ArchiveFactory af,
            final String groupURIs, final String callerSpecifiedMainClassName,
            final String callerSpecifiedAppClientName,
            final String anchorDir) throws IOException, BootException, URISyntaxException, SAXParseException, UserError {
        String[] archiveURIs = groupURIs.split(" ");
        /*
         * Search the app clients in the group in order, checking each for
         * a match on either the caller-specified main class or the caller-specified
         * client name.
         */
        if (archiveURIs.length == 0) {
            final String msg = MessageFormat.format(
                    logger.getResourceBundle().getString("appclient.noClientsInGroup"),
                    groupFacadeURI);
            throw new UserError(msg);
        }

        /*
         * Save the client names and main classes in case we need them in an
         * error to the user.
         */
        final List<String> knownClientNames = new ArrayList<String>();
        final List<String> knownMainClasses = new ArrayList<String>();

        for (String uriText : archiveURIs) {
            URI clientFacadeURI = groupFacadeURI.resolve(uriText);
            ReadableArchive clientFacadeRA = af.openArchive(clientFacadeURI);
            Manifest facadeMF = clientFacadeRA.getManifest();
            if (facadeMF == null) {
                throw new UserError(MessageFormat.format(
                        logger.getResourceBundle().getString("appclient.noMFInFacade"),
                        clientFacadeRA instanceof FileArchive ? 1 : 0,
                        new File(clientFacadeRA.getURI().getPath()).getAbsolutePath()));
            }
            Attributes facadeMainAttrs = facadeMF.getMainAttributes();
            if (facadeMainAttrs == null) {
                throw new UserError(MessageFormat.format(
                        logger.getResourceBundle().getString("appclient.MFMissingEntry"),
                        new File(clientFacadeRA.getURI().getPath()).getAbsolutePath(),
                        GLASSFISH_APPCLIENT));
            }
            final String gfAppClient = facadeMainAttrs.getValue(GLASSFISH_APPCLIENT);
            if (gfAppClient == null || gfAppClient.length() == 0) {
                throw new UserError(MessageFormat.format(
                        logger.getResourceBundle().getString("appclient.MFMissingEntry"),
                        new File(clientFacadeRA.getURI().getPath()).getAbsolutePath(),
                        GLASSFISH_APPCLIENT));
            }
            URI clientURI = clientFacadeURI.resolve(gfAppClient);
            ReadableArchive clientRA = af.openArchive(clientURI);
            if ( ! clientRA.exists()) {
                throw new UserError(MessageFormat.format(
                        logger.getResourceBundle().getString("appclient.missingClient"),
                        new File(clientRA.getURI().getSchemeSpecificPart()).getAbsolutePath()));
            }
            AppClientArchivist facadeClientArchivist = getArchivist(habitat);
            MultiReadableArchive combinedRA = openCombinedReadableArchive(habitat, clientFacadeRA, clientRA);
            final ApplicationClientDescriptor facadeClientDescriptor = facadeClientArchivist.open(combinedRA);
            final String moduleID = Launchable.LaunchableUtil.moduleID(
                    groupFacadeURI, clientURI, facadeClientDescriptor);

            final Manifest clientMF = clientRA.getManifest();
            if (clientMF == null) {
                throw new UserError(MessageFormat.format(
                        logger.getResourceBundle().getString("appclient.noMFInFacade"),
                        clientRA instanceof FileArchive ? 1 : 0,
                        new File(clientRA.getURI().getSchemeSpecificPart()).getAbsolutePath()));
            }
            Attributes mainAttrs = clientMF.getMainAttributes();
            if (mainAttrs == null) {
                throw new UserError(MessageFormat.format(
                        logger.getResourceBundle().getString("appclient.MFMissingEntry"),
                        new File(clientFacadeRA.getURI().getPath()).getAbsolutePath(),
                        Attributes.Name.MAIN_CLASS.toString()));
            }
            final String clientMainClassName = mainAttrs.getValue(Attributes.Name.MAIN_CLASS);
            if (clientMainClassName == null || clientMainClassName.length()== 0) {
                throw new UserError(MessageFormat.format(
                        logger.getResourceBundle().getString("appclient.MFMissingEntry"),
                        new File(clientFacadeRA.getURI().getPath()).getAbsolutePath(),
                        Attributes.Name.MAIN_CLASS.toString()));
            }
            knownMainClasses.add(clientMainClassName);

            knownClientNames.add(moduleID);

            /*
             * Look for an entry corresponding to the
             * main class or app name the caller requested.  Treat as a%
             * special case if the user specifies no main class and no
             * app name - use the first app client present.  Also use the
             * first app client if there is only one present; warn if
             * the user specified a main class or a name but it does not
             * match the single app client that's present.
             */
            
            FacadeLaunchable facade = null;

            /*
             * Earlier releases used the -mainclass option on the appclient
             * command to specify an artitrary class, not restricted to the
             * the main class as specified as the Main-Class in an app client
             * JAR's manifest.  To preserve backward compatibility we need to
             * do the same.  
             */

            if (Launchable.LaunchableUtil.matchesAnyClass(clientRA, callerSpecifiedMainClassName)) {
                facade = new FacadeLaunchable(habitat, clientFacadeRA,
                        facadeMainAttrs, clientRA, callerSpecifiedMainClassName,
                        anchorDir);
                /*
                 * If the caller-specified class name does not match the
                 * Main-Class setting for this client archive then inform the user.
                 */
                if ( ! clientMainClassName.equals(callerSpecifiedMainClassName)) {
                    logger.log(Level.INFO, MessageFormat.format(
                            logger.getResourceBundle().getString("appclient.foundMainClassDiffFromManifest"),
                            new Object[] {
                                    groupFacadeURI,
                                    moduleID,
                                    callerSpecifiedMainClassName,
                                    clientMainClassName
                            }));
                }
            } else if (Launchable.LaunchableUtil.matchesName(
                            moduleID, groupFacadeURI, facadeClientDescriptor, callerSpecifiedAppClientName)) {
                /*
                 * Get the main class name from the matching client JAR's manifest.
                 */
                facade = new FacadeLaunchable(habitat, clientFacadeRA,
                        facadeMainAttrs, clientRA,
                        clientMainClassName,
                        anchorDir);
            } else if (archiveURIs.length == 1) {
                /*
                 * If only one client exists, use the main class recorded in
                 * the group facade unless the caller specified one.
                 */

                facade = new FacadeLaunchable(habitat, clientFacadeRA, facadeMainAttrs,
                        clientRA,
                        (callerSpecifiedMainClassName != null ?
                            callerSpecifiedMainClassName : facadeMainAttrs.getValue(GLASSFISH_APPCLIENT_MAIN_CLASS)),
                        anchorDir);
                /*
                 * If the user specified a main class or an app name then warn
                 * if that value does not match the one client we found - but
                 * go ahead an run it anyway.
                 */
                if ((callerSpecifiedMainClassName != null &&
                         ! clientMainClassName.equals(callerSpecifiedMainClassName) )
                    ||
                    (callerSpecifiedAppClientName != null &&
                        ! Launchable.LaunchableUtil.matchesName(moduleID,
                            groupFacadeURI, facadeClientDescriptor, callerSpecifiedAppClientName))) {

                    logger.log(Level.WARNING, MessageFormat.format(
                            logger.getResourceBundle().getString("appclient.singleNestedClientNoMatch"),
                            new Object[]{groupFacadeURI, knownClientNames.toString(),
                                         knownMainClasses.toString(),
                                         callerSpecifiedMainClassName,
                                         callerSpecifiedAppClientName}));
                }
            }
            if (facade != null) {
                return facade;
            }
        }
        /*
         * No client facade matched the caller-provided selection (either
         * main class name or app client name), or there are multiple clients
         * but the caller did not specify either mainClass or name.
         * Yet we know we're working
         * with a group facade, so report the failure to find a matching
         * client as an error.
         */
        String msg;
        if ((callerSpecifiedAppClientName == null) &&
            (callerSpecifiedMainClassName == null)) {
            final String format = logger.getResourceBundle().getString(
                    "appclient.multClientsNoChoice");
            msg = MessageFormat.format(format, knownMainClasses.toString(), knownClientNames.toString());
        } else {
            final String format = logger.getResourceBundle().getString(
                    "appclient.noMatchingClientInGroup");
            msg = MessageFormat.format(format, groupFacadeURI, callerSpecifiedMainClassName,
                        callerSpecifiedAppClientName, knownMainClasses.toString(),
                        knownClientNames.toString());
        }
        throw new UserError(msg);
    }
}

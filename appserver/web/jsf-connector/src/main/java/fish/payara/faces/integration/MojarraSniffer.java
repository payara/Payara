/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2025 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.faces.integration;

import com.sun.enterprise.util.Utility;
import com.sun.faces.config.FacesInitializer;
import com.sun.faces.config.FacesInitializer2;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletContainerInitializer;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.hk2.classmodel.reflect.Types;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.internal.deployment.GenericSniffer;
import org.glassfish.web.loader.ServletContainerInitializerUtil;
import org.jvnet.hk2.annotations.Service;

import javax.enterprise.deploy.shared.ModuleType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service(name = "mojarra-sniffer")
@Singleton
public class MojarraSniffer extends GenericSniffer {

    private static final String ALLOW_FACES_CDI_INITIALISATION_SYSTEM_PROPERTY = "fish.payara.faces.integration.allowFacesCdiInitialisation";
    private static final String ALLOW_FACES_CDI_INITIALISATION_DEPLOY_PROPERTY = "allowFacesCdiInitialisation";

    // If faces is detected, we need to force Weld to initialise (even in cases where bean discovery has been disabled)
    private static final String[] containers = { "org.glassfish.weld.WeldContainer" };
    private static final Logger logger = Logger.getLogger(MojarraSniffer.class.getName());

    public MojarraSniffer() {
        super("cdi", null, null);
    }

    @Override
    public String[] getContainersNames() {
        return containers;
    }

    @Override
    public boolean supportsArchiveType(ArchiveType archiveType) {
        if (archiveType.toString().equals(ModuleType.WAR.toString())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean handles(DeploymentContext context) {
        if (skipHandler(context)) {
            return false;
        }

        ArchiveType archiveType = habitat.getService(ArchiveType.class, context.getArchiveHandler().getArchiveType());
        if (archiveType != null && !supportsArchiveType(archiveType)) {
            return false;
        }
        if (archiveType != null) {
            context.getSource().setExtraData(ArchiveType.class, archiveType);
        }

        logger.fine("Checking if application has a faces-config.xml file...");
        boolean handles = hasFacesXml(context);

        // Don't bother scanning the classes if we've already found a faces-config.xml file
        if (!handles) {
            logger.fine("No faces-config.xml file found, checking if application contains a class which the Mojarra ServletContainerInitializers should handle");
            handles = hasFacesEnablingClass(context);
        }

        if (handles) {
            logger.fine("Faces content has been detected in application, setting " + ALLOW_FACES_CDI_INITIALISATION_SYSTEM_PROPERTY + " to true");
            context.addTransientAppMetaData(ALLOW_FACES_CDI_INITIALISATION_SYSTEM_PROPERTY, Boolean.TRUE);
            return true;
        }

        logger.fine("No Faces content detected");
        return false;
    }

    private boolean skipHandler(DeploymentContext context) {
        boolean skip = true;
        if (Boolean.getBoolean(ALLOW_FACES_CDI_INITIALISATION_SYSTEM_PROPERTY)) {
            skip = false;
        }

        // Deploy command property takes precedence over system property
        Object propValue = context.getAppProps().get(ALLOW_FACES_CDI_INITIALISATION_DEPLOY_PROPERTY);
        if (propValue != null) {
            skip = !Boolean.parseBoolean((String) propValue);
        }

        return skip;
    }

    private boolean hasFacesXml(DeploymentContext context) {
        return isEntryPresent(context.getSource(), "WEB-INF/faces-config.xml");
    }

    private boolean isEntryPresent(ReadableArchive archive, String entry) {
        boolean entryPresent = false;
        try {
            logger.finer("Checking if " + entry + " exists in " + archive.getName());
            entryPresent = archive.exists(entry);
        } catch (IOException ioException) {
            logger.log(Level.FINE, "Ignoring exception encountered while checking if faces-config.xml file is present", ioException);
        }
        return entryPresent;
    }

    private boolean hasFacesEnablingClass(DeploymentContext context) {
        boolean result = false;

        List<ServletContainerInitializer> facesInitialisers = getMojarraServletContextInitialisers(context);

        Map<Class<?>, List<Class<? extends ServletContainerInitializer>>> interestList =
                ServletContainerInitializerUtil.getInterestList(facesInitialisers);

        Map<Class<? extends ServletContainerInitializer>, Set<Class<?>>> initialiserList =
                ServletContainerInitializerUtil.getInitializerList(
                        facesInitialisers, interestList,
                        getTypes(context),
                        Utility.getClassLoader(), false);

        for (Map.Entry<Class<? extends ServletContainerInitializer>, Set<Class<?>>> initialiserEntry : initialiserList.entrySet()) {
            if (initialiserEntry.getValue() != null && !initialiserEntry.getValue().isEmpty()) {
                if (logger.isLoggable(Level.FINE)) {
                    List<String> classNames = new ArrayList<>();
                    initialiserEntry.getValue().stream().forEach(clazz -> classNames.add(clazz.getName()));
                    logger.fine(initialiserEntry.getKey().getName()
                            + " container initialiser has an interest in the following classes detected within the application: "
                            + Arrays.toString(classNames.toArray()));
                }
                result = true;
                break;
            }
        }

        return result;
    }

    private List<ServletContainerInitializer> getMojarraServletContextInitialisers(DeploymentContext context) {
        ArrayList<ServletContainerInitializer> facesInitialisers = new ArrayList<>();

        logger.finer("Getting Mojarra FacesInitializer and FacesInitializer2 servlet context initialisers");
        Iterable<ServletContainerInitializer> servletContainerInitialisers = ServiceLoader.load(ServletContainerInitializer.class);
        for (ServletContainerInitializer servletContainerInitialiser : servletContainerInitialisers) {
            if (servletContainerInitialiser instanceof FacesInitializer || servletContainerInitialiser instanceof FacesInitializer2) {
                logger.finer("Found " + servletContainerInitialiser.getClass().getName());
                facesInitialisers.add(servletContainerInitialiser);
            }
        }

        return facesInitialisers;
    }

    private Types getTypes(DeploymentContext context) {
        String metadataKey = Types.class.getName();

        Types types = (Types) context.getTransientAppMetadata().get(metadataKey);
        while (types == null) {
            context = ((ExtendedDeploymentContext) context).getParentContext();
            if (context != null) {
                types = (Types) context.getTransientAppMetadata().get(metadataKey);
            } else {
                break;
            }
        }

        if (types == null) {
            logger.fine("No types found!");
        }

        return types;
    }


}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.enterprise.config.serverbeans;

import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.RefContainer;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.ServerRef;
import com.sun.enterprise.config.serverbeans.customvalidators.NotDuplicateTargetName;
import com.sun.enterprise.config.serverbeans.customvalidators.NotTargetKeyword;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import org.glassfish.api.Param;
import org.glassfish.api.admin.config.Named;
import org.glassfish.api.admin.config.PropertiesDesc;
import static org.glassfish.config.support.Constants.NAME_SERVER_REGEX;
import org.glassfish.api.admin.config.ReferenceContainer;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
@Configured
@NotDuplicateTargetName(message="{dg.duplicate.name}", payload=DeploymentGroup.class)
public interface DeploymentGroup extends Named, Payload, RefContainer, PropertyBag, ReferenceContainer {
    
    /**
     * Sets the deployment group name
     * @param value cluster name
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="name", primary = true)
    @Override
    public void setName(String value) throws PropertyVetoException;

    @NotTargetKeyword(message="{dg.reserved.name}", payload=DeploymentGroup.class)
    @Pattern(regexp=NAME_SERVER_REGEX, message="{dg.invalid.name}", payload=DeploymentGroup.class)
    @Override
    public String getName();
    
    /**
     * Gets the value of the serverRef property.
     *
     * List of servers in the cluster
     *
     * @return list of configured {@link ServerRef }
     */
    @Element
    List<DGServerRef> getDGServerRef();
    
    @DuckTyped
    List<Server> getInstances();

    @DuckTyped
    public DGServerRef getDGServerRefByRef(String ref);

    @DuckTyped
    ApplicationRef getApplicationRef(String appName);

    @DuckTyped
    ResourceRef getResourceRef(String refName);

    @DuckTyped
    boolean isResourceRefExists(String refName);

    @DuckTyped
    void createResourceRef(String enabled, String refName) throws TransactionFailure;

    @DuckTyped
    void deleteResourceRef(String refName) throws TransactionFailure;

    /**
     * Returns the DG configuration reference
     * @return the config-ref attribute
     */
    @DuckTyped
    @Override
    String getReference();

    @DuckTyped
    @Override
    boolean isServer();

    @DuckTyped
    @Override
    boolean isDas();

    @DuckTyped
    @Override
    boolean isDeploymentGroup();

    @DuckTyped
    @Override
    boolean isInstance();
    
    /**
     *	Properties as per {@link org.jvnet.hk2.config.types.PropertyBag}
     * @return the list of properties
     */
    @Element
    @PropertiesDesc(props={})
    @Override
    List<Property> getProperty();


    class Duck {
        public static boolean isServer(DeploymentGroup me)  { return false; }
        public static boolean isInstance(DeploymentGroup me) { return false; }
        public static boolean isDas(DeploymentGroup me) { return false; }
        public static boolean isDeploymentGroup(DeploymentGroup me) { return true; }

        public static String getReference(DeploymentGroup me) {
            for (Server server : me.getInstances()) {
                return server.getConfigRef();
            }
            return "";
        }

        public static List<Server> getInstances(DeploymentGroup me) {

            Dom clusterDom = Dom.unwrap(me);
            Domain domain = clusterDom.getHabitat().getService(Domain.class);

            ArrayList<Server> instances = new ArrayList<>();
            for (DGServerRef sRef : me.getDGServerRef()) {
                Server svr =  domain.getServerNamed(sRef.getRef());
                // the instance's domain.xml only has its own server 
                // element and not other server elements in the cluster 
                if (svr != null) {
                    instances.add(domain.getServerNamed(sRef.getRef()));
                }
            }
            return instances;
        }

        public static DGServerRef getDGServerRefByRef(DeploymentGroup me, String name) {
            for (DGServerRef ref : me.getDGServerRef()) {
                if (ref.getRef().equals(name)) {
                    return ref;
                }
            }
            return null;
        }

        public static ApplicationRef getApplicationRef(DeploymentGroup me,
                String appName) {
            for (ApplicationRef appRef : me.getApplicationRef()) {
                if (appRef.getRef().equals(appName)) {
                    return appRef;
                }
            }
            return null;
        }

        public static ResourceRef getResourceRef(DeploymentGroup me, String refName) {
            for (ResourceRef ref : me.getResourceRef()) {
                if (ref.getRef().equals(refName)) {
                    return ref;
                }
            }
            return null;
        }


        public static boolean isResourceRefExists(DeploymentGroup me, String refName) {
            return getResourceRef(me, refName) != null;
        }

        public static void deleteResourceRef(DeploymentGroup me, String refName) throws TransactionFailure {
            final ResourceRef ref = getResourceRef(me, refName);
            if (ref != null) {
                ConfigSupport.apply((DeploymentGroup param) -> param.getResourceRef().remove(ref), me);
            }
        }

        public static void createResourceRef(DeploymentGroup me, final String enabled, final String refName) throws TransactionFailure {

            ConfigSupport.apply((DeploymentGroup param) -> {
                ResourceRef newResourceRef = param.createChild(ResourceRef.class);
                newResourceRef.setEnabled(enabled);
                newResourceRef.setRef(refName);
                param.getResourceRef().add(newResourceRef);
                return newResourceRef;
            }, me);
        }
    } 
    
}

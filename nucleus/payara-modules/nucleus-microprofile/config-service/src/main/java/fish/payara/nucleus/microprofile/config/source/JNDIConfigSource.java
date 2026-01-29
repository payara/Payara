/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.microprofile.config.source;

import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.config.serverbeans.ServerTags;
import java.beans.PropertyVetoException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.glassfish.internal.api.Globals;
import org.glassfish.resourcebase.resources.admin.cli.ResourceUtil;
import org.glassfish.resourcebase.resources.api.ResourceStatus;
import org.glassfish.resources.admin.cli.CustomResourceManager;
import org.glassfish.resources.admin.cli.ResourceConstants;
import static org.glassfish.resources.admin.cli.ResourceConstants.JNDI_NAME;
import org.glassfish.resources.config.CustomResource;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * A Configuration source that retrieved a String property from JNDI If the JNDI
 * entry is not a String null is returned.
 *
 * @author Steve Millidge (Payara Foundation)
 */
public class JNDIConfigSource extends PayaraConfigSource {

    @Override
    public Map<String, String> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public int getOrdinal() {
        String storedOrdinal = getValue("config_ordinal");
        if (storedOrdinal != null) {
            return Integer.parseInt(storedOrdinal);
        }
        return Integer.parseInt(configService.getMPConfig().getJndiOrdinality());
    }

    @Override
    public String getValue(String propertyName) {
        String result = null;
        try {
            InitialContext ctx = new InitialContext();
            Object jndiObj = ctx.lookup(propertyName);
            result = jndiObj.toString();
        } catch (NamingException ex) {
            // ignore who cares we don;t have the property but another source may
        }
        return result;
    }

    @Override
    public String getName() {
        return "JNDI";
    }

    public boolean setValue(String propertyName, String propertyValue, String target) {
        boolean result = false;
        HashMap<String, String> attrList = new HashMap<>();
        attrList.put("factory-class", "org.glassfish.resources.custom.factory.PrimitivesAndStringFactory");
        attrList.put("res-type", "java.lang.String");
        attrList.put(ResourceConstants.ENABLED, Boolean.TRUE.toString());
        attrList.put(JNDI_NAME, propertyName);
        attrList.put(ServerTags.DESCRIPTION, "MicroProfile Config property for " + propertyName);

        Properties props = new Properties();

        props.put("value", propertyValue);

        try {
            CustomResourceManager customResMgr = Globals.getDefaultHabitat().getService(CustomResourceManager.class);
            ResourceStatus status = customResMgr.create(domainConfiguration.getResources(), attrList, props, target);
            if (status.getStatus() == ResourceStatus.SUCCESS) {
                result = true;
            } else {
                if (status.isAlreadyExists()) {
                    Logger.getLogger(JNDIConfigSource.class.getName()).log(Level.WARNING, "Unable to set MicroProfile JNDI Config property as it already exists please delete it using delete-config-property --source jndi --propertyname {0}", propertyName);                    
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(JNDIConfigSource.class.getName()).log(Level.WARNING, "Unable to set MicroProfile JNDI Config property " + propertyName, ex);
        }
        return result;
    }

    public void deleteValue(final String propertyName, String target) throws TransactionFailure {
        // remove the resource reference
        ResourceUtil resourceUtil = Globals.getDefaultHabitat().getService(ResourceUtil.class);
        resourceUtil.deleteResourceRef(propertyName, target);

        ConfigSupport.apply(new SingleConfigCode<Resources>() {

            public Object run(Resources param) throws PropertyVetoException,
                    TransactionFailure {
                CustomResource resource = (CustomResource) domainConfiguration.getResources().getResourceByName(CustomResource.class, propertyName);
                if (resource != null && resource.getJndiName().equals(propertyName)) {
                    return param.getResources().remove(resource);
                }
                return null;
            }
        }, domainConfiguration.getResources());
    }

}

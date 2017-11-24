/*
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.jmx.monitoring.configuration;

import fish.payara.nucleus.notification.configuration.Notifier;
import java.beans.PropertyVetoException;
import java.util.List;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

/**
 * @since 4.1.1.163
 * @author savage
 */
@Configured
public interface MonitoringServiceConfiguration extends ConfigBeanProxy, ConfigExtension, PropertyBag {

    /**
     * Boolean value determining if the service is enabled or disabled.
     *  Default value is false. 
     * @return 
     */
    @Attribute(defaultValue="false")
    String getEnabled();
    void setEnabled(String value) throws PropertyVetoException;

    /**
     * Boolean value determining if bootAMX is invoked by the service.
     *  Default value is false. 
     * @return 
     */
    @Attribute(defaultValue="false")
    String getAmx();
    void setAmx(String value) throws PropertyVetoException;

    /**
     * Frequency of log messages.
     *  Default value is 15 
     * @return 
     */
    @Attribute(defaultValue="15")
    String getLogFrequency();
    void setLogFrequency(String value) throws PropertyVetoException;

    /**
     * TimeUnit for frequency of log messages.
     *  Default value is TimeUnit.SECONDS 
     * @return 
     */
    @Attribute(defaultValue="SECONDS")
    String getLogFrequencyUnit();
    void setLogFrequencyUnit(String value) throws PropertyVetoException;
  
    /**
     * Properties listed in the domain.xml.
     *  Returns a list of properties which are present in the configuration block
     * @return 
     */
    @Element
    @Override
    List<Property> getProperty();
    
    /**
     * Returns a list of the notifiers configured with the monitoring service
     * @since 4.1.2.174
     * @return 
     */
    @Element("*")
    List<Notifier> getNotifierList();

    /**
     * Gets a specific notifier
     * @since 4.1.2.174
     * @param <T>
     * @param type The class name of the notifier to get
     * @return 
     */
    @DuckTyped
    <T extends Notifier> T getNotifierByType(Class type);

    class Duck {
        
        public static <T extends Notifier> T getNotifierByType(MonitoringServiceConfiguration config, Class<T> type) {
            for (Notifier notifier : config.getNotifierList()) {
                try {
                    return type.cast(notifier);
                } catch (Exception e) {
                    // ignore, not the right type.
                }
            }
            return null;
        }

    }
    
}

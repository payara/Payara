/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.healthcheck.configuration;

import java.beans.PropertyVetoException;
import javax.xml.bind.annotation.XmlAttribute;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;

/**
 *
 * @author Susan Rai
 */
@Configured
public interface MonitoredMetric extends ConfigBeanProxy, ConfigExtension {

    @XmlAttribute(required = true)
    @Attribute(required = true)
    String getMetricName();
    void setMetricName(String value) throws PropertyVetoException;
    
    @XmlAttribute(required = false)
    @Attribute(required = false)
    String getDescription();
    void setDescription(String value) throws PropertyVetoException;
    
    @DuckTyped
    boolean equals(MonitoredMetric metric);

    public class Duck {
        public static boolean equals(MonitoredMetric metric1, MonitoredMetric metric2) {
            return metric1.getMetricName().equals(metric2.getMetricName());
        }
    }

}

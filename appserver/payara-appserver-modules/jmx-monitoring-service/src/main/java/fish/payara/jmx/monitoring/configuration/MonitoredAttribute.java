package fish.payara.jmx.monitoring.configuration;

import java.beans.PropertyVetoException;
import javax.xml.bind.annotation.XmlAttribute;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

@Configured
public interface MonitoredAttribute extends ConfigBeanProxy, ConfigExtension {
    
    @XmlAttribute(required = true)
    @Attribute(required = true)
    public String getAttributeName();
    public void setAttributeName(String value) throws PropertyVetoException;
    
    @XmlAttribute(required = true)
    @Attribute(required = true)
    public String getObjectName();
    public void setObjectName(String value) throws PropertyVetoException;
    
    @XmlAttribute(required = false)
    @Attribute(required = false)
    public String getDescription();
    public void setDescription(String value) throws PropertyVetoException;

}

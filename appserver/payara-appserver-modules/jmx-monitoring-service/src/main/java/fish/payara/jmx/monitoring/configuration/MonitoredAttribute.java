package fish.payara.jmx.monitoring.configuration;

import java.beans.PropertyVetoException;
import javax.xml.bind.annotation.XmlAttribute;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;

@Configured
public interface MonitoredAttribute extends ConfigBeanProxy, ConfigExtension {

    @XmlAttribute(required = true)
    @Attribute(required = true)
    String getAttributeName();
    void setAttributeName(String value) throws PropertyVetoException;

    @XmlAttribute(required = true)
    @Attribute(required = true)
    String getObjectName();
    void setObjectName(String value) throws PropertyVetoException;

    @XmlAttribute(required = false)
    @Attribute(required = false)
    String getDescription();
    void setDescription(String value) throws PropertyVetoException;

    @DuckTyped
    boolean equals(MonitoredAttribute attr);

    public class Duck {
        public static boolean equals(MonitoredAttribute attr1, MonitoredAttribute attr2) {
            return attr1.getObjectName().equals(attr2.getObjectName())
                    && attr1.getAttributeName().equals(attr2.getAttributeName());
        }
    }

}

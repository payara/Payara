
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of lite-member complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="lite-member">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="enabled" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "lite-member", namespace = "http://www.hazelcast.com/schema/config")
public class LiteMember {

    @XmlAttribute(name = "enabled", required = true)
    protected boolean enabled;

    /**
     * Gets the value of property enabled.
     * 
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the value of property enabled.
     * 
     */
    public void setEnabled(boolean value) {
        this.enabled = value;
    }

}

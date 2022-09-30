
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of tls-authentication complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="tls-authentication">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.hazelcast.com/schema/config}basic-authentication">
 *       &lt;attribute name="roleAttribute" type="{http://www.w3.org/2001/XMLSchema}string" default="cn" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tls-authentication", namespace = "http://www.hazelcast.com/schema/config")
public class TlsAuthentication
    extends BasicAuthentication
{

    @XmlAttribute(name = "roleAttribute")
    protected String roleAttribute;

    /**
     * Gets the value of property roleAttribute.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRoleAttribute() {
        if (roleAttribute == null) {
            return "cn";
        } else {
            return roleAttribute;
        }
    }

    /**
     * Sets the value of property roleAttribute.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRoleAttribute(String value) {
        this.roleAttribute = value;
    }

}


package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of instance-permission complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="instance-permission">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.hazelcast.com/schema/config}base-permission">
 *       &lt;sequence>
 *         &lt;element name="actions" type="{http://www.hazelcast.com/schema/config}actions"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "instance-permission", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "actions"
})
public class InstancePermission
    extends BasePermission
{

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected Actions actions;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    /**
     * Gets the value of property actions.
     * 
     * @return
     *     possible object is
     *     {@link Actions }
     *     
     */
    public Actions getActions() {
        return actions;
    }

    /**
     * Sets the value of property actions.
     * 
     * @param value
     *     allowed object is
     *     {@link Actions }
     *     
     */
    public void setActions(Actions value) {
        this.actions = value;
    }

    /**
     * Gets the value of property name.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of property name.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

}

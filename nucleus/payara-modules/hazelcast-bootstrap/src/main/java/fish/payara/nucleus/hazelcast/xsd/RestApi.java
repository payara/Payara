
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 Controls access to Hazelcast HTTP REST API.
 *                 The methods available through REST API are grouped to several REST endpoint groups.
 *                 There are 2 levels of configuration:
 *                  - a global switch which enables/disables access to the REST API;
 *                  - REST endpoint group level switch. It is used to check which groups are allowed once the global switch is enabled.
 *             
 * 
 * <p>Java Class of rest-api complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="rest-api">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element name="endpoint-group" type="{http://www.hazelcast.com/schema/config}endpoint-group" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/choice>
 *       &lt;attribute name="enabled" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "rest-api", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "endpointGroup"
})
public class RestApi {

    @XmlElement(name = "endpoint-group", namespace = "http://www.hazelcast.com/schema/config")
    protected List<EndpointGroup> endpointGroup;
    @XmlAttribute(name = "enabled", required = true)
    protected boolean enabled;

    /**
     * Gets the value of the endpointGroup property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the endpointGroup property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEndpointGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link EndpointGroup }
     * 
     * 
     */
    public List<EndpointGroup> getEndpointGroup() {
        if (endpointGroup == null) {
            endpointGroup = new ArrayList<EndpointGroup>();
        }
        return this.endpointGroup;
    }

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

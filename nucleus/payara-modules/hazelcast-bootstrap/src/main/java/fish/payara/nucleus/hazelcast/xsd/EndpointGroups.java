
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of endpoint-groups complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="endpoint-groups">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="endpoint-group" type="{http://www.hazelcast.com/schema/config}endpoint-group"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "endpoint-groups", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "endpointGroup"
})
public class EndpointGroups {

    @XmlElement(name = "endpoint-group", namespace = "http://www.hazelcast.com/schema/config")
    protected List<EndpointGroup> endpointGroup;

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

}

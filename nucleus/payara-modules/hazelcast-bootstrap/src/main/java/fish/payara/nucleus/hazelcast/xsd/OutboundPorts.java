
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of outbound-ports complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="outbound-ports">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ports" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "outbound-ports", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "ports"
})
public class OutboundPorts {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected List<String> ports;

    /**
     * Gets the value of the ports property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the ports property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPorts().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getPorts() {
        if (ports == null) {
            ports = new ArrayList<String>();
        }
        return this.ports;
    }

}

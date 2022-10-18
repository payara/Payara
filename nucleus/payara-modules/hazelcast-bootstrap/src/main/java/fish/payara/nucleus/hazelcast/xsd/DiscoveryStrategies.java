
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java Class of discovery-strategies complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="discovery-strategies">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="node-filter" type="{http://www.hazelcast.com/schema/config}discovery-node-filter" minOccurs="0"/>
 *         &lt;element name="discovery-strategy" type="{http://www.hazelcast.com/schema/config}discovery-strategy" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "discovery-strategies", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "nodeFilter",
    "discoveryStrategy"
})
public class DiscoveryStrategies {

    @XmlElement(name = "node-filter", namespace = "http://www.hazelcast.com/schema/config")
    protected DiscoveryNodeFilter nodeFilter;
    @XmlElement(name = "discovery-strategy", namespace = "http://www.hazelcast.com/schema/config")
    protected List<DiscoveryStrategy> discoveryStrategy;

    /**
     * Gets the value of property nodeFilter.
     * 
     * @return
     *     possible object is
     *     {@link DiscoveryNodeFilter }
     *     
     */
    public DiscoveryNodeFilter getNodeFilter() {
        return nodeFilter;
    }

    /**
     * Sets the value of property nodeFilter.
     * 
     * @param value
     *     allowed object is
     *     {@link DiscoveryNodeFilter }
     *     
     */
    public void setNodeFilter(DiscoveryNodeFilter value) {
        this.nodeFilter = value;
    }

    /**
     * Gets the value of the discoveryStrategy property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the discoveryStrategy property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDiscoveryStrategy().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DiscoveryStrategy }
     * 
     * 
     */
    public List<DiscoveryStrategy> getDiscoveryStrategy() {
        if (discoveryStrategy == null) {
            discoveryStrategy = new ArrayList<DiscoveryStrategy>();
        }
        return this.discoveryStrategy;
    }

}

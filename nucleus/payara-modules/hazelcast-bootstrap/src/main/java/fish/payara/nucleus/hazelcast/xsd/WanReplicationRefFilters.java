
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of wan-replication-ref-filters complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="wan-replication-ref-filters">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="filter-impl" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "wan-replication-ref-filters", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "filterImpl"
})
public class WanReplicationRefFilters {

    @XmlElement(name = "filter-impl", namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected List<String> filterImpl;

    /**
     * Gets the value of the filterImpl property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the filterImpl property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFilterImpl().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getFilterImpl() {
        if (filterImpl == null) {
            filterImpl = new ArrayList<String>();
        }
        return this.filterImpl;
    }

}

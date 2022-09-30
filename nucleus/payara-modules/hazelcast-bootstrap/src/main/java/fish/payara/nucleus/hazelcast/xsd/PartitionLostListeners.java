
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of partition-lost-listeners complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="partition-lost-listeners">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="partition-lost-listener" type="{http://www.hazelcast.com/schema/config}partition-lost-listener" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "partition-lost-listeners", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "partitionLostListener"
})
public class PartitionLostListeners {

    @XmlElement(name = "partition-lost-listener", namespace = "http://www.hazelcast.com/schema/config")
    protected List<PartitionLostListener> partitionLostListener;

    /**
     * Gets the value of the partitionLostListener property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the partitionLostListener property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPartitionLostListener().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PartitionLostListener }
     * 
     * 
     */
    public List<PartitionLostListener> getPartitionLostListener() {
        if (partitionLostListener == null) {
            partitionLostListener = new ArrayList<PartitionLostListener>();
        }
        return this.partitionLostListener;
    }

}

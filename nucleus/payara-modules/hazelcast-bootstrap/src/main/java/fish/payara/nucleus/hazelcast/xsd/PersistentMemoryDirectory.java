
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * 
 *                 The directory where persistent memory is mounted to.
 * 
 *                 If the specified directory id not unique either in the
 *                 directory itself or in the NUMA node specified, the
 *                 configuration will be treated as invalid. Setting the NUMA
 *                 node on the subset of the configured directories while leaving
 *                 not set on others also results in invalid configuration.
 *             
 * 
 * <p>Java Class of persistent-memory-directory complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="persistent-memory-directory">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="numa-node" type="{http://www.w3.org/2001/XMLSchema}int" default="-1" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "persistent-memory-directory", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "value"
})
public class PersistentMemoryDirectory {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "numa-node")
    protected Integer numaNode;

    /**
     * Gets the value of property value.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of property value.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of property numaNode.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getNumaNode() {
        if (numaNode == null) {
            return -1;
        } else {
            return numaNode;
        }
    }

    /**
     * Sets the value of property numaNode.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setNumaNode(Integer value) {
        this.numaNode = value;
    }

}

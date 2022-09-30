
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 Configuration for a merkle tree.
 *                 The merkle tree is a data structure used for efficient comparison of the
 *                 difference in the contents of large data structures. The precision of
 *                 such a comparison mechanism is defined by the depth of the merkle tree.
 *                 A larger depth means that a data synchronization mechanism will be able
 *                 to pinpoint a smaller subset of the data structure contents in which a
 *                 change occurred. This causes the synchronization mechanism to be more
 *                 efficient. On the other hand, a larger tree depth means the merkle tree
 *                 will consume more memory.
 *                 A smaller depth means the data synchronization mechanism will have to
 *                 transfer larger chunks of the data structure in which a possible change
 *                 happened. On the other hand, a shallower tree consumes less memory.
 *                 The depth must be between 2 and 27 (exclusive).
 *                 As the comparison mechanism is iterative, a larger depth will also prolong
 *                 the duration of the comparison mechanism. Care must be taken to not have
 *                 large tree depths if the latency of the comparison operation is high.
 *                 The default depth is 10.
 *                 See https://en.wikipedia.org/wiki/Merkle_tree.
 *             
 * 
 * <p>Java Class of merkle-tree complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="merkle-tree">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="depth" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "merkle-tree", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class MerkleTree {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "unsignedInt")
    protected Long depth;
    @XmlAttribute(name = "enabled")
    protected Boolean enabled;

    /**
     * Gets the value of property depth.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getDepth() {
        return depth;
    }

    /**
     * Sets the value of property depth.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setDepth(Long value) {
        this.depth = value;
    }

    /**
     * Gets the value of property enabled.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isEnabled() {
        if (enabled == null) {
            return false;
        } else {
            return enabled;
        }
    }

    /**
     * Sets the value of property enabled.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEnabled(Boolean value) {
        this.enabled = value;
    }

}

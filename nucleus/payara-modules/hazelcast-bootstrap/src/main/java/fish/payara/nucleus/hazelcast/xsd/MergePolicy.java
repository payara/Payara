
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import java.math.BigInteger;


/**
 * 
 *                 While recovering from split-brain (network partitioning), data structure entries in the small cluster
 *                 merge into the bigger cluster based on the policy set here. When an entry merges into the cluster,
 *                 an entry with the same key (or value) might already exist in the cluster.
 *                 The merge policy resolves these conflicts with different out-of-the-box or custom strategies.
 *                 The out-of-the-box merge polices can be references by their simple class name.
 *                 For custom merge policies you have to provide a fully qualified class name.
 *                 
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;p xmlns="http://www.hazelcast.com/schema/config" xmlns:xs="http://www.w3.org/2001/XMLSchema"&gt;
 *                     The out-of-the-box policies are:
 *                     &lt;br/&gt;DiscardMergePolicy: the entry from the smaller cluster will be discarded.
 *                     &lt;br/&gt;HigherHitsMergePolicy: the entry with the higher number of hits wins.
 *                     &lt;br/&gt;LatestAccessMergePolicy: the entry with the latest access wins.
 *                     &lt;br/&gt;LatestUpdateMergePolicy: the entry with the latest update wins.
 *                     &lt;br/&gt;PassThroughMergePolicy: the entry from the smaller cluster wins.
 *                     &lt;br/&gt;PutIfAbsentMergePolicy: the entry from the smaller cluster wins if it doesn't exist in the cluster.
 *                     &lt;br/&gt;The default policy is: PutIfAbsentMergePolicy
 *                 &lt;/p&gt;
 * </pre>
 * 
 *             
 * 
 * <p>Java Class of merge-policy complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="merge-policy">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="batch-size" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" default="100" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "merge-policy", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "value"
})
public class MergePolicy {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "batch-size")
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger batchSize;

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
     * Gets the value of property batchSize.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getBatchSize() {
        if (batchSize == null) {
            return new BigInteger("100");
        } else {
            return batchSize;
        }
    }

    /**
     * Sets the value of property batchSize.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setBatchSize(BigInteger value) {
        this.batchSize = value;
    }

}


package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 A ReplicatedMap is a map-like data structure with weak consistency
 *                 and values locally stored on every node of the cluster.
 *                 Whenever a value is written asynchronously, the new value will be internally
 *                 distributed to all existing cluster members, and eventually every node will have
 *                 the new value.
 *                 When a new node joins the cluster, the new node initially will request existing
 *                 values from older nodes and replicate them locally.
 *             
 * 
 * <p>Java Class of replicatedmap complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="replicatedmap">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="in-memory-format" type="{http://www.hazelcast.com/schema/config}in-memory-format" minOccurs="0"/>
 *         &lt;element name="async-fillup" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="statistics-enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="entry-listeners" type="{http://www.hazelcast.com/schema/config}entry-listeners" minOccurs="0"/>
 *         &lt;element name="split-brain-protection-ref" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *         &lt;element name="merge-policy" type="{http://www.hazelcast.com/schema/config}merge-policy" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="name" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "replicatedmap", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Replicatedmap {

    @XmlElement(name = "in-memory-format", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "OBJECT")
    @XmlSchemaType(name = "string")
    protected InMemoryFormat inMemoryFormat;
    @XmlElement(name = "async-fillup", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean asyncFillup;
    @XmlElement(name = "statistics-enabled", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean statisticsEnabled;
    @XmlElement(name = "entry-listeners", namespace = "http://www.hazelcast.com/schema/config")
    protected EntryListeners entryListeners;
    @XmlElement(name = "split-brain-protection-ref", namespace = "http://www.hazelcast.com/schema/config")
    protected Object splitBrainProtectionRef;
    @XmlElement(name = "merge-policy", namespace = "http://www.hazelcast.com/schema/config")
    protected MergePolicy mergePolicy;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    /**
     * Gets the value of property inMemoryFormat.
     * 
     * @return
     *     possible object is
     *     {@link InMemoryFormat }
     *     
     */
    public InMemoryFormat getInMemoryFormat() {
        return inMemoryFormat;
    }

    /**
     * Sets the value of property inMemoryFormat.
     * 
     * @param value
     *     allowed object is
     *     {@link InMemoryFormat }
     *     
     */
    public void setInMemoryFormat(InMemoryFormat value) {
        this.inMemoryFormat = value;
    }

    /**
     * Gets the value of property asyncFillup.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isAsyncFillup() {
        return asyncFillup;
    }

    /**
     * Sets the value of property asyncFillup.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAsyncFillup(Boolean value) {
        this.asyncFillup = value;
    }

    /**
     * Gets the value of property statisticsEnabled.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }

    /**
     * Sets the value of property statisticsEnabled.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setStatisticsEnabled(Boolean value) {
        this.statisticsEnabled = value;
    }

    /**
     * Gets the value of property entryListeners.
     * 
     * @return
     *     possible object is
     *     {@link EntryListeners }
     *     
     */
    public EntryListeners getEntryListeners() {
        return entryListeners;
    }

    /**
     * Sets the value of property entryListeners.
     * 
     * @param value
     *     allowed object is
     *     {@link EntryListeners }
     *     
     */
    public void setEntryListeners(EntryListeners value) {
        this.entryListeners = value;
    }

    /**
     * Gets the value of property splitBrainProtectionRef.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getSplitBrainProtectionRef() {
        return splitBrainProtectionRef;
    }

    /**
     * Sets the value of property splitBrainProtectionRef.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setSplitBrainProtectionRef(Object value) {
        this.splitBrainProtectionRef = value;
    }

    /**
     * Gets the value of property mergePolicy.
     * 
     * @return
     *     possible object is
     *     {@link MergePolicy }
     *     
     */
    public MergePolicy getMergePolicy() {
        return mergePolicy;
    }

    /**
     * Sets the value of property mergePolicy.
     * 
     * @param value
     *     allowed object is
     *     {@link MergePolicy }
     *     
     */
    public void setMergePolicy(MergePolicy value) {
        this.mergePolicy = value;
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

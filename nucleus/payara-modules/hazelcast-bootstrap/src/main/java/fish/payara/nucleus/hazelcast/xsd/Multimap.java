
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 Hazelcast MultiMap is a specialized map where you can store multiple values under a single key.
 *                 Just like any other distributed data structure implementation in Hazelcast, MultiMap is distributed
 *                 and thread-safe. Hazelcast MultiMap is not an implementation of java.util.Map due to the difference
 *                 in method signatures. It supports most features of Hazelcast Map except for indexing, predicates and
 *                 MapLoader/MapStore.
 *             
 * 
 * <p>Java Class of multimap complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="multimap">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="backup-count" type="{http://www.hazelcast.com/schema/config}backup-count" minOccurs="0"/>
 *         &lt;element name="async-backup-count" type="{http://www.hazelcast.com/schema/config}backup-count" minOccurs="0"/>
 *         &lt;element name="statistics-enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="binary" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="value-collection-type" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *               &lt;enumeration value="SET"/>
 *               &lt;enumeration value="LIST"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
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
@XmlType(name = "multimap", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Multimap {

    @XmlElement(name = "backup-count", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "1")
    protected Byte backupCount;
    @XmlElement(name = "async-backup-count", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    protected Byte asyncBackupCount;
    @XmlElement(name = "statistics-enabled", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean statisticsEnabled;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean binary;
    @XmlElement(name = "value-collection-type", namespace = "http://www.hazelcast.com/schema/config")
    protected String valueCollectionType;
    @XmlElement(name = "entry-listeners", namespace = "http://www.hazelcast.com/schema/config")
    protected EntryListeners entryListeners;
    @XmlElement(name = "split-brain-protection-ref", namespace = "http://www.hazelcast.com/schema/config")
    protected Object splitBrainProtectionRef;
    @XmlElement(name = "merge-policy", namespace = "http://www.hazelcast.com/schema/config")
    protected MergePolicy mergePolicy;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    /**
     * Gets the value of property backupCount.
     * 
     * @return
     *     possible object is
     *     {@link Byte }
     *     
     */
    public Byte getBackupCount() {
        return backupCount;
    }

    /**
     * Sets the value of property backupCount.
     * 
     * @param value
     *     allowed object is
     *     {@link Byte }
     *     
     */
    public void setBackupCount(Byte value) {
        this.backupCount = value;
    }

    /**
     * Gets the value of property asyncBackupCount.
     * 
     * @return
     *     possible object is
     *     {@link Byte }
     *     
     */
    public Byte getAsyncBackupCount() {
        return asyncBackupCount;
    }

    /**
     * Sets the value of property asyncBackupCount.
     * 
     * @param value
     *     allowed object is
     *     {@link Byte }
     *     
     */
    public void setAsyncBackupCount(Byte value) {
        this.asyncBackupCount = value;
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
     * Gets the value of property binary.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isBinary() {
        return binary;
    }

    /**
     * Sets the value of property binary.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setBinary(Boolean value) {
        this.binary = value;
    }

    /**
     * Gets the value of property valueCollectionType.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValueCollectionType() {
        return valueCollectionType;
    }

    /**
     * Sets the value of property valueCollectionType.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValueCollectionType(String value) {
        this.valueCollectionType = value;
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

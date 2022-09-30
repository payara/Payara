
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of ringbuffer complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="ringbuffer">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="capacity" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="time-to-live-seconds" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="backup-count" type="{http://www.hazelcast.com/schema/config}backup-count" minOccurs="0"/>
 *         &lt;element name="async-backup-count" type="{http://www.hazelcast.com/schema/config}backup-count" minOccurs="0"/>
 *         &lt;element name="in-memory-format" type="{http://www.hazelcast.com/schema/config}in-memory-format" minOccurs="0"/>
 *         &lt;element name="ringbuffer-store" type="{http://www.hazelcast.com/schema/config}ringbuffer-store" minOccurs="0"/>
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
@XmlType(name = "ringbuffer", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Ringbuffer {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "unsignedInt")
    protected Long capacity;
    @XmlElement(name = "time-to-live-seconds", namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "unsignedInt")
    protected Long timeToLiveSeconds;
    @XmlElement(name = "backup-count", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "1")
    protected Byte backupCount;
    @XmlElement(name = "async-backup-count", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    protected Byte asyncBackupCount;
    @XmlElement(name = "in-memory-format", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "BINARY")
    @XmlSchemaType(name = "string")
    protected InMemoryFormat inMemoryFormat;
    @XmlElement(name = "ringbuffer-store", namespace = "http://www.hazelcast.com/schema/config")
    protected RingbufferStore ringbufferStore;
    @XmlElement(name = "split-brain-protection-ref", namespace = "http://www.hazelcast.com/schema/config")
    protected Object splitBrainProtectionRef;
    @XmlElement(name = "merge-policy", namespace = "http://www.hazelcast.com/schema/config")
    protected MergePolicy mergePolicy;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    /**
     * Gets the value of property capacity.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getCapacity() {
        return capacity;
    }

    /**
     * Sets the value of property capacity.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setCapacity(Long value) {
        this.capacity = value;
    }

    /**
     * Gets the value of property timeToLiveSeconds.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    /**
     * Sets the value of property timeToLiveSeconds.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setTimeToLiveSeconds(Long value) {
        this.timeToLiveSeconds = value;
    }

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
     * Gets the value of property ringbufferStore.
     * 
     * @return
     *     possible object is
     *     {@link RingbufferStore }
     *     
     */
    public RingbufferStore getRingbufferStore() {
        return ringbufferStore;
    }

    /**
     * Sets the value of property ringbufferStore.
     * 
     * @param value
     *     allowed object is
     *     {@link RingbufferStore }
     *     
     */
    public void setRingbufferStore(RingbufferStore value) {
        this.ringbufferStore = value;
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

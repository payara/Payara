
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java Class of queue complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="queue">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="priority-comparator-class-name" type="{http://www.hazelcast.com/schema/config}non-space-string" minOccurs="0"/>
 *         &lt;element name="statistics-enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="max-size" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="backup-count" type="{http://www.hazelcast.com/schema/config}backup-count" minOccurs="0"/>
 *         &lt;element name="async-backup-count" type="{http://www.hazelcast.com/schema/config}backup-count" minOccurs="0"/>
 *         &lt;element name="empty-queue-ttl" type="{http://www.hazelcast.com/schema/config}empty-queue-ttl" minOccurs="0"/>
 *         &lt;element name="item-listeners" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="item-listener" type="{http://www.hazelcast.com/schema/config}item-listener" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element ref="{http://www.hazelcast.com/schema/config}queue-store" minOccurs="0"/>
 *         &lt;element name="split-brain-protection-ref" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *         &lt;element name="merge-policy" type="{http://www.hazelcast.com/schema/config}merge-policy" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="name" default="default">
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
@XmlType(name = "queue", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Queue {

    @XmlElement(name = "priority-comparator-class-name", namespace = "http://www.hazelcast.com/schema/config")
    protected String priorityComparatorClassName;
    @XmlElement(name = "statistics-enabled", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean statisticsEnabled;
    @XmlElement(name = "max-size", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    @XmlSchemaType(name = "unsignedInt")
    protected Long maxSize;
    @XmlElement(name = "backup-count", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "1")
    protected Byte backupCount;
    @XmlElement(name = "async-backup-count", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    protected Byte asyncBackupCount;
    @XmlElement(name = "empty-queue-ttl", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "-1")
    protected Integer emptyQueueTtl;
    @XmlElement(name = "item-listeners", namespace = "http://www.hazelcast.com/schema/config")
    protected ItemListeners itemListeners;
    @XmlElement(name = "queue-store", namespace = "http://www.hazelcast.com/schema/config")
    protected QueueStore queueStore;
    @XmlElement(name = "split-brain-protection-ref", namespace = "http://www.hazelcast.com/schema/config")
    protected Object splitBrainProtectionRef;
    @XmlElement(name = "merge-policy", namespace = "http://www.hazelcast.com/schema/config")
    protected MergePolicy mergePolicy;
    @XmlAttribute(name = "name")
    protected String name;

    /**
     * Gets the value of property priorityComparatorClassName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPriorityComparatorClassName() {
        return priorityComparatorClassName;
    }

    /**
     * Sets the value of property priorityComparatorClassName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPriorityComparatorClassName(String value) {
        this.priorityComparatorClassName = value;
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
     * Gets the value of property maxSize.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getMaxSize() {
        return maxSize;
    }

    /**
     * Sets the value of property maxSize.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setMaxSize(Long value) {
        this.maxSize = value;
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
     * Gets the value of property emptyQueueTtl.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getEmptyQueueTtl() {
        return emptyQueueTtl;
    }

    /**
     * Sets the value of property emptyQueueTtl.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setEmptyQueueTtl(Integer value) {
        this.emptyQueueTtl = value;
    }

    /**
     * Gets the value of property itemListeners.
     * 
     * @return
     *     possible object is
     *     {@link ItemListeners }
     *     
     */
    public ItemListeners getItemListeners() {
        return itemListeners;
    }

    /**
     * Sets the value of property itemListeners.
     * 
     * @param value
     *     allowed object is
     *     {@link ItemListeners }
     *     
     */
    public void setItemListeners(ItemListeners value) {
        this.itemListeners = value;
    }

    /**
     * 
     *                         Includes the queue store factory class name and the following properties.
     * 
     *                         Binary: By default, Hazelcast stores the queue items in serialized form in memory.
     *                         Before it inserts the queue items into datastore, it deserializes them. But if you
     *                         will not reach the queue store from an external application, you might prefer that the
     *                         items be inserted in binary form. You can get rid of the de-serialization step; this
     *                         would be a performance optimization. The binary feature is disabled by default.
     * 
     *                         Memory Limit: This is the number of items after which Hazelcast will store items only to
     *                         datastore. For example, if the memory limit is 1000, then the 1001st item will be put
     *                         only to datastore. This feature is useful when you want to avoid out-of-memory conditions.
     *                         The default number for memory-limit is 1000. If you want to always use memory, you can set
     *                         it to Integer.MAX_VALUE.
     * 
     *                         Bulk Load: When the queue is initialized, items are loaded from QueueStore in bulks. Bulk
     *                         load is the size of these bulks. By default, bulk-load is 250.
     *                     
     * 
     * @return
     *     possible object is
     *     {@link QueueStore }
     *     
     */
    public QueueStore getQueueStore() {
        return queueStore;
    }

    /**
     * Sets the value of property queueStore.
     * 
     * @param value
     *     allowed object is
     *     {@link QueueStore }
     *     
     */
    public void setQueueStore(QueueStore value) {
        this.queueStore = value;
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
        if (name == null) {
            return "default";
        } else {
            return name;
        }
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


    /**
     * <p>Java Class of anonymous complex type.
     * 
     * 
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="item-listener" type="{http://www.hazelcast.com/schema/config}item-listener" maxOccurs="unbounded" minOccurs="0"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "itemListener"
    })
    public static class ItemListeners {

        @XmlElement(name = "item-listener", namespace = "http://www.hazelcast.com/schema/config")
        protected List<ItemListener> itemListener;

        /**
         * Gets the value of the itemListener property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the itemListener property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getItemListener().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link ItemListener }
         * 
         * 
         */
        public List<ItemListener> getItemListener() {
            if (itemListener == null) {
                itemListener = new ArrayList<ItemListener>();
            }
            return this.itemListener;
        }

    }

}

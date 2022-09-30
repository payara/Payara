
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of cache complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="cache">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="key-type" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="value-type" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="statistics-enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="management-enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="read-through" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="write-through" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="cache-loader-factory" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="cache-loader" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="cache-writer-factory" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="cache-writer" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="expiry-policy-factory" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;all>
 *                   &lt;element name="timed-expiry-policy-factory" type="{http://www.hazelcast.com/schema/config}timed-expiry-policy-factory" minOccurs="0"/>
 *                 &lt;/all>
 *                 &lt;attribute name="class-name" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="cache-entry-listeners" type="{http://www.hazelcast.com/schema/config}cache-entry-listeners" minOccurs="0"/>
 *         &lt;element name="in-memory-format" type="{http://www.hazelcast.com/schema/config}in-memory-format" minOccurs="0"/>
 *         &lt;element name="backup-count" type="{http://www.hazelcast.com/schema/config}backup-count" minOccurs="0"/>
 *         &lt;element name="async-backup-count" type="{http://www.hazelcast.com/schema/config}backup-count" minOccurs="0"/>
 *         &lt;element name="eviction" type="{http://www.hazelcast.com/schema/config}eviction" minOccurs="0"/>
 *         &lt;element name="wan-replication-ref" type="{http://www.hazelcast.com/schema/config}wan-replication-ref" minOccurs="0"/>
 *         &lt;element name="split-brain-protection-ref" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *         &lt;element name="partition-lost-listeners" type="{http://www.hazelcast.com/schema/config}partition-lost-listeners" minOccurs="0"/>
 *         &lt;element name="merge-policy" type="{http://www.hazelcast.com/schema/config}merge-policy" minOccurs="0"/>
 *         &lt;element name="hot-restart" type="{http://www.hazelcast.com/schema/config}hot-restart" minOccurs="0"/>
 *         &lt;element name="event-journal" type="{http://www.hazelcast.com/schema/config}event-journal" minOccurs="0"/>
 *         &lt;element name="disable-per-entry-invalidation-events" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
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
@XmlType(name = "cache", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Cache {

    @XmlElement(name = "key-type", namespace = "http://www.hazelcast.com/schema/config")
    protected KeyType keyType;
    @XmlElement(name = "value-type", namespace = "http://www.hazelcast.com/schema/config")
    protected ValueType valueType;
    @XmlElement(name = "statistics-enabled", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean statisticsEnabled;
    @XmlElement(name = "management-enabled", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean managementEnabled;
    @XmlElement(name = "read-through", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean readThrough;
    @XmlElement(name = "write-through", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean writeThrough;
    @XmlElement(name = "cache-loader-factory", namespace = "http://www.hazelcast.com/schema/config")
    protected CacheLoaderFactory cacheLoaderFactory;
    @XmlElement(name = "cache-loader", namespace = "http://www.hazelcast.com/schema/config")
    protected CacheLoader cacheLoader;
    @XmlElement(name = "cache-writer-factory", namespace = "http://www.hazelcast.com/schema/config")
    protected CacheWriterFactory cacheWriterFactory;
    @XmlElement(name = "cache-writer", namespace = "http://www.hazelcast.com/schema/config")
    protected CacheWriter cacheWriter;
    @XmlElement(name = "expiry-policy-factory", namespace = "http://www.hazelcast.com/schema/config")
    protected ExpiryPolicyFactory expiryPolicyFactory;
    @XmlElement(name = "cache-entry-listeners", namespace = "http://www.hazelcast.com/schema/config")
    protected CacheEntryListeners cacheEntryListeners;
    @XmlElement(name = "in-memory-format", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "BINARY")
    @XmlSchemaType(name = "string")
    protected InMemoryFormat inMemoryFormat;
    @XmlElement(name = "backup-count", namespace = "http://www.hazelcast.com/schema/config")
    protected Byte backupCount;
    @XmlElement(name = "async-backup-count", namespace = "http://www.hazelcast.com/schema/config")
    protected Byte asyncBackupCount;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Eviction eviction;
    @XmlElement(name = "wan-replication-ref", namespace = "http://www.hazelcast.com/schema/config")
    protected WanReplicationRef wanReplicationRef;
    @XmlElement(name = "split-brain-protection-ref", namespace = "http://www.hazelcast.com/schema/config")
    protected Object splitBrainProtectionRef;
    @XmlElement(name = "partition-lost-listeners", namespace = "http://www.hazelcast.com/schema/config")
    protected PartitionLostListeners partitionLostListeners;
    @XmlElement(name = "merge-policy", namespace = "http://www.hazelcast.com/schema/config")
    protected MergePolicy mergePolicy;
    @XmlElement(name = "hot-restart", namespace = "http://www.hazelcast.com/schema/config")
    protected HotRestart hotRestart;
    @XmlElement(name = "event-journal", namespace = "http://www.hazelcast.com/schema/config")
    protected EventJournal eventJournal;
    @XmlElement(name = "disable-per-entry-invalidation-events", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean disablePerEntryInvalidationEvents;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    /**
     * Gets the value of property keyType.
     * 
     * @return
     *     possible object is
     *     {@link KeyType }
     *     
     */
    public KeyType getKeyType() {
        return keyType;
    }

    /**
     * Sets the value of property keyType.
     * 
     * @param value
     *     allowed object is
     *     {@link KeyType }
     *     
     */
    public void setKeyType(KeyType value) {
        this.keyType = value;
    }

    /**
     * Gets the value of property valueType.
     * 
     * @return
     *     possible object is
     *     {@link ValueType }
     *     
     */
    public ValueType getValueType() {
        return valueType;
    }

    /**
     * Sets the value of property valueType.
     * 
     * @param value
     *     allowed object is
     *     {@link ValueType }
     *     
     */
    public void setValueType(ValueType value) {
        this.valueType = value;
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
     * Gets the value of property managementEnabled.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isManagementEnabled() {
        return managementEnabled;
    }

    /**
     * Sets the value of property managementEnabled.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setManagementEnabled(Boolean value) {
        this.managementEnabled = value;
    }

    /**
     * Gets the value of property readThrough.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isReadThrough() {
        return readThrough;
    }

    /**
     * Sets the value of property readThrough.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setReadThrough(Boolean value) {
        this.readThrough = value;
    }

    /**
     * Gets the value of property writeThrough.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isWriteThrough() {
        return writeThrough;
    }

    /**
     * Sets the value of property writeThrough.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setWriteThrough(Boolean value) {
        this.writeThrough = value;
    }

    /**
     * Gets the value of property cacheLoaderFactory.
     * 
     * @return
     *     possible object is
     *     {@link CacheLoaderFactory }
     *     
     */
    public CacheLoaderFactory getCacheLoaderFactory() {
        return cacheLoaderFactory;
    }

    /**
     * Sets the value of property cacheLoaderFactory.
     * 
     * @param value
     *     allowed object is
     *     {@link CacheLoaderFactory }
     *     
     */
    public void setCacheLoaderFactory(CacheLoaderFactory value) {
        this.cacheLoaderFactory = value;
    }

    /**
     * Gets the value of property cacheLoader.
     * 
     * @return
     *     possible object is
     *     {@link CacheLoader }
     *     
     */
    public CacheLoader getCacheLoader() {
        return cacheLoader;
    }

    /**
     * Sets the value of property cacheLoader.
     * 
     * @param value
     *     allowed object is
     *     {@link CacheLoader }
     *     
     */
    public void setCacheLoader(CacheLoader value) {
        this.cacheLoader = value;
    }

    /**
     * Gets the value of property cacheWriterFactory.
     * 
     * @return
     *     possible object is
     *     {@link CacheWriterFactory }
     *     
     */
    public CacheWriterFactory getCacheWriterFactory() {
        return cacheWriterFactory;
    }

    /**
     * Sets the value of property cacheWriterFactory.
     * 
     * @param value
     *     allowed object is
     *     {@link CacheWriterFactory }
     *     
     */
    public void setCacheWriterFactory(CacheWriterFactory value) {
        this.cacheWriterFactory = value;
    }

    /**
     * Gets the value of property cacheWriter.
     * 
     * @return
     *     possible object is
     *     {@link CacheWriter }
     *     
     */
    public CacheWriter getCacheWriter() {
        return cacheWriter;
    }

    /**
     * Sets the value of property cacheWriter.
     * 
     * @param value
     *     allowed object is
     *     {@link CacheWriter }
     *     
     */
    public void setCacheWriter(CacheWriter value) {
        this.cacheWriter = value;
    }

    /**
     * Gets the value of property expiryPolicyFactory.
     * 
     * @return
     *     possible object is
     *     {@link ExpiryPolicyFactory }
     *     
     */
    public ExpiryPolicyFactory getExpiryPolicyFactory() {
        return expiryPolicyFactory;
    }

    /**
     * Sets the value of property expiryPolicyFactory.
     * 
     * @param value
     *     allowed object is
     *     {@link ExpiryPolicyFactory }
     *     
     */
    public void setExpiryPolicyFactory(ExpiryPolicyFactory value) {
        this.expiryPolicyFactory = value;
    }

    /**
     * Gets the value of property cacheEntryListeners.
     * 
     * @return
     *     possible object is
     *     {@link CacheEntryListeners }
     *     
     */
    public CacheEntryListeners getCacheEntryListeners() {
        return cacheEntryListeners;
    }

    /**
     * Sets the value of property cacheEntryListeners.
     * 
     * @param value
     *     allowed object is
     *     {@link CacheEntryListeners }
     *     
     */
    public void setCacheEntryListeners(CacheEntryListeners value) {
        this.cacheEntryListeners = value;
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
     * Gets the value of property eviction.
     * 
     * @return
     *     possible object is
     *     {@link Eviction }
     *     
     */
    public Eviction getEviction() {
        return eviction;
    }

    /**
     * Sets the value of property eviction.
     * 
     * @param value
     *     allowed object is
     *     {@link Eviction }
     *     
     */
    public void setEviction(Eviction value) {
        this.eviction = value;
    }

    /**
     * Gets the value of property wanReplicationRef.
     * 
     * @return
     *     possible object is
     *     {@link WanReplicationRef }
     *     
     */
    public WanReplicationRef getWanReplicationRef() {
        return wanReplicationRef;
    }

    /**
     * Sets the value of property wanReplicationRef.
     * 
     * @param value
     *     allowed object is
     *     {@link WanReplicationRef }
     *     
     */
    public void setWanReplicationRef(WanReplicationRef value) {
        this.wanReplicationRef = value;
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
     * Gets the value of property partitionLostListeners.
     * 
     * @return
     *     possible object is
     *     {@link PartitionLostListeners }
     *     
     */
    public PartitionLostListeners getPartitionLostListeners() {
        return partitionLostListeners;
    }

    /**
     * Sets the value of property partitionLostListeners.
     * 
     * @param value
     *     allowed object is
     *     {@link PartitionLostListeners }
     *     
     */
    public void setPartitionLostListeners(PartitionLostListeners value) {
        this.partitionLostListeners = value;
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
     * Gets the value of property hotRestart.
     * 
     * @return
     *     possible object is
     *     {@link HotRestart }
     *     
     */
    public HotRestart getHotRestart() {
        return hotRestart;
    }

    /**
     * Sets the value of property hotRestart.
     * 
     * @param value
     *     allowed object is
     *     {@link HotRestart }
     *     
     */
    public void setHotRestart(HotRestart value) {
        this.hotRestart = value;
    }

    /**
     * Gets the value of property eventJournal.
     * 
     * @return
     *     possible object is
     *     {@link EventJournal }
     *     
     */
    public EventJournal getEventJournal() {
        return eventJournal;
    }

    /**
     * Sets the value of property eventJournal.
     * 
     * @param value
     *     allowed object is
     *     {@link EventJournal }
     *     
     */
    public void setEventJournal(EventJournal value) {
        this.eventJournal = value;
    }

    /**
     * Gets the value of property disablePerEntryInvalidationEvents.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isDisablePerEntryInvalidationEvents() {
        return disablePerEntryInvalidationEvents;
    }

    /**
     * Sets the value of property disablePerEntryInvalidationEvents.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDisablePerEntryInvalidationEvents(Boolean value) {
        this.disablePerEntryInvalidationEvents = value;
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


    /**
     * <p>Java Class of anonymous complex type.
     * 
     * 
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class CacheLoader {

        @XmlAttribute(name = "class-name", required = true)
        protected String className;

        /**
         * Gets the value of property className.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getClassName() {
            return className;
        }

        /**
         * Sets the value of property className.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setClassName(String value) {
            this.className = value;
        }

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
     *       &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class CacheLoaderFactory {

        @XmlAttribute(name = "class-name", required = true)
        @XmlSchemaType(name = "anySimpleType")
        protected String className;

        /**
         * Gets the value of property className.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getClassName() {
            return className;
        }

        /**
         * Sets the value of property className.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setClassName(String value) {
            this.className = value;
        }

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
     *       &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class CacheWriter {

        @XmlAttribute(name = "class-name", required = true)
        protected String className;

        /**
         * Gets the value of property className.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getClassName() {
            return className;
        }

        /**
         * Sets the value of property className.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setClassName(String value) {
            this.className = value;
        }

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
     *       &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class CacheWriterFactory {

        @XmlAttribute(name = "class-name", required = true)
        @XmlSchemaType(name = "anySimpleType")
        protected String className;

        /**
         * Gets the value of property className.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getClassName() {
            return className;
        }

        /**
         * Sets the value of property className.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setClassName(String value) {
            this.className = value;
        }

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
     *       &lt;all>
     *         &lt;element name="timed-expiry-policy-factory" type="{http://www.hazelcast.com/schema/config}timed-expiry-policy-factory" minOccurs="0"/>
     *       &lt;/all>
     *       &lt;attribute name="class-name" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {

    })
    public static class ExpiryPolicyFactory {

        @XmlElement(name = "timed-expiry-policy-factory", namespace = "http://www.hazelcast.com/schema/config")
        protected TimedExpiryPolicyFactory timedExpiryPolicyFactory;
        @XmlAttribute(name = "class-name")
        @XmlSchemaType(name = "anySimpleType")
        protected String className;

        /**
         * Gets the value of property timedExpiryPolicyFactory.
         * 
         * @return
         *     possible object is
         *     {@link TimedExpiryPolicyFactory }
         *     
         */
        public TimedExpiryPolicyFactory getTimedExpiryPolicyFactory() {
            return timedExpiryPolicyFactory;
        }

        /**
         * Sets the value of property timedExpiryPolicyFactory.
         * 
         * @param value
         *     allowed object is
         *     {@link TimedExpiryPolicyFactory }
         *     
         */
        public void setTimedExpiryPolicyFactory(TimedExpiryPolicyFactory value) {
            this.timedExpiryPolicyFactory = value;
        }

        /**
         * Gets the value of property className.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getClassName() {
            return className;
        }

        /**
         * Sets the value of property className.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setClassName(String value) {
            this.className = value;
        }

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
     *       &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class KeyType {

        @XmlAttribute(name = "class-name", required = true)
        @XmlSchemaType(name = "anySimpleType")
        protected String className;

        /**
         * Gets the value of property className.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getClassName() {
            return className;
        }

        /**
         * Sets the value of property className.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setClassName(String value) {
            this.className = value;
        }

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
     *       &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class ValueType {

        @XmlAttribute(name = "class-name", required = true)
        @XmlSchemaType(name = "anySimpleType")
        protected String className;

        /**
         * Gets the value of property className.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getClassName() {
            return className;
        }

        /**
         * Sets the value of property className.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setClassName(String value) {
            this.className = value;
        }

    }

}

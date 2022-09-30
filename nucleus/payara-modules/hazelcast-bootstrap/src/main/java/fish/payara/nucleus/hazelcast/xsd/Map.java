
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of map complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="map">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="metadata-policy" type="{http://www.hazelcast.com/schema/config}metadata-policy" minOccurs="0"/>
 *         &lt;element name="in-memory-format" type="{http://www.hazelcast.com/schema/config}in-memory-format" minOccurs="0"/>
 *         &lt;element name="statistics-enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="per-entry-stats-enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="cache-deserialized-values" type="{http://www.hazelcast.com/schema/config}cache-deserialized-values" minOccurs="0"/>
 *         &lt;element name="backup-count" type="{http://www.hazelcast.com/schema/config}backup-count" minOccurs="0"/>
 *         &lt;element name="async-backup-count" type="{http://www.hazelcast.com/schema/config}backup-count" minOccurs="0"/>
 *         &lt;element name="time-to-live-seconds" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="max-idle-seconds" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="eviction" type="{http://www.hazelcast.com/schema/config}eviction-map" minOccurs="0"/>
 *         &lt;element name="merge-policy" type="{http://www.hazelcast.com/schema/config}merge-policy" minOccurs="0"/>
 *         &lt;element name="read-backup-data" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="merkle-tree" type="{http://www.hazelcast.com/schema/config}merkle-tree" minOccurs="0"/>
 *         &lt;element name="hot-restart" type="{http://www.hazelcast.com/schema/config}hot-restart" minOccurs="0"/>
 *         &lt;element name="event-journal" type="{http://www.hazelcast.com/schema/config}event-journal" minOccurs="0"/>
 *         &lt;element name="map-store" type="{http://www.hazelcast.com/schema/config}map-store" minOccurs="0"/>
 *         &lt;element name="near-cache" type="{http://www.hazelcast.com/schema/config}near-cache" minOccurs="0"/>
 *         &lt;element name="wan-replication-ref" type="{http://www.hazelcast.com/schema/config}wan-replication-ref" minOccurs="0"/>
 *         &lt;element name="indexes" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="index" type="{http://www.hazelcast.com/schema/config}index" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="attributes" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="attribute" type="{http://www.hazelcast.com/schema/config}map-attribute" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="entry-listeners" type="{http://www.hazelcast.com/schema/config}entry-listeners" minOccurs="0"/>
 *         &lt;element name="partition-lost-listeners" type="{http://www.hazelcast.com/schema/config}partition-lost-listeners" minOccurs="0"/>
 *         &lt;element name="partition-strategy" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="split-brain-protection-ref" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *         &lt;element name="query-caches" type="{http://www.hazelcast.com/schema/config}query-caches" minOccurs="0"/>
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
@XmlType(name = "map", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Map {

    @XmlElement(name = "metadata-policy", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "OFF")
    @XmlSchemaType(name = "string")
    protected MetadataPolicy metadataPolicy;
    @XmlElement(name = "in-memory-format", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "BINARY")
    @XmlSchemaType(name = "string")
    protected InMemoryFormat inMemoryFormat;
    @XmlElement(name = "statistics-enabled", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean statisticsEnabled;
    @XmlElement(name = "per-entry-stats-enabled", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean perEntryStatsEnabled;
    @XmlElement(name = "cache-deserialized-values", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "INDEX-ONLY")
    @XmlSchemaType(name = "string")
    protected CacheDeserializedValues cacheDeserializedValues;
    @XmlElement(name = "backup-count", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "1")
    protected Byte backupCount;
    @XmlElement(name = "async-backup-count", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    protected Byte asyncBackupCount;
    @XmlElement(name = "time-to-live-seconds", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    @XmlSchemaType(name = "unsignedInt")
    protected Long timeToLiveSeconds;
    @XmlElement(name = "max-idle-seconds", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    @XmlSchemaType(name = "unsignedInt")
    protected Long maxIdleSeconds;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected EvictionMap eviction;
    @XmlElement(name = "merge-policy", namespace = "http://www.hazelcast.com/schema/config")
    protected MergePolicy mergePolicy;
    @XmlElement(name = "read-backup-data", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean readBackupData;
    @XmlElement(name = "merkle-tree", namespace = "http://www.hazelcast.com/schema/config")
    protected MerkleTree merkleTree;
    @XmlElement(name = "hot-restart", namespace = "http://www.hazelcast.com/schema/config")
    protected HotRestart hotRestart;
    @XmlElement(name = "event-journal", namespace = "http://www.hazelcast.com/schema/config")
    protected EventJournal eventJournal;
    @XmlElement(name = "map-store", namespace = "http://www.hazelcast.com/schema/config")
    protected MapStore mapStore;
    @XmlElement(name = "near-cache", namespace = "http://www.hazelcast.com/schema/config")
    protected NearCache nearCache;
    @XmlElement(name = "wan-replication-ref", namespace = "http://www.hazelcast.com/schema/config")
    protected WanReplicationRef wanReplicationRef;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Indexes indexes;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Attributes attributes;
    @XmlElement(name = "entry-listeners", namespace = "http://www.hazelcast.com/schema/config")
    protected EntryListeners entryListeners;
    @XmlElement(name = "partition-lost-listeners", namespace = "http://www.hazelcast.com/schema/config")
    protected PartitionLostListeners partitionLostListeners;
    @XmlElement(name = "partition-strategy", namespace = "http://www.hazelcast.com/schema/config")
    protected String partitionStrategy;
    @XmlElement(name = "split-brain-protection-ref", namespace = "http://www.hazelcast.com/schema/config")
    protected Object splitBrainProtectionRef;
    @XmlElement(name = "query-caches", namespace = "http://www.hazelcast.com/schema/config")
    protected QueryCaches queryCaches;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    /**
     * Gets the value of property metadataPolicy.
     * 
     * @return
     *     possible object is
     *     {@link MetadataPolicy }
     *     
     */
    public MetadataPolicy getMetadataPolicy() {
        return metadataPolicy;
    }

    /**
     * Sets the value of property metadataPolicy.
     * 
     * @param value
     *     allowed object is
     *     {@link MetadataPolicy }
     *     
     */
    public void setMetadataPolicy(MetadataPolicy value) {
        this.metadataPolicy = value;
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
     * Gets the value of property perEntryStatsEnabled.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isPerEntryStatsEnabled() {
        return perEntryStatsEnabled;
    }

    /**
     * Sets the value of property perEntryStatsEnabled.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setPerEntryStatsEnabled(Boolean value) {
        this.perEntryStatsEnabled = value;
    }

    /**
     * Gets the value of property cacheDeserializedValues.
     * 
     * @return
     *     possible object is
     *     {@link CacheDeserializedValues }
     *     
     */
    public CacheDeserializedValues getCacheDeserializedValues() {
        return cacheDeserializedValues;
    }

    /**
     * Sets the value of property cacheDeserializedValues.
     * 
     * @param value
     *     allowed object is
     *     {@link CacheDeserializedValues }
     *     
     */
    public void setCacheDeserializedValues(CacheDeserializedValues value) {
        this.cacheDeserializedValues = value;
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
     * Gets the value of property maxIdleSeconds.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getMaxIdleSeconds() {
        return maxIdleSeconds;
    }

    /**
     * Sets the value of property maxIdleSeconds.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setMaxIdleSeconds(Long value) {
        this.maxIdleSeconds = value;
    }

    /**
     * Gets the value of property eviction.
     * 
     * @return
     *     possible object is
     *     {@link EvictionMap }
     *     
     */
    public EvictionMap getEviction() {
        return eviction;
    }

    /**
     * Sets the value of property eviction.
     * 
     * @param value
     *     allowed object is
     *     {@link EvictionMap }
     *     
     */
    public void setEviction(EvictionMap value) {
        this.eviction = value;
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
     * Gets the value of property readBackupData.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isReadBackupData() {
        return readBackupData;
    }

    /**
     * Sets the value of property readBackupData.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setReadBackupData(Boolean value) {
        this.readBackupData = value;
    }

    /**
     * Gets the value of property merkleTree.
     * 
     * @return
     *     possible object is
     *     {@link MerkleTree }
     *     
     */
    public MerkleTree getMerkleTree() {
        return merkleTree;
    }

    /**
     * Sets the value of property merkleTree.
     * 
     * @param value
     *     allowed object is
     *     {@link MerkleTree }
     *     
     */
    public void setMerkleTree(MerkleTree value) {
        this.merkleTree = value;
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
     * Gets the value of property mapStore.
     * 
     * @return
     *     possible object is
     *     {@link MapStore }
     *     
     */
    public MapStore getMapStore() {
        return mapStore;
    }

    /**
     * Sets the value of property mapStore.
     * 
     * @param value
     *     allowed object is
     *     {@link MapStore }
     *     
     */
    public void setMapStore(MapStore value) {
        this.mapStore = value;
    }

    /**
     * Gets the value of property nearCache.
     * 
     * @return
     *     possible object is
     *     {@link NearCache }
     *     
     */
    public NearCache getNearCache() {
        return nearCache;
    }

    /**
     * Sets the value of property nearCache.
     * 
     * @param value
     *     allowed object is
     *     {@link NearCache }
     *     
     */
    public void setNearCache(NearCache value) {
        this.nearCache = value;
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
     * Gets the value of property indexes.
     * 
     * @return
     *     possible object is
     *     {@link Indexes }
     *     
     */
    public Indexes getIndexes() {
        return indexes;
    }

    /**
     * Sets the value of property indexes.
     * 
     * @param value
     *     allowed object is
     *     {@link Indexes }
     *     
     */
    public void setIndexes(Indexes value) {
        this.indexes = value;
    }

    /**
     * Gets the value of property attributes.
     * 
     * @return
     *     possible object is
     *     {@link Attributes }
     *     
     */
    public Attributes getAttributes() {
        return attributes;
    }

    /**
     * Sets the value of property attributes.
     * 
     * @param value
     *     allowed object is
     *     {@link Attributes }
     *     
     */
    public void setAttributes(Attributes value) {
        this.attributes = value;
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
     * Gets the value of property partitionStrategy.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPartitionStrategy() {
        return partitionStrategy;
    }

    /**
     * Sets the value of property partitionStrategy.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPartitionStrategy(String value) {
        this.partitionStrategy = value;
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
     * Gets the value of property queryCaches.
     * 
     * @return
     *     possible object is
     *     {@link QueryCaches }
     *     
     */
    public QueryCaches getQueryCaches() {
        return queryCaches;
    }

    /**
     * Sets the value of property queryCaches.
     * 
     * @param value
     *     allowed object is
     *     {@link QueryCaches }
     *     
     */
    public void setQueryCaches(QueryCaches value) {
        this.queryCaches = value;
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
     *       &lt;sequence>
     *         &lt;element name="attribute" type="{http://www.hazelcast.com/schema/config}map-attribute" maxOccurs="unbounded" minOccurs="0"/>
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
        "attribute"
    })
    public static class Attributes {

        @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
        protected List<MapAttribute> attribute;

        /**
         * Gets the value of the attribute property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the attribute property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getAttribute().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link MapAttribute }
         * 
         * 
         */
        public List<MapAttribute> getAttribute() {
            if (attribute == null) {
                attribute = new ArrayList<MapAttribute>();
            }
            return this.attribute;
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
     *       &lt;sequence>
     *         &lt;element name="index" type="{http://www.hazelcast.com/schema/config}index" maxOccurs="unbounded" minOccurs="0"/>
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
        "index"
    })
    public static class Indexes {

        @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
        protected List<Index> index;

        /**
         * Gets the value of the index property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the index property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getIndex().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Index }
         * 
         * 
         */
        public List<Index> getIndex() {
            if (index == null) {
                index = new ArrayList<Index>();
            }
            return this.index;
        }

    }

}

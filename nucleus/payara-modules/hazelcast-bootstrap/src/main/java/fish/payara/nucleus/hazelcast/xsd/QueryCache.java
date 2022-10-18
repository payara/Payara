
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
 * <p>Java Class of query-cache complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="query-cache">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="include-value" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="predicate" type="{http://www.hazelcast.com/schema/config}predicate"/>
 *         &lt;element name="entry-listeners" type="{http://www.hazelcast.com/schema/config}entry-listeners" minOccurs="0"/>
 *         &lt;element name="in-memory-format" type="{http://www.hazelcast.com/schema/config}in-memory-format" minOccurs="0"/>
 *         &lt;element name="populate" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="coalesce" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="delay-seconds" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="batch-size" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="buffer-size" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="eviction" type="{http://www.hazelcast.com/schema/config}eviction" minOccurs="0"/>
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
@XmlType(name = "query-cache", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class QueryCache {

    @XmlElement(name = "include-value", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean includeValue;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected Predicate predicate;
    @XmlElement(name = "entry-listeners", namespace = "http://www.hazelcast.com/schema/config")
    protected EntryListeners entryListeners;
    @XmlElement(name = "in-memory-format", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "BINARY")
    @XmlSchemaType(name = "string")
    protected InMemoryFormat inMemoryFormat;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean populate;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean coalesce;
    @XmlElement(name = "delay-seconds", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    @XmlSchemaType(name = "unsignedInt")
    protected Long delaySeconds;
    @XmlElement(name = "batch-size", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "1")
    @XmlSchemaType(name = "unsignedInt")
    protected Long batchSize;
    @XmlElement(name = "buffer-size", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "16")
    @XmlSchemaType(name = "unsignedInt")
    protected Long bufferSize;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Eviction eviction;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Indexes indexes;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    /**
     * Gets the value of property includeValue.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isIncludeValue() {
        return includeValue;
    }

    /**
     * Sets the value of property includeValue.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIncludeValue(Boolean value) {
        this.includeValue = value;
    }

    /**
     * Gets the value of property predicate.
     * 
     * @return
     *     possible object is
     *     {@link Predicate }
     *     
     */
    public Predicate getPredicate() {
        return predicate;
    }

    /**
     * Sets the value of property predicate.
     * 
     * @param value
     *     allowed object is
     *     {@link Predicate }
     *     
     */
    public void setPredicate(Predicate value) {
        this.predicate = value;
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
     * Gets the value of property populate.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isPopulate() {
        return populate;
    }

    /**
     * Sets the value of property populate.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setPopulate(Boolean value) {
        this.populate = value;
    }

    /**
     * Gets the value of property coalesce.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isCoalesce() {
        return coalesce;
    }

    /**
     * Sets the value of property coalesce.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setCoalesce(Boolean value) {
        this.coalesce = value;
    }

    /**
     * Gets the value of property delaySeconds.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getDelaySeconds() {
        return delaySeconds;
    }

    /**
     * Sets the value of property delaySeconds.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setDelaySeconds(Long value) {
        this.delaySeconds = value;
    }

    /**
     * Gets the value of property batchSize.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getBatchSize() {
        return batchSize;
    }

    /**
     * Sets the value of property batchSize.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setBatchSize(Long value) {
        this.batchSize = value;
    }

    /**
     * Gets the value of property bufferSize.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getBufferSize() {
        return bufferSize;
    }

    /**
     * Sets the value of property bufferSize.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setBufferSize(Long value) {
        this.bufferSize = value;
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

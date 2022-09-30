
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of near-cache complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="near-cache">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="in-memory-format" type="{http://www.hazelcast.com/schema/config}in-memory-format" minOccurs="0"/>
 *         &lt;element name="serialize-keys" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="invalidate-on-change" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="time-to-live-seconds" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="max-idle-seconds" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="eviction" type="{http://www.hazelcast.com/schema/config}eviction" minOccurs="0"/>
 *         &lt;element name="cache-local-entries" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" default="default" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "near-cache", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class NearCache {

    @XmlElement(name = "in-memory-format", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "BINARY")
    @XmlSchemaType(name = "string")
    protected InMemoryFormat inMemoryFormat;
    @XmlElement(name = "serialize-keys", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean serializeKeys;
    @XmlElement(name = "invalidate-on-change", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean invalidateOnChange;
    @XmlElement(name = "time-to-live-seconds", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    @XmlSchemaType(name = "unsignedInt")
    protected Long timeToLiveSeconds;
    @XmlElement(name = "max-idle-seconds", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    @XmlSchemaType(name = "unsignedInt")
    protected Long maxIdleSeconds;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Eviction eviction;
    @XmlElement(name = "cache-local-entries", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean cacheLocalEntries;
    @XmlAttribute(name = "name")
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
     * Gets the value of property serializeKeys.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isSerializeKeys() {
        return serializeKeys;
    }

    /**
     * Sets the value of property serializeKeys.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSerializeKeys(Boolean value) {
        this.serializeKeys = value;
    }

    /**
     * Gets the value of property invalidateOnChange.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isInvalidateOnChange() {
        return invalidateOnChange;
    }

    /**
     * Sets the value of property invalidateOnChange.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setInvalidateOnChange(Boolean value) {
        this.invalidateOnChange = value;
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
     * Gets the value of property cacheLocalEntries.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isCacheLocalEntries() {
        return cacheLocalEntries;
    }

    /**
     * Sets the value of property cacheLocalEntries.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setCacheLocalEntries(Boolean value) {
        this.cacheLocalEntries = value;
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

}

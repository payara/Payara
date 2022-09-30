
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of flake-id-generator complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="flake-id-generator">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="prefetch-count" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}int">
 *               &lt;minInclusive value="1"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="prefetch-validity-millis" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/>
 *         &lt;element name="epoch-start" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/>
 *         &lt;element name="node-id-offset" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/>
 *         &lt;element name="bits-sequence" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}int">
 *               &lt;minInclusive value="0"/>
 *               &lt;maxInclusive value="63"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="bits-node-id" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}int">
 *               &lt;minInclusive value="0"/>
 *               &lt;maxInclusive value="63"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="allowed-future-millis" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}long">
 *               &lt;minInclusive value="0"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="statistics-enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
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
@XmlType(name = "flake-id-generator", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class FlakeIdGenerator {

    @XmlElement(name = "prefetch-count", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "100")
    protected Integer prefetchCount;
    @XmlElement(name = "prefetch-validity-millis", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "600000")
    protected Long prefetchValidityMillis;
    @XmlElement(name = "epoch-start", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "1514764800000")
    protected Long epochStart;
    @XmlElement(name = "node-id-offset", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    protected Long nodeIdOffset;
    @XmlElement(name = "bits-sequence", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "6")
    protected Integer bitsSequence;
    @XmlElement(name = "bits-node-id", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "16")
    protected Integer bitsNodeId;
    @XmlElement(name = "allowed-future-millis", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "15000")
    protected Long allowedFutureMillis;
    @XmlElement(name = "statistics-enabled", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean statisticsEnabled;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    /**
     * Gets the value of property prefetchCount.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getPrefetchCount() {
        return prefetchCount;
    }

    /**
     * Sets the value of property prefetchCount.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setPrefetchCount(Integer value) {
        this.prefetchCount = value;
    }

    /**
     * Gets the value of property prefetchValidityMillis.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getPrefetchValidityMillis() {
        return prefetchValidityMillis;
    }

    /**
     * Sets the value of property prefetchValidityMillis.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setPrefetchValidityMillis(Long value) {
        this.prefetchValidityMillis = value;
    }

    /**
     * Gets the value of property epochStart.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getEpochStart() {
        return epochStart;
    }

    /**
     * Sets the value of property epochStart.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setEpochStart(Long value) {
        this.epochStart = value;
    }

    /**
     * Gets the value of property nodeIdOffset.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getNodeIdOffset() {
        return nodeIdOffset;
    }

    /**
     * Sets the value of property nodeIdOffset.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setNodeIdOffset(Long value) {
        this.nodeIdOffset = value;
    }

    /**
     * Gets the value of property bitsSequence.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getBitsSequence() {
        return bitsSequence;
    }

    /**
     * Sets the value of property bitsSequence.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setBitsSequence(Integer value) {
        this.bitsSequence = value;
    }

    /**
     * Gets the value of property bitsNodeId.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getBitsNodeId() {
        return bitsNodeId;
    }

    /**
     * Sets the value of property bitsNodeId.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setBitsNodeId(Integer value) {
        this.bitsNodeId = value;
    }

    /**
     * Gets the value of property allowedFutureMillis.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getAllowedFutureMillis() {
        return allowedFutureMillis;
    }

    /**
     * Sets the value of property allowedFutureMillis.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setAllowedFutureMillis(Long value) {
        this.allowedFutureMillis = value;
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

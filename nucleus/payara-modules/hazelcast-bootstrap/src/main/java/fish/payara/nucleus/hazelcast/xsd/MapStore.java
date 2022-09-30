
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of map-store complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="map-store">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element ref="{http://www.hazelcast.com/schema/config}factory-or-class-name" minOccurs="0"/>
 *         &lt;element name="write-delay-seconds" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="write-batch-size" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="write-coalescing" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="properties" type="{http://www.hazelcast.com/schema/config}properties" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *       &lt;attribute name="initial-mode">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *             &lt;enumeration value="LAZY"/>
 *             &lt;enumeration value="EAGER"/>
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
@XmlType(name = "map-store", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class MapStore {

    @XmlElementRef(name = "factory-or-class-name", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false)
    protected JAXBElement<Object> factoryOrClassName;
    @XmlElement(name = "write-delay-seconds", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    @XmlSchemaType(name = "unsignedInt")
    protected Long writeDelaySeconds;
    @XmlElement(name = "write-batch-size", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "1")
    @XmlSchemaType(name = "unsignedInt")
    protected Long writeBatchSize;
    @XmlElement(name = "write-coalescing", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean writeCoalescing;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Properties properties;
    @XmlAttribute(name = "enabled")
    protected Boolean enabled;
    @XmlAttribute(name = "initial-mode")
    protected String initialMode;

    /**
     * 
     *                         The name of the class implementing MapLoader and/or MapStore.
     *                     
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Object }{@code >}
     *     {@link JAXBElement }{@code <}{@link Object }{@code >}
     *     {@link JAXBElement }{@code <}{@link Object }{@code >}
     *     
     */
    public JAXBElement<Object> getFactoryOrClassName() {
        return factoryOrClassName;
    }

    /**
     * Sets the value of property factoryOrClassName.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Object }{@code >}
     *     {@link JAXBElement }{@code <}{@link Object }{@code >}
     *     {@link JAXBElement }{@code <}{@link Object }{@code >}
     *     
     */
    public void setFactoryOrClassName(JAXBElement<Object> value) {
        this.factoryOrClassName = value;
    }

    /**
     * Gets the value of property writeDelaySeconds.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getWriteDelaySeconds() {
        return writeDelaySeconds;
    }

    /**
     * Sets the value of property writeDelaySeconds.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setWriteDelaySeconds(Long value) {
        this.writeDelaySeconds = value;
    }

    /**
     * Gets the value of property writeBatchSize.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getWriteBatchSize() {
        return writeBatchSize;
    }

    /**
     * Sets the value of property writeBatchSize.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setWriteBatchSize(Long value) {
        this.writeBatchSize = value;
    }

    /**
     * Gets the value of property writeCoalescing.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isWriteCoalescing() {
        return writeCoalescing;
    }

    /**
     * Sets the value of property writeCoalescing.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setWriteCoalescing(Boolean value) {
        this.writeCoalescing = value;
    }

    /**
     * Gets the value of property properties.
     * 
     * @return
     *     possible object is
     *     {@link Properties }
     *     
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets the value of property properties.
     * 
     * @param value
     *     allowed object is
     *     {@link Properties }
     *     
     */
    public void setProperties(Properties value) {
        this.properties = value;
    }

    /**
     * Gets the value of property enabled.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isEnabled() {
        if (enabled == null) {
            return true;
        } else {
            return enabled;
        }
    }

    /**
     * Sets the value of property enabled.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEnabled(Boolean value) {
        this.enabled = value;
    }

    /**
     * Gets the value of property initialMode.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getInitialMode() {
        return initialMode;
    }

    /**
     * Sets the value of property initialMode.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setInitialMode(String value) {
        this.initialMode = value;
    }

}

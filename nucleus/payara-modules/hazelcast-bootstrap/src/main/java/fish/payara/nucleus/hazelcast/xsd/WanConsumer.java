
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 Config for processing WAN events received from a target cluster.
 *                 You can configure certain behaviour when processing incoming WAN events
 *                 or even configure your own implementation for a WAN consumer. A custom
 *                 WAN consumer allows you to define custom processing logic and is usually
 *                 used in combination with a custom WAN publisher.
 *                 A custom consumer is optional and you may simply omit defining it which
 *                 will cause the default processing logic to be used.
 *             
 * 
 * <p>Java Class of wan-consumer complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="wan-consumer">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="class-name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="properties" type="{http://www.hazelcast.com/schema/config}properties" minOccurs="0"/>
 *         &lt;element name="persist-wan-replicated-data" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "wan-consumer", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class WanConsumer {

    @XmlElement(name = "class-name", namespace = "http://www.hazelcast.com/schema/config")
    protected String className;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Properties properties;
    @XmlElement(name = "persist-wan-replicated-data", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean persistWanReplicatedData;

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
     * Gets the value of property persistWanReplicatedData.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isPersistWanReplicatedData() {
        return persistWanReplicatedData;
    }

    /**
     * Sets the value of property persistWanReplicatedData.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setPersistWanReplicatedData(Boolean value) {
        this.persistWanReplicatedData = value;
    }

}

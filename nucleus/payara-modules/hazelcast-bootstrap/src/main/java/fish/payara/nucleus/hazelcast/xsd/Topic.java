
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of topic complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="topic">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="statistics-enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="global-ordering-enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="message-listeners" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="message-listener" type="{http://www.hazelcast.com/schema/config}listener-base" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="multi-threading-enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
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
@XmlType(name = "topic", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Topic {

    @XmlElement(name = "statistics-enabled", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean statisticsEnabled;
    @XmlElement(name = "global-ordering-enabled", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean globalOrderingEnabled;
    @XmlElement(name = "message-listeners", namespace = "http://www.hazelcast.com/schema/config")
    protected MessageListeners messageListeners;
    @XmlElement(name = "multi-threading-enabled", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean multiThreadingEnabled;
    @XmlAttribute(name = "name")
    protected String name;

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
     * Gets the value of property globalOrderingEnabled.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isGlobalOrderingEnabled() {
        return globalOrderingEnabled;
    }

    /**
     * Sets the value of property globalOrderingEnabled.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setGlobalOrderingEnabled(Boolean value) {
        this.globalOrderingEnabled = value;
    }

    /**
     * Gets the value of property messageListeners.
     * 
     * @return
     *     possible object is
     *     {@link MessageListeners }
     *     
     */
    public MessageListeners getMessageListeners() {
        return messageListeners;
    }

    /**
     * Sets the value of property messageListeners.
     * 
     * @param value
     *     allowed object is
     *     {@link MessageListeners }
     *     
     */
    public void setMessageListeners(MessageListeners value) {
        this.messageListeners = value;
    }

    /**
     * Gets the value of property multiThreadingEnabled.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isMultiThreadingEnabled() {
        return multiThreadingEnabled;
    }

    /**
     * Sets the value of property multiThreadingEnabled.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setMultiThreadingEnabled(Boolean value) {
        this.multiThreadingEnabled = value;
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
     *         &lt;element name="message-listener" type="{http://www.hazelcast.com/schema/config}listener-base" maxOccurs="unbounded" minOccurs="0"/>
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
        "messageListener"
    })
    public static class MessageListeners {

        @XmlElement(name = "message-listener", namespace = "http://www.hazelcast.com/schema/config")
        protected List<ListenerBase> messageListener;

        /**
         * Gets the value of the messageListener property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the messageListener property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getMessageListener().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link ListenerBase }
         * 
         * 
         */
        public List<ListenerBase> getMessageListener() {
            if (messageListener == null) {
                messageListener = new ArrayList<ListenerBase>();
            }
            return this.messageListener;
        }

    }

}

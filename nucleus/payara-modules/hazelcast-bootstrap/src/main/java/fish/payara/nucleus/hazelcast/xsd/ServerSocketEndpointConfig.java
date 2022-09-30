
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of server-socket-endpoint-config complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="server-socket-endpoint-config">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="port" type="{http://www.hazelcast.com/schema/config}port" minOccurs="0"/>
 *         &lt;element name="public-address" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="reuse-address" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="outbound-ports" type="{http://www.hazelcast.com/schema/config}outbound-ports" minOccurs="0"/>
 *         &lt;element name="interfaces" type="{http://www.hazelcast.com/schema/config}interfaces" minOccurs="0"/>
 *         &lt;element name="ssl" type="{http://www.hazelcast.com/schema/config}factory-class-with-properties" minOccurs="0"/>
 *         &lt;element name="socket-interceptor" type="{http://www.hazelcast.com/schema/config}socket-interceptor" minOccurs="0"/>
 *         &lt;element name="symmetric-encryption" type="{http://www.hazelcast.com/schema/config}symmetric-encryption" minOccurs="0"/>
 *         &lt;element name="socket-options" type="{http://www.hazelcast.com/schema/config}socket-options" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" default="" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "server-socket-endpoint-config", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class ServerSocketEndpointConfig {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Port port;
    @XmlElement(name = "public-address", namespace = "http://www.hazelcast.com/schema/config")
    protected String publicAddress;
    @XmlElement(name = "reuse-address", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean reuseAddress;
    @XmlElement(name = "outbound-ports", namespace = "http://www.hazelcast.com/schema/config")
    protected OutboundPorts outboundPorts;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Interfaces interfaces;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected FactoryClassWithProperties ssl;
    @XmlElement(name = "socket-interceptor", namespace = "http://www.hazelcast.com/schema/config")
    protected SocketInterceptor socketInterceptor;
    @XmlElement(name = "symmetric-encryption", namespace = "http://www.hazelcast.com/schema/config")
    protected SymmetricEncryption symmetricEncryption;
    @XmlElement(name = "socket-options", namespace = "http://www.hazelcast.com/schema/config")
    protected SocketOptions socketOptions;
    @XmlAttribute(name = "name")
    protected String name;

    /**
     * Gets the value of property port.
     * 
     * @return
     *     possible object is
     *     {@link Port }
     *     
     */
    public Port getPort() {
        return port;
    }

    /**
     * Sets the value of property port.
     * 
     * @param value
     *     allowed object is
     *     {@link Port }
     *     
     */
    public void setPort(Port value) {
        this.port = value;
    }

    /**
     * Gets the value of property publicAddress.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPublicAddress() {
        return publicAddress;
    }

    /**
     * Sets the value of property publicAddress.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPublicAddress(String value) {
        this.publicAddress = value;
    }

    /**
     * Gets the value of property reuseAddress.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isReuseAddress() {
        return reuseAddress;
    }

    /**
     * Sets the value of property reuseAddress.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setReuseAddress(Boolean value) {
        this.reuseAddress = value;
    }

    /**
     * Gets the value of property outboundPorts.
     * 
     * @return
     *     possible object is
     *     {@link OutboundPorts }
     *     
     */
    public OutboundPorts getOutboundPorts() {
        return outboundPorts;
    }

    /**
     * Sets the value of property outboundPorts.
     * 
     * @param value
     *     allowed object is
     *     {@link OutboundPorts }
     *     
     */
    public void setOutboundPorts(OutboundPorts value) {
        this.outboundPorts = value;
    }

    /**
     * Gets the value of property interfaces.
     * 
     * @return
     *     possible object is
     *     {@link Interfaces }
     *     
     */
    public Interfaces getInterfaces() {
        return interfaces;
    }

    /**
     * Sets the value of property interfaces.
     * 
     * @param value
     *     allowed object is
     *     {@link Interfaces }
     *     
     */
    public void setInterfaces(Interfaces value) {
        this.interfaces = value;
    }

    /**
     * Gets the value of property ssl.
     * 
     * @return
     *     possible object is
     *     {@link FactoryClassWithProperties }
     *     
     */
    public FactoryClassWithProperties getSsl() {
        return ssl;
    }

    /**
     * Sets the value of property ssl.
     * 
     * @param value
     *     allowed object is
     *     {@link FactoryClassWithProperties }
     *     
     */
    public void setSsl(FactoryClassWithProperties value) {
        this.ssl = value;
    }

    /**
     * Gets the value of property socketInterceptor.
     * 
     * @return
     *     possible object is
     *     {@link SocketInterceptor }
     *     
     */
    public SocketInterceptor getSocketInterceptor() {
        return socketInterceptor;
    }

    /**
     * Sets the value of property socketInterceptor.
     * 
     * @param value
     *     allowed object is
     *     {@link SocketInterceptor }
     *     
     */
    public void setSocketInterceptor(SocketInterceptor value) {
        this.socketInterceptor = value;
    }

    /**
     * Gets the value of property symmetricEncryption.
     * 
     * @return
     *     possible object is
     *     {@link SymmetricEncryption }
     *     
     */
    public SymmetricEncryption getSymmetricEncryption() {
        return symmetricEncryption;
    }

    /**
     * Sets the value of property symmetricEncryption.
     * 
     * @param value
     *     allowed object is
     *     {@link SymmetricEncryption }
     *     
     */
    public void setSymmetricEncryption(SymmetricEncryption value) {
        this.symmetricEncryption = value;
    }

    /**
     * Gets the value of property socketOptions.
     * 
     * @return
     *     possible object is
     *     {@link SocketOptions }
     *     
     */
    public SocketOptions getSocketOptions() {
        return socketOptions;
    }

    /**
     * Sets the value of property socketOptions.
     * 
     * @param value
     *     allowed object is
     *     {@link SocketOptions }
     *     
     */
    public void setSocketOptions(SocketOptions value) {
        this.socketOptions = value;
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
            return "";
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

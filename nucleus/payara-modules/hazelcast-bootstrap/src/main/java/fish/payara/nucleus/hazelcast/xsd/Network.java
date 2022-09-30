
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of network complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="network">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="public-address" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="port" type="{http://www.hazelcast.com/schema/config}port" minOccurs="0"/>
 *         &lt;element name="reuse-address" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="outbound-ports" type="{http://www.hazelcast.com/schema/config}outbound-ports" minOccurs="0"/>
 *         &lt;element name="join" type="{http://www.hazelcast.com/schema/config}join" minOccurs="0"/>
 *         &lt;element name="interfaces" type="{http://www.hazelcast.com/schema/config}interfaces" minOccurs="0"/>
 *         &lt;element name="ssl" type="{http://www.hazelcast.com/schema/config}factory-class-with-properties" minOccurs="0"/>
 *         &lt;element name="socket-interceptor" type="{http://www.hazelcast.com/schema/config}socket-interceptor" minOccurs="0"/>
 *         &lt;element name="symmetric-encryption" type="{http://www.hazelcast.com/schema/config}symmetric-encryption" minOccurs="0"/>
 *         &lt;element name="member-address-provider" type="{http://www.hazelcast.com/schema/config}member-address-provider" minOccurs="0"/>
 *         &lt;element name="failure-detector" type="{http://www.hazelcast.com/schema/config}failure-detector" minOccurs="0"/>
 *         &lt;element name="rest-api" type="{http://www.hazelcast.com/schema/config}rest-api" minOccurs="0"/>
 *         &lt;element name="memcache-protocol" type="{http://www.hazelcast.com/schema/config}memcache-protocol" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "network", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Network {

    @XmlElement(name = "public-address", namespace = "http://www.hazelcast.com/schema/config")
    protected String publicAddress;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Port port;
    @XmlElement(name = "reuse-address", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean reuseAddress;
    @XmlElement(name = "outbound-ports", namespace = "http://www.hazelcast.com/schema/config")
    protected OutboundPorts outboundPorts;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Join join;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Interfaces interfaces;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected FactoryClassWithProperties ssl;
    @XmlElement(name = "socket-interceptor", namespace = "http://www.hazelcast.com/schema/config")
    protected SocketInterceptor socketInterceptor;
    @XmlElement(name = "symmetric-encryption", namespace = "http://www.hazelcast.com/schema/config")
    protected SymmetricEncryption symmetricEncryption;
    @XmlElement(name = "member-address-provider", namespace = "http://www.hazelcast.com/schema/config")
    protected MemberAddressProvider memberAddressProvider;
    @XmlElement(name = "failure-detector", namespace = "http://www.hazelcast.com/schema/config")
    protected FailureDetector failureDetector;
    @XmlElement(name = "rest-api", namespace = "http://www.hazelcast.com/schema/config")
    protected RestApi restApi;
    @XmlElement(name = "memcache-protocol", namespace = "http://www.hazelcast.com/schema/config")
    protected MemcacheProtocol memcacheProtocol;

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
     * Gets the value of property join.
     * 
     * @return
     *     possible object is
     *     {@link Join }
     *     
     */
    public Join getJoin() {
        return join;
    }

    /**
     * Sets the value of property join.
     * 
     * @param value
     *     allowed object is
     *     {@link Join }
     *     
     */
    public void setJoin(Join value) {
        this.join = value;
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
     * Gets the value of property memberAddressProvider.
     * 
     * @return
     *     possible object is
     *     {@link MemberAddressProvider }
     *     
     */
    public MemberAddressProvider getMemberAddressProvider() {
        return memberAddressProvider;
    }

    /**
     * Sets the value of property memberAddressProvider.
     * 
     * @param value
     *     allowed object is
     *     {@link MemberAddressProvider }
     *     
     */
    public void setMemberAddressProvider(MemberAddressProvider value) {
        this.memberAddressProvider = value;
    }

    /**
     * Gets the value of property failureDetector.
     * 
     * @return
     *     possible object is
     *     {@link FailureDetector }
     *     
     */
    public FailureDetector getFailureDetector() {
        return failureDetector;
    }

    /**
     * Sets the value of property failureDetector.
     * 
     * @param value
     *     allowed object is
     *     {@link FailureDetector }
     *     
     */
    public void setFailureDetector(FailureDetector value) {
        this.failureDetector = value;
    }

    /**
     * Gets the value of property restApi.
     * 
     * @return
     *     possible object is
     *     {@link RestApi }
     *     
     */
    public RestApi getRestApi() {
        return restApi;
    }

    /**
     * Sets the value of property restApi.
     * 
     * @param value
     *     allowed object is
     *     {@link RestApi }
     *     
     */
    public void setRestApi(RestApi value) {
        this.restApi = value;
    }

    /**
     * Gets the value of property memcacheProtocol.
     * 
     * @return
     *     possible object is
     *     {@link MemcacheProtocol }
     *     
     */
    public MemcacheProtocol getMemcacheProtocol() {
        return memcacheProtocol;
    }

    /**
     * Sets the value of property memcacheProtocol.
     * 
     * @param value
     *     allowed object is
     *     {@link MemcacheProtocol }
     *     
     */
    public void setMemcacheProtocol(MemcacheProtocol value) {
        this.memcacheProtocol = value;
    }

}

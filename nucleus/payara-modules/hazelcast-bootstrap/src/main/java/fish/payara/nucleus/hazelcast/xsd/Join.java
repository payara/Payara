
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 The `join` configuration element is used to enable the Hazelcast instances to form a cluster,
 *                 i.e. to join the members. Three ways can be used to join the members: discovery by TCP/IP, by
 *                 multicast, and by discovery on AWS (EC2 auto-discovery).
 *             
 * 
 * <p>Java Class of join complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="join">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="multicast" type="{http://www.hazelcast.com/schema/config}multicast" minOccurs="0"/>
 *         &lt;element name="tcp-ip" type="{http://www.hazelcast.com/schema/config}tcp-ip" minOccurs="0"/>
 *         &lt;element name="aws" type="{http://www.hazelcast.com/schema/config}aliased-discovery-strategy" minOccurs="0"/>
 *         &lt;element name="gcp" type="{http://www.hazelcast.com/schema/config}aliased-discovery-strategy" minOccurs="0"/>
 *         &lt;element name="azure" type="{http://www.hazelcast.com/schema/config}aliased-discovery-strategy" minOccurs="0"/>
 *         &lt;element name="kubernetes" type="{http://www.hazelcast.com/schema/config}aliased-discovery-strategy" minOccurs="0"/>
 *         &lt;element name="eureka" type="{http://www.hazelcast.com/schema/config}aliased-discovery-strategy" minOccurs="0"/>
 *         &lt;element name="auto-detection" type="{http://www.hazelcast.com/schema/config}aliased-discovery-strategy" minOccurs="0"/>
 *         &lt;element name="discovery-strategies" type="{http://www.hazelcast.com/schema/config}discovery-strategies" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "join", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Join {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Multicast multicast;
    @XmlElement(name = "tcp-ip", namespace = "http://www.hazelcast.com/schema/config")
    protected TcpIp tcpIp;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected AliasedDiscoveryStrategy aws;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected AliasedDiscoveryStrategy gcp;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected AliasedDiscoveryStrategy azure;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected AliasedDiscoveryStrategy kubernetes;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected AliasedDiscoveryStrategy eureka;
    @XmlElement(name = "auto-detection", namespace = "http://www.hazelcast.com/schema/config")
    protected AliasedDiscoveryStrategy autoDetection;
    @XmlElement(name = "discovery-strategies", namespace = "http://www.hazelcast.com/schema/config")
    protected DiscoveryStrategies discoveryStrategies;

    /**
     * Gets the value of property multicast.
     * 
     * @return
     *     possible object is
     *     {@link Multicast }
     *     
     */
    public Multicast getMulticast() {
        return multicast;
    }

    /**
     * Sets the value of property multicast.
     * 
     * @param value
     *     allowed object is
     *     {@link Multicast }
     *     
     */
    public void setMulticast(Multicast value) {
        this.multicast = value;
    }

    /**
     * Gets the value of property tcpIp.
     * 
     * @return
     *     possible object is
     *     {@link TcpIp }
     *     
     */
    public TcpIp getTcpIp() {
        return tcpIp;
    }

    /**
     * Sets the value of property tcpIp.
     * 
     * @param value
     *     allowed object is
     *     {@link TcpIp }
     *     
     */
    public void setTcpIp(TcpIp value) {
        this.tcpIp = value;
    }

    /**
     * Gets the value of property aws.
     * 
     * @return
     *     possible object is
     *     {@link AliasedDiscoveryStrategy }
     *     
     */
    public AliasedDiscoveryStrategy getAws() {
        return aws;
    }

    /**
     * Sets the value of property aws.
     * 
     * @param value
     *     allowed object is
     *     {@link AliasedDiscoveryStrategy }
     *     
     */
    public void setAws(AliasedDiscoveryStrategy value) {
        this.aws = value;
    }

    /**
     * Gets the value of property gcp.
     * 
     * @return
     *     possible object is
     *     {@link AliasedDiscoveryStrategy }
     *     
     */
    public AliasedDiscoveryStrategy getGcp() {
        return gcp;
    }

    /**
     * Sets the value of property gcp.
     * 
     * @param value
     *     allowed object is
     *     {@link AliasedDiscoveryStrategy }
     *     
     */
    public void setGcp(AliasedDiscoveryStrategy value) {
        this.gcp = value;
    }

    /**
     * Gets the value of property azure.
     * 
     * @return
     *     possible object is
     *     {@link AliasedDiscoveryStrategy }
     *     
     */
    public AliasedDiscoveryStrategy getAzure() {
        return azure;
    }

    /**
     * Sets the value of property azure.
     * 
     * @param value
     *     allowed object is
     *     {@link AliasedDiscoveryStrategy }
     *     
     */
    public void setAzure(AliasedDiscoveryStrategy value) {
        this.azure = value;
    }

    /**
     * Gets the value of property kubernetes.
     * 
     * @return
     *     possible object is
     *     {@link AliasedDiscoveryStrategy }
     *     
     */
    public AliasedDiscoveryStrategy getKubernetes() {
        return kubernetes;
    }

    /**
     * Sets the value of property kubernetes.
     * 
     * @param value
     *     allowed object is
     *     {@link AliasedDiscoveryStrategy }
     *     
     */
    public void setKubernetes(AliasedDiscoveryStrategy value) {
        this.kubernetes = value;
    }

    /**
     * Gets the value of property eureka.
     * 
     * @return
     *     possible object is
     *     {@link AliasedDiscoveryStrategy }
     *     
     */
    public AliasedDiscoveryStrategy getEureka() {
        return eureka;
    }

    /**
     * Sets the value of property eureka.
     * 
     * @param value
     *     allowed object is
     *     {@link AliasedDiscoveryStrategy }
     *     
     */
    public void setEureka(AliasedDiscoveryStrategy value) {
        this.eureka = value;
    }

    /**
     * Gets the value of property autoDetection.
     * 
     * @return
     *     possible object is
     *     {@link AliasedDiscoveryStrategy }
     *     
     */
    public AliasedDiscoveryStrategy getAutoDetection() {
        return autoDetection;
    }

    /**
     * Sets the value of property autoDetection.
     * 
     * @param value
     *     allowed object is
     *     {@link AliasedDiscoveryStrategy }
     *     
     */
    public void setAutoDetection(AliasedDiscoveryStrategy value) {
        this.autoDetection = value;
    }

    /**
     * Gets the value of property discoveryStrategies.
     * 
     * @return
     *     possible object is
     *     {@link DiscoveryStrategies }
     *     
     */
    public DiscoveryStrategies getDiscoveryStrategies() {
        return discoveryStrategies;
    }

    /**
     * Sets the value of property discoveryStrategies.
     * 
     * @param value
     *     allowed object is
     *     {@link DiscoveryStrategies }
     *     
     */
    public void setDiscoveryStrategies(DiscoveryStrategies value) {
        this.discoveryStrategies = value;
    }

}

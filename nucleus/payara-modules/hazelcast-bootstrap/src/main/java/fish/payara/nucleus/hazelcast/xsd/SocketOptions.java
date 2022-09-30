
package fish.payara.nucleus.hazelcast.xsd;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of socket-options complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="socket-options">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="buffer-direct" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="tcp-no-delay" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="keep-alive" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="connect-timeout-seconds" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" minOccurs="0"/>
 *         &lt;element name="send-buffer-size-kb" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" minOccurs="0"/>
 *         &lt;element name="receive-buffer-size-kb" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" minOccurs="0"/>
 *         &lt;element name="linger-seconds" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "socket-options", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class SocketOptions {

    @XmlElement(name = "buffer-direct", namespace = "http://www.hazelcast.com/schema/config")
    protected Boolean bufferDirect;
    @XmlElement(name = "tcp-no-delay", namespace = "http://www.hazelcast.com/schema/config")
    protected Boolean tcpNoDelay;
    @XmlElement(name = "keep-alive", namespace = "http://www.hazelcast.com/schema/config")
    protected Boolean keepAlive;
    @XmlElement(name = "connect-timeout-seconds", namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger connectTimeoutSeconds;
    @XmlElement(name = "send-buffer-size-kb", namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger sendBufferSizeKb;
    @XmlElement(name = "receive-buffer-size-kb", namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger receiveBufferSizeKb;
    @XmlElement(name = "linger-seconds", namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger lingerSeconds;

    /**
     * Gets the value of property bufferDirect.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isBufferDirect() {
        return bufferDirect;
    }

    /**
     * Sets the value of property bufferDirect.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setBufferDirect(Boolean value) {
        this.bufferDirect = value;
    }

    /**
     * Gets the value of property tcpNoDelay.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    /**
     * Sets the value of property tcpNoDelay.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setTcpNoDelay(Boolean value) {
        this.tcpNoDelay = value;
    }

    /**
     * Gets the value of property keepAlive.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isKeepAlive() {
        return keepAlive;
    }

    /**
     * Sets the value of property keepAlive.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setKeepAlive(Boolean value) {
        this.keepAlive = value;
    }

    /**
     * Gets the value of property connectTimeoutSeconds.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    /**
     * Sets the value of property connectTimeoutSeconds.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setConnectTimeoutSeconds(BigInteger value) {
        this.connectTimeoutSeconds = value;
    }

    /**
     * Gets the value of property sendBufferSizeKb.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getSendBufferSizeKb() {
        return sendBufferSizeKb;
    }

    /**
     * Sets the value of property sendBufferSizeKb.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setSendBufferSizeKb(BigInteger value) {
        this.sendBufferSizeKb = value;
    }

    /**
     * Gets the value of property receiveBufferSizeKb.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getReceiveBufferSizeKb() {
        return receiveBufferSizeKb;
    }

    /**
     * Sets the value of property receiveBufferSizeKb.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setReceiveBufferSizeKb(BigInteger value) {
        this.receiveBufferSizeKb = value;
    }

    /**
     * Gets the value of property lingerSeconds.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getLingerSeconds() {
        return lingerSeconds;
    }

    /**
     * Sets the value of property lingerSeconds.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setLingerSeconds(BigInteger value) {
        this.lingerSeconds = value;
    }

}

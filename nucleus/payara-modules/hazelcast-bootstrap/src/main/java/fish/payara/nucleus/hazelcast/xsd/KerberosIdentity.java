
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of kerberos-identity complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="kerberos-identity">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="realm" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="security-realm" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="principal" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="keytab-file" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="service-name-prefix" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="spn" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="use-canonical-hostname" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "kerberos-identity", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class KerberosIdentity {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected String realm;
    @XmlElement(name = "security-realm", namespace = "http://www.hazelcast.com/schema/config")
    protected String securityRealm;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected String principal;
    @XmlElement(name = "keytab-file", namespace = "http://www.hazelcast.com/schema/config")
    protected String keytabFile;
    @XmlElement(name = "service-name-prefix", namespace = "http://www.hazelcast.com/schema/config")
    protected String serviceNamePrefix;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected String spn;
    @XmlElement(name = "use-canonical-hostname", namespace = "http://www.hazelcast.com/schema/config")
    protected Boolean useCanonicalHostname;

    /**
     * Gets the value of property realm.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Sets the value of property realm.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRealm(String value) {
        this.realm = value;
    }

    /**
     * Gets the value of property securityRealm.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSecurityRealm() {
        return securityRealm;
    }

    /**
     * Sets the value of property securityRealm.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSecurityRealm(String value) {
        this.securityRealm = value;
    }

    /**
     * Gets the value of property principal.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPrincipal() {
        return principal;
    }

    /**
     * Sets the value of property principal.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPrincipal(String value) {
        this.principal = value;
    }

    /**
     * Gets the value of property keytabFile.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getKeytabFile() {
        return keytabFile;
    }

    /**
     * Sets the value of property keytabFile.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setKeytabFile(String value) {
        this.keytabFile = value;
    }

    /**
     * Gets the value of property serviceNamePrefix.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getServiceNamePrefix() {
        return serviceNamePrefix;
    }

    /**
     * Sets the value of property serviceNamePrefix.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setServiceNamePrefix(String value) {
        this.serviceNamePrefix = value;
    }

    /**
     * Gets the value of property spn.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSpn() {
        return spn;
    }

    /**
     * Sets the value of property spn.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSpn(String value) {
        this.spn = value;
    }

    /**
     * Gets the value of property useCanonicalHostname.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isUseCanonicalHostname() {
        return useCanonicalHostname;
    }

    /**
     * Sets the value of property useCanonicalHostname.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setUseCanonicalHostname(Boolean value) {
        this.useCanonicalHostname = value;
    }

}

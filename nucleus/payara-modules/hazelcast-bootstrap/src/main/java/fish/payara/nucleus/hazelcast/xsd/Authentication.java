
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of authentication complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="authentication">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;element name="jaas" type="{http://www.hazelcast.com/schema/config}login-modules"/>
 *         &lt;element name="tls" type="{http://www.hazelcast.com/schema/config}tls-authentication"/>
 *         &lt;element name="ldap" type="{http://www.hazelcast.com/schema/config}ldap-authentication"/>
 *         &lt;element name="kerberos" type="{http://www.hazelcast.com/schema/config}kerberos-authentication"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "authentication", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "jaas",
    "tls",
    "ldap",
    "kerberos"
})
public class Authentication {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected LoginModules jaas;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected TlsAuthentication tls;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected LdapAuthentication ldap;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected KerberosAuthentication kerberos;

    /**
     * Gets the value of property jaas.
     * 
     * @return
     *     possible object is
     *     {@link LoginModules }
     *     
     */
    public LoginModules getJaas() {
        return jaas;
    }

    /**
     * Sets the value of property jaas.
     * 
     * @param value
     *     allowed object is
     *     {@link LoginModules }
     *     
     */
    public void setJaas(LoginModules value) {
        this.jaas = value;
    }

    /**
     * Gets the value of property tls.
     * 
     * @return
     *     possible object is
     *     {@link TlsAuthentication }
     *     
     */
    public TlsAuthentication getTls() {
        return tls;
    }

    /**
     * Sets the value of property tls.
     * 
     * @param value
     *     allowed object is
     *     {@link TlsAuthentication }
     *     
     */
    public void setTls(TlsAuthentication value) {
        this.tls = value;
    }

    /**
     * Gets the value of property ldap.
     * 
     * @return
     *     possible object is
     *     {@link LdapAuthentication }
     *     
     */
    public LdapAuthentication getLdap() {
        return ldap;
    }

    /**
     * Sets the value of property ldap.
     * 
     * @param value
     *     allowed object is
     *     {@link LdapAuthentication }
     *     
     */
    public void setLdap(LdapAuthentication value) {
        this.ldap = value;
    }

    /**
     * Gets the value of property kerberos.
     * 
     * @return
     *     possible object is
     *     {@link KerberosAuthentication }
     *     
     */
    public KerberosAuthentication getKerberos() {
        return kerberos;
    }

    /**
     * Sets the value of property kerberos.
     * 
     * @param value
     *     allowed object is
     *     {@link KerberosAuthentication }
     *     
     */
    public void setKerberos(KerberosAuthentication value) {
        this.kerberos = value;
    }

}

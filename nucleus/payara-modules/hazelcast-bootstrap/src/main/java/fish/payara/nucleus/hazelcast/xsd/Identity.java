
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of identity complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="identity">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;element name="username-password" type="{http://www.hazelcast.com/schema/config}username-password"/>
 *         &lt;element name="credentials-factory" type="{http://www.hazelcast.com/schema/config}security-object"/>
 *         &lt;element name="token" type="{http://www.hazelcast.com/schema/config}token"/>
 *         &lt;element name="kerberos" type="{http://www.hazelcast.com/schema/config}kerberos-identity"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "identity", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "usernamePassword",
    "credentialsFactory",
    "token",
    "kerberos"
})
public class Identity {

    @XmlElement(name = "username-password", namespace = "http://www.hazelcast.com/schema/config")
    protected UsernamePassword usernamePassword;
    @XmlElement(name = "credentials-factory", namespace = "http://www.hazelcast.com/schema/config")
    protected SecurityObject credentialsFactory;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Token token;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected KerberosIdentity kerberos;

    /**
     * Gets the value of property usernamePassword.
     * 
     * @return
     *     possible object is
     *     {@link UsernamePassword }
     *     
     */
    public UsernamePassword getUsernamePassword() {
        return usernamePassword;
    }

    /**
     * Sets the value of property usernamePassword.
     * 
     * @param value
     *     allowed object is
     *     {@link UsernamePassword }
     *     
     */
    public void setUsernamePassword(UsernamePassword value) {
        this.usernamePassword = value;
    }

    /**
     * Gets the value of property credentialsFactory.
     * 
     * @return
     *     possible object is
     *     {@link SecurityObject }
     *     
     */
    public SecurityObject getCredentialsFactory() {
        return credentialsFactory;
    }

    /**
     * Sets the value of property credentialsFactory.
     * 
     * @param value
     *     allowed object is
     *     {@link SecurityObject }
     *     
     */
    public void setCredentialsFactory(SecurityObject value) {
        this.credentialsFactory = value;
    }

    /**
     * Gets the value of property token.
     * 
     * @return
     *     possible object is
     *     {@link Token }
     *     
     */
    public Token getToken() {
        return token;
    }

    /**
     * Sets the value of property token.
     * 
     * @param value
     *     allowed object is
     *     {@link Token }
     *     
     */
    public void setToken(Token value) {
        this.token = value;
    }

    /**
     * Gets the value of property kerberos.
     * 
     * @return
     *     possible object is
     *     {@link KerberosIdentity }
     *     
     */
    public KerberosIdentity getKerberos() {
        return kerberos;
    }

    /**
     * Sets the value of property kerberos.
     * 
     * @param value
     *     allowed object is
     *     {@link KerberosIdentity }
     *     
     */
    public void setKerberos(KerberosIdentity value) {
        this.kerberos = value;
    }

}

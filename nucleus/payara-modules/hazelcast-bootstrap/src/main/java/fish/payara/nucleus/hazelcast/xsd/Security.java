
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of security complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="security">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="client-permission-policy" type="{http://www.hazelcast.com/schema/config}security-object" minOccurs="0"/>
 *         &lt;element name="client-permissions" type="{http://www.hazelcast.com/schema/config}permissions" minOccurs="0"/>
 *         &lt;element name="security-interceptors" type="{http://www.hazelcast.com/schema/config}interceptors" minOccurs="0"/>
 *         &lt;element name="client-block-unmapped-actions" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="realms" type="{http://www.hazelcast.com/schema/config}realms" minOccurs="0"/>
 *         &lt;element name="member-authentication" type="{http://www.hazelcast.com/schema/config}realm-reference" minOccurs="0"/>
 *         &lt;element name="client-authentication" type="{http://www.hazelcast.com/schema/config}realm-reference" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "security", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Security {

    @XmlElement(name = "client-permission-policy", namespace = "http://www.hazelcast.com/schema/config")
    protected SecurityObject clientPermissionPolicy;
    @XmlElement(name = "client-permissions", namespace = "http://www.hazelcast.com/schema/config")
    protected Permissions clientPermissions;
    @XmlElement(name = "security-interceptors", namespace = "http://www.hazelcast.com/schema/config")
    protected Interceptors securityInterceptors;
    @XmlElement(name = "client-block-unmapped-actions", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean clientBlockUnmappedActions;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Realms realms;
    @XmlElement(name = "member-authentication", namespace = "http://www.hazelcast.com/schema/config")
    protected RealmReference memberAuthentication;
    @XmlElement(name = "client-authentication", namespace = "http://www.hazelcast.com/schema/config")
    protected RealmReference clientAuthentication;
    @XmlAttribute(name = "enabled")
    protected Boolean enabled;

    /**
     * Gets the value of property clientPermissionPolicy.
     * 
     * @return
     *     possible object is
     *     {@link SecurityObject }
     *     
     */
    public SecurityObject getClientPermissionPolicy() {
        return clientPermissionPolicy;
    }

    /**
     * Sets the value of property clientPermissionPolicy.
     * 
     * @param value
     *     allowed object is
     *     {@link SecurityObject }
     *     
     */
    public void setClientPermissionPolicy(SecurityObject value) {
        this.clientPermissionPolicy = value;
    }

    /**
     * Gets the value of property clientPermissions.
     * 
     * @return
     *     possible object is
     *     {@link Permissions }
     *     
     */
    public Permissions getClientPermissions() {
        return clientPermissions;
    }

    /**
     * Sets the value of property clientPermissions.
     * 
     * @param value
     *     allowed object is
     *     {@link Permissions }
     *     
     */
    public void setClientPermissions(Permissions value) {
        this.clientPermissions = value;
    }

    /**
     * Gets the value of property securityInterceptors.
     * 
     * @return
     *     possible object is
     *     {@link Interceptors }
     *     
     */
    public Interceptors getSecurityInterceptors() {
        return securityInterceptors;
    }

    /**
     * Sets the value of property securityInterceptors.
     * 
     * @param value
     *     allowed object is
     *     {@link Interceptors }
     *     
     */
    public void setSecurityInterceptors(Interceptors value) {
        this.securityInterceptors = value;
    }

    /**
     * Gets the value of property clientBlockUnmappedActions.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isClientBlockUnmappedActions() {
        return clientBlockUnmappedActions;
    }

    /**
     * Sets the value of property clientBlockUnmappedActions.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setClientBlockUnmappedActions(Boolean value) {
        this.clientBlockUnmappedActions = value;
    }

    /**
     * Gets the value of property realms.
     * 
     * @return
     *     possible object is
     *     {@link Realms }
     *     
     */
    public Realms getRealms() {
        return realms;
    }

    /**
     * Sets the value of property realms.
     * 
     * @param value
     *     allowed object is
     *     {@link Realms }
     *     
     */
    public void setRealms(Realms value) {
        this.realms = value;
    }

    /**
     * Gets the value of property memberAuthentication.
     * 
     * @return
     *     possible object is
     *     {@link RealmReference }
     *     
     */
    public RealmReference getMemberAuthentication() {
        return memberAuthentication;
    }

    /**
     * Sets the value of property memberAuthentication.
     * 
     * @param value
     *     allowed object is
     *     {@link RealmReference }
     *     
     */
    public void setMemberAuthentication(RealmReference value) {
        this.memberAuthentication = value;
    }

    /**
     * Gets the value of property clientAuthentication.
     * 
     * @return
     *     possible object is
     *     {@link RealmReference }
     *     
     */
    public RealmReference getClientAuthentication() {
        return clientAuthentication;
    }

    /**
     * Sets the value of property clientAuthentication.
     * 
     * @param value
     *     allowed object is
     *     {@link RealmReference }
     *     
     */
    public void setClientAuthentication(RealmReference value) {
        this.clientAuthentication = value;
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
            return false;
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

}

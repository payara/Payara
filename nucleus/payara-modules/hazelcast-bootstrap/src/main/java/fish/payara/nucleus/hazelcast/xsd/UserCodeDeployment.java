
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of user-code-deployment complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="user-code-deployment">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="class-cache-mode" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *               &lt;enumeration value="OFF"/>
 *               &lt;enumeration value="ETERNAL"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="provider-mode" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *               &lt;enumeration value="OFF"/>
 *               &lt;enumeration value="LOCAL_CLASSES_ONLY"/>
 *               &lt;enumeration value="LOCAL_AND_CACHED_CLASSES"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="blacklist-prefixes" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="whitelist-prefixes" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="provider-filter" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="enabled" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "user-code-deployment", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class UserCodeDeployment {

    @XmlElement(name = "class-cache-mode", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "ETERNAL")
    protected String classCacheMode;
    @XmlElement(name = "provider-mode", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "LOCAL_AND_CACHED_CLASSES")
    protected String providerMode;
    @XmlElement(name = "blacklist-prefixes", namespace = "http://www.hazelcast.com/schema/config")
    protected String blacklistPrefixes;
    @XmlElement(name = "whitelist-prefixes", namespace = "http://www.hazelcast.com/schema/config")
    protected String whitelistPrefixes;
    @XmlElement(name = "provider-filter", namespace = "http://www.hazelcast.com/schema/config")
    protected String providerFilter;
    @XmlAttribute(name = "enabled", required = true)
    protected boolean enabled;

    /**
     * Gets the value of property classCacheMode.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClassCacheMode() {
        return classCacheMode;
    }

    /**
     * Sets the value of property classCacheMode.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClassCacheMode(String value) {
        this.classCacheMode = value;
    }

    /**
     * Gets the value of property providerMode.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProviderMode() {
        return providerMode;
    }

    /**
     * Sets the value of property providerMode.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProviderMode(String value) {
        this.providerMode = value;
    }

    /**
     * Gets the value of property blacklistPrefixes.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBlacklistPrefixes() {
        return blacklistPrefixes;
    }

    /**
     * Sets the value of property blacklistPrefixes.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBlacklistPrefixes(String value) {
        this.blacklistPrefixes = value;
    }

    /**
     * Gets the value of property whitelistPrefixes.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getWhitelistPrefixes() {
        return whitelistPrefixes;
    }

    /**
     * Sets the value of property whitelistPrefixes.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setWhitelistPrefixes(String value) {
        this.whitelistPrefixes = value;
    }

    /**
     * Gets the value of property providerFilter.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProviderFilter() {
        return providerFilter;
    }

    /**
     * Sets the value of property providerFilter.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProviderFilter(String value) {
        this.providerFilter = value;
    }

    /**
     * Gets the value of property enabled.
     * 
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the value of property enabled.
     * 
     */
    public void setEnabled(boolean value) {
        this.enabled = value;
    }

}

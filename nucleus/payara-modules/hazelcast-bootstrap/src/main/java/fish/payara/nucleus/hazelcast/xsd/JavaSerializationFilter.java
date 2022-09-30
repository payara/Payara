
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of java-serialization-filter complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="java-serialization-filter">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="blacklist" type="{http://www.hazelcast.com/schema/config}filter-list" minOccurs="0"/>
 *         &lt;element name="whitelist" type="{http://www.hazelcast.com/schema/config}filter-list" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="defaults-disabled" default="false">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}boolean">
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
@XmlType(name = "java-serialization-filter", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class JavaSerializationFilter {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected FilterList blacklist;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected FilterList whitelist;
    @XmlAttribute(name = "defaults-disabled")
    protected Boolean defaultsDisabled;

    /**
     * Gets the value of property blacklist.
     * 
     * @return
     *     possible object is
     *     {@link FilterList }
     *     
     */
    public FilterList getBlacklist() {
        return blacklist;
    }

    /**
     * Sets the value of property blacklist.
     * 
     * @param value
     *     allowed object is
     *     {@link FilterList }
     *     
     */
    public void setBlacklist(FilterList value) {
        this.blacklist = value;
    }

    /**
     * Gets the value of property whitelist.
     * 
     * @return
     *     possible object is
     *     {@link FilterList }
     *     
     */
    public FilterList getWhitelist() {
        return whitelist;
    }

    /**
     * Sets the value of property whitelist.
     * 
     * @param value
     *     allowed object is
     *     {@link FilterList }
     *     
     */
    public void setWhitelist(FilterList value) {
        this.whitelist = value;
    }

    /**
     * Gets the value of property defaultsDisabled.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isDefaultsDisabled() {
        if (defaultsDisabled == null) {
            return false;
        } else {
            return defaultsDisabled;
        }
    }

    /**
     * Sets the value of property defaultsDisabled.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDefaultsDisabled(Boolean value) {
        this.defaultsDisabled = value;
    }

}

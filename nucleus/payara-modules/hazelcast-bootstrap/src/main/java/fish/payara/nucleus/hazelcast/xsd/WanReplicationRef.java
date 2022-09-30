
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of wan-replication-ref complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="wan-replication-ref">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="merge-policy-class-name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="filters" type="{http://www.hazelcast.com/schema/config}wan-replication-ref-filters" minOccurs="0"/>
 *         &lt;element name="republishing-enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "wan-replication-ref", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class WanReplicationRef {

    @XmlElement(name = "merge-policy-class-name", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "com.hazelcast.spi.merge.PassThroughMergePolicy")
    protected String mergePolicyClassName;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected WanReplicationRefFilters filters;
    @XmlElement(name = "republishing-enabled", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean republishingEnabled;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    /**
     * Gets the value of property mergePolicyClassName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMergePolicyClassName() {
        return mergePolicyClassName;
    }

    /**
     * Sets the value of property mergePolicyClassName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMergePolicyClassName(String value) {
        this.mergePolicyClassName = value;
    }

    /**
     * Gets the value of property filters.
     * 
     * @return
     *     possible object is
     *     {@link WanReplicationRefFilters }
     *     
     */
    public WanReplicationRefFilters getFilters() {
        return filters;
    }

    /**
     * Sets the value of property filters.
     * 
     * @param value
     *     allowed object is
     *     {@link WanReplicationRefFilters }
     *     
     */
    public void setFilters(WanReplicationRefFilters value) {
        this.filters = value;
    }

    /**
     * Gets the value of property republishingEnabled.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isRepublishingEnabled() {
        return republishingEnabled;
    }

    /**
     * Sets the value of property republishingEnabled.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setRepublishingEnabled(Boolean value) {
        this.republishingEnabled = value;
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
        return name;
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

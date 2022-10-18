
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java Class of base-permission complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="base-permission">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="endpoints" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="endpoint" maxOccurs="unbounded">
 *                     &lt;simpleType>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                       &lt;/restriction>
 *                     &lt;/simpleType>
 *                   &lt;/element>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="principal" type="{http://www.w3.org/2001/XMLSchema}string" default="*" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "base-permission", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "endpoints"
})
@XmlSeeAlso({
    ManagementPermission.class,
    InstancePermission.class
})
public class BasePermission {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Endpoints endpoints;
    @XmlAttribute(name = "principal")
    protected String principal;

    /**
     * Gets the value of property endpoints.
     * 
     * @return
     *     possible object is
     *     {@link Endpoints }
     *     
     */
    public Endpoints getEndpoints() {
        return endpoints;
    }

    /**
     * Sets the value of property endpoints.
     * 
     * @param value
     *     allowed object is
     *     {@link Endpoints }
     *     
     */
    public void setEndpoints(Endpoints value) {
        this.endpoints = value;
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
        if (principal == null) {
            return "*";
        } else {
            return principal;
        }
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
     * <p>Java Class of anonymous complex type.
     * 
     * 
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="endpoint" maxOccurs="unbounded">
     *           &lt;simpleType>
     *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *             &lt;/restriction>
     *           &lt;/simpleType>
     *         &lt;/element>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "endpoint"
    })
    public static class Endpoints {

        @XmlElement(namespace = "http://www.hazelcast.com/schema/config", required = true, defaultValue = "127.0.0.1")
        protected List<String> endpoint;

        /**
         * Gets the value of the endpoint property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the endpoint property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getEndpoint().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link String }
         * 
         * 
         */
        public List<String> getEndpoint() {
            if (endpoint == null) {
                endpoint = new ArrayList<String>();
            }
            return this.endpoint;
        }

    }

}

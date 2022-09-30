
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of cache-entry-listener complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="cache-entry-listener">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="cache-entry-listener-factory" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="cache-entry-event-filter-factory" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/all>
 *       &lt;attribute name="old-value-required" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="synchronous" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cache-entry-listener", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class CacheEntryListener {

    @XmlElement(name = "cache-entry-listener-factory", namespace = "http://www.hazelcast.com/schema/config")
    protected CacheEntryListenerFactory cacheEntryListenerFactory;
    @XmlElement(name = "cache-entry-event-filter-factory", namespace = "http://www.hazelcast.com/schema/config")
    protected CacheEntryEventFilterFactory cacheEntryEventFilterFactory;
    @XmlAttribute(name = "old-value-required")
    protected Boolean oldValueRequired;
    @XmlAttribute(name = "synchronous")
    protected Boolean synchronous;

    /**
     * Gets the value of property cacheEntryListenerFactory.
     * 
     * @return
     *     possible object is
     *     {@link CacheEntryListenerFactory }
     *     
     */
    public CacheEntryListenerFactory getCacheEntryListenerFactory() {
        return cacheEntryListenerFactory;
    }

    /**
     * Sets the value of property cacheEntryListenerFactory.
     * 
     * @param value
     *     allowed object is
     *     {@link CacheEntryListenerFactory }
     *     
     */
    public void setCacheEntryListenerFactory(CacheEntryListenerFactory value) {
        this.cacheEntryListenerFactory = value;
    }

    /**
     * Gets the value of property cacheEntryEventFilterFactory.
     * 
     * @return
     *     possible object is
     *     {@link CacheEntryEventFilterFactory }
     *     
     */
    public CacheEntryEventFilterFactory getCacheEntryEventFilterFactory() {
        return cacheEntryEventFilterFactory;
    }

    /**
     * Sets the value of property cacheEntryEventFilterFactory.
     * 
     * @param value
     *     allowed object is
     *     {@link CacheEntryEventFilterFactory }
     *     
     */
    public void setCacheEntryEventFilterFactory(CacheEntryEventFilterFactory value) {
        this.cacheEntryEventFilterFactory = value;
    }

    /**
     * Gets the value of property oldValueRequired.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isOldValueRequired() {
        if (oldValueRequired == null) {
            return false;
        } else {
            return oldValueRequired;
        }
    }

    /**
     * Sets the value of property oldValueRequired.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setOldValueRequired(Boolean value) {
        this.oldValueRequired = value;
    }

    /**
     * Gets the value of property synchronous.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isSynchronous() {
        if (synchronous == null) {
            return false;
        } else {
            return synchronous;
        }
    }

    /**
     * Sets the value of property synchronous.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setSynchronous(Boolean value) {
        this.synchronous = value;
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
     *       &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class CacheEntryEventFilterFactory {

        @XmlAttribute(name = "class-name", required = true)
        @XmlSchemaType(name = "anySimpleType")
        protected String className;

        /**
         * Gets the value of property className.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getClassName() {
            return className;
        }

        /**
         * Sets the value of property className.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setClassName(String value) {
            this.className = value;
        }

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
     *       &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class CacheEntryListenerFactory {

        @XmlAttribute(name = "class-name", required = true)
        @XmlSchemaType(name = "anySimpleType")
        protected String className;

        /**
         * Gets the value of property className.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getClassName() {
            return className;
        }

        /**
         * Sets the value of property className.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setClassName(String value) {
            this.className = value;
        }

    }

}

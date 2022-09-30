
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of serialization complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="serialization">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="portable-version" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="use-native-byte-order" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="byte-order" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *               &lt;enumeration value="BIG_ENDIAN"/>
 *               &lt;enumeration value="LITTLE_ENDIAN"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="enable-compression" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="enable-shared-object" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="allow-unsafe" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="allow-override-default-serializers" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="data-serializable-factories" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="data-serializable-factory" type="{http://www.hazelcast.com/schema/config}serialization-factory" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="portable-factories" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="portable-factory" type="{http://www.hazelcast.com/schema/config}serialization-factory" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="serializers" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;choice maxOccurs="unbounded">
 *                   &lt;element name="global-serializer" type="{http://www.hazelcast.com/schema/config}global-serializer" minOccurs="0"/>
 *                   &lt;element name="serializer" type="{http://www.hazelcast.com/schema/config}serializer" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/choice>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="check-class-def-errors" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="java-serialization-filter" type="{http://www.hazelcast.com/schema/config}java-serialization-filter" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "serialization", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Serialization {

    @XmlElement(name = "portable-version", namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "unsignedInt")
    protected Long portableVersion;
    @XmlElement(name = "use-native-byte-order", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean useNativeByteOrder;
    @XmlElement(name = "byte-order", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "BIG_ENDIAN")
    protected String byteOrder;
    @XmlElement(name = "enable-compression", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean enableCompression;
    @XmlElement(name = "enable-shared-object", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean enableSharedObject;
    @XmlElement(name = "allow-unsafe", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean allowUnsafe;
    @XmlElement(name = "allow-override-default-serializers", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean allowOverrideDefaultSerializers;
    @XmlElement(name = "data-serializable-factories", namespace = "http://www.hazelcast.com/schema/config")
    protected DataSerializableFactories dataSerializableFactories;
    @XmlElement(name = "portable-factories", namespace = "http://www.hazelcast.com/schema/config")
    protected PortableFactories portableFactories;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Serializers serializers;
    @XmlElement(name = "check-class-def-errors", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean checkClassDefErrors;
    @XmlElement(name = "java-serialization-filter", namespace = "http://www.hazelcast.com/schema/config")
    protected JavaSerializationFilter javaSerializationFilter;

    /**
     * Gets the value of property portableVersion.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getPortableVersion() {
        return portableVersion;
    }

    /**
     * Sets the value of property portableVersion.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setPortableVersion(Long value) {
        this.portableVersion = value;
    }

    /**
     * Gets the value of property useNativeByteOrder.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isUseNativeByteOrder() {
        return useNativeByteOrder;
    }

    /**
     * Sets the value of property useNativeByteOrder.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setUseNativeByteOrder(Boolean value) {
        this.useNativeByteOrder = value;
    }

    /**
     * Gets the value of property byteOrder.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getByteOrder() {
        return byteOrder;
    }

    /**
     * Sets the value of property byteOrder.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setByteOrder(String value) {
        this.byteOrder = value;
    }

    /**
     * Gets the value of property enableCompression.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isEnableCompression() {
        return enableCompression;
    }

    /**
     * Sets the value of property enableCompression.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEnableCompression(Boolean value) {
        this.enableCompression = value;
    }

    /**
     * Gets the value of property enableSharedObject.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isEnableSharedObject() {
        return enableSharedObject;
    }

    /**
     * Sets the value of property enableSharedObject.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEnableSharedObject(Boolean value) {
        this.enableSharedObject = value;
    }

    /**
     * Gets the value of property allowUnsafe.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isAllowUnsafe() {
        return allowUnsafe;
    }

    /**
     * Sets the value of property allowUnsafe.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAllowUnsafe(Boolean value) {
        this.allowUnsafe = value;
    }

    /**
     * Gets the value of property allowOverrideDefaultSerializers.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isAllowOverrideDefaultSerializers() {
        return allowOverrideDefaultSerializers;
    }

    /**
     * Sets the value of property allowOverrideDefaultSerializers.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAllowOverrideDefaultSerializers(Boolean value) {
        this.allowOverrideDefaultSerializers = value;
    }

    /**
     * Gets the value of property dataSerializableFactories.
     * 
     * @return
     *     possible object is
     *     {@link DataSerializableFactories }
     *     
     */
    public DataSerializableFactories getDataSerializableFactories() {
        return dataSerializableFactories;
    }

    /**
     * Sets the value of property dataSerializableFactories.
     * 
     * @param value
     *     allowed object is
     *     {@link DataSerializableFactories }
     *     
     */
    public void setDataSerializableFactories(DataSerializableFactories value) {
        this.dataSerializableFactories = value;
    }

    /**
     * Gets the value of property portableFactories.
     * 
     * @return
     *     possible object is
     *     {@link PortableFactories }
     *     
     */
    public PortableFactories getPortableFactories() {
        return portableFactories;
    }

    /**
     * Sets the value of property portableFactories.
     * 
     * @param value
     *     allowed object is
     *     {@link PortableFactories }
     *     
     */
    public void setPortableFactories(PortableFactories value) {
        this.portableFactories = value;
    }

    /**
     * Gets the value of property serializers.
     * 
     * @return
     *     possible object is
     *     {@link Serializers }
     *     
     */
    public Serializers getSerializers() {
        return serializers;
    }

    /**
     * Sets the value of property serializers.
     * 
     * @param value
     *     allowed object is
     *     {@link Serializers }
     *     
     */
    public void setSerializers(Serializers value) {
        this.serializers = value;
    }

    /**
     * Gets the value of property checkClassDefErrors.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isCheckClassDefErrors() {
        return checkClassDefErrors;
    }

    /**
     * Sets the value of property checkClassDefErrors.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setCheckClassDefErrors(Boolean value) {
        this.checkClassDefErrors = value;
    }

    /**
     * Gets the value of property javaSerializationFilter.
     * 
     * @return
     *     possible object is
     *     {@link JavaSerializationFilter }
     *     
     */
    public JavaSerializationFilter getJavaSerializationFilter() {
        return javaSerializationFilter;
    }

    /**
     * Sets the value of property javaSerializationFilter.
     * 
     * @param value
     *     allowed object is
     *     {@link JavaSerializationFilter }
     *     
     */
    public void setJavaSerializationFilter(JavaSerializationFilter value) {
        this.javaSerializationFilter = value;
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
     *         &lt;element name="data-serializable-factory" type="{http://www.hazelcast.com/schema/config}serialization-factory" maxOccurs="unbounded" minOccurs="0"/>
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
        "dataSerializableFactory"
    })
    public static class DataSerializableFactories {

        @XmlElement(name = "data-serializable-factory", namespace = "http://www.hazelcast.com/schema/config")
        protected List<SerializationFactory> dataSerializableFactory;

        /**
         * Gets the value of the dataSerializableFactory property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the dataSerializableFactory property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getDataSerializableFactory().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link SerializationFactory }
         * 
         * 
         */
        public List<SerializationFactory> getDataSerializableFactory() {
            if (dataSerializableFactory == null) {
                dataSerializableFactory = new ArrayList<SerializationFactory>();
            }
            return this.dataSerializableFactory;
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
     *       &lt;sequence>
     *         &lt;element name="portable-factory" type="{http://www.hazelcast.com/schema/config}serialization-factory" maxOccurs="unbounded" minOccurs="0"/>
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
        "portableFactory"
    })
    public static class PortableFactories {

        @XmlElement(name = "portable-factory", namespace = "http://www.hazelcast.com/schema/config")
        protected List<SerializationFactory> portableFactory;

        /**
         * Gets the value of the portableFactory property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the portableFactory property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getPortableFactory().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link SerializationFactory }
         * 
         * 
         */
        public List<SerializationFactory> getPortableFactory() {
            if (portableFactory == null) {
                portableFactory = new ArrayList<SerializationFactory>();
            }
            return this.portableFactory;
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
     *       &lt;choice maxOccurs="unbounded">
     *         &lt;element name="global-serializer" type="{http://www.hazelcast.com/schema/config}global-serializer" minOccurs="0"/>
     *         &lt;element name="serializer" type="{http://www.hazelcast.com/schema/config}serializer" maxOccurs="unbounded" minOccurs="0"/>
     *       &lt;/choice>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "globalSerializerOrSerializer"
    })
    public static class Serializers {

        @XmlElements({
            @XmlElement(name = "global-serializer", namespace = "http://www.hazelcast.com/schema/config", type = GlobalSerializer.class),
            @XmlElement(name = "serializer", namespace = "http://www.hazelcast.com/schema/config", type = Serializer.class)
        })
        protected List<Object> globalSerializerOrSerializer;

        /**
         * Gets the value of the globalSerializerOrSerializer property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the globalSerializerOrSerializer property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getGlobalSerializerOrSerializer().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link GlobalSerializer }
         * {@link Serializer }
         * 
         * 
         */
        public List<Object> getGlobalSerializerOrSerializer() {
            if (globalSerializerOrSerializer == null) {
                globalSerializerOrSerializer = new ArrayList<Object>();
            }
            return this.globalSerializerOrSerializer;
        }

    }

}

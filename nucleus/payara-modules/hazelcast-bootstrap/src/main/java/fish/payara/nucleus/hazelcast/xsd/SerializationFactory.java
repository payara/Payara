
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java Class of serialization-factory complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="serialization-factory">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="factory-id" use="required" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "serialization-factory", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "value"
})
public class SerializationFactory {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "factory-id", required = true)
    @XmlSchemaType(name = "unsignedInt")
    protected long factoryId;

    /**
     * Gets the value of property value.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of property value.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of property factoryId.
     * 
     */
    public long getFactoryId() {
        return factoryId;
    }

    /**
     * Sets the value of property factoryId.
     * 
     */
    public void setFactoryId(long value) {
        this.factoryId = value;
    }

}

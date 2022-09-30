
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java Class of global-serializer complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="global-serializer">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="override-java-serialization" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "global-serializer", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "value"
})
public class GlobalSerializer {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "override-java-serialization")
    protected Boolean overrideJavaSerialization;

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
     * Gets the value of property overrideJavaSerialization.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isOverrideJavaSerialization() {
        if (overrideJavaSerialization == null) {
            return false;
        } else {
            return overrideJavaSerialization;
        }
    }

    /**
     * Sets the value of property overrideJavaSerialization.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setOverrideJavaSerialization(Boolean value) {
        this.overrideJavaSerialization = value;
    }

}

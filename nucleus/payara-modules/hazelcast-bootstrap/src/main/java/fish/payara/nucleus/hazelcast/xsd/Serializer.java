
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of serializer complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="serializer">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="class-name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="type-class" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "serializer", namespace = "http://www.hazelcast.com/schema/config")
public class Serializer {

    @XmlAttribute(name = "class-name", required = true)
    protected String className;
    @XmlAttribute(name = "type-class", required = true)
    protected String typeClass;

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

    /**
     * Gets the value of property typeClass.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTypeClass() {
        return typeClass;
    }

    /**
     * Sets the value of property typeClass.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTypeClass(String value) {
        this.typeClass = value;
    }

}

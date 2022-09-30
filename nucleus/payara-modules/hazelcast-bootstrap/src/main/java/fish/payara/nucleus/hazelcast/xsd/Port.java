
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java Class of port complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="port">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>unsignedShort">
 *       &lt;attribute name="auto-increment" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *       &lt;attribute name="port-count" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" default="100" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "port", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "value"
})
public class Port {

    @XmlValue
    @XmlSchemaType(name = "unsignedShort")
    protected int value;
    @XmlAttribute(name = "auto-increment")
    protected Boolean autoIncrement;
    @XmlAttribute(name = "port-count")
    @XmlSchemaType(name = "unsignedInt")
    protected Long portCount;

    /**
     * Gets the value of property value.
     * 
     */
    public int getValue() {
        return value;
    }

    /**
     * Sets the value of property value.
     * 
     */
    public void setValue(int value) {
        this.value = value;
    }

    /**
     * Gets the value of property autoIncrement.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isAutoIncrement() {
        if (autoIncrement == null) {
            return true;
        } else {
            return autoIncrement;
        }
    }

    /**
     * Sets the value of property autoIncrement.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAutoIncrement(Boolean value) {
        this.autoIncrement = value;
    }

    /**
     * Gets the value of property portCount.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public long getPortCount() {
        if (portCount == null) {
            return  100L;
        } else {
            return portCount;
        }
    }

    /**
     * Sets the value of property portCount.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setPortCount(Long value) {
        this.portCount = value;
    }

}

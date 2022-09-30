
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of item-listener complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="item-listener">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.hazelcast.com/schema/config>listener-base">
 *       &lt;attribute name="include-value" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "item-listener", namespace = "http://www.hazelcast.com/schema/config")
@XmlSeeAlso({
    EntryListener.class
})
public class ItemListener
    extends ListenerBase
{

    @XmlAttribute(name = "include-value")
    protected Boolean includeValue;

    /**
     * Gets the value of property includeValue.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isIncludeValue() {
        if (includeValue == null) {
            return true;
        } else {
            return includeValue;
        }
    }

    /**
     * Sets the value of property includeValue.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIncludeValue(Boolean value) {
        this.includeValue = value;
    }

}

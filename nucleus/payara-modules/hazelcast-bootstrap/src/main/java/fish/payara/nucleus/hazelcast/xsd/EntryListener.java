
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of entry-listener complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="entry-listener">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.hazelcast.com/schema/config>item-listener">
 *       &lt;attribute name="local" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "entry-listener", namespace = "http://www.hazelcast.com/schema/config")
public class EntryListener
    extends ItemListener
{

    @XmlAttribute(name = "local")
    protected Boolean local;

    /**
     * Gets the value of property local.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isLocal() {
        if (local == null) {
            return false;
        } else {
            return local;
        }
    }

    /**
     * Sets the value of property local.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setLocal(Boolean value) {
        this.local = value;
    }

}

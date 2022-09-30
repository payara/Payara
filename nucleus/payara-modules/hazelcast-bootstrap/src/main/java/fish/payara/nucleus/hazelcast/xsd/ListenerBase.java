
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * 
 *                 One of the following: membership-listener, instance-listener, migration-listener or
 *                 partition-lost-listener.
 *             
 * 
 * <p>Java Class of listener-base complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="listener-base">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.hazelcast.com/schema/config>non-space-string">
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "listener-base", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "value"
})
@XmlSeeAlso({
    PartitionLostListener.class,
    SplitBrainProtectionListener.class,
    ItemListener.class
})
public class ListenerBase {

    @XmlValue
    protected String value;

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

}

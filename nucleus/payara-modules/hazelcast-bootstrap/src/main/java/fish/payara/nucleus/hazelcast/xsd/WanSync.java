
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of wan-sync complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="wan-sync">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="consistency-check-strategy" type="{http://www.hazelcast.com/schema/config}consistency-check-strategy" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "wan-sync", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class WanSync {

    @XmlElement(name = "consistency-check-strategy", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "NONE")
    @XmlSchemaType(name = "string")
    protected ConsistencyCheckStrategy consistencyCheckStrategy;

    /**
     * Gets the value of property consistencyCheckStrategy.
     * 
     * @return
     *     possible object is
     *     {@link ConsistencyCheckStrategy }
     *     
     */
    public ConsistencyCheckStrategy getConsistencyCheckStrategy() {
        return consistencyCheckStrategy;
    }

    /**
     * Sets the value of property consistencyCheckStrategy.
     * 
     * @param value
     *     allowed object is
     *     {@link ConsistencyCheckStrategy }
     *     
     */
    public void setConsistencyCheckStrategy(ConsistencyCheckStrategy value) {
        this.consistencyCheckStrategy = value;
    }

}

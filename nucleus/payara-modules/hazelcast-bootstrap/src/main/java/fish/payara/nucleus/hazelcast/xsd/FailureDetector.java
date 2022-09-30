
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of failure-detector complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="failure-detector">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="icmp" type="{http://www.hazelcast.com/schema/config}icmp"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "failure-detector", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class FailureDetector {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected Icmp icmp;

    /**
     * Gets the value of property icmp.
     * 
     * @return
     *     possible object is
     *     {@link Icmp }
     *     
     */
    public Icmp getIcmp() {
        return icmp;
    }

    /**
     * Sets the value of property icmp.
     * 
     * @param value
     *     allowed object is
     *     {@link Icmp }
     *     
     */
    public void setIcmp(Icmp value) {
        this.icmp = value;
    }

}

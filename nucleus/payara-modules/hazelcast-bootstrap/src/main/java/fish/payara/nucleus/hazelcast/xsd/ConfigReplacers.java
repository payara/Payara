
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of config-replacers complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="config-replacers">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="replacer" type="{http://www.hazelcast.com/schema/config}replacer" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="fail-if-value-missing" default="true">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}boolean">
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "config-replacers", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "replacer"
})
public class ConfigReplacers {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected List<Replacer> replacer;
    @XmlAttribute(name = "fail-if-value-missing")
    protected Boolean failIfValueMissing;

    /**
     * Gets the value of the replacer property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the replacer property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReplacer().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Replacer }
     * 
     * 
     */
    public List<Replacer> getReplacer() {
        if (replacer == null) {
            replacer = new ArrayList<Replacer>();
        }
        return this.replacer;
    }

    /**
     * Gets the value of property failIfValueMissing.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isFailIfValueMissing() {
        if (failIfValueMissing == null) {
            return true;
        } else {
            return failIfValueMissing;
        }
    }

    /**
     * Sets the value of property failIfValueMissing.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setFailIfValueMissing(Boolean value) {
        this.failIfValueMissing = value;
    }

}

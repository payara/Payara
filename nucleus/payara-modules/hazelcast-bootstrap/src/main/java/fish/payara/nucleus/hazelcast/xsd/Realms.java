
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of realms complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="realms">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="realm" type="{http://www.hazelcast.com/schema/config}realm" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "realms", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "realm"
})
public class Realms {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected List<Realm> realm;

    /**
     * Gets the value of the realm property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the realm property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRealm().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Realm }
     * 
     * 
     */
    public List<Realm> getRealm() {
        if (realm == null) {
            realm = new ArrayList<Realm>();
        }
        return this.realm;
    }

}

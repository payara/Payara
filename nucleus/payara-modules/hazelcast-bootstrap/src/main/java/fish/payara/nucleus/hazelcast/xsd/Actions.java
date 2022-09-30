
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of actions complex type.
 *
 * <pre>
 * &lt;complexType name="actions">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="action" maxOccurs="unbounded">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *               &lt;enumeration value="all"/>
 *               &lt;enumeration value="create"/>
 *               &lt;enumeration value="destroy"/>
 *               &lt;enumeration value="modify"/>
 *               &lt;enumeration value="read"/>
 *               &lt;enumeration value="remove"/>
 *               &lt;enumeration value="lock"/>
 *               &lt;enumeration value="listen"/>
 *               &lt;enumeration value="release"/>
 *               &lt;enumeration value="acquire"/>
 *               &lt;enumeration value="put"/>
 *               &lt;enumeration value="add"/>
 *               &lt;enumeration value="index"/>
 *               &lt;enumeration value="intercept"/>
 *               &lt;enumeration value="publish"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actions", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "action"
})
public class Actions {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected List<String> action;

    /**
     * Gets the value of the action property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the action property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAction().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getAction() {
        if (action == null) {
            action = new ArrayList<String>();
        }
        return this.action;
    }

}

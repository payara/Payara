
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java Class of login-modules complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="login-modules">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="login-module" type="{http://www.hazelcast.com/schema/config}login-module" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "login-modules", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "loginModule"
})
public class LoginModules {

    @XmlElement(name = "login-module", namespace = "http://www.hazelcast.com/schema/config")
    protected List<LoginModule> loginModule;

    /**
     * Gets the value of the loginModule property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the loginModule property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLoginModule().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link LoginModule }
     * 
     * 
     */
    public List<LoginModule> getLoginModule() {
        if (loginModule == null) {
            loginModule = new ArrayList<LoginModule>();
        }
        return this.loginModule;
    }

}

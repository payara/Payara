
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java Class of kerberos-authentication complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="kerberos-authentication">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.hazelcast.com/schema/config}basic-authentication">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="relax-flags-check" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="use-name-without-realm" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="security-realm" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="principal" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="keytab-file" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ldap" type="{http://www.hazelcast.com/schema/config}ldap-authentication" minOccurs="0"/>
 *       &lt;/choice>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "kerberos-authentication", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "relaxFlagsCheckOrUseNameWithoutRealmOrSecurityRealm"
})
public class KerberosAuthentication
    extends BasicAuthentication
{

    @XmlElementRefs({
        @XmlElementRef(name = "keytab-file", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "security-realm", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "use-name-without-realm", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "principal", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "ldap", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "relax-flags-check", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false)
    })
    protected List<JAXBElement<?>> relaxFlagsCheckOrUseNameWithoutRealmOrSecurityRealm;

    /**
     * Gets the value of the relaxFlagsCheckOrUseNameWithoutRealmOrSecurityRealm property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the relaxFlagsCheckOrUseNameWithoutRealmOrSecurityRealm property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRelaxFlagsCheckOrUseNameWithoutRealmOrSecurityRealm().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link LdapAuthentication }{@code >}
     * 
     * 
     */
    public List<JAXBElement<?>> getRelaxFlagsCheckOrUseNameWithoutRealmOrSecurityRealm() {
        if (relaxFlagsCheckOrUseNameWithoutRealmOrSecurityRealm == null) {
            relaxFlagsCheckOrUseNameWithoutRealmOrSecurityRealm = new ArrayList<JAXBElement<?>>();
        }
        return this.relaxFlagsCheckOrUseNameWithoutRealmOrSecurityRealm;
    }

}

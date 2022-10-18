
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
 * <p>Java Class of ldap-authentication complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="ldap-authentication">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.hazelcast.com/schema/config}basic-authentication">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="url" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="socket-factory-class-name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="parse-dn" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="role-context" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="role-filter" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="role-mapping-attribute" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="role-mapping-mode" type="{http://www.hazelcast.com/schema/config}ldap-role-mapping-mode" minOccurs="0"/>
 *         &lt;element name="role-name-attribute" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="role-recursion-max-depth" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="role-search-scope" type="{http://www.hazelcast.com/schema/config}ldap-search-scope" minOccurs="0"/>
 *         &lt;element name="user-name-attribute" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="system-user-dn" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="system-user-password" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="system-authentication" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="security-realm" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="password-attribute" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="user-context" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="user-filter" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="user-search-scope" type="{http://www.hazelcast.com/schema/config}ldap-search-scope" minOccurs="0"/>
 *         &lt;element name="skip-authentication" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/choice>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ldap-authentication", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "urlOrSocketFactoryClassNameOrParseDn"
})
public class LdapAuthentication
    extends BasicAuthentication
{

    @XmlElementRefs({
        @XmlElementRef(name = "system-authentication", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "system-user-password", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "role-search-scope", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "security-realm", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "user-name-attribute", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "role-mapping-mode", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "parse-dn", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "system-user-dn", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "role-filter", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "role-recursion-max-depth", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "socket-factory-class-name", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "role-context", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "role-mapping-attribute", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "role-name-attribute", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "user-context", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "user-search-scope", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "skip-authentication", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "user-filter", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "url", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "password-attribute", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false)
    })
    protected List<JAXBElement<?>> urlOrSocketFactoryClassNameOrParseDn;

    /**
     * Gets the value of the urlOrSocketFactoryClassNameOrParseDn property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the urlOrSocketFactoryClassNameOrParseDn property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getUrlOrSocketFactoryClassNameOrParseDn().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link LdapSearchScope }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link LdapRoleMappingMode }{@code >}
     * {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link Integer }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link LdapSearchScope }{@code >}
     * {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * 
     */
    public List<JAXBElement<?>> getUrlOrSocketFactoryClassNameOrParseDn() {
        if (urlOrSocketFactoryClassNameOrParseDn == null) {
            urlOrSocketFactoryClassNameOrParseDn = new ArrayList<JAXBElement<?>>();
        }
        return this.urlOrSocketFactoryClassNameOrParseDn;
    }

}


package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java Class of basic-authentication complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="basic-authentication">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="skip-identity" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="skip-endpoint" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="skip-role" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "basic-authentication", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "skipIdentityOrSkipEndpointOrSkipRole"
})
@XmlSeeAlso({
    TlsAuthentication.class,
    KerberosAuthentication.class,
    LdapAuthentication.class
})
public class BasicAuthentication {

    @XmlElementRefs({
        @XmlElementRef(name = "skip-identity", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "skip-endpoint", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "skip-role", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false)
    })
    protected List<JAXBElement<Boolean>> skipIdentityOrSkipEndpointOrSkipRole;

    /**
     * Gets the value of the skipIdentityOrSkipEndpointOrSkipRole property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the skipIdentityOrSkipEndpointOrSkipRole property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSkipIdentityOrSkipEndpointOrSkipRole().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     * {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     * {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     * 
     * 
     */
    public List<JAXBElement<Boolean>> getSkipIdentityOrSkipEndpointOrSkipRole() {
        if (skipIdentityOrSkipEndpointOrSkipRole == null) {
            skipIdentityOrSkipEndpointOrSkipRole = new ArrayList<JAXBElement<Boolean>>();
        }
        return this.skipIdentityOrSkipEndpointOrSkipRole;
    }

}

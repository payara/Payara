
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of secure-store complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="secure-store">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://www.hazelcast.com/schema/config}secure-store-substitution-group" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "secure-store", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "secureStoreSubstitutionGroup"
})
public class SecureStore {

    @XmlElementRef(name = "secure-store-substitution-group", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false)
    protected JAXBElement<?> secureStoreSubstitutionGroup;

    /**
     * Gets the value of property secureStoreSubstitutionGroup.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Object }{@code >}
     *     {@link JAXBElement }{@code <}{@link Keystore }{@code >}
     *     {@link JAXBElement }{@code <}{@link Vault }{@code >}
     *     
     */
    public JAXBElement<?> getSecureStoreSubstitutionGroup() {
        return secureStoreSubstitutionGroup;
    }

    /**
     * Sets the value of property secureStoreSubstitutionGroup.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Object }{@code >}
     *     {@link JAXBElement }{@code <}{@link Keystore }{@code >}
     *     {@link JAXBElement }{@code <}{@link Vault }{@code >}
     *     
     */
    public void setSecureStoreSubstitutionGroup(JAXBElement<?> value) {
        this.secureStoreSubstitutionGroup = value;
    }

}

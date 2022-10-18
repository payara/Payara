
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlMixed;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java Class of wan-custom-publisher complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="wan-custom-publisher">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="publisher-id" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="class-name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="properties" type="{http://www.hazelcast.com/schema/config}properties" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "wan-custom-publisher", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "content"
})
public class WanCustomPublisher {

    @XmlElementRefs({
        @XmlElementRef(name = "properties", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "publisher-id", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "class-name", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class)
    })
    @XmlMixed
    protected List<Serializable> content;

    /**
     * Gets the value of the content property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the content property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getContent().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link Properties }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link String }
     * 
     * 
     */
    public List<Serializable> getContent() {
        if (content == null) {
            content = new ArrayList<Serializable>();
        }
        return this.content;
    }

}

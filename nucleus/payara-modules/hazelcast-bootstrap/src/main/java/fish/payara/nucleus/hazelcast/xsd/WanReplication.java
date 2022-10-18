
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlMixed;
import jakarta.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java Class of wan-replication complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="wan-replication">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="batch-publisher" type="{http://www.hazelcast.com/schema/config}wan-batch-publisher" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="custom-publisher" type="{http://www.hazelcast.com/schema/config}wan-custom-publisher" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="consumer" type="{http://www.hazelcast.com/schema/config}wan-consumer" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "wan-replication", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "content"
})
public class WanReplication {

    @XmlElementRefs({
        @XmlElementRef(name = "consumer", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "custom-publisher", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "batch-publisher", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false)
    })
    @XmlMixed
    protected List<Serializable> content;
    @XmlAttribute(name = "name", required = true)
    protected String name;

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
     * {@link JAXBElement }{@code <}{@link WanCustomPublisher }{@code >}
     * {@link JAXBElement }{@code <}{@link WanConsumer }{@code >}
     * {@link String }
     * {@link JAXBElement }{@code <}{@link WanBatchPublisher }{@code >}
     * 
     * 
     */
    public List<Serializable> getContent() {
        if (content == null) {
            content = new ArrayList<Serializable>();
        }
        return this.content;
    }

    /**
     * Gets the value of property name.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of property name.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

}

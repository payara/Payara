
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
 * <p>Java Class of filter-list complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="filter-list">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;element name="class" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="package" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="prefix" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "filter-list", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "clazzOrPackageOrPrefix"
})
public class FilterList {

    @XmlElementRefs({
        @XmlElementRef(name = "class", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "prefix", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "package", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false)
    })
    protected List<JAXBElement<String>> clazzOrPackageOrPrefix;

    /**
     * Gets the value of the clazzOrPackageOrPrefix property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the clazzOrPackageOrPrefix property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getClazzOrPackageOrPrefix().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * 
     */
    public List<JAXBElement<String>> getClazzOrPackageOrPrefix() {
        if (clazzOrPackageOrPrefix == null) {
            clazzOrPackageOrPrefix = new ArrayList<JAXBElement<String>>();
        }
        return this.clazzOrPackageOrPrefix;
    }

}

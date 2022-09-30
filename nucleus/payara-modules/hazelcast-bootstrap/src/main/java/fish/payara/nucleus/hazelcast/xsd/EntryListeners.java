
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of entry-listeners complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="entry-listeners">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="entry-listener" type="{http://www.hazelcast.com/schema/config}entry-listener" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "entry-listeners", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "entryListener"
})
public class EntryListeners {

    @XmlElement(name = "entry-listener", namespace = "http://www.hazelcast.com/schema/config")
    protected List<EntryListener> entryListener;

    /**
     * Gets the value of the entryListener property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the entryListener property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getEntryListener().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link EntryListener }
     * 
     * 
     */
    public List<EntryListener> getEntryListener() {
        if (entryListener == null) {
            entryListener = new ArrayList<EntryListener>();
        }
        return this.entryListener;
    }

}

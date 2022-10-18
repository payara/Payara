
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java Class of cache-entry-listeners complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="cache-entry-listeners">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="cache-entry-listener" type="{http://www.hazelcast.com/schema/config}cache-entry-listener" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cache-entry-listeners", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "cacheEntryListener"
})
public class CacheEntryListeners {

    @XmlElement(name = "cache-entry-listener", namespace = "http://www.hazelcast.com/schema/config")
    protected List<CacheEntryListener> cacheEntryListener;

    /**
     * Gets the value of the cacheEntryListener property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cacheEntryListener property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCacheEntryListener().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CacheEntryListener }
     * 
     * 
     */
    public List<CacheEntryListener> getCacheEntryListener() {
        if (cacheEntryListener == null) {
            cacheEntryListener = new ArrayList<CacheEntryListener>();
        }
        return this.cacheEntryListener;
    }

}

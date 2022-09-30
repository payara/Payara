
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of query-caches complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="query-caches">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="query-cache" type="{http://www.hazelcast.com/schema/config}query-cache" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "query-caches", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "queryCache"
})
public class QueryCaches {

    @XmlElement(name = "query-cache", namespace = "http://www.hazelcast.com/schema/config")
    protected List<QueryCache> queryCache;

    /**
     * Gets the value of the queryCache property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the queryCache property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getQueryCache().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link QueryCache }
     * 
     * 
     */
    public List<QueryCache> getQueryCache() {
        if (queryCache == null) {
            queryCache = new ArrayList<QueryCache>();
        }
        return this.queryCache;
    }

}

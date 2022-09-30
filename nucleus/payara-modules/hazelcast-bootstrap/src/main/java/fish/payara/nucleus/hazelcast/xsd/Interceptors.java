
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of interceptors complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="interceptors">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="interceptor" type="{http://www.hazelcast.com/schema/config}interceptor" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "interceptors", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "interceptor"
})
public class Interceptors {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config", required = true)
    protected List<Interceptor> interceptor;

    /**
     * Gets the value of the interceptor property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the interceptor property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInterceptor().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Interceptor }
     * 
     * 
     */
    public List<Interceptor> getInterceptor() {
        if (interceptor == null) {
            interceptor = new ArrayList<Interceptor>();
        }
        return this.interceptor;
    }

}

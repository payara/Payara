
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                     A split brain protection function that keeps track of the last heartbeat timestamp per each member.
 *                     For a member to be considered live (for the purpose to conclude whether the minimum cluster size property is satisfied)
 *                     a heartbeat must have been received at most 
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;code xmlns="http://www.hazelcast.com/schema/config" xmlns:xs="http://www.w3.org/2001/XMLSchema"&gt;heartbeat-tolerance&lt;/code&gt;
 * </pre>
 *  milliseconds before
 *                     current time.
 *                 
 * 
 * <p>Java Class of anonymous complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="heartbeat-tolerance-millis" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" default="5000" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
public class RecentlyActiveSplitBrainProtection {

    @XmlAttribute(name = "heartbeat-tolerance-millis")
    @XmlSchemaType(name = "unsignedInt")
    protected Long heartbeatToleranceMillis;

    /**
     * Gets the value of property heartbeatToleranceMillis.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public long getHeartbeatToleranceMillis() {
        if (heartbeatToleranceMillis == null) {
            return  5000L;
        } else {
            return heartbeatToleranceMillis;
        }
    }

    /**
     * Sets the value of property heartbeatToleranceMillis.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setHeartbeatToleranceMillis(Long value) {
        this.heartbeatToleranceMillis = value;
    }

}

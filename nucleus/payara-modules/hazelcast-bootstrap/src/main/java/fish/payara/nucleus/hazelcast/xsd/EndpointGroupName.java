
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of endpoint-group-name.
 * 
 * 
 * <p>
 * <pre>
 * &lt;simpleType name="endpoint-group-name">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="CLUSTER_READ"/>
 *     &lt;enumeration value="CLUSTER_WRITE"/>
 *     &lt;enumeration value="HEALTH_CHECK"/>
 *     &lt;enumeration value="HOT_RESTART"/>
 *     &lt;enumeration value="WAN"/>
 *     &lt;enumeration value="DATA"/>
 *     &lt;enumeration value="CP"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "endpoint-group-name", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum EndpointGroupName {

    CLUSTER_READ,
    CLUSTER_WRITE,
    HEALTH_CHECK,
    HOT_RESTART,
    WAN,
    DATA,
    CP;

    public String value() {
        return name();
    }

    public static EndpointGroupName fromValue(String v) {
        return valueOf(v);
    }

}

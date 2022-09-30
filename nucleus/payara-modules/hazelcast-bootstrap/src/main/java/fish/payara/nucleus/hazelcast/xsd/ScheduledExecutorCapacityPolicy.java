
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of scheduled-executor-capacity-policy.
 * 
 * 
 * <p>
 * <pre>
 * &lt;simpleType name="scheduled-executor-capacity-policy">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="PER_NODE"/>
 *     &lt;enumeration value="PER_PARTITION"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "scheduled-executor-capacity-policy", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum ScheduledExecutorCapacityPolicy {

    PER_NODE,
    PER_PARTITION;

    public String value() {
        return name();
    }

    public static ScheduledExecutorCapacityPolicy fromValue(String v) {
        return valueOf(v);
    }

}

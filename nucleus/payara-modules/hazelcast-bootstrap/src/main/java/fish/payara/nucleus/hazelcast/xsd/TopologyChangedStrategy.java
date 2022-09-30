
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of topology-changed-strategy.
 * 
 *
 * <p>
 * <pre>
 * &lt;simpleType name="topology-changed-strategy">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="CANCEL_RUNNING_OPERATION"/>
 *     &lt;enumeration value="DISCARD_AND_RESTART"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "topology-changed-strategy", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum TopologyChangedStrategy {

    CANCEL_RUNNING_OPERATION,
    DISCARD_AND_RESTART;

    public String value() {
        return name();
    }

    public static TopologyChangedStrategy fromValue(String v) {
        return valueOf(v);
    }

}

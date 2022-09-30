
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of wan-queue-full-behavior.
 * 
 *
 * <p>
 * <pre>
 * &lt;simpleType name="wan-queue-full-behavior">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="DISCARD_AFTER_MUTATION"/>
 *     &lt;enumeration value="THROW_EXCEPTION"/>
 *     &lt;enumeration value="THROW_EXCEPTION_ONLY_IF_REPLICATION_ACTIVE"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "wan-queue-full-behavior", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum WanQueueFullBehavior {

    DISCARD_AFTER_MUTATION,
    THROW_EXCEPTION,
    THROW_EXCEPTION_ONLY_IF_REPLICATION_ACTIVE;

    public String value() {
        return name();
    }

    public static WanQueueFullBehavior fromValue(String v) {
        return valueOf(v);
    }

}

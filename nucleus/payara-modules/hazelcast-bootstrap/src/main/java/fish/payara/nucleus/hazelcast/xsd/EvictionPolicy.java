
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of eviction-policy.
 * 
 *
 * <p>
 * <pre>
 * &lt;simpleType name="eviction-policy">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="NONE"/>
 *     &lt;enumeration value="LRU"/>
 *     &lt;enumeration value="LFU"/>
 *     &lt;enumeration value="RANDOM"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "eviction-policy", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum EvictionPolicy {

    NONE,
    LRU,
    LFU,
    RANDOM;

    public String value() {
        return name();
    }

    public static EvictionPolicy fromValue(String v) {
        return valueOf(v);
    }

}


package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of max-size-policy-map.
 * 
 * 
 * <p>
 * <pre>
 * &lt;simpleType name="max-size-policy-map">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="PER_NODE"/>
 *     &lt;enumeration value="PER_PARTITION"/>
 *     &lt;enumeration value="USED_HEAP_SIZE"/>
 *     &lt;enumeration value="USED_HEAP_PERCENTAGE"/>
 *     &lt;enumeration value="FREE_HEAP_SIZE"/>
 *     &lt;enumeration value="FREE_HEAP_PERCENTAGE"/>
 *     &lt;enumeration value="USED_NATIVE_MEMORY_SIZE"/>
 *     &lt;enumeration value="USED_NATIVE_MEMORY_PERCENTAGE"/>
 *     &lt;enumeration value="FREE_NATIVE_MEMORY_SIZE"/>
 *     &lt;enumeration value="FREE_NATIVE_MEMORY_PERCENTAGE"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "max-size-policy-map", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum MaxSizePolicyMap {

    PER_NODE,
    PER_PARTITION,
    USED_HEAP_SIZE,
    USED_HEAP_PERCENTAGE,
    FREE_HEAP_SIZE,
    FREE_HEAP_PERCENTAGE,
    USED_NATIVE_MEMORY_SIZE,
    USED_NATIVE_MEMORY_PERCENTAGE,
    FREE_NATIVE_MEMORY_SIZE,
    FREE_NATIVE_MEMORY_PERCENTAGE;

    public String value() {
        return name();
    }

    public static MaxSizePolicyMap fromValue(String v) {
        return valueOf(v);
    }

}

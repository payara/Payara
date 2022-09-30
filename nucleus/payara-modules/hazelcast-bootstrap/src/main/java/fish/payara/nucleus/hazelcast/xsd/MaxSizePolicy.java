
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of max-size-policy.
 * 
 *
 * <p>
 * <pre>
 * &lt;simpleType name="max-size-policy">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="ENTRY_COUNT"/>
 *     &lt;enumeration value="USED_NATIVE_MEMORY_SIZE"/>
 *     &lt;enumeration value="USED_NATIVE_MEMORY_PERCENTAGE"/>
 *     &lt;enumeration value="FREE_NATIVE_MEMORY_SIZE"/>
 *     &lt;enumeration value="FREE_NATIVE_MEMORY_PERCENTAGE"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "max-size-policy", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum MaxSizePolicy {

    ENTRY_COUNT,
    USED_NATIVE_MEMORY_SIZE,
    USED_NATIVE_MEMORY_PERCENTAGE,
    FREE_NATIVE_MEMORY_SIZE,
    FREE_NATIVE_MEMORY_PERCENTAGE;

    public String value() {
        return name();
    }

    public static MaxSizePolicy fromValue(String v) {
        return valueOf(v);
    }

}

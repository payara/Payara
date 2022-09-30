
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of memory-allocator-type.
 * 
 *
 * <p>
 * <pre>
 * &lt;simpleType name="memory-allocator-type">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="STANDARD"/>
 *     &lt;enumeration value="POOLED"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "memory-allocator-type", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum MemoryAllocatorType {

    STANDARD,
    POOLED;

    public String value() {
        return name();
    }

    public static MemoryAllocatorType fromValue(String v) {
        return valueOf(v);
    }

}


package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of memory-unit.
 * 
 * 
 * <p>
 * <pre>
 * &lt;simpleType name="memory-unit">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="BYTES"/>
 *     &lt;enumeration value="KILOBYTES"/>
 *     &lt;enumeration value="MEGABYTES"/>
 *     &lt;enumeration value="GIGABYTES"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "memory-unit", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum MemoryUnit {

    BYTES,
    KILOBYTES,
    MEGABYTES,
    GIGABYTES;

    public String value() {
        return name();
    }

    public static MemoryUnit fromValue(String v) {
        return valueOf(v);
    }

}


package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of time-unit.
 * 
 *
 * <p>
 * <pre>
 * &lt;simpleType name="time-unit">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="NANOSECONDS"/>
 *     &lt;enumeration value="MICROSECONDS"/>
 *     &lt;enumeration value="MILLISECONDS"/>
 *     &lt;enumeration value="SECONDS"/>
 *     &lt;enumeration value="MINUTES"/>
 *     &lt;enumeration value="HOURS"/>
 *     &lt;enumeration value="DAYS"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "time-unit", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum TimeUnit {

    NANOSECONDS,
    MICROSECONDS,
    MILLISECONDS,
    SECONDS,
    MINUTES,
    HOURS,
    DAYS;

    public String value() {
        return name();
    }

    public static TimeUnit fromValue(String v) {
        return valueOf(v);
    }

}


package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of protect-on.
 * 
 *
 * <p>
 * <pre>
 * &lt;simpleType name="protect-on">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="READ"/>
 *     &lt;enumeration value="WRITE"/>
 *     &lt;enumeration value="READ_WRITE"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "protect-on", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum ProtectOn {

    READ,
    WRITE,
    READ_WRITE;

    public String value() {
        return name();
    }

    public static ProtectOn fromValue(String v) {
        return valueOf(v);
    }

}

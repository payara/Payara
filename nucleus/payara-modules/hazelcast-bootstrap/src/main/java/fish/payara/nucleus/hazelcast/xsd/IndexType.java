
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of index-type.
 * 
 *
 * <p>
 * <pre>
 * &lt;simpleType name="index-type">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="SORTED"/>
 *     &lt;enumeration value="HASH"/>
 *     &lt;enumeration value="BITMAP"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "index-type", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum IndexType {

    SORTED,
    HASH,
    BITMAP;

    public String value() {
        return name();
    }

    public static IndexType fromValue(String v) {
        return valueOf(v);
    }

}

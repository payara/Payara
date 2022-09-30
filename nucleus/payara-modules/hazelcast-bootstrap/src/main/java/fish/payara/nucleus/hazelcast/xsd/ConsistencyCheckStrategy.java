
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of consistency-check-strategy.
 * 
 * 
 * <p>
 * <pre>
 * &lt;simpleType name="consistency-check-strategy">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="NONE"/>
 *     &lt;enumeration value="MERKLE_TREES"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "consistency-check-strategy", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum ConsistencyCheckStrategy {

    NONE,
    MERKLE_TREES;

    public String value() {
        return name();
    }

    public static ConsistencyCheckStrategy fromValue(String v) {
        return valueOf(v);
    }

}

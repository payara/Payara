
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of expiry-policy-type.
 * 
 *
 * <p>
 * <pre>
 * &lt;simpleType name="expiry-policy-type">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="CREATED"/>
 *     &lt;enumeration value="ACCESSED"/>
 *     &lt;enumeration value="ETERNAL"/>
 *     &lt;enumeration value="MODIFIED"/>
 *     &lt;enumeration value="TOUCHED"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "expiry-policy-type", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum ExpiryPolicyType {

    CREATED,
    ACCESSED,
    ETERNAL,
    MODIFIED,
    TOUCHED;

    public String value() {
        return name();
    }

    public static ExpiryPolicyType fromValue(String v) {
        return valueOf(v);
    }

}


package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of wan-acknowledge-type.
 * 
 * 
 * <p>
 * <pre>
 * &lt;simpleType name="wan-acknowledge-type">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="ACK_ON_OPERATION_COMPLETE"/>
 *     &lt;enumeration value="ACK_ON_RECEIPT"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "wan-acknowledge-type", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum WanAcknowledgeType {

    ACK_ON_OPERATION_COMPLETE,
    ACK_ON_RECEIPT;

    public String value() {
        return name();
    }

    public static WanAcknowledgeType fromValue(String v) {
        return valueOf(v);
    }

}

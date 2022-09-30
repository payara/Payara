
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of permission-on-join-operation.
 * 
 * 
 * <p>
 * <pre>
 * &lt;simpleType name="permission-on-join-operation">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="RECEIVE"/>
 *     &lt;enumeration value="SEND"/>
 *     &lt;enumeration value="NONE"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "permission-on-join-operation", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum PermissionOnJoinOperation {

    RECEIVE,
    SEND,
    NONE;

    public String value() {
        return name();
    }

    public static PermissionOnJoinOperation fromValue(String v) {
        return valueOf(v);
    }

}

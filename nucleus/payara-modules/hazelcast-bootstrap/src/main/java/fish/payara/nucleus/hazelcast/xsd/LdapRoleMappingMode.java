
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of ldap-role-mapping-mode.
 * 
 * 
 * <p>
 * <pre>
 * &lt;simpleType name="ldap-role-mapping-mode">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="attribute"/>
 *     &lt;enumeration value="direct"/>
 *     &lt;enumeration value="reverse"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ldap-role-mapping-mode", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum LdapRoleMappingMode {

    @XmlEnumValue("attribute")
    ATTRIBUTE("attribute"),
    @XmlEnumValue("direct")
    DIRECT("direct"),
    @XmlEnumValue("reverse")
    REVERSE("reverse");
    private final String value;

    LdapRoleMappingMode(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static LdapRoleMappingMode fromValue(String v) {
        for (LdapRoleMappingMode c: LdapRoleMappingMode.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}

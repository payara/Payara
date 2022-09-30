
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of ldap-search-scope.
 * 
 *
 * <p>
 * <pre>
 * &lt;simpleType name="ldap-search-scope">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="object"/>
 *     &lt;enumeration value="one-level"/>
 *     &lt;enumeration value="subtree"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ldap-search-scope", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum LdapSearchScope {

    @XmlEnumValue("object")
    OBJECT("object"),
    @XmlEnumValue("one-level")
    ONE_LEVEL("one-level"),
    @XmlEnumValue("subtree")
    SUBTREE("subtree");
    private final String value;

    LdapSearchScope(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static LdapSearchScope fromValue(String v) {
        for (LdapSearchScope c: LdapSearchScope.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}

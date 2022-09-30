
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of metadata-policy.
 * 
 *
 * <p>
 * <pre>
 * &lt;simpleType name="metadata-policy">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="CREATE_ON_UPDATE"/>
 *     &lt;enumeration value="OFF"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "metadata-policy", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum MetadataPolicy {

    CREATE_ON_UPDATE,
    OFF;

    public String value() {
        return name();
    }

    public static MetadataPolicy fromValue(String v) {
        return valueOf(v);
    }

}

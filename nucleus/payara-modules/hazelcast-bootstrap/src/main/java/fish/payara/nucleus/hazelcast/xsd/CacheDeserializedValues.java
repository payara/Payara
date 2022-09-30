
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of cache-deserialized-values.
 * 
 * 
 * <p>
 * <pre>
 * &lt;simpleType name="cache-deserialized-values">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="NEVER"/>
 *     &lt;enumeration value="ALWAYS"/>
 *     &lt;enumeration value="INDEX-ONLY"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "cache-deserialized-values", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum CacheDeserializedValues {

    NEVER("NEVER"),
    ALWAYS("ALWAYS"),
    @XmlEnumValue("INDEX-ONLY")
    INDEX_ONLY("INDEX-ONLY");
    private final String value;

    CacheDeserializedValues(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CacheDeserializedValues fromValue(String v) {
        for (CacheDeserializedValues c: CacheDeserializedValues.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}

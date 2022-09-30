
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of bitmap-index-unique-key-transformation.
 * 
 * 
 * <p>
 * <pre>
 * &lt;simpleType name="bitmap-index-unique-key-transformation">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="OBJECT"/>
 *     &lt;enumeration value="LONG"/>
 *     &lt;enumeration value="RAW"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "bitmap-index-unique-key-transformation", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum BitmapIndexUniqueKeyTransformation {

    OBJECT,
    LONG,
    RAW;

    public String value() {
        return name();
    }

    public static BitmapIndexUniqueKeyTransformation fromValue(String v) {
        return valueOf(v);
    }

}

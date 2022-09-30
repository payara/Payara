
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of in-memory-format.
 * 
 * 
 * <p>
 * <pre>
 * &lt;simpleType name="in-memory-format">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="BINARY"/>
 *     &lt;enumeration value="OBJECT"/>
 *     &lt;enumeration value="NATIVE"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "in-memory-format", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum InMemoryFormat {

    BINARY,
    OBJECT,
    NATIVE;

    public String value() {
        return name();
    }

    public static InMemoryFormat fromValue(String v) {
        return valueOf(v);
    }

}

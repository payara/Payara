
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of topic-overload-policy.
 * 
 * 
 * <p>
 * <pre>
 * &lt;simpleType name="topic-overload-policy">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="DISCARD_OLDEST"/>
 *     &lt;enumeration value="DISCARD_NEWEST"/>
 *     &lt;enumeration value="BLOCK"/>
 *     &lt;enumeration value="ERROR"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "topic-overload-policy", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum TopicOverloadPolicy {

    DISCARD_OLDEST,
    DISCARD_NEWEST,
    BLOCK,
    ERROR;

    public String value() {
        return name();
    }

    public static TopicOverloadPolicy fromValue(String v) {
        return valueOf(v);
    }

}

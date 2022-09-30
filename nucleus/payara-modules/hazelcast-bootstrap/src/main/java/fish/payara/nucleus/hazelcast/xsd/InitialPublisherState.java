
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of initial-publisher-state.
 * 
 * 
 * <p>
 * <pre>
 * &lt;simpleType name="initial-publisher-state">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="REPLICATING"/>
 *     &lt;enumeration value="PAUSED"/>
 *     &lt;enumeration value="STOPPED"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "initial-publisher-state", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum InitialPublisherState {

    REPLICATING,
    PAUSED,
    STOPPED;

    public String value() {
        return name();
    }

    public static InitialPublisherState fromValue(String v) {
        return valueOf(v);
    }

}

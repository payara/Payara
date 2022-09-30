
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of persistent-memory-mode.
 * 
 *
 * <p>
 * <pre>
 * &lt;simpleType name="persistent-memory-mode">
 *   &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *     &lt;enumeration value="MOUNTED"/>
 *     &lt;enumeration value="SYSTEM_MEMORY"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "persistent-memory-mode", namespace = "http://www.hazelcast.com/schema/config")
@XmlEnum
public enum PersistentMemoryMode {


    /**
     * 
     *                         The persistent memory is mounted into the file system (also known as FS DAX mode).
     *                     
     * 
     */
    MOUNTED,

    /**
     * 
     *                         The persistent memory is onlined as system memory (also known as KMEM DAX mode).
     *                     
     * 
     */
    SYSTEM_MEMORY;

    public String value() {
        return name();
    }

    public static PersistentMemoryMode fromValue(String v) {
        return valueOf(v);
    }

}

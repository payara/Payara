
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of partition-lost-listener complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="partition-lost-listener">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.hazelcast.com/schema/config>listener-base">
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "partition-lost-listener", namespace = "http://www.hazelcast.com/schema/config")
public class PartitionLostListener
    extends ListenerBase
{


}

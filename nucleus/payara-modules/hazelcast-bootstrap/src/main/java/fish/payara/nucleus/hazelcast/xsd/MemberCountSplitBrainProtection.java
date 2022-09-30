
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                     Determines split based protection based on the number of currently live cluster members.
 *                     The minimum number of members required in a cluster for the cluster to remain in an
 *                     operational state is configured separately in 
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;code xmlns="http://www.hazelcast.com/schema/config" xmlns:xs="http://www.w3.org/2001/XMLSchema"&gt;&amp;lt;minimum-cluster-size&amp;gt;&lt;/code&gt;
 * </pre>
 *  element.
 *                     If the number of members is below the defined minimum at any time,
 *                     the operations are rejected and the rejected operations throw a SplitBrainProtectionException to
 *                     their callers.
 *                 
 * 
 * <p>Java Class of anonymous complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
public class MemberCountSplitBrainProtection {


}

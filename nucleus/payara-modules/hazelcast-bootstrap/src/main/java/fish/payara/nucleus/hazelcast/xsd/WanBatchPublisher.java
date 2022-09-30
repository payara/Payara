
package fish.payara.nucleus.hazelcast.xsd;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of wan-batch-publisher complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="wan-batch-publisher">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="cluster-name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="snapshot-enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="initial-publisher-state" type="{http://www.hazelcast.com/schema/config}initial-publisher-state" minOccurs="0"/>
 *         &lt;element name="queue-capacity" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *         &lt;element name="batch-size" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *         &lt;element name="batch-max-delay-millis" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *         &lt;element name="response-timeout-millis" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *         &lt;element name="queue-full-behavior" type="{http://www.hazelcast.com/schema/config}wan-queue-full-behavior" minOccurs="0"/>
 *         &lt;element name="acknowledge-type" type="{http://www.hazelcast.com/schema/config}wan-acknowledge-type" minOccurs="0"/>
 *         &lt;element name="discovery-period-seconds" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *         &lt;element name="max-target-endpoints" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *         &lt;element name="max-concurrent-invocations" type="{http://www.w3.org/2001/XMLSchema}integer" minOccurs="0"/>
 *         &lt;element name="use-endpoint-private-address" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="idle-min-park-ns" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/>
 *         &lt;element name="idle-max-park-ns" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/>
 *         &lt;element name="publisher-id" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="target-endpoints" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="aws" type="{http://www.hazelcast.com/schema/config}aliased-discovery-strategy" minOccurs="0"/>
 *         &lt;element name="gcp" type="{http://www.hazelcast.com/schema/config}aliased-discovery-strategy" minOccurs="0"/>
 *         &lt;element name="azure" type="{http://www.hazelcast.com/schema/config}aliased-discovery-strategy" minOccurs="0"/>
 *         &lt;element name="kubernetes" type="{http://www.hazelcast.com/schema/config}aliased-discovery-strategy" minOccurs="0"/>
 *         &lt;element name="eureka" type="{http://www.hazelcast.com/schema/config}aliased-discovery-strategy" minOccurs="0"/>
 *         &lt;element name="discovery-strategies" type="{http://www.hazelcast.com/schema/config}discovery-strategies" minOccurs="0"/>
 *         &lt;element name="sync" type="{http://www.hazelcast.com/schema/config}wan-sync" minOccurs="0"/>
 *         &lt;element name="endpoint" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="properties" type="{http://www.hazelcast.com/schema/config}properties" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "wan-batch-publisher", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "content"
})
public class WanBatchPublisher {

    @XmlElementRefs({
        @XmlElementRef(name = "queue-full-behavior", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "snapshot-enabled", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "use-endpoint-private-address", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "batch-max-delay-millis", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "queue-capacity", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "target-endpoints", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "properties", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "max-target-endpoints", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "azure", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "discovery-period-seconds", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "cluster-name", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "batch-size", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "eureka", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "publisher-id", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "initial-publisher-state", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "max-concurrent-invocations", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "response-timeout-millis", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "idle-min-park-ns", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "discovery-strategies", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "acknowledge-type", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "kubernetes", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "idle-max-park-ns", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "aws", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "sync", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "gcp", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class),
        @XmlElementRef(name = "endpoint", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class)
    })
    @XmlMixed
    protected List<Serializable> content;

    /**
     * Gets the value of the content property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the content property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getContent().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link WanQueueFullBehavior }{@code >}
     * {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     * {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     * {@link JAXBElement }{@code <}{@link BigInteger }{@code >}
     * {@link JAXBElement }{@code <}{@link BigInteger }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link Properties }{@code >}
     * {@link JAXBElement }{@code <}{@link BigInteger }{@code >}
     * {@link JAXBElement }{@code <}{@link AliasedDiscoveryStrategy }{@code >}
     * {@link JAXBElement }{@code <}{@link BigInteger }{@code >}
     * {@link String }
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link BigInteger }{@code >}
     * {@link JAXBElement }{@code <}{@link AliasedDiscoveryStrategy }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link InitialPublisherState }{@code >}
     * {@link JAXBElement }{@code <}{@link BigInteger }{@code >}
     * {@link JAXBElement }{@code <}{@link BigInteger }{@code >}
     * {@link JAXBElement }{@code <}{@link Long }{@code >}
     * {@link JAXBElement }{@code <}{@link DiscoveryStrategies }{@code >}
     * {@link JAXBElement }{@code <}{@link WanAcknowledgeType }{@code >}
     * {@link JAXBElement }{@code <}{@link AliasedDiscoveryStrategy }{@code >}
     * {@link JAXBElement }{@code <}{@link Long }{@code >}
     * {@link JAXBElement }{@code <}{@link AliasedDiscoveryStrategy }{@code >}
     * {@link JAXBElement }{@code <}{@link WanSync }{@code >}
     * {@link JAXBElement }{@code <}{@link AliasedDiscoveryStrategy }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * 
     */
    public List<Serializable> getContent() {
        if (content == null) {
            content = new ArrayList<Serializable>();
        }
        return this.content;
    }

}

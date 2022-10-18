
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java Class of permissions complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="permissions">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element name="all-permissions" type="{http://www.hazelcast.com/schema/config}base-permission" minOccurs="0"/>
 *         &lt;element name="map-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="queue-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="multimap-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="topic-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="list-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="set-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="lock-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="atomic-long-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="atomic-reference-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="countdown-latch-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="semaphore-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="flake-id-generator-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="executor-service-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="durable-executor-service-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="cardinality-estimator-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="scheduled-executor-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="pn-counter-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="transaction-permission" type="{http://www.hazelcast.com/schema/config}base-permission" minOccurs="0"/>
 *         &lt;element name="cache-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="user-code-deployment-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="config-permission" type="{http://www.hazelcast.com/schema/config}base-permission" minOccurs="0"/>
 *         &lt;element name="ring-buffer-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="reliable-topic-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="replicatedmap-permission" type="{http://www.hazelcast.com/schema/config}instance-permission" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="management-permission" type="{http://www.hazelcast.com/schema/config}management-permission" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/choice>
 *       &lt;attribute name="on-join-operation" type="{http://www.hazelcast.com/schema/config}permission-on-join-operation" default="RECEIVE" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "permissions", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "allPermissionsOrMapPermissionOrQueuePermission"
})
public class Permissions {

    @XmlElementRefs({
        @XmlElementRef(name = "replicatedmap-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "atomic-reference-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "pn-counter-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "config-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "all-permissions", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "map-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "executor-service-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "topic-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "semaphore-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "management-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "ring-buffer-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "user-code-deployment-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "queue-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "multimap-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "countdown-latch-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "atomic-long-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "transaction-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "set-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "cardinality-estimator-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "lock-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "flake-id-generator-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "scheduled-executor-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "durable-executor-service-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "list-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "cache-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "reliable-topic-permission", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false)
    })
    protected List<JAXBElement<? extends BasePermission>> allPermissionsOrMapPermissionOrQueuePermission;
    @XmlAttribute(name = "on-join-operation")
    protected PermissionOnJoinOperation onJoinOperation;

    /**
     * Gets the value of the allPermissionsOrMapPermissionOrQueuePermission property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the allPermissionsOrMapPermissionOrQueuePermission property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAllPermissionsOrMapPermissionOrQueuePermission().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link BasePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link BasePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link ManagementPermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link BasePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * {@link JAXBElement }{@code <}{@link InstancePermission }{@code >}
     * 
     * 
     */
    public List<JAXBElement<? extends BasePermission>> getAllPermissionsOrMapPermissionOrQueuePermission() {
        if (allPermissionsOrMapPermissionOrQueuePermission == null) {
            allPermissionsOrMapPermissionOrQueuePermission = new ArrayList<JAXBElement<? extends BasePermission>>();
        }
        return this.allPermissionsOrMapPermissionOrQueuePermission;
    }

    /**
     * Gets the value of property onJoinOperation.
     * 
     * @return
     *     possible object is
     *     {@link PermissionOnJoinOperation }
     *     
     */
    public PermissionOnJoinOperation getOnJoinOperation() {
        if (onJoinOperation == null) {
            return PermissionOnJoinOperation.RECEIVE;
        } else {
            return onJoinOperation;
        }
    }

    /**
     * Sets the value of property onJoinOperation.
     * 
     * @param value
     *     allowed object is
     *     {@link PermissionOnJoinOperation }
     *     
     */
    public void setOnJoinOperation(PermissionOnJoinOperation value) {
        this.onJoinOperation = value;
    }

}

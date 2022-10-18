
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java Class of partition-group complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="partition-group">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="member-group" maxOccurs="unbounded" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="interface" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="255" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="group-type">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.hazelcast.com/schema/config}non-space-string">
 *             &lt;enumeration value="HOST_AWARE"/>
 *             &lt;enumeration value="CUSTOM"/>
 *             &lt;enumeration value="PER_MEMBER"/>
 *             &lt;enumeration value="ZONE_AWARE"/>
 *             &lt;enumeration value="NODE_AWARE"/>
 *             &lt;enumeration value="PLACEMENT_AWARE"/>
 *             &lt;enumeration value="SPI"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "partition-group", namespace = "http://www.hazelcast.com/schema/config", propOrder = {
    "memberGroup"
})
public class PartitionGroup {

    @XmlElement(name = "member-group", namespace = "http://www.hazelcast.com/schema/config")
    protected List<MemberGroup> memberGroup;
    @XmlAttribute(name = "enabled")
    protected Boolean enabled;
    @XmlAttribute(name = "group-type")
    protected String groupType;

    /**
     * Gets the value of the memberGroup property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the memberGroup property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMemberGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MemberGroup }
     * 
     * 
     */
    public List<MemberGroup> getMemberGroup() {
        if (memberGroup == null) {
            memberGroup = new ArrayList<MemberGroup>();
        }
        return this.memberGroup;
    }

    /**
     * Gets the value of property enabled.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isEnabled() {
        if (enabled == null) {
            return false;
        } else {
            return enabled;
        }
    }

    /**
     * Sets the value of property enabled.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setEnabled(Boolean value) {
        this.enabled = value;
    }

    /**
     * Gets the value of property groupType.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGroupType() {
        return groupType;
    }

    /**
     * Sets the value of property groupType.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGroupType(String value) {
        this.groupType = value;
    }


    /**
     * <p>Java Class of anonymous complex type.
     * 
     * 
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="interface" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="255" minOccurs="0"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "_interface"
    })
    public static class MemberGroup {

        @XmlElement(name = "interface", namespace = "http://www.hazelcast.com/schema/config")
        protected List<String> _interface;

        /**
         * Gets the value of the interface property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the interface property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getInterface().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link String }
         * 
         * 
         */
        public List<String> getInterface() {
            if (_interface == null) {
                _interface = new ArrayList<String>();
            }
            return this._interface;
        }

    }

}

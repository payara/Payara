
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java Class of split-brain-protection complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="split-brain-protection">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="minimum-cluster-size" type="{http://www.hazelcast.com/schema/config}minimum-cluster-size" minOccurs="0"/>
 *         &lt;element name="protect-on" type="{http://www.hazelcast.com/schema/config}protect-on" minOccurs="0"/>
 *         &lt;element name="function-class-name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="listeners" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="listener" type="{http://www.hazelcast.com/schema/config}split-brain-protection-listener" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element ref="{http://www.hazelcast.com/schema/config}choice-of-split-brain-protection-function" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="enabled" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="name" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
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
@XmlType(name = "split-brain-protection", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class SplitBrainProtection {

    @XmlElement(name = "minimum-cluster-size", namespace = "http://www.hazelcast.com/schema/config")
    protected Integer minimumClusterSize;
    @XmlElement(name = "protect-on", namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "string")
    protected ProtectOn protectOn;
    @XmlElement(name = "function-class-name", namespace = "http://www.hazelcast.com/schema/config")
    protected String functionClassName;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Listeners listeners;
    @XmlElementRef(name = "choice-of-split-brain-protection-function", namespace = "http://www.hazelcast.com/schema/config", type = JAXBElement.class, required = false)
    protected JAXBElement<?> choiceOfSplitBrainProtectionFunction;
    @XmlAttribute(name = "enabled", required = true)
    protected boolean enabled;
    @XmlAttribute(name = "name", required = true)
    protected String name;

    /**
     * Gets the value of property minimumClusterSize.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMinimumClusterSize() {
        return minimumClusterSize;
    }

    /**
     * Sets the value of property minimumClusterSize.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMinimumClusterSize(Integer value) {
        this.minimumClusterSize = value;
    }

    /**
     * Gets the value of property protectOn.
     * 
     * @return
     *     possible object is
     *     {@link ProtectOn }
     *     
     */
    public ProtectOn getProtectOn() {
        return protectOn;
    }

    /**
     * Sets the value of property protectOn.
     * 
     * @param value
     *     allowed object is
     *     {@link ProtectOn }
     *     
     */
    public void setProtectOn(ProtectOn value) {
        this.protectOn = value;
    }

    /**
     * Gets the value of property functionClassName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFunctionClassName() {
        return functionClassName;
    }

    /**
     * Sets the value of property functionClassName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFunctionClassName(String value) {
        this.functionClassName = value;
    }

    /**
     * Gets the value of property listeners.
     * 
     * @return
     *     possible object is
     *     {@link Listeners }
     *     
     */
    public Listeners getListeners() {
        return listeners;
    }

    /**
     * Sets the value of property listeners.
     * 
     * @param value
     *     allowed object is
     *     {@link Listeners }
     *     
     */
    public void setListeners(Listeners value) {
        this.listeners = value;
    }

    /**
     * Gets the value of property choiceOfSplitBrainProtectionFunction.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ProbabilisticSplitBrainProtection }{@code >}
     *     {@link JAXBElement }{@code <}{@link RecentlyActiveSplitBrainProtection }{@code >}
     *     {@link JAXBElement }{@code <}{@link MemberCountSplitBrainProtection }{@code >}
     *     {@link JAXBElement }{@code <}{@link Object }{@code >}
     *     
     */
    public JAXBElement<?> getChoiceOfSplitBrainProtectionFunction() {
        return choiceOfSplitBrainProtectionFunction;
    }

    /**
     * Sets the value of property choiceOfSplitBrainProtectionFunction.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ProbabilisticSplitBrainProtection }{@code >}
     *     {@link JAXBElement }{@code <}{@link RecentlyActiveSplitBrainProtection }{@code >}
     *     {@link JAXBElement }{@code <}{@link MemberCountSplitBrainProtection }{@code >}
     *     {@link JAXBElement }{@code <}{@link Object }{@code >}
     *     
     */
    public void setChoiceOfSplitBrainProtectionFunction(JAXBElement<?> value) {
        this.choiceOfSplitBrainProtectionFunction = value;
    }

    /**
     * Gets the value of property enabled.
     * 
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the value of property enabled.
     * 
     */
    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    /**
     * Gets the value of property name.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of property name.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
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
     *         &lt;element name="listener" type="{http://www.hazelcast.com/schema/config}split-brain-protection-listener" maxOccurs="unbounded"/>
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
        "listener"
    })
    public static class Listeners {

        @XmlElement(namespace = "http://www.hazelcast.com/schema/config", required = true)
        protected List<SplitBrainProtectionListener> listener;

        /**
         * Gets the value of the listener property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the listener property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getListener().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link SplitBrainProtectionListener }
         * 
         * 
         */
        public List<SplitBrainProtectionListener> getListener() {
            if (listener == null) {
                listener = new ArrayList<SplitBrainProtectionListener>();
            }
            return this.listener;
        }

    }

}

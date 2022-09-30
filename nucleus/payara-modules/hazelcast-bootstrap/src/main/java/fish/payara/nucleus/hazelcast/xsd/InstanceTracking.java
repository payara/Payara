
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 Configures tracking of a running Hazelcast instance. For now, this is
 *                 limited to writing information about the Hazelcast instance to a file
 *                 while the instance is starting.
 *                 The file is overwritten on every start of the Hazelcast instance and if
 *                 multiple instance share the same file system, every instance will
 *                 overwrite the tracking file of a previously started instance.
 *                 If this instance is unable to write the file, the exception is logged and
 *                 the instance is allowed to start.
 *             
 * 
 * <p>Java Class of instance-tracking complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="instance-tracking">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="file-name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="format-pattern" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "instance-tracking", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class InstanceTracking {

    @XmlElement(name = "file-name", namespace = "http://www.hazelcast.com/schema/config")
    protected String fileName;
    @XmlElement(name = "format-pattern", namespace = "http://www.hazelcast.com/schema/config")
    protected String formatPattern;
    @XmlAttribute(name = "enabled")
    protected Boolean enabled;

    /**
     * Gets the value of property fileName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the value of property fileName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFileName(String value) {
        this.fileName = value;
    }

    /**
     * Gets the value of property formatPattern.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFormatPattern() {
        return formatPattern;
    }

    /**
     * Sets the value of property formatPattern.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFormatPattern(String value) {
        this.formatPattern = value;
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

}

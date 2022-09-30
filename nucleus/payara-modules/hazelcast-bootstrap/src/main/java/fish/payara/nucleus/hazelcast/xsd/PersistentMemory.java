
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                 Configuration for persistent memory (e.g. Intel Optane) devices.
 *             
 * 
 * <p>Java Class of persistent-memory complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="persistent-memory">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="directories" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;choice maxOccurs="unbounded">
 *                   &lt;element name="directory" type="{http://www.hazelcast.com/schema/config}persistent-memory-directory"/>
 *                 &lt;/choice>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/all>
 *       &lt;attribute name="enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="mode" type="{http://www.hazelcast.com/schema/config}persistent-memory-mode" default="MOUNTED" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "persistent-memory", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class PersistentMemory {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Directories directories;
    @XmlAttribute(name = "enabled")
    protected Boolean enabled;
    @XmlAttribute(name = "mode")
    protected PersistentMemoryMode mode;

    /**
     * Gets the value of property directories.
     * 
     * @return
     *     possible object is
     *     {@link Directories }
     *     
     */
    public Directories getDirectories() {
        return directories;
    }

    /**
     * Sets the value of property directories.
     * 
     * @param value
     *     allowed object is
     *     {@link Directories }
     *     
     */
    public void setDirectories(Directories value) {
        this.directories = value;
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
     * Gets the value of property mode.
     * 
     * @return
     *     possible object is
     *     {@link PersistentMemoryMode }
     *     
     */
    public PersistentMemoryMode getMode() {
        if (mode == null) {
            return PersistentMemoryMode.MOUNTED;
        } else {
            return mode;
        }
    }

    /**
     * Sets the value of property mode.
     * 
     * @param value
     *     allowed object is
     *     {@link PersistentMemoryMode }
     *     
     */
    public void setMode(PersistentMemoryMode value) {
        this.mode = value;
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
     *       &lt;choice maxOccurs="unbounded">
     *         &lt;element name="directory" type="{http://www.hazelcast.com/schema/config}persistent-memory-directory"/>
     *       &lt;/choice>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "directory"
    })
    public static class Directories {

        @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
        protected List<PersistentMemoryDirectory> directory;

        /**
         * Gets the value of the directory property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the directory property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getDirectory().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link PersistentMemoryDirectory }
         * 
         * 
         */
        public List<PersistentMemoryDirectory> getDirectory() {
            if (directory == null) {
                directory = new ArrayList<PersistentMemoryDirectory>();
            }
            return this.directory;
        }

    }

}

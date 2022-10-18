
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import java.math.BigDecimal;
import java.math.BigInteger;


/**
 * <p>Java Class of native-memory complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="native-memory">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="size" type="{http://www.hazelcast.com/schema/config}memory-size" minOccurs="0"/>
 *         &lt;element name="min-block-size" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" minOccurs="0"/>
 *         &lt;element name="page-size" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" minOccurs="0"/>
 *         &lt;element name="metadata-space-percentage" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}decimal">
 *               &lt;totalDigits value="3"/>
 *               &lt;fractionDigits value="1"/>
 *               &lt;minInclusive value="5"/>
 *               &lt;maxInclusive value="95"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="persistent-memory-directory" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="persistent-memory" type="{http://www.hazelcast.com/schema/config}persistent-memory" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="allocator-type" type="{http://www.hazelcast.com/schema/config}memory-allocator-type" default="POOLED" />
 *       &lt;attribute name="enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "native-memory", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class NativeMemory {

    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected MemorySize size;
    @XmlElement(name = "min-block-size", namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger minBlockSize;
    @XmlElement(name = "page-size", namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger pageSize;
    @XmlElement(name = "metadata-space-percentage", namespace = "http://www.hazelcast.com/schema/config")
    protected BigDecimal metadataSpacePercentage;
    @XmlElement(name = "persistent-memory-directory", namespace = "http://www.hazelcast.com/schema/config")
    protected String persistentMemoryDirectory;
    @XmlElement(name = "persistent-memory", namespace = "http://www.hazelcast.com/schema/config")
    protected PersistentMemory persistentMemory;
    @XmlAttribute(name = "allocator-type")
    protected MemoryAllocatorType allocatorType;
    @XmlAttribute(name = "enabled")
    protected Boolean enabled;

    /**
     * Gets the value of property size.
     * 
     * @return
     *     possible object is
     *     {@link MemorySize }
     *     
     */
    public MemorySize getSize() {
        return size;
    }

    /**
     * Sets the value of property size.
     * 
     * @param value
     *     allowed object is
     *     {@link MemorySize }
     *     
     */
    public void setSize(MemorySize value) {
        this.size = value;
    }

    /**
     * Gets the value of property minBlockSize.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getMinBlockSize() {
        return minBlockSize;
    }

    /**
     * Sets the value of property minBlockSize.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setMinBlockSize(BigInteger value) {
        this.minBlockSize = value;
    }

    /**
     * Gets the value of property pageSize.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getPageSize() {
        return pageSize;
    }

    /**
     * Sets the value of property pageSize.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setPageSize(BigInteger value) {
        this.pageSize = value;
    }

    /**
     * Gets the value of property metadataSpacePercentage.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getMetadataSpacePercentage() {
        return metadataSpacePercentage;
    }

    /**
     * Sets the value of property metadataSpacePercentage.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setMetadataSpacePercentage(BigDecimal value) {
        this.metadataSpacePercentage = value;
    }

    /**
     * Gets the value of property persistentMemoryDirectory.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPersistentMemoryDirectory() {
        return persistentMemoryDirectory;
    }

    /**
     * Sets the value of property persistentMemoryDirectory.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPersistentMemoryDirectory(String value) {
        this.persistentMemoryDirectory = value;
    }

    /**
     * Gets the value of property persistentMemory.
     * 
     * @return
     *     possible object is
     *     {@link PersistentMemory }
     *     
     */
    public PersistentMemory getPersistentMemory() {
        return persistentMemory;
    }

    /**
     * Sets the value of property persistentMemory.
     * 
     * @param value
     *     allowed object is
     *     {@link PersistentMemory }
     *     
     */
    public void setPersistentMemory(PersistentMemory value) {
        this.persistentMemory = value;
    }

    /**
     * Gets the value of property allocatorType.
     * 
     * @return
     *     possible object is
     *     {@link MemoryAllocatorType }
     *     
     */
    public MemoryAllocatorType getAllocatorType() {
        if (allocatorType == null) {
            return MemoryAllocatorType.POOLED;
        } else {
            return allocatorType;
        }
    }

    /**
     * Sets the value of property allocatorType.
     * 
     * @param value
     *     allowed object is
     *     {@link MemoryAllocatorType }
     *     
     */
    public void setAllocatorType(MemoryAllocatorType value) {
        this.allocatorType = value;
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

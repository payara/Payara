
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of hot-restart-persistence complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="hot-restart-persistence">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="base-dir" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="backup-dir" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="parallelism" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="validation-timeout-seconds" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="data-load-timeout-seconds" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="cluster-data-recovery-policy" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="auto-remove-stale-data" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="encryption-at-rest" type="{http://www.hazelcast.com/schema/config}encryption-at-rest" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="enabled" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "hot-restart-persistence", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class HotRestartPersistence {

    @XmlElement(name = "base-dir", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "hot-restart")
    protected String baseDir;
    @XmlElement(name = "backup-dir", namespace = "http://www.hazelcast.com/schema/config")
    protected String backupDir;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "unsignedInt")
    protected Long parallelism;
    @XmlElement(name = "validation-timeout-seconds", namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "unsignedInt")
    protected Long validationTimeoutSeconds;
    @XmlElement(name = "data-load-timeout-seconds", namespace = "http://www.hazelcast.com/schema/config")
    @XmlSchemaType(name = "unsignedInt")
    protected Long dataLoadTimeoutSeconds;
    @XmlElement(name = "cluster-data-recovery-policy", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "FULL_RECOVERY_ONLY")
    protected String clusterDataRecoveryPolicy;
    @XmlElement(name = "auto-remove-stale-data", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "true")
    protected Boolean autoRemoveStaleData;
    @XmlElement(name = "encryption-at-rest", namespace = "http://www.hazelcast.com/schema/config")
    protected EncryptionAtRest encryptionAtRest;
    @XmlAttribute(name = "enabled", required = true)
    protected boolean enabled;

    /**
     * Gets the value of property baseDir.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBaseDir() {
        return baseDir;
    }

    /**
     * Sets the value of property baseDir.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBaseDir(String value) {
        this.baseDir = value;
    }

    /**
     * Gets the value of property backupDir.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBackupDir() {
        return backupDir;
    }

    /**
     * Sets the value of property backupDir.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBackupDir(String value) {
        this.backupDir = value;
    }

    /**
     * Gets the value of property parallelism.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getParallelism() {
        return parallelism;
    }

    /**
     * Sets the value of property parallelism.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setParallelism(Long value) {
        this.parallelism = value;
    }

    /**
     * Gets the value of property validationTimeoutSeconds.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getValidationTimeoutSeconds() {
        return validationTimeoutSeconds;
    }

    /**
     * Sets the value of property validationTimeoutSeconds.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setValidationTimeoutSeconds(Long value) {
        this.validationTimeoutSeconds = value;
    }

    /**
     * Gets the value of property dataLoadTimeoutSeconds.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getDataLoadTimeoutSeconds() {
        return dataLoadTimeoutSeconds;
    }

    /**
     * Sets the value of property dataLoadTimeoutSeconds.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setDataLoadTimeoutSeconds(Long value) {
        this.dataLoadTimeoutSeconds = value;
    }

    /**
     * Gets the value of property clusterDataRecoveryPolicy.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClusterDataRecoveryPolicy() {
        return clusterDataRecoveryPolicy;
    }

    /**
     * Sets the value of property clusterDataRecoveryPolicy.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClusterDataRecoveryPolicy(String value) {
        this.clusterDataRecoveryPolicy = value;
    }

    /**
     * Gets the value of property autoRemoveStaleData.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isAutoRemoveStaleData() {
        return autoRemoveStaleData;
    }

    /**
     * Sets the value of property autoRemoveStaleData.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setAutoRemoveStaleData(Boolean value) {
        this.autoRemoveStaleData = value;
    }

    /**
     * Gets the value of property encryptionAtRest.
     * 
     * @return
     *     possible object is
     *     {@link EncryptionAtRest }
     *     
     */
    public EncryptionAtRest getEncryptionAtRest() {
        return encryptionAtRest;
    }

    /**
     * Sets the value of property encryptionAtRest.
     * 
     * @param value
     *     allowed object is
     *     {@link EncryptionAtRest }
     *     
     */
    public void setEncryptionAtRest(EncryptionAtRest value) {
        this.encryptionAtRest = value;
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

}

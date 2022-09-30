
package fish.payara.nucleus.hazelcast.xsd;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of cp-subsystem complex type.
 * 
 *
 * 
 * <pre>
 * &lt;complexType name="cp-subsystem">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="cp-member-count" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="group-size" type="{http://www.hazelcast.com/schema/config}cp-group-size" minOccurs="0"/>
 *         &lt;element name="session-time-to-live-seconds" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="session-heartbeat-interval-seconds" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="missing-cp-member-auto-removal-seconds" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="fail-on-indeterminate-operation-state" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="persistence-enabled" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="base-dir" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="data-load-timeout-seconds" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="raft-algorithm" type="{http://www.hazelcast.com/schema/config}raft-algorithm" minOccurs="0"/>
 *         &lt;element name="semaphores" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="semaphore" type="{http://www.hazelcast.com/schema/config}semaphore" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="locks" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="fenced-lock" type="{http://www.hazelcast.com/schema/config}fenced-lock" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cp-subsystem", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class CpSubsystem {

    @XmlElement(name = "cp-member-count", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    @XmlSchemaType(name = "unsignedInt")
    protected Long cpMemberCount;
    @XmlElement(name = "group-size", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    @XmlSchemaType(name = "unsignedInt")
    protected Long groupSize;
    @XmlElement(name = "session-time-to-live-seconds", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "60")
    @XmlSchemaType(name = "unsignedInt")
    protected Long sessionTimeToLiveSeconds;
    @XmlElement(name = "session-heartbeat-interval-seconds", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "5")
    @XmlSchemaType(name = "unsignedInt")
    protected Long sessionHeartbeatIntervalSeconds;
    @XmlElement(name = "missing-cp-member-auto-removal-seconds", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    @XmlSchemaType(name = "unsignedInt")
    protected Long missingCpMemberAutoRemovalSeconds;
    @XmlElement(name = "fail-on-indeterminate-operation-state", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean failOnIndeterminateOperationState;
    @XmlElement(name = "persistence-enabled", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "false")
    protected Boolean persistenceEnabled;
    @XmlElement(name = "base-dir", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "cp-data")
    protected String baseDir;
    @XmlElement(name = "data-load-timeout-seconds", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "120")
    @XmlSchemaType(name = "unsignedInt")
    protected Long dataLoadTimeoutSeconds;
    @XmlElement(name = "raft-algorithm", namespace = "http://www.hazelcast.com/schema/config")
    protected RaftAlgorithm raftAlgorithm;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Semaphores semaphores;
    @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
    protected Locks locks;

    /**
     * Gets the value of property cpMemberCount.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getCpMemberCount() {
        return cpMemberCount;
    }

    /**
     * Sets the value of property cpMemberCount.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setCpMemberCount(Long value) {
        this.cpMemberCount = value;
    }

    /**
     * Gets the value of property groupSize.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getGroupSize() {
        return groupSize;
    }

    /**
     * Sets the value of property groupSize.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setGroupSize(Long value) {
        this.groupSize = value;
    }

    /**
     * Gets the value of property sessionTimeToLiveSeconds.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getSessionTimeToLiveSeconds() {
        return sessionTimeToLiveSeconds;
    }

    /**
     * Sets the value of property sessionTimeToLiveSeconds.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setSessionTimeToLiveSeconds(Long value) {
        this.sessionTimeToLiveSeconds = value;
    }

    /**
     * Gets the value of property sessionHeartbeatIntervalSeconds.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getSessionHeartbeatIntervalSeconds() {
        return sessionHeartbeatIntervalSeconds;
    }

    /**
     * Sets the value of property sessionHeartbeatIntervalSeconds.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setSessionHeartbeatIntervalSeconds(Long value) {
        this.sessionHeartbeatIntervalSeconds = value;
    }

    /**
     * Gets the value of property missingCpMemberAutoRemovalSeconds.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getMissingCpMemberAutoRemovalSeconds() {
        return missingCpMemberAutoRemovalSeconds;
    }

    /**
     * Sets the value of property missingCpMemberAutoRemovalSeconds.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setMissingCpMemberAutoRemovalSeconds(Long value) {
        this.missingCpMemberAutoRemovalSeconds = value;
    }

    /**
     * Gets the value of property failOnIndeterminateOperationState.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isFailOnIndeterminateOperationState() {
        return failOnIndeterminateOperationState;
    }

    /**
     * Sets the value of property failOnIndeterminateOperationState.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setFailOnIndeterminateOperationState(Boolean value) {
        this.failOnIndeterminateOperationState = value;
    }

    /**
     * Gets the value of property persistenceEnabled.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }

    /**
     * Sets the value of property persistenceEnabled.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setPersistenceEnabled(Boolean value) {
        this.persistenceEnabled = value;
    }

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
     * Gets the value of property raftAlgorithm.
     * 
     * @return
     *     possible object is
     *     {@link RaftAlgorithm }
     *     
     */
    public RaftAlgorithm getRaftAlgorithm() {
        return raftAlgorithm;
    }

    /**
     * Sets the value of property raftAlgorithm.
     * 
     * @param value
     *     allowed object is
     *     {@link RaftAlgorithm }
     *     
     */
    public void setRaftAlgorithm(RaftAlgorithm value) {
        this.raftAlgorithm = value;
    }

    /**
     * Gets the value of property semaphores.
     * 
     * @return
     *     possible object is
     *     {@link Semaphores }
     *     
     */
    public Semaphores getSemaphores() {
        return semaphores;
    }

    /**
     * Sets the value of property semaphores.
     * 
     * @param value
     *     allowed object is
     *     {@link Semaphores }
     *     
     */
    public void setSemaphores(Semaphores value) {
        this.semaphores = value;
    }

    /**
     * Gets the value of property locks.
     * 
     * @return
     *     possible object is
     *     {@link Locks }
     *     
     */
    public Locks getLocks() {
        return locks;
    }

    /**
     * Sets the value of property locks.
     * 
     * @param value
     *     allowed object is
     *     {@link Locks }
     *     
     */
    public void setLocks(Locks value) {
        this.locks = value;
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
     *         &lt;element name="fenced-lock" type="{http://www.hazelcast.com/schema/config}fenced-lock" maxOccurs="unbounded" minOccurs="0"/>
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
        "fencedLock"
    })
    public static class Locks {

        @XmlElement(name = "fenced-lock", namespace = "http://www.hazelcast.com/schema/config")
        protected List<FencedLock> fencedLock;

        /**
         * Gets the value of the fencedLock property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the fencedLock property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getFencedLock().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link FencedLock }
         * 
         * 
         */
        public List<FencedLock> getFencedLock() {
            if (fencedLock == null) {
                fencedLock = new ArrayList<FencedLock>();
            }
            return this.fencedLock;
        }

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
     *         &lt;element name="semaphore" type="{http://www.hazelcast.com/schema/config}semaphore" maxOccurs="unbounded" minOccurs="0"/>
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
        "semaphore"
    })
    public static class Semaphores {

        @XmlElement(namespace = "http://www.hazelcast.com/schema/config")
        protected List<Semaphore> semaphore;

        /**
         * Gets the value of the semaphore property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the semaphore property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getSemaphore().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Semaphore }
         * 
         * 
         */
        public List<Semaphore> getSemaphore() {
            if (semaphore == null) {
                semaphore = new ArrayList<Semaphore>();
            }
            return this.semaphore;
        }

    }

}

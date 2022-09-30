
package fish.payara.nucleus.hazelcast.xsd;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java Class of raft-algorithm complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="raft-algorithm">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="leader-election-timeout-in-millis" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="leader-heartbeat-period-in-millis" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="max-missed-leader-heartbeat-count" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="append-request-max-entry-count" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="commit-index-advance-count-to-snapshot" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="uncommitted-entry-count-to-reject-new-appends" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *         &lt;element name="append-request-backoff-timeout-in-millis" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "raft-algorithm", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class RaftAlgorithm {

    @XmlElement(name = "leader-election-timeout-in-millis", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "2000")
    @XmlSchemaType(name = "unsignedInt")
    protected Long leaderElectionTimeoutInMillis;
    @XmlElement(name = "leader-heartbeat-period-in-millis", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "5000")
    @XmlSchemaType(name = "unsignedInt")
    protected Long leaderHeartbeatPeriodInMillis;
    @XmlElement(name = "max-missed-leader-heartbeat-count", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "5")
    @XmlSchemaType(name = "unsignedInt")
    protected Long maxMissedLeaderHeartbeatCount;
    @XmlElement(name = "append-request-max-entry-count", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "100")
    @XmlSchemaType(name = "unsignedInt")
    protected Long appendRequestMaxEntryCount;
    @XmlElement(name = "commit-index-advance-count-to-snapshot", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "10000")
    @XmlSchemaType(name = "unsignedInt")
    protected Long commitIndexAdvanceCountToSnapshot;
    @XmlElement(name = "uncommitted-entry-count-to-reject-new-appends", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "100")
    @XmlSchemaType(name = "unsignedInt")
    protected Long uncommittedEntryCountToRejectNewAppends;
    @XmlElement(name = "append-request-backoff-timeout-in-millis", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "100")
    @XmlSchemaType(name = "unsignedInt")
    protected Long appendRequestBackoffTimeoutInMillis;

    /**
     * Gets the value of property leaderElectionTimeoutInMillis.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getLeaderElectionTimeoutInMillis() {
        return leaderElectionTimeoutInMillis;
    }

    /**
     * Sets the value of property leaderElectionTimeoutInMillis.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setLeaderElectionTimeoutInMillis(Long value) {
        this.leaderElectionTimeoutInMillis = value;
    }

    /**
     * Gets the value of property leaderHeartbeatPeriodInMillis.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getLeaderHeartbeatPeriodInMillis() {
        return leaderHeartbeatPeriodInMillis;
    }

    /**
     * Sets the value of property leaderHeartbeatPeriodInMillis.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setLeaderHeartbeatPeriodInMillis(Long value) {
        this.leaderHeartbeatPeriodInMillis = value;
    }

    /**
     * Gets the value of property maxMissedLeaderHeartbeatCount.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getMaxMissedLeaderHeartbeatCount() {
        return maxMissedLeaderHeartbeatCount;
    }

    /**
     * Sets the value of property maxMissedLeaderHeartbeatCount.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setMaxMissedLeaderHeartbeatCount(Long value) {
        this.maxMissedLeaderHeartbeatCount = value;
    }

    /**
     * Gets the value of property appendRequestMaxEntryCount.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getAppendRequestMaxEntryCount() {
        return appendRequestMaxEntryCount;
    }

    /**
     * Sets the value of property appendRequestMaxEntryCount.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setAppendRequestMaxEntryCount(Long value) {
        this.appendRequestMaxEntryCount = value;
    }

    /**
     * Gets the value of property commitIndexAdvanceCountToSnapshot.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getCommitIndexAdvanceCountToSnapshot() {
        return commitIndexAdvanceCountToSnapshot;
    }

    /**
     * Sets the value of property commitIndexAdvanceCountToSnapshot.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setCommitIndexAdvanceCountToSnapshot(Long value) {
        this.commitIndexAdvanceCountToSnapshot = value;
    }

    /**
     * Gets the value of property uncommittedEntryCountToRejectNewAppends.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getUncommittedEntryCountToRejectNewAppends() {
        return uncommittedEntryCountToRejectNewAppends;
    }

    /**
     * Sets the value of property uncommittedEntryCountToRejectNewAppends.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setUncommittedEntryCountToRejectNewAppends(Long value) {
        this.uncommittedEntryCountToRejectNewAppends = value;
    }

    /**
     * Gets the value of property appendRequestBackoffTimeoutInMillis.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getAppendRequestBackoffTimeoutInMillis() {
        return appendRequestBackoffTimeoutInMillis;
    }

    /**
     * Sets the value of property appendRequestBackoffTimeoutInMillis.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setAppendRequestBackoffTimeoutInMillis(Long value) {
        this.appendRequestBackoffTimeoutInMillis = value;
    }

}

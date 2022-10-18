
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import java.math.BigInteger;


/**
 * 
 *                     A probabilistic split brain protection function based on Phi Accrual failure detector. See
 *                     com.hazelcast.internal.cluster.fd.PhiAccrualClusterFailureDetector for implementation
 *                     details. Configuration:
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;br xmlns="http://www.hazelcast.com/schema/config" xmlns:xs="http://www.w3.org/2001/XMLSchema"/&gt;
 * </pre>
 * 
 *                     - 
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;code xmlns="http://www.hazelcast.com/schema/config" xmlns:xs="http://www.w3.org/2001/XMLSchema"&gt;acceptable-heartbeat-pause-millis&lt;/code&gt;
 * </pre>
 * : duration in milliseconds corresponding to number
 *                     of potentially lost/delayed heartbeats that will be accepted before considering it to be an anomaly.
 *                     This margin is important to be able to survive sudden, occasional, pauses in heartbeat arrivals,
 *                     due to for example garbage collection or network drops.
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;br xmlns="http://www.hazelcast.com/schema/config" xmlns:xs="http://www.w3.org/2001/XMLSchema"/&gt;
 * </pre>
 * 
 *                     - 
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;code xmlns="http://www.hazelcast.com/schema/config" xmlns:xs="http://www.w3.org/2001/XMLSchema"&gt;suspicion-threshold&lt;/code&gt;
 * </pre>
 * : threshold for suspicion level. A low threshold is prone to generate
 *                     many wrong suspicions but ensures a quick detection in the event of a real crash. Conversely, a high
 *                     threshold generates fewer mistakes but needs more time to detect actual crashes.
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;br xmlns="http://www.hazelcast.com/schema/config" xmlns:xs="http://www.w3.org/2001/XMLSchema"/&gt;
 * </pre>
 * 
 *                     - 
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;code xmlns="http://www.hazelcast.com/schema/config" xmlns:xs="http://www.w3.org/2001/XMLSchema"&gt;max-sample-size&lt;/code&gt;
 * </pre>
 * : number of samples to use for calculation of mean and standard
 *                     deviation of inter-arrival times.
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;br xmlns="http://www.hazelcast.com/schema/config" xmlns:xs="http://www.w3.org/2001/XMLSchema"/&gt;
 * </pre>
 * 
 *                     - 
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;code xmlns="http://www.hazelcast.com/schema/config" xmlns:xs="http://www.w3.org/2001/XMLSchema"&gt;heartbeat-interval-millis&lt;/code&gt;
 * </pre>
 * : bootstrap the stats with heartbeats that corresponds to
 *                     this duration in milliseconds, with a rather high standard deviation (since environment is unknown
 *                     in the beginning)
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;br xmlns="http://www.hazelcast.com/schema/config" xmlns:xs="http://www.w3.org/2001/XMLSchema"/&gt;
 * </pre>
 * 
 *                     - 
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;&lt;code xmlns="http://www.hazelcast.com/schema/config" xmlns:xs="http://www.w3.org/2001/XMLSchema"&gt;min-std-deviation-millis&lt;/code&gt;
 * </pre>
 * : minimum standard deviation (in milliseconds) to use for the normal
 *                     distribution used when calculating phi. Too low standard deviation might result in too much
 *                     sensitivity for sudden, but normal, deviations in heartbeat inter arrival times.
 *                 
 * 
 * <p>Java Class of anonymous complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="acceptable-heartbeat-pause-millis" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" default="60000" />
 *       &lt;attribute name="suspicion-threshold" type="{http://www.w3.org/2001/XMLSchema}double" default="10" />
 *       &lt;attribute name="max-sample-size" type="{http://www.w3.org/2001/XMLSchema}unsignedInt" default="200" />
 *       &lt;attribute name="min-std-deviation-millis" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" default="100" />
 *       &lt;attribute name="heartbeat-interval-millis" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" default="5000" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
public class ProbabilisticSplitBrainProtection {

    @XmlAttribute(name = "acceptable-heartbeat-pause-millis")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger acceptableHeartbeatPauseMillis;
    @XmlAttribute(name = "suspicion-threshold")
    protected Double suspicionThreshold;
    @XmlAttribute(name = "max-sample-size")
    @XmlSchemaType(name = "unsignedInt")
    protected Long maxSampleSize;
    @XmlAttribute(name = "min-std-deviation-millis")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger minStdDeviationMillis;
    @XmlAttribute(name = "heartbeat-interval-millis")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger heartbeatIntervalMillis;

    /**
     * Gets the value of property acceptableHeartbeatPauseMillis.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getAcceptableHeartbeatPauseMillis() {
        if (acceptableHeartbeatPauseMillis == null) {
            return new BigInteger("60000");
        } else {
            return acceptableHeartbeatPauseMillis;
        }
    }

    /**
     * Sets the value of property acceptableHeartbeatPauseMillis.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setAcceptableHeartbeatPauseMillis(BigInteger value) {
        this.acceptableHeartbeatPauseMillis = value;
    }

    /**
     * Gets the value of property suspicionThreshold.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public double getSuspicionThreshold() {
        if (suspicionThreshold == null) {
            return  10.0D;
        } else {
            return suspicionThreshold;
        }
    }

    /**
     * Sets the value of property suspicionThreshold.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setSuspicionThreshold(Double value) {
        this.suspicionThreshold = value;
    }

    /**
     * Gets the value of property maxSampleSize.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public long getMaxSampleSize() {
        if (maxSampleSize == null) {
            return  200L;
        } else {
            return maxSampleSize;
        }
    }

    /**
     * Sets the value of property maxSampleSize.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setMaxSampleSize(Long value) {
        this.maxSampleSize = value;
    }

    /**
     * Gets the value of property minStdDeviationMillis.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getMinStdDeviationMillis() {
        if (minStdDeviationMillis == null) {
            return new BigInteger("100");
        } else {
            return minStdDeviationMillis;
        }
    }

    /**
     * Sets the value of property minStdDeviationMillis.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setMinStdDeviationMillis(BigInteger value) {
        this.minStdDeviationMillis = value;
    }

    /**
     * Gets the value of property heartbeatIntervalMillis.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getHeartbeatIntervalMillis() {
        if (heartbeatIntervalMillis == null) {
            return new BigInteger("5000");
        } else {
            return heartbeatIntervalMillis;
        }
    }

    /**
     * Sets the value of property heartbeatIntervalMillis.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setHeartbeatIntervalMillis(BigInteger value) {
        this.heartbeatIntervalMillis = value;
    }

}

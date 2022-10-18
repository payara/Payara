
package fish.payara.nucleus.hazelcast.xsd;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import java.math.BigInteger;


/**
 * 
 *                 SQL service configuration.
 *             
 * 
 * <p>Java Class of sql complex type.
 * 
 * 
 * 
 * <pre>
 * &lt;complexType name="sql">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="executor-pool-size" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="statement-timeout-millis" type="{http://www.w3.org/2001/XMLSchema}unsignedLong" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sql", namespace = "http://www.hazelcast.com/schema/config", propOrder = {

})
public class Sql {

    @XmlElement(name = "executor-pool-size", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "-1")
    protected Integer executorPoolSize;
    @XmlElement(name = "statement-timeout-millis", namespace = "http://www.hazelcast.com/schema/config", defaultValue = "0")
    @XmlSchemaType(name = "unsignedLong")
    protected BigInteger statementTimeoutMillis;

    /**
     * Gets the value of property executorPoolSize.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getExecutorPoolSize() {
        return executorPoolSize;
    }

    /**
     * Sets the value of property executorPoolSize.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setExecutorPoolSize(Integer value) {
        this.executorPoolSize = value;
    }

    /**
     * Gets the value of property statementTimeoutMillis.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getStatementTimeoutMillis() {
        return statementTimeoutMillis;
    }

    /**
     * Sets the value of property statementTimeoutMillis.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setStatementTimeoutMillis(BigInteger value) {
        this.statementTimeoutMillis = value;
    }

}

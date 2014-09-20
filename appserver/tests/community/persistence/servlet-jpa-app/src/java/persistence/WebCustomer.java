/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

/*
 * WebCustomer.java
 *
 * Created on March 17, 2008, 12:59 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package persistence;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;


/**
 * Entity class WebCustomer
 * 
 * @author adminuser
 */
@Entity
@Table(name = "WEB_CUSTOMER")
@NamedQueries( {
        @NamedQuery(name = "WebCustomer.findByCustId", query = "SELECT w FROM WebCustomer w WHERE w.custId = :custId"),
        @NamedQuery(name = "WebCustomer.findByCity", query = "SELECT w FROM WebCustomer w WHERE w.city = :city"),
        @NamedQuery(name = "WebCustomer.findByCustname", query = "SELECT w FROM WebCustomer w WHERE w.custname = :custname")
    })
public class WebCustomer implements Serializable {

    @Id
    @Column(name = "CUST_ID", nullable = false)
    private Integer custId;

    @Column(name = "CITY")
    private String city;

    @Column(name = "CUSTNAME")
    private String custname;
    
    /** Creates a new instance of WebCustomer */
    public WebCustomer() {
    }

    /**
     * Creates a new instance of WebCustomer with the specified values.
     * @param custId the custId of the WebCustomer
     */
    public WebCustomer(Integer custId) {
        this.custId = custId;
    }

    /**
     * Gets the custId of this WebCustomer.
     * @return the custId
     */
    public Integer getCustId() {
        return this.custId;
    }

    /**
     * Sets the custId of this WebCustomer to the specified value.
     * @param custId the new custId
     */
    public void setCustId(Integer custId) {
        this.custId = custId;
    }

    /**
     * Gets the city of this WebCustomer.
     * @return the city
     */
    public String getCity() {
        return this.city;
    }

    /**
     * Sets the city of this WebCustomer to the specified value.
     * @param city the new city
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * Gets the custname of this WebCustomer.
     * @return the custname
     */
    public String getCustname() {
        return this.custname;
    }

    /**
     * Sets the custname of this WebCustomer to the specified value.
     * @param custname the new custname
     */
    public void setCustname(String custname) {
        this.custname = custname;
    }

    /**
     * Returns a hash code value for the object.  This implementation computes 
     * a hash code value based on the id fields in this object.
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (this.custId != null ? this.custId.hashCode() : 0);
        return hash;
    }

    /**
     * Determines whether another object is equal to this WebCustomer.  The result is 
     * <code>true</code> if and only if the argument is not null and is a WebCustomer object that 
     * has the same id field values as this object.
     * @param object the reference object with which to compare
     * @return <code>true</code> if this object is the same as the argument;
     * <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof WebCustomer)) {
            return false;
        }
        WebCustomer other = (WebCustomer)object;
        if (this.custId != other.custId && (this.custId == null || !this.custId.equals(other.custId))) return false;
        return true;
    }

    /**
     * Returns a string representation of the object.  This implementation constructs 
     * that representation based on the id fields.
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return "persistence.WebCustomer[custId=" + custId + "]";
    }
    
}

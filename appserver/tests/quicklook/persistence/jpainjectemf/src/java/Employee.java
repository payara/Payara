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

package myapp;

import javax.persistence.*;

@Entity
@Table(name="EMPLOYEE")
public class Employee implements java.io.Serializable {

    private int	     	     id;
    private String	     firstName;
    private String	     lastName;
    private Department       department;

    public Employee() {
    }

    public Employee(int id, String firstName, String lastName)
    {
        	this.id = id;
        	this.firstName = firstName;
        	this.lastName = lastName;
    }

    public Employee(int id, String firstName, String lastName, 
		    Department department)
    {
        	this.id = id;
        	this.firstName = firstName;
        	this.lastName = lastName;
        	this.department = department;
    }

   // ===========================================================
   // getters and setters for the state fields
    @Id
    @Column(name="ID")
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    @Column(name="FIRSTNAME")
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Column(name="LASTNAME")
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }


    // ===========================================================
    // getters and setters for the association fields
    // @ManyToOne
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="DEPARTMENT_ID")
    public Department getDepartment() {
        return department;
    }

    @Transient
    public Department getDepartmentNoWeaving() {
        try {
            java.lang.reflect.Field f = Employee.class.getDeclaredField("department");
            return (Department) f.get(this);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Please change argument to getDeclaredField", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String toString() {
        return "Employee id=" + getId() 
                + ", firstName=" + getFirstName() 
                + ", lastName=" + getLastName() 
                + ", department=" + getDepartment();
    }


}


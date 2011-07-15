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

import javax.ejb.Stateless;
import javax.persistence.*;
import java.util.*;

@Stateless
public class TestEJB {
    
    @PersistenceContext EntityManager em;

    public TestEJB() {
    }

    // initData
    public boolean test1() {
        boolean pass= false;

	Employee e1 = new Employee(1, "emp1", 1000);
	Employee e2 = new Employee(2, "emp2", 2000);
	Employee e3 = new Employee(3, "emp3", 3000);
	Employee e4 = new Employee(4, "emp4", 4000);
	Project p1 = new Project(1, "proj1");
	Collection<Employee> employees = new ArrayList<Employee>();
	employees.add(e1);
	employees.add(e2);
	employees.add(e3);
	employees.add(e4);
	p1.setEmployees(employees);
     
	// Persist Cascade without long name
	try {
	  System.out.println("1. Persisting project....");
	  em.persist(p1);
          em.flush();
	  pass = true;
        } catch(Throwable e){
	  e.printStackTrace();
        }
        return pass;
    }

    // persist Employee with a long name
    public boolean test2() {
        boolean pass= false;
	Employee e5 = new Employee(5, "myLongName5", 5000);
	try {
	  System.out.println("2. Persisting employee with long name....");
	  em.persist(e5);
          em.flush();
	  System.out.println("Error: not get BV ex for persist");
	} catch(javax.validation.ConstraintViolationException ex){
	  System.out.println("Expected BV Ex");
	  pass= true;
	  String msg = ex.getMessage();
	  System.out.println("msg="+msg);
	} catch(Throwable e){
	  System.out.println("Unexpected Ex");
	  e.printStackTrace();
        }
        return pass;
    }


    // update Employee with a long name
    public boolean test3() {
        boolean pass= false;
	try {
	  System.out.println("3. Updating employee with long name....");
	  Employee e = em.find(Employee.class, 3);
	  e.setName("myLongName3");
	  em.flush();
	  System.out.println("Error: not get BV ex for update");
	} catch (javax.validation.ConstraintViolationException ex) {
	  System.out.println("Expected BV Ex");
	  pass= true;
	  String msg = ex.getMessage();
	  System.out.println("msg="+msg);
	} catch(Throwable e){
	  System.out.println("Unexpected Ex");
	  e.printStackTrace();
        }
        return pass;
    }

    // remove Employee with a long name
    public boolean test4() {
        boolean pass= false;
        try {
	  System.out.println("4. Removing employee with long name....");
	  Employee e = em.find(Employee.class, 1);
	  e.setName("myLongName1");
	  em.remove(e);
	  em.flush();
	  System.out.println("OK: not get BV ex for remove");
	  pass= true;
	} catch (javax.validation.ConstraintViolationException ex) {
	  System.out.println("BV Ex");
	  String msg = ex.getMessage();
	  System.out.println("msg="+msg);
	} catch(Throwable e){
	  System.out.println("Unexpected Ex");
	  e.printStackTrace();
        }
        return pass;
    }

    // verify previous operations
    public boolean test5() {
        boolean pass= false;
        boolean bvsize = true;
	try {
	  System.out.println("5. Verifying employee ....");
	  Employee emp = null;
	  Query q= em.createQuery("SELECT e FROM Employee e");
	  List result = q.getResultList();
	  int size = result.size();
	  for (int i = 0 ; i < size ; i++) {
	    emp = (Employee) result.get(i);          
	    String name = emp.getName();
	    System.out.println("i=" + i + ", name=" + name);
	    if (name.length() > 5) {
	      bvsize = false;
	    }
	  }
	  System.out.println("size =" +size+", bvsize="+bvsize);
	  if (size == 3 && bvsize){
	    pass = true;
	  }
	} catch(Throwable e){
	  System.out.println("Unexpected Ex");
	  e.printStackTrace();
        }
        return pass;
    }


}





/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package com.sun.enterprise.tools.classmodel;

import static org.junit.Assert.*;

import org.junit.Test;


public class UtilitiesTest {

  @Test
  public void testSortInhabitants() {
    StringBuilder sb = new StringBuilder();
    sb.append("class=service1,index=contract1:name1,index=annotation1\n");
    sb.append("class=service2,index=contract1,index=annotation1\n");
    sb.append("class=service3,index=contract2:name2,index=contract1:name2,index=annotation2,index=annotation1,b=2,a=1\n");
    sb.append("class=service4,a=1\n");
    String expected = sb.toString();
    String output = Utilities.sortInhabitantsDescriptor(testCase(), false);
    assertEquals(expected, output);
  }

  @Test
  public void testSortInhabitantsAndContents() {
    StringBuilder sb = new StringBuilder();
    sb.append("class=service1,index=annotation1,index=contract1:name1\n");
    sb.append("class=service2,index=annotation1,index=contract1\n");
    sb.append("class=service3,index=annotation1,index=annotation2,index=contract1:name2,index=contract2:name2,a=1,b=2\n");
    sb.append("class=service4,a=1\n");
    String expected = sb.toString();
    String output = Utilities.sortInhabitantsDescriptor(testCase(), true);
    assertEquals(expected, output);
  }
  
  @Test
  public void testEmptySort() {
    String output = Utilities.sortInhabitantsDescriptor("", true);
    assertEquals("", output);
  }
  
  private String testCase() {
    StringBuilder sb = new StringBuilder();
    sb.append("# comment\n");
    sb.append("class=service2,index=contract1,index=annotation1\n");
    sb.append("class=service1,index=contract1:name1,index=annotation1\n");
    sb.append("class=service4,a=1\n");
    sb.append("class=service3,index=contract2:name2,index=contract1:name2,index=annotation2,index=annotation1,b=2,a=1\n");
    return sb.toString();
  }

}

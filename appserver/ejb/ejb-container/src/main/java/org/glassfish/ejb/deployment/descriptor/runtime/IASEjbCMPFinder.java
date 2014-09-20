/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.deployment.descriptor.runtime;
    
import org.glassfish.deployment.common.Descriptor;

public class IASEjbCMPFinder extends Descriptor {
    
      private String method_name = null;
      private String query_params = null;
      private String query_filter = null;
      private String query_variables = null;
      private String query_ordering = null;
      
      public IASEjbCMPFinder () {
      }
      
      public IASEjbCMPFinder(String method_name, String params, String filter) {
        this.method_name = method_name;
        this.query_params = params;
        this.query_filter = filter;
        this.query_variables = "";
      }

      public IASEjbCMPFinder(String method_name, String params, String filter, String variables) {
        this.method_name = method_name;
        this.query_params = params;
        this.query_filter = filter;
        this.query_variables = variables;
      }
      
      public String getMethodName() {
        return method_name;
      }
      
      public String getQueryParameterDeclaration() {
        return query_params;
      }

      public String getQueryFilter() {
        return query_filter;
      }
 
      public String getQueryVariables() {
        return query_variables;
      }
      
      public void setMethodName(String name) {
        method_name = name;
      }
      
      public void setQueryParameterDeclaration(String qry) {
        query_params = qry;
      }
 
      public void setQueryVariables(String qryvariables) {
        query_variables = qryvariables;
      }
      
      public void setQueryFilter(String qryfilter) {
        query_filter = qryfilter;
      }
      
      public String getQueryOrdering() {
	  return query_ordering;
      }
      
      public void setQueryOrdering(String qryordering) {
	  query_ordering = qryordering;
      }
}
      

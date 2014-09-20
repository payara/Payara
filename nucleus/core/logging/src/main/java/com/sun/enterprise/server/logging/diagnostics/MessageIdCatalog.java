/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.server.logging.diagnostics;

import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

/**
 * Simple catalog class to locate Diagnostic Information based on
 * message id as the key.  resource bundle is located using the module name.
 *
 * @author Carla Mott
 */
public class MessageIdCatalog{
    
     /**
      * Get all the documented DiagnosticCauses for a given message id.
      * The results will be localized based on the current locale of
      * the AppServer's JVM.
      */
     public ArrayList getDiagnosticCausesForMessageId( String messageId, String moduleName ) {
	    if (moduleName == null || messageId == null)
		    return null;
	    ResourceBundle rb = java.util.logging.Logger.getLogger(moduleName).getResourceBundle();
         String cause = null;
         ArrayList causes = null;
         if( rb != null ) {
             for( int i = 1; i < DiagConstants.MAX_CAUSES_AND_CHECKS; i++ ) {
                 // The convention used to document diagnostic causes in
                 // resource bundle is
                 // <MsgId>.diag.cause.1= <Cause 1>
                 // <MsgId>.diag.cause.2= <Cause 2> ....
                 try { 
                     cause = rb.getString( messageId + 
                             DiagConstants.CAUSE_PREFIX + i );
                 } catch( MissingResourceException e ) {
                     // We couldn't find any causes listed for the message
                     // id or we have found all. In either case we are
                     // covered here.
                     break;
                 }
                 if( cause == null ) { break; }
                 if( causes == null ) { 
                         causes = new ArrayList( ); 
                 }
                 causes.add( cause );
             }
        }
        return causes;
     }

     /**
      * Get all the documented DiagnosticChecks for a given message id.
      * The results will be localized based on the current locale of
      * the AppServer's JVM.
      */
     public ArrayList getDiagnosticChecksForMessageId( String messageId, String moduleName ) {

         if (moduleName == null || messageId == null)
             return null;
         ResourceBundle rb = java.util.logging.Logger.getLogger(moduleName).getResourceBundle();

         String check = null;
         ArrayList checks = null;
         if( rb != null ) {
             for( int i = 1; i < DiagConstants.MAX_CAUSES_AND_CHECKS; i++ ) {
                 // The convention used to document diagnostic checks in
                 // resource bundle is
                 // <MsgId>.diag.check.1= <Check 1>
                 // <MsgId>.diag.check.2= <Check 2> ....
                 try {
                         check = rb.getString( messageId + 
                                 DiagConstants.CHECK_PREFIX + i );
                 } catch( MissingResourceException e ) {
                     // We couldn't find any checks listed for the message
                     // id or we have found all. In either case we are
                     // covered here.
                     break;
                 }
                 if( check == null ) break;
                 if( checks == null ) { 
                     checks = new ArrayList( );
                 }
                 checks.add( check );
             }
         }
         return checks; 
     }

     /**
      * We may collect lot of diagnostic causes and diagnostic checks for
      * some common message id from the field. We may document those 
      * even after the product is shipped. We are planning to generate the
      * HTML's from the resource bundle's diagnostics and update the javadoc
      * or knowledgebase site. This URI should help us to locate the latest
      * and greatest diagnostic info based on the message id.
      */
     /*
     need to get the module id from the logger name.  The first part of the name maps.
     public String getDiagnosticURIForMessageId( String messageId, String moduleName ) {
         ResourceBundle rb = java.util.logging.Logger.getLogger(moduleName).getResourceBundle();
         if( moduleId == null ) { return null; }
         return DiagConstants.URI_PREFIX + moduleId + "/" + messageId;
     }

     public Diagnostics getDiagnosticsForMessageId( String messageId ) {
         ArrayList causes = getDiagnosticCausesForMessageId( messageId );
         ArrayList checks = getDiagnosticChecksForMessageId( messageId );
         if( ( causes == null )
           &&( checks == null ) ) {
             return null;
         }
         Diagnostics diagnostics = new Diagnostics( messageId );
         diagnostics.setPossibleCauses( causes );
         diagnostics.setDiagnosticChecks( checks );
         diagnostics.setURI( getDiagnosticURIForMessageId( messageId ) );
         return diagnostics;
     }
     */
}
     

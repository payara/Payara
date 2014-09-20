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

package org.glassfish.admin.amx.logging;

import javax.management.MBeanOperationInfo;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;
import java.util.Map;
import org.glassfish.admin.amx.annotation.Description;
import org.glassfish.admin.amx.annotation.ManagedAttribute;
import org.glassfish.admin.amx.annotation.ManagedOperation;
import org.glassfish.admin.amx.annotation.Param;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

/**
	Supports accessing logging information in multiple ways.  The following are supported:
	<ul>
	<li>Emission of pseudo real-time JMX Notifications when a
		log record is created--see {@link LogRecordEmitter}</li>
	<li>Access to existing log file contents--see {@link LogFileAccess}</li>
	<li>Querying for log entries--see {@link LogQuery}</li>
	</ul>
	@since AS 9.0
 */
@Taxonomy(stability = Stability.EXPERIMENTAL)
@AMXMBeanMetadata(singleton=true, globalSingleton=true, leaf=true)
public interface Logging
	extends AMXProxy, LogQuery
    // LogFileAccess    not implemented yet
    // LogRecordEmitter    not implemented yet
    // LogAnalyzer    not implemented yet
{
   /**
		Sets the log level of the Logger for the specified module.  This operation
		will not effect a change to the corresponding loggin configuration for that module.
		
		@param module	a module name as specified in {@link LogModuleNames}.
		@param level	a log level
     */
    @ManagedOperation(impact=MBeanOperationInfo.ACTION)
    @Description( "Sets the log level of the Logger for the specified module" )
    public void setModuleLogLevel(
        @Param(name="moduleName") final String moduleName,
        @Param(name="level") final String level );

   /**
   		Gets the log level of the Logger for the specified module, which may or may not
   		be the same as that found in the configuration.
		
   		
   		@param moduleName a module name as specified in {@link LogModuleNames}
    */
    @ManagedOperation(impact=MBeanOperationInfo.INFO)
    @Description( "Gets the log level of the Logger for the specified module" )
    public String getModuleLogLevel( @Param(name="moduleName") final String moduleName );

      /**
    		Sets the log level of the Logger for the specified module.  This operation
    		will not effect a change to the corresponding loggin configuration for that module.

        */
    @ManagedOperation(impact=MBeanOperationInfo.ACTION)
    @Description( "Sets the value of one or more logging properties" )
    public void updateLoggingProperties(
          @Param(name="properties") final Map <String, String> properties );

    /**
        Gets all the logging properties in the logging.properties file
     */
     @ManagedAttribute
     @Description( "Gets all the logging properties" )
     public Map<String, String> getLoggingProperties( );

    /**
     Gets the configuration properties for logging
     */
     @ManagedAttribute
     @Description( "Get logging configuration properties" )
     public Map<String, String>  getLoggingAttributes( );


    /**
       		Sets the value of one or more of the logging configuration properties .

     */

    @ManagedOperation(impact=MBeanOperationInfo.ACTION)
    @Description( "Set value of the value of one or more of the logging configuration properties." )
    public void updateLoggingAttributes(
        @Param(name="properties") final Map <String, String> properties );


    /**
        This method may be used to verify that your Logging listener is working
        correctly.
        @param level the log level of the log message.
        @param message  the message to be placed in Notif.getMessage()
     */
     
    @ManagedOperation(impact=MBeanOperationInfo.INFO)
    public void testEmitLogMessage(
         @Param(name="level") final String level,
         @Param(name="message") final String message );
}








/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jts.trace;

import java.io.*;
import java.util.logging.*;
import javax.transaction.xa.XAException;

import com.sun.jts.CosTransactions.*;
import org.omg.CosTransactions.*;

/**
 * This class is used to set trace properties and print trace statements. The print method does the printing.
 * All the methods are unsynchronized. Since setting of properties doesn't happen simultaneously with print
 * in current usage, this is fine. The tracing should be enabled/disabled by calling 
 * <code> Configuration.enableTrace()/disableTrace()</code>
 * prior to any operation on TraceUtil.
 * It uses TraceRecordFormatter for formatting the trace record. 
 *
 * @author <a href="mailto:kannan.srinivasan@sun.com">Kannan Srinivasan</a>
 * @version 1.0
 */
public class TraceUtil
{
	private static int m_currentTraceLevel = TraceLevel.IAS_JTS_TRACE_TRIVIAL ;
	private static char m_fieldDelimiter = ':';
	private static String m_traceRecordTag = "iAS_JTS_Trace> ";
	private static PrintWriter m_traceWriter = null ;

	static
	{
		m_traceWriter = new PrintWriter(System.out);
	}

 /**
   * Initialises the trace class with given output writer. 
   *
   * @param traceWriter an <code>PrintWriter</code> value
   */
    public static void init(PrintWriter traceWriter)
    {
        setTraceWriter(traceWriter);
    }

  /**
   * Sets the output writer. By default the output writer is set to Stdout.
   *
   * @param traceWriter an <code>PrintWriter</code> value
   */
    public static void setTraceWriter(PrintWriter traceWriter)
    {
	m_traceWriter = traceWriter;
    }

  /**
   * Gets the current output writer.
   *
   * @return an <code>PrintWriter</code> value
   */
    public static PrintWriter getTraceWriter()
    {
        return m_traceWriter;
    }

  /**
   * Gets the current trace level. Returns an integer as per the TraceLevel constants.
   *
   * @return an <code>int</code> value
   */
    public static int getCurrentTraceLevel()
    {
        return m_currentTraceLevel;
    }

  /**
   * Sets the current trace level. The argument is tested for its validity and trace level is set.
   * Else an exception is raised.
   *
   * @param traceLevel an <code>int</code> value
   * @exception InvalidTraceLevelException if an error occurs
   */
    public static void setCurrentTraceLevel(int traceLevel) throws InvalidTraceLevelException
    {
        if(Configuration.isTraceEnabled())
        {
            int i;
            boolean traceLevelSet = false;
            for(i = 0; i <= TraceLevel.IAS_JTS_MAX_TRACE_LEVEL; i++)
            {
                if(traceLevel == i)
                {
                    m_currentTraceLevel = traceLevel;
                    traceLevelSet = true;
                    break;
                }
            } 
            if(!traceLevelSet)
                throw new InvalidTraceLevelException();

        }
    }

  /**
   * This method formats and writes the trace record to output writer. The method is called
   * with a tracelevel, which is checked with current trace level and if found equal or higher,
   * the print is carried out. This method takes an PrintWriter also, which is used to write the
   * output. This given outputWriter would override the set outputWriter. The origin object is printed
   * using its toString() method.
   * @param traceLevel an <code>int</code> value
   * @param outWriter an <code>PrintWriter</code> value
   * @param tid an <code>Object</code> value
   * @param origin an <code>Object</code> value
   * @param msg a <code>String</code> value
   */
    public static void print(int traceLevel, PrintWriter outWriter, Object tid, Object origin, String msg) 
    {
            if( traceLevel <= m_currentTraceLevel )
            {
            	String traceRecord = TraceRecordFormatter.createTraceRecord(tid, origin, msg); 
		outWriter.println(traceRecord);
            }
    }

  /**
   * This method formats and writes the trace record to current output writer. The method is 
   * called with a tracelevel, which is checked with current trace level and if found equal 
   * or higher, the print is carried out. This method doesn't take a otid and tries to recover
   * it from current obejct asscociated with this thread
   * @param traceLevel an <code>int</code> value
   * @param origin an <code>Object</code> value
   * @param msg a <code>String</code> value
   */
    public static void print(int traceLevel, Object origin, String msg) 
    {
	try{
		 print(traceLevel,
	          ((com.sun.jts.CosTransactions.TopCoordinator)
		  com.sun.jts.CosTransactions.CurrentTransaction.getCurrent().get_localCoordinator()).get_transaction_name(),
		  origin,
		  msg
		  );
	}catch(Exception e){
    		print(traceLevel,null,origin,msg);
	}
    }

  /**
   * This method formats and writes the trace record to current output writer. The method is called
   * with a tracelevel, which is checked with current trace level and if found equal or higher,
   * the print is carried out. This uses the currently set output writer to write the trace output.
   * @param traceLevel an <code>int</code> value
   * @param tid an <code>Object</code> value
   * @param origin an <code>Object</code> value
   * @param msg a <code>String</code> value
   */
    public static void print(int traceLevel, Object tid, Object origin, String msg) 
    {
    	print(traceLevel,m_traceWriter,tid,origin,msg);
    }

  /**
   * Gets the current field delimiter used in formatting trace record. The default is ':'.
   *
   * @return a <code>char</code> value
   */
    public static char getFieldDelimiter()
    {
        return m_fieldDelimiter;
     }    

  /**
   * Sets the current field delimiter.
   *
   * @param delimiter a <code>char</code> value
   */
    public static void setFieldDelimiter(char delimiter)
    {
        m_fieldDelimiter = delimiter;
    }

  /**
   * Gets the current trace record tag used in formatting of trace record. The default is 
   * 'iAS_JTS_Trace> '.
   *
   * @return a <code>String</code> value
   */
    public static String getTraceRecordTag()
    {
        return m_traceRecordTag;
     }    

  /**
   * Sets the trace record tag.
   *
   * @param traceRecordTag a <code>String</code> value
   */
    public static void setTraceRecordTag(String traceRecordTag)
    {
        m_traceRecordTag = traceRecordTag;
    }

   /**
    * Returns details about Oracle XAException if available.
    * Returns an default message if it is not Oracle XAException
    *
    * @param exception an <code>XAException</code> to get info from
    * @param logger the <code>Logger</code> to use to report errors extracting the data
    * @return a <code>String</code> value
    */
    public static String getXAExceptionInfo(XAException exception, Logger logger) {
        Class<? extends XAException> aClass = exception.getClass();
        if (aClass.getName().indexOf("OracleXAException") < 0) {
            return exception.getMessage();
        }

        StringBuffer msg = new StringBuffer();
        try {
                String oracleError = "" + invokeMethod(exception, aClass, "getOracleError", logger);
                String oracleSQLError = "" + invokeMethod(exception, aClass, "getOracleSQLError", logger);
                String xaError = "" + invokeMethod(exception, aClass, "getXAError", logger);
                msg.append("\n XAException = ").append(exception.getMessage()).
                        append("\n OracleError = ").append(oracleError).
                        append("\n OracleSQLError = ").append(oracleSQLError).
                        append("\n XAError = ").append(xaError);
        } catch (Exception e) {
            logger.log(Level.WARNING, "getXAExceptionInfo failed with exception:", e);
        }
        return msg.toString();
    }

    /**
     * Invokes specified no-arg method on the specified instance.
     *
     * @param instance the instance to invoke the method on
     * @param clz the <code>Class</code> that the method is defined on
     * @param method the method name
     * @param logger the <code>Logger</code> to use to report errors extracting the data
     * @return result of the method invocation
     */
    private static Object invokeMethod(Object instance, Class clz, String mname, Logger logger) {
        try {
            java.lang.reflect.Method m = clz.getMethod(mname, null);
            return m.invoke(instance, null);
        } catch (Exception e) {
            logger.log(Level.FINE, "", e);
        } 
        return null;
    }
}

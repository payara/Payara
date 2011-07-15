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

package com.sun.jts.trace;

import org.omg.CosTransactions.*;
import javax.transaction.xa.*;
import java.util.*;

/**
 * This class is used to format the trace record. 
 *
 * @author <a href="mailto:kannan.srinivasan@sun.com">Kannan Srinivasan</a>
 * @version 1.0
 */
public class TraceRecordFormatter 
{
  /**
   * Returns the formatted record, by accepting the simple string 
   * message, tid and originator, which can be written to OutputStream
   * @param tid an <code>Object</code> value
   * @param origin an <code>Object</code> value
   * @param message a <code>String</code> value
   * @return a <code>String</code> value
   */
    public static String createTraceRecord(Object tid, Object origin, String message)
    {
        StringBuffer strBuf = new StringBuffer(TraceUtil.getTraceRecordTag());
        strBuf.append(TraceUtil.getCurrentTraceLevel())
              .append(TraceUtil.getFieldDelimiter());
        if(tid == null)
        {
            strBuf.append("<unknown-tid>");
        }
        else
        {
            if(tid instanceof String)
            {
                strBuf.append(tid);
            }
            else if(tid instanceof otid_t)
            {    
                        strBuf.append(convertToString(((otid_t)tid).tid));
            }
        }
        strBuf.append(TraceUtil.getFieldDelimiter())
              .append(System.currentTimeMillis())
              .append(TraceUtil.getFieldDelimiter());
        if(origin == null)
        {
            strBuf.append("<unknown-origin>");
        }
        else
        {
            strBuf.append(origin);
        }
        strBuf.append(TraceUtil.getFieldDelimiter()).append(message).append("\n");
        return strBuf.toString();
    }

  /**
   * Returns the scheme used to format the record. This prints the different components (fields) of
   * the trace and record and their order of occurrence.
   *
   * @return a <code>String</code> value
   */
    public static String getTraceRecordScheme()
    {
        String traceRecordScheme = "<trace-record-tag><level><separator><omg-tid><separator><current-time-in-milliseconds><separator><originator><separator><message>";
        return traceRecordScheme;
    }

  /**
   * Helper method to convert a byte arror to string. This is typically used for printing Xids.
   *
   * @param byteArray a <code>byte[]</code> value
   * @return a <code>String</code> value
   */
    public static String convertToString(byte[] byteArray)
    {
        int i;
        StringBuffer strBuf=new StringBuffer();
        for(i = 0; i < byteArray.length; i++)
         {
            strBuf.append(byteArray[i]);    
        }    
        return strBuf.toString();
    }

  /**
   * Converts an array of xids to string that can be printed. Its a helper method.
   *
   * @param xidArray a <code>Xid[]</code> value
   * @return a <code>String</code> value
   */
    public static String convertXidArrayToString(Xid[] xidArray)
    {
    if(xidArray.length != 0)
    {
            int i;
            StringBuffer strBuf = new StringBuffer("[ ");
            for(i = 0; i < xidArray.length - 1; i++)
            {
                strBuf.append(convertToString(xidArray[i].getGlobalTransactionId()))
                        .append(", ");
            }
            strBuf.append(xidArray[xidArray.length - 1]).append(" ]");
            return strBuf.toString();
    }
    else
        return " null ";
    }

  /**
   * Helper method to convert properties to string.
   *
   * @param prop a <code>Properties</code> value
   * @return a <code>String</code> value
   */
    public static String convertPropsToString(Properties prop)
    {
        if(prop==null){
        return "{null}";
        }
        StringBuffer strBuf =  new StringBuffer("{ ");
        for(Enumeration e = prop.propertyNames(); e.hasMoreElements(); )
        {
            Object obj = e.nextElement();
            strBuf.append("[ ").append(obj).append("->");
            Object val=prop.getProperty((String)obj);
            if(val==null)
                strBuf.append("null");
            else
                strBuf.append((String)val);        
            strBuf.append(" ] ");
        }        
        strBuf.append("}");
        return strBuf.toString();
    } 
}

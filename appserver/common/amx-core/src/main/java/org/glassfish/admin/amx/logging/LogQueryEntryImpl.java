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

import static org.glassfish.admin.amx.logging.LogRecordFields.*;
import org.glassfish.admin.amx.util.ArrayUtil;
import org.glassfish.admin.amx.util.ObjectUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

//import static org.glassfish.admin.amx.logging.LogRecordFields;

/**
    <b>INTERNAL USE ONLY--not part of the API</b>
    
	@since AS 9.0
 */
@Taxonomy(stability = Stability.EXPERIMENTAL)
public final class LogQueryEntryImpl
    implements LogQueryEntry
{
    private transient Map<String,String>    mNameValuePairsMap;
    
    final long      mRecordNumber;
    final Date      mDate;
    final String    mLevel;
    final String    mProductName;
    final String    mMessage;
    final String    mMessageID;
    final String    mModule;
    final String    mNameValuePairs;
    
       public 
    LogQueryEntryImpl(
        final long      recordNumber,
        final Date      date,
        final String    level,
        final String    productName,
        final String    module,
        final String    nameValuePairs,
        final String    messageID,
        final String    message)
    {
        if ( date == null || level == null || message == null ||
             nameValuePairs == null )
        {
            throw new IllegalArgumentException();
        }
        
        mRecordNumber   = recordNumber;
        mDate           = date;
        mLevel          = Level.parse( level ).toString();
        mProductName    = productName;
        mModule         = module;
        mMessage        = message;
        mMessageID      = messageID;
        mNameValuePairs = nameValuePairs;
    }
    
        public 
    LogQueryEntryImpl( final Object[] values )
    {
        if ( values.length != NUM_FIELDS )
        {
            throw new IllegalArgumentException( "wrong number of fields: " + values.length);
        }
   
        mRecordNumber   = (Long)values[ RECORD_NUMBER_INDEX ];
        mDate           = (Date)values[ DATE_INDEX ];
        mLevel          = Level.parse( (String)values[ LEVEL_INDEX ] ).toString();
        mProductName    = (String)values[ PRODUCT_NAME_INDEX ];
        mMessageID      = (String)values[ MESSAGE_ID_INDEX ];
        mModule         = (String)values[ MODULE_INDEX ];
        mMessage        = (String)values[ MESSAGE_INDEX ];
        mNameValuePairs = (String)values[ NAME_VALUE_PAIRS_INDEX ];
    }
    
        public Object[]
    getFields()
    {
        final Object[]  fields  = new Object[ NUM_FIELDS ];
        
        fields[ RECORD_NUMBER_INDEX ]  = mRecordNumber;
        fields[ DATE_INDEX ]           = mDate;
        fields[ LEVEL_INDEX ]          = mLevel;
        fields[ PRODUCT_NAME_INDEX ]   = mProductName;
        fields[ MESSAGE_ID_INDEX ]     = mMessageID;
        fields[ MODULE_INDEX ]         = mModule;
        fields[ MESSAGE_INDEX ]        = mMessage;
        fields[ NAME_VALUE_PAIRS_INDEX ]= mNameValuePairs;
        
        return fields;
	}
	
	/*
        public 
    LogQueryEntryImpl( final CompositeData data )
    {
        this( OpenMBeanUtil.compositeDataToMap( data ) );
    }
        public CompositeType
    getCompositeType()
        throws OpenDataException
    {
        return OpenMBeanUtil.mapToCompositeType( getMapClassName(),
            getMapClassName(), asMap(), null );
    }
    
        public CompositeData
    asCompositeData()
        throws OpenDataException
    {
        return new CompositeDataSupport( getCompositeType(), asMap() );
    }
    
    */


        public long
    getRecordNumber()
    {
        return mRecordNumber;
    }
    
        public Date
    getDate()
    {
        return mDate;
    }
    
        public String
    getModule()
    {
        return mModule;
    }
    
        public String
    getLevel()
    {
        return mLevel;
    }
    
        public String
    getProductName()
    {
        return mProductName;
    }
    
        public String
    getMessage()
    {
        return mMessage;
    }
    
        public String
    getMessageID()
    {
        return mMessageID;
    }
    
        public String
    getNameValuePairs()
    {
        return mNameValuePairs;
    }
    
    /** delimiter between name/value pairs */
    private static final String NVP_PAIRS_DELIM = ";";
    /** delimiter between name and value */
    private static final String PAIR_DELIM = "=";
    
        private Map<String,String>
    parseNameValuePairs()
    {
        final String src    = getNameValuePairs();
        final Map<String,String> m   = new HashMap<String,String>();
        
        final String[]  pairs   = src.split( NVP_PAIRS_DELIM );
        
        for( String pair : pairs )
        {
            final int   idx = pair.indexOf( PAIR_DELIM );
            if ( idx < 0 )
            {
                throw new IllegalArgumentException( src );
            }
            final String    name    = pair.substring( 0, idx ).trim();
            final String    value   = pair.substring( idx + 1, pair.length() ).trim();
            
            m.put( name, value );
        }
        
        return m;
    }
    
        public Map<String,String>
    getNameValuePairsMap()
    {
        if ( mNameValuePairsMap == null )
        {
            mNameValuePairsMap  = parseNameValuePairs();
        }
        
        return mNameValuePairsMap;
    }
    
        public String
    getThreadID()
    {
        return getNameValuePairsMap().get( THREAD_ID_KEY );
    }
    
        public String
    getObjectName()
    {
        return getNameValuePairsMap().get( OBJECTNAME_KEY );
    }
    
        public String
    toString()
    {
        final String D = "|";
        
        //  [#|DATE|LEVEL|PRODUCT_NAME|MODULE|NAME_VALUE_PAIRS|MESSAGE|#]
        return "[#" +
            getRecordNumber() + D +
            getDate() + D +
            getLevel() + D +
            getProductName() + D +
            getModule() + D +
            getNameValuePairs() + D +
            getMessage() + D +
            getMessageID() + D +
            "]";
    }
    
 	    public int
 	hashCode()
 	{
 	    return ObjectUtil.hashCode( mDate, mLevel,
 	        mProductName, mMessage, mMessageID, mModule, mNameValuePairs) ^
 	        ObjectUtil.hashCode( mRecordNumber );
 	}
    
        public boolean
    equals( final Object rhs )
    {
        boolean  equal   = false;
        
        if ( this == rhs )
        {
            equal   = true;
        }
        else if ( rhs instanceof LogQueryEntry )
        {
           final LogQueryEntry e   = (LogQueryEntry)rhs;
           
           equal    = ArrayUtil.arraysEqual( getFields(), e.getFields() );
        }

        return equal;
    }
}







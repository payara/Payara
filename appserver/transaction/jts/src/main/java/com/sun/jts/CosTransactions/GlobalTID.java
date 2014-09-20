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

//----------------------------------------------------------------------------
//
// Module:      GlobalTID.java
//
// Description: Transaction global identifier object implementation.
//
// Product:     com.sun.jts.CosTransactions
//
// Author:      Simon Holdsworth
//
// Date:        March, 1997
//
// Copyright (c):   1995-1997 IBM Corp.
//
//   The source code for this program is not published or otherwise divested
//   of its trade secrets, irrespective of what has been deposited with the
//   U.S. Copyright Office.
//
//   This software contains confidential and proprietary information of
//   IBM Corp.
//----------------------------------------------------------------------------

package com.sun.jts.CosTransactions;

// Import required classes.

import java.io.*;

import org.omg.CosTransactions.*;
import com.sun.jts.trace.*;


import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

/**This class provides a wrapper for the otid_t class in the
 * org.omg.CosTSInteroperation package to allow us to add operations.
 *
 * @version 0.01
 *
 * @author Simon Holdsworth, IBM Corporation
 *
 * @see
*/
//----------------------------------------------------------------------------
// CHANGE HISTORY
//
// Version By     Change Description
//   0.01  SAJH   Initial implementation.
//-----------------------------------------------------------------------------
public class GlobalTID extends Object {
    otid_t realTID = null;

    private String stringForm = null;
    private int hashCode = 0;
    private boolean hashed = false;

	/*
		Logger to log transaction messages
	*/
    static Logger _logger = LogDomains.getLogger(GlobalTID.class, LogDomains.TRANSACTION_LOGGER);

    /*
     * Keep this line at the end of all variable initialization as
     * The class's static initializer should not create an instance of the class 
     * before all of the static final fields are assigned.
     */
    static GlobalTID NULL_GLOBAL_TID = new GlobalTID(-1,-1,null);
    
    /**Creates a new global identifier which is a copy of the parameter.
     *
     * @param otherTID  The other global identifier.
     *
     * @return
     *
     * @see
     */
    public GlobalTID( otid_t otherTID ) {
        realTID = otherTID;
    }

    /**Creates a new global identifier from the given values.
     *
     * @param formatID      The format identifier.
     * @param bqual_length  The branch qualifier.
     * @param data          The identifier data.
     *
     * @return
     *
     * @see
     */
    GlobalTID( int    formatID,
               int    bqual_length,
               byte[] tid ) {
        realTID = new otid_t(formatID, bqual_length, tid);
    }

    /**
     * Creates a new global identifier object.
     *
     * @param xid The Xid object containing transaction id.
     */
    public GlobalTID(javax.transaction.xa.Xid xid) {
        
        int glen = xid.getGlobalTransactionId().length;
        int blen = xid.getBranchQualifier().length;
        byte[] xidRep = new byte[glen + blen];
        
        System.arraycopy(xid.getGlobalTransactionId(), 0, xidRep, 0, glen);
        System.arraycopy(xid.getBranchQualifier(), 0, xidRep, glen, blen);
        
        realTID = new otid_t(xid.getFormatId(), blen, xidRep);
    }  

    /**Creates a GlobalTID from the given stream.
     *
     * @param dataIn  The DataInputStream for the operation.
     *
     * @return
     *
     * @see
     */
    GlobalTID( DataInputStream dataIn ) {
        try {
            int formatID     = dataIn.readInt();
            int bqualLength  = dataIn.readInt();
            int bufferlen    = dataIn.readUnsignedShort();
            byte[] tid = new byte[bufferlen];
            dataIn.read(tid);

            realTID = new otid_t(formatID,bqualLength,tid);
        } catch( Throwable exc ) {}
    }

    /**Creates a global identifier from a byte array.
     *
     * @param  The array of bytes.
     *
     * @return
     *
     * @see
     */
    GlobalTID( byte[] bytes ) {
        int formatID =  (bytes[0]&255) +
            ((bytes[1]&255) << 8) +
            ((bytes[2]&255) << 16) +
            ((bytes[3]&255) << 24);
        int bqualLength =  (bytes[4]&255) +
            ((bytes[5]&255) << 8) +
            ((bytes[6]&255) << 16) +
            ((bytes[7]&255) << 24);
        byte[] tid = new byte[bytes.length-8];
        System.arraycopy(bytes,8,tid,0,tid.length);

        realTID = new otid_t(formatID,bqualLength,tid);
    }

    /**Creates a new global identifier which is a copy of the target object.
     *
     * @param
     *
     * @return  The copy.
     *
     * @see
     */
    final GlobalTID copy() {
        GlobalTID result = new GlobalTID(realTID);
        result.hashed = hashed;
        result.hashCode = hashCode;
        result.stringForm = stringForm;

        return result;
    }

    /**Determines whether the global identifier represents the null transaction
     * identifier.
     *
     * @param
     *
     * @return  Indicates whether the global identifier is null.
     *
     * @see
     */
    final boolean isNullTID() {
        return realTID.formatID == -1;
    }

    /**Compares the two global identifiers. Delegates to #isSameTIDInternal
     * for the actual implementation to avoid comparison with otid_t
     * that FindBugs doesn't like.
     *
     * @param other  The other global identifier to compare.
     *
     * @return  Indicates the two global identifiers are equal.
     *
     * @see
     */
    public final boolean equals( Object other ) {
        return isSameTIDInternal(other);
    }

    private final boolean isSameTIDInternal(Object other) {
        otid_t otherTID = null;

        if( other == null )
            return false;
        else if( other instanceof otid_t )
            otherTID = (otid_t)other;
        else if( other instanceof GlobalTID )
            otherTID = ((GlobalTID)other).realTID;
        else
            return false;

        return isSameTID(otherTID);
    }

    public final boolean isSameTID( otid_t otherTID ) {

        if (otherTID == null)
            return false;
        
        boolean result = false;

        // If the references are equal, return immediately.

        if( realTID == otherTID ) return true;

        // If the formats are different, then the identifiers cannot be the same.

        if( realTID.formatID != otherTID.formatID ) return false;

        // Determine the GTRID length for each transaction identifier.

        int firstGTRID = realTID.tid.length - realTID.bqual_length;
        int secondGTRID = otherTID.tid.length - otherTID.bqual_length;

        // If the GTRID lengths are different, the identifiers are different.

        if( firstGTRID != secondGTRID )
            return false;

        // Compare the global part of the identifier.

        result = true;
        for( int pos = 0; pos < firstGTRID && result; pos++ )
            result = (realTID.tid[pos] == otherTID.tid[pos] );

        return result;
    }

    /**Returns a hash value for the global identifier.
     *
     * @param
     *
     * @return  The hash value.
     *
     * @see
     */

    public final int hashCode() {

        // If the hash code has already been calculated, then return the value.

        if( hashed )
            return hashCode;

        hashCode = 0;

        // Add up the values in the XID.

        if( realTID.tid != null )
            for( int pos = 0; pos < realTID.tid.length; pos++ )
                hashCode += realTID.tid[pos];

        // Add in the formatId and branch qualifier length.

        hashCode += realTID.formatID + realTID.bqual_length;

        // Multiply the result by the "magic hashing constant".

        hashCode *= 0x71824361;

        hashed = true;

        return hashCode;
    }

    public GlobalTID(String stid){
		//invalid data
        if(stid==null){
             return ;
		}

		//there was no proper formatId
		if(stid.equals("[NULL ID]")){
                        realTID = new otid_t(-1, -1, null);
			//realTID.formatID=-1;
			return;
		}
		if(_logger.isLoggable(Level.FINEST))
			_logger.logp(Level.FINEST,"GlobalTID","GlobalTID(String)",
					"Tid is: "+stid);

		//main part starts here
   		char [] ctid =stid.toCharArray();

		int colon=stid.indexOf(":");

		//bqualLen and globalLen are not real lengths but twice of them
		int globalLen=0;
		int bqualLen=0;
		if(colon==-1){
			//there was no bqual_length in the tid
			globalLen=ctid.length-2;
		}
		else{
			globalLen=colon-1;
			bqualLen=ctid.length -3 - globalLen;
		}

		if( (globalLen%2!=0) || (bqualLen%2 !=0)){
			if(_logger.isLoggable(Level.FINEST)){
				_logger.logp(Level.FINEST,"GlobalTID", "GlobalTID(String)",
						"Corrupted gtid string , total length is not integral");
        	}
			throw new RuntimeException("invalid global tid");
		}


		byte [] b=new byte[(globalLen+bqualLen)/2];
		int index=1;
		int bIndex=0;

		//while b gets filled
		while(bIndex<b.length){

			int t=ctid[index++];
			int t1=ctid[index++];
			if(_logger.isLoggable(Level.FINEST))
				_logger.logp(Level.FINEST,"GlobalTID", "GlobalTID(String)",
					 	"Index is : "+bIndex+" value of t,t1 is : "+t+","+t1);	
			if( t >= 'A'){
				t = t - 'A'+10;
			}
			else{
				t=t-'0';
			}
			if( t1 >= 'A'){
				t1 = t1 - 'A'+10;
			}
			else{
				t1=t1-'0';
			}
			if(_logger.isLoggable(Level.FINEST))
				_logger.logp(Level.FINEST,"GlobalTID", "GlobalTID(String)",
						" Value of t,t1 is : "+t+","+t1);
			t=t<<4;
			if(_logger.isLoggable(Level.FINEST))
				_logger.logp(Level.FINEST,"GlobalTID", "GlobalTID(String)",
						"Value of t is : "+t);
			t=t|t1;
			
			if(_logger.isLoggable(Level.FINEST))
				_logger.logp(Level.FINEST,"GlobalTID", "GlobalTID(String)",
						" Value of t is :  "+t);
			b[bIndex++] = (byte)t;
			if(_logger.isLoggable(Level.FINEST))
				_logger.logp(Level.FINEST,"GlobalTID", "GlobalTID(String)",
						"Value of t is : "+(byte)t);
		}

        realTID = new otid_t(TransactionState.XID_FORMAT_ID,bqualLen/2,b);
		if(_logger.isLoggable(Level.FINEST))
			_logger.logp(Level.FINEST,"GlobalTID", "GlobalTID(String)",
					"created gtid : "+this);
    }


    /**Converts the global identifier to a string.
     *
     * @param
     *
     * @return  The string representation of the identifier.
     *
     * @see
     */

    public final String toString() {

        // Return a string for the null transaction id.

        if( realTID.formatID == -1 )
            return "[NULL ID]"/*#Frozen*/;

        // If we have a cached copy of the string form of the global identifier, return
        // it now.

        if( stringForm != null ) return stringForm;

        // Otherwise format the global identifier.

        //char[] buff = new char[realTID.tid.length*2 + 2 + (realTID.bqual_length>0?1:0)];
        char[] buff = new char[realTID.tid.length*2 + (realTID.bqual_length>0?1:0)];
        int pos = 0;
        //buff[pos++] = '[';

        // Convert the global transaction identifier into a string of hex digits.

        int globalLen = realTID.tid.length - realTID.bqual_length;
        for( int i = 0; i < globalLen; i++ ) {
            int currCharHigh = (realTID.tid[i]&0xf0) >> 4;
            int currCharLow  = realTID.tid[i]&0x0f;
            buff[pos++] = (char)(currCharHigh + (currCharHigh > 9 ? 'A'-10 : '0'));
            buff[pos++] = (char)(currCharLow  + (currCharLow  > 9 ? 'A'-10 : '0'));
        }

        if( realTID.bqual_length > 0 ) {
            //buff[pos++] = ':';
            buff[pos++] = '_';
            for( int i = 0; i < realTID.bqual_length; i++ ) {
                int currCharHigh = (realTID.tid[i+globalLen]&0xf0) >> 4;
                int currCharLow  = realTID.tid[i+globalLen]&0x0f;
                buff[pos++] = (char)(currCharHigh + (currCharHigh > 9 ? 'A'-10 : '0'));
                buff[pos++] = (char)(currCharLow  + (currCharLow  > 9 ? 'A'-10 : '0'));
            }
        }
        //buff[pos] = ']';

        // Cache the string form of the global identifier.

        stringForm = new String(buff);

        return stringForm;
    }

    /**Converts the global identifier to a byte array.
     *
     * @param
     *
     * @return  The byte array representation of the identifier.
     *
     * @see
     */
    final byte[] toBytes() {
        if( realTID.formatID == -1 )
            return null;

        byte[] result = new byte[realTID.tid.length + 8];

        result[0] = (byte) realTID.formatID;
        result[1] = (byte)(realTID.formatID >> 8);
        result[2] = (byte)(realTID.formatID >> 16);
        result[3] = (byte)(realTID.formatID >> 24);
        result[4] = (byte) realTID.bqual_length;
        result[5] = (byte)(realTID.bqual_length >> 8);
        result[6] = (byte)(realTID.bqual_length >> 16);
        result[7] = (byte)(realTID.bqual_length >> 24);

        System.arraycopy(realTID.tid,0,result,8,realTID.tid.length);

        return result;
    }

    final byte[] toTidBytes() {
        return realTID.tid;
    }

    static GlobalTID fromTIDBytes(byte[] bytes) {
        return new GlobalTID(TransactionState.XID_FORMAT_ID, 0, bytes);
    }


    /**Writes the contents of the global identifier to the given stream.
     *
     * @param dataOut  The DataOutputStream for the operation.
     *
     * @return
     *
     * @see
     */
    final void write( DataOutputStream dataOut ) {
        try {
            dataOut.writeInt(realTID.formatID);
            dataOut.writeInt(realTID.bqual_length);
            dataOut.writeShort(realTID.tid.length);
            dataOut.write(realTID.tid,0,realTID.tid.length);
        } catch( Throwable exc ) {}
    }
}

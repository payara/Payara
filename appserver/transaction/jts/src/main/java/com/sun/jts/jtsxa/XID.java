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

//----------------------------------------------------------------------------
//
// Module:      XID.java
//
// Description: An implementation of the X/Open transaction idenifier (Xid).
//
// Product:     com.sun.jts.jtsxa
//
// Author:      Malcolm Ayres
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

package com.sun.jts.jtsxa;

import org.omg.CosTransactions.*;
import javax.transaction.xa.Xid;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;

/**
 * The XID class provides an implementation of the X/Open
 * transaction identifier it implements the javax.transaction.xa.Xid interface.
 */
public class XID implements Xid {

    //-----------------------------------------------------------------------//
    // Data Area                                                             //
    //-----------------------------------------------------------------------//

    /**
     * The format identifier for the XID. A value of -1 indicates
     * that the NULLXID.
     */
    private int formatID;   // Format identifier
                            // (-1) means that the XID is null
    /**
     * The number of bytes in the global transaction identfier
     */
    private int gtrid_length;  // Value from 1 through 64

    /**
     * The number of bytes in the branch qualifier
     */
    private int bqual_length;  // Value from 1 through 64

    /**
     * The data for the XID.
     * <p> The XID is made up of two contiguous parts. The first (of size
     * <b>gtrid_length</b>) is the global transaction identfier and the second
     * (of size <b>bqual_length</b>) is the branch qualifier.
     * <p>If the <b>formatID</b> is -1, indicating the NULLXID,
     * the data is ignored.
     */
    private byte data[];       // The XID data (size XIDDATASIZE)

    // ADDITION
    private byte cachedBqual[] = null;
    private byte cachedGtrid[] = null;


    //-----------------------------------------------------------------------//
    // Constants                                                             //
    //-----------------------------------------------------------------------//

    /**
     * The size of <b>data</b>.
     */
    static private final int XIDDATASIZE= 128; // Size in bytes

    /**
     * The maximum size of the global transaction identifier.
     */
    static public  final int MAXGTRIDSIZE= 64; // Maximum size (in bytes) of gtrid

    /**
     * The maximum size of the branch qualifier.
     */
    static public  final int MAXBQUALSIZE= 64; // Maximum size (in bytes) of bqual

    static private final String hextab= "0123456789ABCDEF"/*#Frozen*/;

    static Logger _logger = LogDomains.getLogger(XID.class, LogDomains.TRANSACTION_LOGGER);

    //-----------------------------------------------------------------------//
    // XID::Constructor                                                      //
    //-----------------------------------------------------------------------//

    /**
     * Constructs a new null XID.
     * <p>After construction the data within the XID should be initialized.
     */
    public XID() {
        data= new byte[XIDDATASIZE];
        formatID = -1;
    }

    //-----------------------------------------------------------------------//
    // XID::Methods                                                          //
    //-----------------------------------------------------------------------//

    /**
     * Initialize an XID using another XID as the source of data.
     *
     * @param from the XID to initialize this XID from
     *
     */
    public void copy(XID from) {
        int i;

        formatID = -1;                    // Default, null transaction
        if (from == null)                // If source is physically null
        {
            return;                        // Return the NULL transaction
        }

        if (from.formatID == (-1))       // If source is a NULL transaction
        {
            return;                        // Return the NULL transaction
        }

        gtrid_length= from.gtrid_length;
        bqual_length= from.bqual_length;

        if (data != null && from.data != null) {
            System.arraycopy(from.data, 0, data, 0, XIDDATASIZE);
        }

        formatID= from.formatID;         // Last, in case of failure
    }

    /*
     * Copy the XID from an otid_t format XID.
     */

    /**
     * Initialize an XID using an omg otid_t as the source of data.
     *
     * @param from the OMG otid_t to initialize this XID from
     *
     * @see org.omg.CosTransactions.otid_t
     */
    public void copy(otid_t from) {
        int               i;
        int               L;

        formatID= -1;                    // Default, null transaction
        if (from == null)                // If source is physically null
        {
            return;                        // Return the NULL transaction
        }

        if (from.formatID == (-1))       // If source is a NULL transaction
        {
            return;                        // Return the NULL transaction
        }

        L= from.tid.length;
        gtrid_length= L - from.bqual_length;
        bqual_length= from.bqual_length;

        if (data != null) {
            System.arraycopy(from.tid, 0, data, 0, L);
        }

        formatID= from.formatID;         // Last, in case of failure
    }

    /*
     * Are the XIDs equal?
     */

    /**
     * Determine whether or not two objects of this type are equal.
     *
     * @param o the object to be compared with this XID.
     *
     * @return Returns true of the supplied object represents the same
     *                 global transaction as this, otherwise returns false.
     */
    public boolean equals(Object o) {
        XID               other;   // The "other" XID
        int               L;       // Combined gtrid_length + bqual_length
        int               i;

        if (!(o instanceof XID))        // If the other XID isn't an XID
        {
            return false;                  // It can't be equal
        }

        other = (XID)o;                   // The other XID, now properly cast

        if (formatID == (-1) && other.formatID == (-1))
        {
            return true;
        }

        if (formatID != other.formatID
                ||gtrid_length != other.gtrid_length
                ||bqual_length != other.bqual_length) {
            return false;
        }

        L = gtrid_length + bqual_length;

        for (i = 0; i < L; i++) {
            if (data[i] != other.data[i]) {
                return false;
            }
        }

        return true;
    }

    /*
     * Compute the hash code.
     */

    /**
     * Compute the hash code.
     *
     * @return the computed hashcode
     */
    public int hashCode() {
        if (formatID == (-1)) {
            return (-1);
        }

        return formatID + gtrid_length - bqual_length;

    }

    /*
     * Convert to String
     *
     * <p> This is normally used to display the XID when debugging.
     */

    /**
     * Return a string representing this XID.
     *
     * @return the string representation of this XID
     */
    public String toString() {
        /* toString() method is slightly expensive and this needs to be done because 
	 * some of the drivers XAResource methods have the "trace("some thing " + xid)" 
         * kind of code which is executing this method resulting in performance degradation.
         */
        if (_logger.isLoggable(Level.FINE)) {
            StringBuffer      d;             // Data String, in Hexidecimal
            String            s;             // Resultant String

            int               i;
            int               v;
            int               L;

            L= gtrid_length + bqual_length;
            d= new StringBuffer(L + L);

            // Convert data string to hex
            for (i = 0; i < L; i++) {
                v = data[i] & 0xff;
                d.append(hextab.charAt(v/16));
                d.append(hextab.charAt(v&15));
                if ((i+1) % 4 == 0 && (i+1) < L) {
                    d.append(" ");
                }
            }

            s = "{XID: " +
                         "formatID("     + formatID     + "), " +
                         "gtrid_length(" + gtrid_length + "), " +
                         "bqual_length(" + bqual_length + "), " +
                         "data("         + d            + ")" +
                         "}"/*#Frozen*/;

            return s;
        }
        else
           return "(Available at FINE log level)"; /*#Frozen*/
    }

    /*
     * Return branch qualifier
     */

    /**
     * Returns the branch qualifier for this XID.
     *
     * @return the branch qualifier
     */
    public byte[] getBranchQualifier() {
        if (cachedBqual != null) {
            return cachedBqual;
        }
        byte[] bqual = new byte[bqual_length];
        System.arraycopy(data,gtrid_length,bqual,0,bqual_length);
        return bqual;
    }

    /*
     * Set branch qualifier.
     *
     * Note that the branch qualifier has a maximum size.
     */

    /**
     * Set the branch qualifier for this XID.
     *
     * @param qual a Byte array containing the branch qualifier to be set. If
     *              the size of the array exceeds MAXBQUALSIZE, only the first
     *              MAXBQUALSIZE elements of qual will be used.
     */
    public void setBranchQualifier(byte[] qual) {
        bqual_length = qual.length > MAXBQUALSIZE ? MAXBQUALSIZE : qual.length;
        System.arraycopy(qual, 0, data, gtrid_length, bqual_length);
        cachedBqual = qual;
    }

    /**
     * Obtain the format identifier part of the XID.
     *
     * @return Format identifier. -1 indicates a null XID
     */
    public int getFormatID() {
        return formatID;
    }

    /**
     * Set the format identifier part of the XID.
     *
     * @param Format identifier. -1 indicates a null Xid.
     */
    public void setFormatID(int formatID) {
        this.formatID = formatID;
        return;
    }

    /*
     * Determine if an array of bytes equals the branch qualifier
     */

    /**
     * Compares the input parameter with the branch qualifier for equality.
     *
     * @return true if equal
     */
    public boolean isEqualBranchQualifier(byte[] data) {

        int L = data.length > MAXBQUALSIZE?MAXBQUALSIZE:data.length;
        int i;

        if (L != bqual_length) {
            return false;
        }

        for (i = 0; i < L; i++) {
            if (data[i] != this.data[gtrid_length + i]) {
                return false;
            }
        }

        return true;
    }

    // added by TN

    /**
     * Return whether the Gtrid of this is equal to the Gtrid of xid
     */
    public boolean isEqualGtrid(XID xid) {
        if (this.gtrid_length != xid.gtrid_length) {
            return false;
        }

        for (int i=0; i<gtrid_length; i++) {
            if (this.data[i] != xid.data[i]) {
                return false;
            }
        }

        return true;
    }

    /*
     * Return global transaction identifier
     */

    /**
     * Returns the global transaction identifier for this XID.
     *
     * @return the global transaction identifier
     */
    public byte[] getGlobalTransactionIdentifier() {
       if (cachedGtrid != null) {
           return cachedGtrid;
       }
        byte[] gtrid = new byte[gtrid_length];
        System.arraycopy(data, 0, gtrid, 0, gtrid_length);
        cachedGtrid = gtrid;
        return gtrid;
    }

    // Addition by Tony Ng to make this class implements
    // javax.transaction.xa.Xid

    public int getFormatId() {
        return getFormatID();
    }

    public byte[] getGlobalTransactionId() {
        return getGlobalTransactionIdentifier();
    }
}

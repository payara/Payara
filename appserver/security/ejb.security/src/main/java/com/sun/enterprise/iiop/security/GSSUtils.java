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

package com.sun.enterprise.iiop.security;

import java.io.IOException;
import java.io.InputStream;
import sun.security.util.ObjectIdentifier;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;

import com.sun.corba.ee.org.omg.GSSUP.GSSUPMechOID;
import com.sun.corba.ee.org.omg.CSI.GSS_NT_Export_Name_OID;
import com.sun.corba.ee.org.omg.CSI.GSS_NT_Scoped_Username_OID;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.logging.*;
import com.sun.logging.*;

/*
 * @author    Sekhar Vajjhala 
 * (Almost complete rewrite of an old version)
 *         
 */

public class GSSUtils 
{
    private static final java.util.logging.Logger _logger =
       LogDomains.getLogger(GSSUtils.class, LogDomains.CORBA_LOGGER);

    public static final ObjectIdentifier GSSUP_MECH_OID;

    public static final ObjectIdentifier GSS_NT_EXPORT_NAME_OID;

    /* GSS_NT_SCOPED_USERNAME_OID is currently not used by this class. It is
     * defined here for the sake of completeness.
     */
    public static final ObjectIdentifier GSS_NT_SCOPED_USERNAME_OID;
    
    private static byte[] mech;

    static {

        int i ; // index
        ObjectIdentifier x = null;
        
        /* Construct an ObjectIdentifer by extracting each OID */
      
        try {
            i = GSSUPMechOID.value.indexOf(':');
            x = new ObjectIdentifier( GSSUPMechOID.value.substring(i+1));
	} catch(IOException e) {
            x = null;
            _logger.log(Level.SEVERE,"iiop.IOexception",e);
	}
        GSSUP_MECH_OID = x;
         
        try {
            i = GSS_NT_Export_Name_OID.value.indexOf(':');
            x = new ObjectIdentifier( GSS_NT_Export_Name_OID.value.substring(i+1));
	} catch(IOException e) {
            x = null;
                _logger.log(Level.SEVERE,"iiop.IOexception",e);
	}
        GSS_NT_EXPORT_NAME_OID = x;
        
        try {
            i = GSS_NT_Scoped_Username_OID.value.indexOf(':');
            x = new ObjectIdentifier( GSS_NT_Scoped_Username_OID.value.substring(i+1));
	} catch(IOException e) {
            x = null;
                _logger.log(Level.SEVERE,"iiop.IOexception",e);
	}
        GSS_NT_SCOPED_USERNAME_OID = x;

        try {
	    if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"GSSUP_MECH_OID: " + dumpHex(getDER(GSSUP_MECH_OID)));
		_logger.log(Level.FINE,"GSS_NT_EXPORT_NAME_OID: " + dumpHex(getDER(GSS_NT_EXPORT_NAME_OID)));
		_logger.log(Level.FINE,"GSS_NT_SCOPED_USERNAME_OID: " + dumpHex(getDER(GSS_NT_SCOPED_USERNAME_OID)));
	    }
	} catch(IOException e) {
                _logger.log(Level.SEVERE,"iiop.IOexception",e);
	}
        
        try {
	    mech = GSSUtils.getDER(GSSUtils.GSSUP_MECH_OID);
	} catch(IOException io) {
	    mech = null;
	}
    }

    // Dumps the hex values in the given byte array
    public static String dumpHex( byte[] octets ) {
	StringBuffer result = new StringBuffer( "" );
	for( int i = 0; i < octets.length; i++ ) {
	    if( (i != 0) && ((i % 16) == 0) ) result.append( "\n    " );
	    int b = octets[i];
	    if( b < 0 ) b = 256 + b;
	    String hex = Integer.toHexString( b );
	    if( hex.length() == 1 ) {
		hex = "0" + hex;
	    }
	    result.append( hex + " " );
	}

	return result.toString();
    }

    /* Import the exported name from the mechanism independent 
     * exported name.
     */
     
    public static byte[] importName(ObjectIdentifier oid, byte[] externalName)
	throws IOException
    {
	    if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"Attempting to import mechanism independent name");
		_logger.log(Level.FINE,dumpHex(externalName));
	}

	IOException e = new IOException("Invalid Name");

	if (externalName[0] != 0x04)
	    throw e;

        if (externalName[1] != 0x01)
	    throw e;

	int mechoidlen = (((int)externalName[2]) << 8)+ (externalName[3] & 0xff);

        if(_logger.isLoggable(Level.FINE)) 
		_logger.log(Level.FINE,"Mech OID length = " + mechoidlen);
	if (externalName.length < (4 + mechoidlen + 4))
	    throw e;

        /* get the mechanism OID and verify it is the same as oid
         * passed as an argument.
         */

        byte[] deroid = new byte[mechoidlen];
	System.arraycopy(externalName, 4, deroid, 0, mechoidlen);
        ObjectIdentifier oid1 = getOID(deroid);
        if  ( ! oid1.equals(oid) )
            throw e;

        int pos = 4 + mechoidlen;

	int namelen =   (((int) externalName[pos])   << 24) 
                      + (((int) externalName[pos+1]) << 16) 
                      + (((int) externalName[pos+2]) << 8)
                      + (((int) externalName[pos+3]));

        pos += 4; // start of the mechanism specific exported name

        if (externalName.length != (4 + mechoidlen + 4 + namelen))
	    throw e;

        byte[] name = new byte[externalName.length - pos];
        System.arraycopy(externalName, pos, name, 0, externalName.length - pos);
        if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"Mechanism specific name:");
		_logger.log(Level.FINE,dumpHex(name));
		_logger.log(Level.FINE,"Successfully imported mechanism independent name");
	}
	return name;
    }

    /* verify if exportedName is of object ObjectIdentifier.*/
     
    public static boolean verifyMechOID(ObjectIdentifier oid, byte[] externalName)
	throws IOException
    {
        if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"Attempting to verify mechanism independent name");
		_logger.log(Level.FINE,dumpHex(externalName));
	}

	IOException e = new IOException("Invalid Name");

	if (externalName[0] != 0x04)
	    throw e;

        if (externalName[1] != 0x01)
	    throw e;

	int mechoidlen = (((int)externalName[2]) << 8)+ (externalName[3] & 0xff);

	    if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"Mech OID length = " + mechoidlen);
            }
	if (externalName.length < (4 + mechoidlen + 4))
	    throw e;

        /* get the mechanism OID and verify it is the same as oid
         * passed as an argument.
         */

        byte[] deroid = new byte[mechoidlen];
	System.arraycopy(externalName, 4, deroid, 0, mechoidlen);
        ObjectIdentifier oid1 = getOID(deroid);
        if  ( ! oid1.equals(oid) )
            return false;
        else
            return true;
    }


    /*
     *  Generate an exported name as specified in [RFC 2743]
     *  section 3.2, "Mechanism-Independent Exported Name Object Format". 
     *  For convenience, the format of the exported name is reproduced here
     *  from [RFC2743] :
     * 
     *  Format:
     *  Bytes
     *  2          0x04 0x01
     *  2          mech OID length (len)
     *  len        mech OID's DER value
     *  4          exported name len
     *  name len   exported name
     *
     */

    public static byte[] createExportedName(ObjectIdentifier oid, byte[] extName)
	throws IOException
    {
	byte[] oidDER = getDER(oid);
	int tokensize =  2 + 2 + oidDER.length + 4 + extName.length;

	byte[] token = new byte[tokensize];

        // construct the Exported Name 
        int pos = 0;

        token[0] = 0x04;
        token[1] = 0x01;
        token[2] = (byte)(oidDER.length & 0xFF00);
        token[3] = (byte)(oidDER.length & 0x00FF);

        pos = 4;
        System.arraycopy(oidDER, 0, token, pos, oidDER.length);
        pos += oidDER.length;

        int namelen = extName.length;

        token[pos++] = (byte)(namelen & 0xFF000000);
        token[pos++] = (byte)(namelen & 0x00FF0000);
        token[pos++] = (byte)(namelen & 0x0000FF00);
        token[pos++] = (byte)(namelen & 0x000000FF);

        System.arraycopy(extName, 0, token, pos, namelen);

        return token;
    }

    /*
     * Return the DER representation of an ObjectIdentifier.
     * The DER representation is as follows:
     *   
     *    0x06            --  Tag for OBJECT IDENTIFIER
     *    derOID.length   --  length in octets of OID
     *    DER value of OID -- written as specified byte the DER representation
     *                        for an ObjectIdentifier.
     */

    public static byte[] getDER(ObjectIdentifier id) 
	throws IOException
    {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,"Returning OID in DER format");
            _logger.log(Level.FINE,"    OID = " + id.toString());
        }
	DerOutputStream dos = new DerOutputStream();
	dos.putOID(id);
	byte[] oid = dos.toByteArray();
        if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"    DER OID: " + dumpHex(oid));
        }
	return oid;
    }

    /*
     * Return the OID corresponding to an OID represented in DER format
     * as follows:
     *
     *    0x06            --  Tag for OBJECT IDENTIFIER
     *    derOID.length   --  length in octets of OID
     *    DER value of OID -- written as specified byte the DER representation
     *                        for an ObjectIdentifier.
     */

     public static ObjectIdentifier getOID(byte[] derOID)
         throws IOException
     {
         DerInputStream dis = new DerInputStream(derOID);
         ObjectIdentifier oid = dis.getOID();

         /* Note: getOID() method call generates an IOException
          *       if derOID contains any malformed data
          */
         return oid;
     }


    /*
     * Construct a mechanism level independent token as specified in section
     * 3.1, [RFC 2743]. This consists of a token tag followed byte a mechanism
     * specific token. The format - here for convenience - is as follows:
     *
     *  Token Tag                      Description
     *
     *     0x60                       | Tag for [APPLICATION 0] SEQUENCE
     *     <token-length-octets>      |
     *     0x06                       | Along with the next two entries
     *     <object-identifier-length> | is a DER encoding of an object 
     *     <object-identifier-octets> | identifier
     * 
     *  Mechanism specific token      | format defined by the mechanism itself
     *                                  outside of RFC 2743.
     */

    public static byte[] createMechIndToken(ObjectIdentifier mechoid, byte mechtok[])
	throws IOException 
    {
	byte [] deroid = getDER(mechoid);

        byte [] token = new byte[  1  // for 0x60
                                 + getDERLengthSize(deroid.length+mechtok.length)
				 + deroid.length
                                 + mechtok.length];
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"Going to create a mechanism independent token");
        }
	int index=0;

	token[index++] = 0x60;

	index = writeDERLength(token, index, deroid.length + mechtok.length);

        System.arraycopy(deroid, 0, token, index, deroid.length);

	index += deroid.length;
	System.arraycopy(mechtok, 0, token, index, mechtok.length);

        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"Mechanism independent token created: ");
            _logger.log(Level.FINE,dumpHex(token));
        }
	return token;
    }

    /*
     * Retrieve a mechanism specific token from a mechanism independent token.
     * The format of a mechanism independent token is specified in section
     * 3.1, [RFC 2743].
     */

    public static byte[] getMechToken(ObjectIdentifier oid, byte[] token) {

	byte[] mechtoken = null;
	    if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"Received mechanism independent token: ");
		_logger.log(Level.FINE,dumpHex(token));
	}

	try {
	    int index      = verifyTokenHeader(oid, token);
	    int mechtoklen = token.length - index;
	    mechtoken      = new byte[mechtoklen];
            System.arraycopy(token, index, mechtoken, 0, mechtoklen);
	    if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"Mechanism specific token : ");
		_logger.log(Level.FINE,dumpHex(mechtoken));
	    }
	} catch(IOException e) {
            _logger.log(Level.SEVERE,"iiop.IOexception",e);
	}
	return mechtoken;
    }


   /* Verfies the header of a mechanism independent token. The header must
    * be as specified in RFC 2743, section 3.1. The header must contain
    * an object identifier specified by the first parameter.
    *
    * If the header is well formed, then the starting position of the
    * mechanism specific token within the token is returned.
    * 
    * If the header is mal formed, then an exception is thrown.
    */

    private static int verifyTokenHeader(ObjectIdentifier oid, byte [] token)
	throws IOException
    {
	int index=0;
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"Attempting to verify tokenheader in the mechanism independent token.");
        }
        // verify header
	if (token[index++] != 0x60) 
	    throw new IOException("Defective Token");

        int toklen = readDERLength(token, index); // derOID length + token length

        if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"Mech OID length + Mech specific length = " + toklen);
        }
        index += getDERLengthSize(toklen);
        if(_logger.isLoggable(Level.FINE)){
		_logger.log(Level.FINE,"Mechanism OID index : " + index);
        }

        if (token[index] != 0x06) 
	    throw new IOException("Defective Token");

        byte[] buf = new byte[ token.length - index ];

	System.arraycopy(token, index, buf, 0, token.length - index);

        ObjectIdentifier mechoid = getOID(buf);

        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,"Comparing mech OID in token with the expected mech OID");
            _logger.log(Level.FINE,"mech OID: " + dumpHex(getDER(mechoid)));
            _logger.log(Level.FINE,"expected mech OID: " + dumpHex(getDER(oid)));
	}

        if ( ! mechoid.equals(oid)) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,"mech OID in token does not match expected mech OID");
            }
            throw new IOException("Defective token");
        }
        int mechoidlen = getDER(oid).length;

        if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"Mechanism specific token index : " + index + mechoidlen);
		_logger.log(Level.FINE,"Successfully verified header in the mechanism independent token.");
	}
        return (index + mechoidlen); // starting position of mech specific token
    }

    static int getDERLengthSize(int length)
    {
	if (length < (1<<7))
	    return (1);
	else if (length < (1<<8))
	    return (2);
	else if (length < (1<<16))
	    return (3);
	else if (length < (1<<24))
	    return (4);
	else
	    return (5);
    }

    static int writeDERLength(byte [] token, int index, int length)
    {
	if (length < (1<<7)) {
	    token[index++] = (byte)length;
	} else {
	    token[index++] = (byte)(getDERLengthSize(length)+127);
	    if (length >= (1<<24))
		token[index++] = (byte)(length>>24);
	    if (length >= (1<<16))
		token[index++] = (byte)((length>>16)&0xff);
	    if (length >= (1<<8))
		token[index++] = (byte)((length>>8)&0xff);
	    token[index++] = (byte)(length&0xff);
	}
	return (index);
    }

    static int readDERLength(byte[] token, int index)
    {
	byte sf;
        int ret = 0;
        int nooctets;

	sf = token[index++];

	if ((sf & 0x80) == 0x80) { // value > 128
	    // bit 8 is 1 ; bits 0-7 of first bye is the number of octets
	    nooctets = (sf & 0x7f) ; // remove the 8th bit
            for (; nooctets != 0; nooctets--)
                ret = (ret<<8) + (token[index++] & 0x00FF);
	} else 
            ret = sf;
  
	return(ret);
    }
    
   /**
     * Return the ASN.1 encoded representation of a GSS mechanism identifier.
     * Currently only the GSSUP Mechanism is supported.
     */
    public static byte[] getMechanism() {
        byte[] mechCopy = Arrays.copyOf(mech, mech.length);
        return mechCopy;
    }


    public static void main(String[] args) {
	try {
            byte[] len = new byte[3];
            len[0] = (byte) 0x82;
            len[1] = (byte) 0x01;
            len[2] = (byte) 0xd3;
	    if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"Length byte array : " + dumpHex(len));
		_logger.log(Level.FINE," Der length = " + readDERLength(len,0));
	    }
	    String name = "default";
	    byte[] externalName = createExportedName(GSSUtils.GSSUP_MECH_OID, name.getBytes());
	    byte[] m = importName(GSSUtils.GSSUP_MECH_OID, externalName);
	    if(_logger.isLoggable(Level.FINE))
                _logger.log(Level.FINE,"BAR:" + new String(m));
	    String msg = "dummy_gss_export_sec_context" ;
	    byte[] foo = createMechIndToken(GSSUtils.GSSUP_MECH_OID, msg.getBytes());
	    if(_logger.isLoggable(Level.FINE))
                _logger.log(Level.FINE,"FOO:" + dumpHex(foo));
	    byte[] msg1 = getMechToken(GSSUtils.GSSUP_MECH_OID, foo); 
	    if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,"BAR:" + dumpHex(msg1));
	        _logger.log(Level.FINE,"BAR string: " + new String(msg1));
	    }
	} catch(Exception e) {
                _logger.log(Level.SEVERE,"iiop.name_exception",e);
	}
    }

}


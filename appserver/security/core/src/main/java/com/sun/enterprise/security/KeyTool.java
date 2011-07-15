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

package com.sun.enterprise.security;

import java.security.Security;
import java.security.Provider;

import java.io.*;
import java.util.*;
import java.security.cert.*;
import java.security.KeyStore;
import java.security.Key;
import com.sun.enterprise.util.*;
import java.util.logging.*;
import com.sun.logging.*;

/**
 * Wraps the J2SE's keytool after adding our provider.
 * Provides the PKCS12 functionality - read a PKCS12 format
 * keystore and replicate it into a "JKS" type keystore.
 * @author Harish Prabandham
 * @author Harpreet Singh
 */
public final class KeyTool {

    private static Logger _logger=null;
    static {
        _logger=LogDomains.getLogger(KeyTool.class, LogDomains.SECURITY_LOGGER);
    }

    private static final String JSSE_PROVIDER =
        "com.sun.net.ssl.internal.ssl.Provider";

    // The PKCS12 file to be replicated
    private File inputFile=null;
    // The JKS file output from the replication
    private File outputFile=null;
    private char[] jksKeyStorePass;
    private char[] pkcsKeyStorePass = null;
    // Password of the key
    private char[] jksKeyPass = null;
    private char[] pkcsKeyPass = null;

    private String provider = null;
    // The respective in-memory keystores
    private KeyStore pkcs12KeyStore = null;
    private KeyStore jksKeyStore = null;
    
    private static String PKCS12 = "-pkcs12";
    private static String INFILE = "-pkcsFile";
    private static String OUTFILE = "-jksFile";
    private static String PKCSKEYSTOREPASS = "-pkcsKeyStorePass";
    private static String PKCSKEYPASS = "-pkcsKeyPass";
    // following 2 options not used currently - set to default
    private static String JKSKEYSTOREPASS = "-jksKeyStorePass";
    private static String JKSKEYPASS = "-jksKeyPass";
    private static LocalStringManagerImpl localStrings =
	new LocalStringManagerImpl(KeyTool.class);

    /**
     * The class is only instantiated for PKCS12 - all other 
     * keytool functionality is passed to the sun.security.tools.KeyTool
     * @param the file name of the PKCS12 file
     * @param the output file name of the JKS file
     * @param the provider - In this case SunJSSE
     * @param password to the PKCS12 keystore
     * @param password to the key in the PKCS12 keystore 
     * @param password to the JKS keystore
     * @param password to the key in the JKS keystore
     * currently it has to be the same as the JKS keystore password
     * @exception Problem in loading the keystores
     */
    public KeyTool (String infile, String outfile, String pkcsKeyStorePass,
		    String pkcsKeyPass, String jksKeyStorePass, 
		    String jksKeyPass,
		    String provider) throws IOException {
	    inputFile = new File (infile);
	    outputFile = new File (outfile);
	    this.pkcsKeyStorePass = pkcsKeyStorePass.toCharArray ();
	    this.pkcsKeyPass = pkcsKeyPass.toCharArray ();
	    this.jksKeyStorePass = jksKeyStorePass.toCharArray ();
	    this.jksKeyPass = jksKeyPass.toCharArray ();
	    this.provider = provider;
	    // if the output file exists delete it and create a new file
	    try{
		if (outputFile.exists ()){
		    throw new IOException ("Output file already exists!");
		}
		// Get the keystores from the engines.
		pkcs12KeyStore = KeyStore.getInstance ("PKCS12", provider);
		jksKeyStore = KeyStore.getInstance ("JKS");

	    } catch (Exception e) {
		// catch possible security and io exceptions
		throw new IOException (e.getMessage ());
	    }
	    readKeyStores ();  
    }
    /**
     * Load both the keystore's into memory.
     * The PKCS12 is loaded from the file and the JKS file
     * is created.
     */
    public void readKeyStores() throws IOException {
	FileInputStream pkcsFis = null;
	FileInputStream jksFis = null;
	try {
	    pkcsFis = new FileInputStream(inputFile);
	    jksFis = new FileInputStream (outputFile);
	} catch(Exception e) {

	} finally {
	    try {
		pkcs12KeyStore.load(pkcsFis, pkcsKeyStorePass);
		// Dont need a password as creating a new 
		// keystore.
		jksKeyStore.load (jksFis, null);
	    } catch(Exception ce) {
		// Can't do much... too bad.
	        _logger.log(Level.SEVERE,
                            "java_security.KeyStore_load_exception",ce);
	    }finally{
                if (pkcsFis != null) {
                    pkcsFis.close();
                }
                if (jksFis != null) {
                    jksFis.close();
                }
            }
        }
    }   
    /**
     * Write the JKS keystore that is populated with values from 
     * the PKCS12 keystore to the outputfile.
     */
    public void writeJksKeyStore() throws IOException {
	FileOutputStream fos = null;
	try {
	    fos = new FileOutputStream(outputFile);
	} catch(Exception e) {
	    // No problem we'll create one....
	    // e.printStackTrace();
	} finally {
	    try {
		jksKeyStore.store (fos, jksKeyStorePass);
	    } catch(Exception ce) {
		// Can't do much... too bad.
	        _logger.log(Level.SEVERE,
                            "java_security.KeyStore_store_exception",ce);
	    }
	    if(fos != null)
		fos.close();
        }
    }   
    /** 
     * Copies the keys and certificates in the PKCS12 file to 
     * the in-memory JKS keystore
     * @exception If the keystore has not been instantiated or
     * the password to the key is'nt proper
     */
    public void replicatePkcs12ToJks () throws Exception {
	Enumeration e = pkcs12KeyStore.aliases ();
	for (; e.hasMoreElements (); ){
	    String alias = (String)e.nextElement ();
	    if (pkcs12KeyStore.isKeyEntry (alias)){
		
		/* Get the key and associated certificate chain
		 * from PKCS12 keystore and put in JKS keystore
		 */
		Key key = pkcs12KeyStore.getKey (alias, pkcsKeyPass);
		Certificate[] certs = 
		    pkcs12KeyStore.getCertificateChain (alias);
		jksKeyStore.setKeyEntry (alias,  key, jksKeyPass, certs);
	    } else if (pkcs12KeyStore.isCertificateEntry (alias)){

		jksKeyStore.setCertificateEntry 
		    (alias, pkcs12KeyStore.getCertificate (alias));
	    }
	}
    }
    /**
     * Prints the information in the PKCS12 keystore
     */
    public void info () throws Exception{
        _logger.log(Level.FINEST," Keystore Information");
        _logger.log(Level.FINEST," Type = " + pkcs12KeyStore.getType ());
        _logger.log(Level.FINEST," Provider = "+ pkcs12KeyStore.getProvider ());
        _logger.log(Level.FINEST," KeyStore size = "+pkcs12KeyStore.size ());
	Enumeration e = pkcs12KeyStore.aliases ();
        _logger.log(Level.FINEST," Kstore Aliases ");
	for (; e.hasMoreElements (); ){
	    String alias = (String)e.nextElement ();
            _logger.log(Level.FINEST," Alias = "+ alias);
	    if (pkcs12KeyStore.isKeyEntry (alias)){
                _logger.log(Level.FINEST,"Alias is a key entry ");
		Key key = pkcs12KeyStore.getKey (alias, pkcsKeyPass);
                _logger.log(Level.FINEST," Format = "+key.getFormat ());
	    } else if (pkcs12KeyStore.isCertificateEntry (alias)){
                _logger.log(Level.FINEST," Alias is a certificate entry");
	    }
	}
        _logger.log(Level.FINEST," End of Information");
    }
    /** 
     * Initializes the provider to be the JSSE provider
     */
    public static void initProvider() {
	try { 
	    Provider p =
		(Provider) Class.forName(JSSE_PROVIDER).newInstance();
	    Security.addProvider(p);

	} catch(Exception e) {
	    _logger.log(Level.SEVERE,"java_security.provider_exception",e);
	}
    } 
    /**
     * Gets the provider name for JSSE
     */
    public static String getProviderName (){
	try{
	    Provider p = 
		(Provider) Class.forName(JSSE_PROVIDER).newInstance();
	    return p.getName ();
	} catch (Exception e) {
	    _logger.log(Level.SEVERE,"java_security.getName_exception",e);	
	}
	return null;
    }
    public static void help (boolean exit){

	System.out.println 
	    (localStrings.getLocalString ("enterprise.security.keytool",
					  "keytool"));
	System.out.println
	    (localStrings.getLocalString
	     ("enterprise.security.keytooloptions", "PKCS Options:"));
	System.out.println (" "+ PKCS12 + 
			    " "+ INFILE + " fileName" +
			    " "+ PKCSKEYSTOREPASS + " password" +
			    " "+PKCSKEYPASS +" password" +
			    " "+OUTFILE+ " outputFileName"+
			    " "+JKSKEYSTOREPASS + " password"); 
	/* uncomment when support for this present in JSSE
	   System.Out.Println (" "+JKSKEYPASS+ " password"); 
	*/
	if (exit)
	    System.exit (-1);
    }
    public static void main(String[] args) {
	boolean pkcs = false;
	initProvider();
	String provider = null;
	String inFile = null;
	String outFile = null;
	String jksKeyPass  = null;
	String jksKeyStorePass = null;
	String pkcsKeyPass = null;
	String pkcsKeyStorePass = null;
	try{
	    if (args.length == 0){
		help (false);
		sun.security.tools.KeyTool.main (args);
	    }
	    if (args[0].equalsIgnoreCase (PKCS12)){
		pkcs = true;
		if (args.length != 11)
		    help (true);
		if (!args[1].equalsIgnoreCase (INFILE))
		    help (true);
		inFile = args[2];
		if (!args[3].equalsIgnoreCase (PKCSKEYSTOREPASS))
		    help (true);
		pkcsKeyStorePass = args[4];
		if (!args[5].equalsIgnoreCase (PKCSKEYPASS))
		    help (true);
		pkcsKeyPass = args[6];
		if (!args[7].equalsIgnoreCase (OUTFILE))
		    help (true);
		outFile = args[8];
		if (!args[9].equalsIgnoreCase (JKSKEYSTOREPASS))
		    help (true);
		
		jksKeyStorePass = args[10];
		jksKeyPass = jksKeyStorePass;
		/*
		// Uncomment the following when support
		// for different keystore and key pass present in JSSE

		if (!args[11].equalsIgnoreCase (JKSKEYPASS))
		    help ();
		jksKeyPass = args[12];
		*/
	    }
	    if (!pkcs){
		sun.security.tools.KeyTool.main(args);
	    } else{
		provider = getProviderName ();
		KeyTool kt = new KeyTool (inFile, outFile, pkcsKeyStorePass,
					  pkcsKeyPass, jksKeyStorePass,
					  jksKeyPass,
					  provider);
		kt.replicatePkcs12ToJks ();
		kt.writeJksKeyStore ();
	    }
	} catch (Exception e){
	    _logger.log(Level.SEVERE,"java_security.main_exception",e);
	}
    }
}




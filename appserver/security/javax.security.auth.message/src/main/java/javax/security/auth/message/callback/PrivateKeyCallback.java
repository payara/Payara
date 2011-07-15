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

package javax.security.auth.message.callback;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import javax.security.auth.callback.Callback;
import javax.security.auth.x500.X500Principal;

/**
 * Callback for acquiring a Public Key Infrastructure (PKI) private key 
 * and its corresponding certificate chain.
 * This Callback may be used by client or server authentication modules 
 * to obtain private keys or private key references, from key 
 * repositories available to the CallbackHandler that processes the Callback.
 *
 * @version %I%, %G%
 */
public class PrivateKeyCallback implements Callback {

    private Request request;
    private PrivateKey key;
    private Certificate[] chain;

    /**
     * Marker interface for private key request types.
     */
    public static interface Request { };

    /**
     * Request type for private keys that are identified using an alias.
     */
    public static class AliasRequest implements Request {
	private String alias;

	/**
	 * Construct an AliasRequest with an alias.
	 *
	 * <p> The alias is used to directly identify the private key
	 * to be returned.  The corresponding certificate chain for the
	 * private key is also returned.
	 *
	 * <p> If the alias is null,
	 * the handler of the callback relies on its own default.
	 *
	 * @param alias Name identifier for the private key, or null.
	 */
	public AliasRequest(String alias) {
	    this.alias = alias;
	}

	/**
	 * Get the alias.
	 *
	 * @return The alias, or null.
	 */
	public String getAlias() {
	    return alias;
	}
    }

    /**
     * Request type for private keys that are identified using a SubjectKeyID
     */
    public static class SubjectKeyIDRequest implements Request {
	private byte[] id;

	/**
	 * Construct a SubjectKeyIDRequest with an subjectKeyID.
	 *
	 * <p> The subjectKeyID is used to directly identify the private key
	 * to be returned.  The corresponding certificate chain for the
	 * private key is also returned.
	 *
	 * <p> If the subjectKeyID is null,
	 * the handler of the callback relies on its own default.
	 *
	 * @param subjectKeyID Identifier for the private key, or null.
	 */
	public SubjectKeyIDRequest(byte[] subjectKeyID) {
	    if (subjectKeyID != null) {
		this.id = (byte[])subjectKeyID.clone();
	    }
	}

	/**
	 * Get the subjectKeyID.
	 *
	 * @return The subjectKeyID, or null.
	 */
	public byte[] getSubjectKeyID() {
	    return id;
	}
    }

    /**
     * Request type for private keys that are identified using an
     * issuer/serial number.
     */
    public static class IssuerSerialNumRequest implements Request {
	private X500Principal issuer;
	private BigInteger serialNum;

	/**
	 * Constructs a IssuerSerialNumRequest with an issuer/serial number.
	 *
	 * <p> The issuer/serial number is used to identify a
	 * public key certificate.  The corresponding private key
	 * is returned in the callback.  The corresponding certificate chain
	 * for the private key is also returned.
	 *
	 * If the issuer/serialNumber parameters are null,
	 * the handler of the callback relies on its own defaults.
	 *
	 * @param issuer The X500Principal name of the certificate issuer,
	 *		or null.
	 *
	 * @param serialNumber The serial number of the certificate,
	 *		or null.
	 */
	public IssuerSerialNumRequest(X500Principal issuer,
					BigInteger serialNumber) {
	    this.issuer = issuer;
	    this.serialNum = serialNumber;
	}

	/**
	 * Get the issuer.
	 *
	 * @return The issuer, or null.
	 */
	public X500Principal getIssuer() {
	    return issuer;
	}

	/**
	 * Get the serial number.
	 *
	 * @return The serial number, or null.
	 */
	public BigInteger getSerialNum() {
	    return serialNum;
	}
    }

    /**
     * Request type for private keys that are identified using a
     * certificate digest or thumbprint. 
     */
    public static class DigestRequest implements Request {
	private String algorithm;
	private byte[] digest;

	/**
	 * Constructs a DigestRequest with a digest value and algorithm 
	 * identifier.
	 *
	 * <p> The digest of the certificate whose private key is returned 
	 * must match the provided digest. The certificate digest is computed 
	 * by applying the specified algorithm to the bytes of the certificate.
	 * For example: 
	 * <code>
	 * MessageDigest.getInstance(algorithm).digest(cert.getEncoded())
	 </code>. The corresponding certificate chain for the private key is 
	 * also returned. 
	 *
	 * If the digest or algorithm parameters are null,
	 * the handler of the callback relies on its own defaults.
	 *
	 * @param digest The digest value to use to select the corresponding 
	 * certificate and private key (or null).
	 *
	 * @param algorithm A string value identifying the digest algorithm. 
	 * The value passed to this parameter may be null. If it is not null, 
	 * it must conform to the requirements for the algorithm parameter of 
	 * <code>java.security.MessageDigest.getInstance()</code>.
	 */
	public DigestRequest(byte[] digest, String algorithm) {
	    this.digest = digest;
	    this.algorithm = algorithm;
	}

	/**
	 * Get the digest value.
	 *
	 * @return The digest value which must match the digest of the
	 * certificate corresponding to the returned private key.
	 */
	public byte[] getDigest() {
	    return digest;
	}

	/**
	 * Get the algorithm identifier.
	 *
	 * @return The identifer of the algorithm used to compute the digest.
	 */
	public String getAlgorithm() {
	    return algorithm;
	}
    }

    /**
     * Constructs this PrivateKeyCallback with a private key Request object.
     *
     * <p> The <i>request</i> object identifies the private key
     * to be returned.  The corresponding certificate chain for the
     * private key is also returned.
     *
     * <p> If the <i>request</i> object is null,
     * the handler of the callback relies on its own default.
     *
     * @param request Identifier for the private key, or null.
     */
    public PrivateKeyCallback(Request request) {
	this.request = request;
    }

    /**
     * Used by the CallbackHandler to
     * get the Request object that identifies the private key to be returned.
     *
     * @return The Request object which identifies the private key
     *		to be returned, or null.  If null, the handler of the callback
     *		relies on its own default.
     */
    public Request getRequest() {
	return request;
    }

    /**
     * Used by the CallbackHandler to set the requested private key and
     * the corresponding certificate chain within the Callback.
     *
     * <p> If the requested private key or chain could not be found,
     * then both values must be set to null.
     *
     * @param key The private key, or null.
     *
     * @param chain The corresponding certificate chain, or null.
     */
    public void setKey(PrivateKey key, Certificate[] chain) {
	this.key = key;
	this.chain = chain;
    }

    /**
     * Used to obtain the private key set within the Callback.
     *
     * @return The private key, or null if the key could not be found.
     */
    public PrivateKey getKey() {
	return key;
    }

    /**
     * Used to obtain the certicicate chain set within the Callback.
     *
     * @return The certificate chain, or null if the chain could not be found.
     */
    public Certificate[] getChain() {
	return chain;
    }
}

















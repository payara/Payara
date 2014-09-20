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

package com.sun.enterprise.security.ssl.manager;

import java.util.HashSet;
import java.util.Iterator;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

/**
 * This class combines an array of X509TrustManagers into one.
 * @author Shing Wai Chan
 **/
public class UnifiedX509TrustManager implements X509TrustManager {
    private X509TrustManager[] mgrs = null;
    private X509Certificate[] issuers = {};

    public UnifiedX509TrustManager(X509TrustManager[] mgrs) {
        if (mgrs == null) {
            throw new IllegalArgumentException("Null array of X509TrustManagers");
        }
        this.mgrs = mgrs;

        HashSet tset = new HashSet(); //for uniqueness
        for (int i = 0; i < mgrs.length; i++) {
            X509Certificate[] tcerts = mgrs[i].getAcceptedIssuers();
            if (tcerts != null && tcerts.length > 0) {
                for (int j = 0; j < tcerts.length; j++) {
                    tset.add(tcerts[j]);
                }
            }
        }
        issuers = new X509Certificate[tset.size()];
        Iterator iter = tset.iterator();
        for (int i = 0; iter.hasNext(); i++) {
            issuers[i] = (X509Certificate)iter.next();
        }
    }

    // ---------- implements X509TrustManager -----------
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        CertificateException cex = null;
        for (int i = 0; i < mgrs.length; i++) {
            try {
                cex = null; //reset exception status
                mgrs[i].checkClientTrusted(chain, authType);
                break;
            } catch(CertificateException ex) {
                cex = ex;
            }
        }
        if (cex != null) {
            throw cex;
        }
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        CertificateException cex = null;
        for (int i = 0; i < mgrs.length; i++) {
            try {
                cex = null; //reset exception status
                mgrs[i].checkServerTrusted(chain, authType);
                break;
            } catch(CertificateException ex) {
                cex = ex;
            }
        }
        if (cex != null) {
            throw cex;
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        return issuers;
    }
}

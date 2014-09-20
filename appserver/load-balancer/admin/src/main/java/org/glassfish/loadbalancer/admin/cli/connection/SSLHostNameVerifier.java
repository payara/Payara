/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.loadbalancer.admin.cli.connection;

import javax.net.ssl.SSLSession;
import javax.net.ssl.HostnameVerifier;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import org.glassfish.loadbalancer.admin.cli.LbLogUtil;

/**
 *
 * @author sv96363
 */
public class SSLHostNameVerifier implements HostnameVerifier {

    /**
     * matches the hostname of the Load balancer to CN attribute of the
     * certificate obtained.
     * @param hostname hostname of the load balancer
     * @param session  SSL session information
     * @return true - if the LB host name and CN attribute in the certificate
     * matches, false otherwise
     */
    @Override
    public boolean verify(String hostname, SSLSession session) {
        if (session != null) {
            Certificate[] certs = null;
            try {
                certs = session.getPeerCertificates();
            } catch (Exception e) {
            }
            if (certs == null) {
                String msg = LbLogUtil.getStringManager().getString("NoPeerCert", hostname);
                LbLogUtil.getLogger().warning(msg);
                return false;
            }
            for (int i = 0; i < certs.length; i++) {
                if (certs[i] instanceof X509Certificate) {
                    X500Principal prin =
                            ((X509Certificate) certs[i]).getSubjectX500Principal();
                    String hName = null;
                    String dn = prin.getName();
                    // Look for name of the cert in the CN attribute
                    int cnIdx = dn.indexOf("CN=");
                    if (cnIdx != -1) {
                        String cnStr = dn.substring(cnIdx, dn.length());
                        int commaIdx = cnStr.indexOf(",");
                        // if the CN is the last element in the string, then
                        // there won't be a ',' after that.
                        // The principal could be either CN=chandu.sfbay,C=US
                        // or C=US,CN=chandu.sfbay
                        if (commaIdx == -1) {
                            commaIdx = dn.length();
                        }
                        hName = dn.substring(cnIdx + 3, commaIdx);
                    }
                    if (hostname.equals(hName)) {
                        return true;
                    }
                } else {
                    String msg = LbLogUtil.getStringManager().getString("NotX905Cert", hostname);
                    LbLogUtil.getLogger().warning(msg);
                }
            }
            // Now, try to match if it matches the hostname from the SSLSession
            if (hostname.equals(session.getPeerHost())) {
                return true;
            }
        }
		if (session != null) {
			String msg = LbLogUtil.getStringManager().getString("NotCertMatch",
					hostname, new String(session.getId()));
			LbLogUtil.getLogger().warning(msg);
		}
        return false;
    }
}

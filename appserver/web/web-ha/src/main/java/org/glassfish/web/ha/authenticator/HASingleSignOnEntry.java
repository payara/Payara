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

package org.glassfish.web.ha.authenticator;

import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import org.apache.catalina.Container;
import org.apache.catalina.core.StandardContext;

import org.apache.catalina.Session;
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.authenticator.SingleSignOnEntry;

import java.io.*;
import java.security.Principal;

/**
 * @author Shing Wai Chan
 */
public class HASingleSignOnEntry extends SingleSignOnEntry {
    protected long maxIdleTime;

    protected JavaEEIOUtils ioUtils;

    protected HASingleSignOnEntryMetadata metadata = null;

    // default constructor is required by backing store
    public HASingleSignOnEntry() {
        this(null, null, null, null, null, 0, 0, 0, null);
    }

    public HASingleSignOnEntry(Container container, HASingleSignOnEntryMetadata m,
            JavaEEIOUtils ioUtils) {
        this(m.getId(), null, m.getAuthType(),
                m.getUserName(), m.getRealmName(),
                m.getLastAccessTime(), m.getMaxIdleTime(), m.getVersion(),
                ioUtils);

        ByteArrayInputStream bais = null;
        BufferedInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bais = new ByteArrayInputStream(m.getPrincipalBytes());
            bis = new BufferedInputStream(bais);
            ois = ioUtils.createObjectInputStream(bis, true, this.getClass().getClassLoader());
            this.principal = (Principal)ois.readObject();
        } catch(Exception ex) {
            throw new IllegalStateException(ex);
        } finally {
            if (bais != null) {
                try {
                    bais.close();
                } catch(IOException ex) {
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch(IOException ex) {
                }
            }
            if (ois != null) {
                try {
                    ois.close();
                } catch(IOException ex) {
                }
            }
        }

        for (HASessionData data: m.getHASessionDataSet()) {
            StandardContext context = (StandardContext)container.findChild(data.getContextPath());
            Session session = null;
            try {
                session = context.getManager().findSession(data.getSessionId());
            } catch(IOException ex) {
                throw new IllegalStateException(ex);
            }
            sessions.add(session);
        }
    }

    public HASingleSignOnEntry(String id, Principal principal, String authType,
            String username, String realmName,
            long lastAccessTime, long maxIdleTime, long version,
            JavaEEIOUtils ioUtils) {
        
        super(id, version, principal, authType, username, realmName);
        this.lastAccessTime = lastAccessTime;
        this.maxIdleTime = maxIdleTime;
        this.ioUtils = ioUtils;

        metadata = new HASingleSignOnEntryMetadata(
                id, version, convertToByteArray(principal), authType,
                username, realmName,
                lastAccessTime, maxIdleTime);
    }

    public HASingleSignOnEntryMetadata getMetadata() {
        return metadata;
    }

    public long getMaxIdleTime() {
        return maxIdleTime;
    }

    @Override
    public synchronized boolean addSession(SingleSignOn sso, Session session) {
        boolean result = super.addSession(sso, session);
        if (result) {
            metadata.addHASessionData(new HASessionData(session.getId(),
                session.getManager().getContainer().getName()));
        }

        return result;
    }

    @Override
    public synchronized void removeSession(Session session) {
        super.removeSession(session);
        metadata.removeHASessionData(new HASessionData(session.getId(),
                session.getManager().getContainer().getName()));
    }

    @Override
    public void setLastAccessTime(long lastAccessTime) {
        super.setLastAccessTime(lastAccessTime);
        metadata.setLastAccessTime(lastAccessTime);
    }

    @Override
    public long incrementAndGetVersion() {
        long ver = super.incrementAndGetVersion();
        metadata.setVersion(ver);
        return ver;
    }

    // convert a Serializable object into byte array
    private byte[] convertToByteArray(Object obj) {
        ByteArrayOutputStream baos = null;
        BufferedOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            baos = new ByteArrayOutputStream();
            bos = new BufferedOutputStream(baos);
            oos = ioUtils.createObjectOutputStream(bos, true);
            oos.writeObject(obj);
        } catch(Exception ex) {
            throw new IllegalStateException(ex);
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch(Exception ex) {
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch(Exception ex) {
                }
            }
            if (oos != null) {
                try {
                    oos.close();
                } catch(Exception ex) {
                }
            }
        }

        return baos.toByteArray();
    }
}

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

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Shing Wai Chan
 */
public class HASingleSignOnEntryMetadata implements Serializable {
    protected String id = null;

    protected String authType = null;

    protected byte[] principalBytes = null;

    protected Set<HASessionData> sessionDataSet = new HashSet<HASessionData>();

    protected String userName = null;

    protected String realmName = null;

    protected long lastAccessTime;

    protected long maxIdleTime;

    protected long version;

    // default constructor is required by backing store
    public HASingleSignOnEntryMetadata() {
    }

    public HASingleSignOnEntryMetadata(String id, long version,
            byte[] principalBytes, String authType,
            String userName, String realmName,
            long lastAccessTime, long maxIdleTime) {
        
        this.id = id;
        this.version = version;
        this.principalBytes = ((principalBytes != null) ? ((byte[])principalBytes.clone()) : null);
        this.authType = authType;
        this.userName = userName;;
        this.realmName = realmName;
        this.lastAccessTime = lastAccessTime;
        this.maxIdleTime = maxIdleTime;
    }

    public String getId() {
        return id;
    }

    public byte[] getPrincipalBytes() {
        return principalBytes;
    }

    public String getAuthType() {
        return authType;
    }

    public String getUserName() {
        return userName;
    }

    public String getRealmName() {
        return realmName;
    }

    public Set<HASessionData> getHASessionDataSet() {
        return sessionDataSet;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public long getVersion() {
        return version;
    }

    void setVersion(long version) {
        this.version = version;
    }

    public long getMaxIdleTime() {
        return maxIdleTime;
    }

    boolean addHASessionData(HASessionData sessionData) {
        return sessionDataSet.add(sessionData);
    }

    boolean removeHASessionData(HASessionData sessionData) {
        return sessionDataSet.remove(sessionData);
    }

    @Override
    public String toString() {
        return "HASingleSignOnEntryMetadata{" +
                "id='" + id + '\'' +
                ", version=" + version +
                ", authType='" + authType + '\'' +
                ", sessionDataSet.size=" + sessionDataSet.size() +
                ", userName='" + userName + '\'' +
                ", realmName='" + realmName + '\'' +
                ", lastAccessTime=" + lastAccessTime +
                ", maxIdleTime=" + maxIdleTime +
                '}';
    }
}

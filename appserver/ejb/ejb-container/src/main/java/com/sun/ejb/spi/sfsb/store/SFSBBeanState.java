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

/*
 * SFSBBeanState.java
 *
 * Created on May 12, 2003, 3:21 PM
 */

package com.sun.ejb.spi.sfsb.store;

import com.sun.enterprise.naming.util.ObjectInputStreamWithLoader;
import org.glassfish.ha.store.annotations.Attribute;
import org.glassfish.ha.store.annotations.StoreEntry;
import org.glassfish.ha.store.api.Storeable;

import java.io.*;
import java.util.Random;

/**
 * @author Mahesh Kannan
 */
@StoreEntry
public class SFSBBeanState
        implements Storeable {

    private Serializable sessionId = null;

    private long lastAccess = 0L;

    private boolean isNew = false;

    private byte[] state = null;

    private long version = -1;

    private long maxIdleTime;

    public SFSBBeanState(Serializable sessionId, long lastAccess, boolean isNew, byte[] state, long version) {
        this.sessionId = sessionId;
        this.lastAccess = lastAccess;
        this.isNew = isNew;
        this.state = state;
        this.version = version;
    }

    public Serializable getSessionId() {
        return sessionId;
    }

    @Attribute
    public void setSessionId(Serializable sessionId) {
        this.sessionId = sessionId;
    }

    public long getLastAccess() {
        return lastAccess;
    }

    @Attribute
    public void setLastAccess(long lastAccess) {
        this.lastAccess = lastAccess;
    }

    public boolean isNew() {
        return isNew;
    }

    @Attribute
    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public byte[] getState() {
        return state;
    }

    @Attribute
    public void setState(byte[] state) {
        this.state = state;
    }

    public long getVersion() {
        return version;
    }

    @Attribute
    public void setVersion(long version) {
        this.version = version;
    }

    public long getMaxIdleTime() {
        return maxIdleTime;
    }

    @Attribute
    public void setMaxIdleTime(long maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    @Override
    public long _storeable_getVersion() {
        return getVersion();
    }

    @Override
    public void _storeable_setVersion(long version) {
        setVersion(version);
    }

    @Override
    public long _storeable_getLastAccessTime() {
        return getLastAccess();
    }

    @Override
    public void _storeable_setLastAccessTime(long lastAccessTime) {
        setLastAccess(lastAccessTime);
    }

    @Override
    public long _storeable_getMaxIdleTime() {
        return getMaxIdleTime();
    }

    @Override
    public void _storeable_setMaxIdleTime(long maxIdleTime) {
        setMaxIdleTime(maxIdleTime);
    }

    @Override
    public String[] _storeable_getAttributeNames() {
        return new String[0];  //FIXME
    }

    @Override
    public boolean[] _storeable_getDirtyStatus() {
        return new boolean[0]; //FIXME
    }

    @Override
    public void _storeable_writeState(OutputStream os) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(os);
        try {
            oos.writeObject(sessionId);
            oos.writeLong(version);
            oos.writeLong(lastAccess);
            oos.writeLong(maxIdleTime);
            oos.writeBoolean(isNew);

            oos.writeInt(state.length);
            oos.write(state);
        } finally {
            try { oos.flush(); oos.close(); } catch (Exception ex) {}
        }
    }

    @Override
    public void _storeable_readState(InputStream is) throws IOException {
        ObjectInputStream ois = new ObjectInputStreamWithLoader(is, SFSBBeanState.class.getClassLoader());

        try {
            sessionId = (Serializable) ois.readObject();
            version = ois.readLong();
            lastAccess = ois.readLong();
            maxIdleTime = ois.readLong();
            isNew = ois.readBoolean();
            
            int len = ois.readInt();
            state = new byte[len];
            int index = 0;

            for (int remaining = len; remaining > 0;) {
                int count = ois.read(state, index, remaining);
                if (count < 0) {
                    throw new IOException("EOF while still (" + remaining + "/" + len + ") more bytes to read");
                }
                
                remaining -= count;
                index += count;
            }
        } catch (ClassNotFoundException cnfEx) {
            throw new IOException(cnfEx);
        } finally {
            try { ois.close(); } catch (Exception ex) {}
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SFSBBeanState{ ")
                .append("sessionId=").append(sessionId)
                .append(", lastAccess=").append(lastAccess)
                .append(", isNew=").append(isNew)
                .append(", state.length=").append(state.length)
                .append(", version=").append(version)
                .append(", maxIdleTime=").append(maxIdleTime)
                .append("}");

//        for (int i=0; i<state.length; i++) {
//            byte b = state[i];
//            sb.append("_" + b);
//        }

        return sb.toString();
    }
}

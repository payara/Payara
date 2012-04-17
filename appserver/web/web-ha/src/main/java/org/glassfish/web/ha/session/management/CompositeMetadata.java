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

package org.glassfish.web.ha.session.management;


import org.glassfish.ha.store.api.Storeable;
import org.glassfish.ha.store.spi.StorableMap;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * A class to hold a collection of children SessionAttributeMetadata. This class is
 * used mainly to store a collection of AttributeMetaData that are part of a
 * WebSession. The metadata about the web session itself can be obtained
 * directly from the CompositeMetadata itself, while the metadata of its
 * attributes can be obtained from the individual SessionAttributeMetadata that is part
 * of the collection returned by getEntries().
 */
public final class CompositeMetadata implements Storeable {


    private long version;

    private long maxInactiveInterval;

    private long lastAccessTime;

    private byte[] state;

    private String stringExtraParam;

    private Map<String, SessionAttributeMetadata> attributesMap = new HashMap<String, SessionAttributeMetadata>();

    private transient Collection<SessionAttributeMetadata> entries;

    private transient Set<String> _dirtyAttributeNames = new HashSet<String>();

    private transient static String[] _attributeNames = new String[]{
            "trunkState", "stringExtraParam", "sessionAttributes"
    };

    private transient static Set<String> saveALL = new HashSet<String>();

    private transient static Set<String> saveEP = new HashSet<String>();

    private boolean[] dirtyBits = new boolean[]{false, false, false};

    static {
        saveALL.add(ReplicationAttributeNames.STATE);
        saveALL.add(ReplicationAttributeNames.EXTRA_PARAM);
        saveEP.add(ReplicationAttributeNames.EXTRA_PARAM);
    }

    /**
     * Every Storeable must have a public no arg constructor
     */
    public CompositeMetadata() {

    }

    /**
     * Construct a CompositeMetadata object
     *
     * @param version             The version of the data. A freshly created state has a version ==
     *                            0
     * @param lastAccessTime      the last access time of the state. This must be used in
     *                            conjunction with getMaxInactiveInterval to determine if the
     *                            state is idle enough to be removed.
     * @param maxInactiveInterval the maximum time that this state can be idle in the store
     *                            before it can be removed.
     * @param entries             the SessionAttributeMetadata that are part of this Metadata
     */
    public CompositeMetadata(long version, long lastAccessTime,
                             long maxInactiveInterval, Collection<SessionAttributeMetadata> entries, byte[] state, String stringExtraParam) {

        this.version = version;
        this.lastAccessTime = lastAccessTime;
        this.maxInactiveInterval = maxInactiveInterval;
        this.entries = entries;
        dirtyBits[2] = true;
        if (state != null) {
            setState(state);
        } else {
            dirtyBits[0] = false;
        }
        setStringExtraParam(stringExtraParam);
    }

    public byte[] getState() {
        return this.state;
    }

    public void setState(byte[] state) {
        this.state = state;
        dirtyBits[0] = true;
    }

    public String getStringExtraParam() {
        return stringExtraParam;
    }

    public void setStringExtraParam(String stringExtraParam) {
        this.stringExtraParam = stringExtraParam;
        dirtyBits[1] = true;
    }

    /**
     * Returns a collection of Metadata (or its subclass). Note that though it
     * is possible to have a compositeMetadata  itself as part of this
     * collection, typically they contain only AttributeMetaData
     *
     * @return a collection of SessionAttributeMetadata
     */
    public Collection<SessionAttributeMetadata> getEntries() {
        return attributesMap.values();
    }

    public long getVersion() {
        return version;
    }

    @Override
    public long _storeable_getVersion() {
        return version;
    }

    @Override
    public void _storeable_setVersion(long version) {
        this.version = version;
    }

    @Override
    public long _storeable_getLastAccessTime() {
        return lastAccessTime;
    }

    @Override
    public void _storeable_setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    @Override
    public long _storeable_getMaxIdleTime() {
        return maxInactiveInterval;
    }

    @Override
    public void _storeable_setMaxIdleTime(long maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    @Override
    public String[] _storeable_getAttributeNames() {
        return _attributeNames;
    }

    @Override
    public boolean[] _storeable_getDirtyStatus() {
        return dirtyBits;
    }

    @Override
    public void _storeable_writeState(OutputStream os) throws IOException {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(os);
            dos.writeLong(version);
            dos.writeLong(lastAccessTime);
            dos.writeLong(maxInactiveInterval);

            for (int i = 0; i < dirtyBits.length; i++) {
                dos.writeBoolean(dirtyBits[i]);
            }

            if (dirtyBits[0]) {
                dos.writeInt(state == null ? 0 : state.length);
                if (state != null) {
                    dos.write(state);
                }
            }

            if (dirtyBits[1]) {
                if (stringExtraParam == null) {
                    dos.writeInt(0);
                } else {
                    byte[] sd = stringExtraParam.getBytes(Charset.defaultCharset());
                    dos.writeInt(sd.length);
                    dos.write(sd);
                }
            }

            if (dirtyBits[2]) {
                dos.writeInt(entries.size());
                for (SessionAttributeMetadata attr : entries) {
                    byte[] opNameInBytes = attr.getOperation().toString().getBytes(Charset.defaultCharset());
                    dos.writeInt(opNameInBytes.length);
                    dos.write(opNameInBytes);

                    String attrName = attr.getAttributeName();
                    if (attrName == null) {
                        dos.writeInt(0); //NOTE: We don't allow null attrNames!!
                    } else {
                        byte[] attrNameData = attrName.getBytes(Charset.defaultCharset());
                        dos.writeInt(attrNameData.length);
                        dos.write(attrNameData);

                        if ((attr.getOperation() == SessionAttributeMetadata.Operation.ADD) ||
                                attr.getOperation() == SessionAttributeMetadata.Operation.UPDATE) {
                            byte[] attrData = attr.getState();
                            if (attrData == null) {
                                dos.writeInt(0);
                            } else {
                                dos.writeInt(attrData.length);
                                dos.write(attrData);
                            }
                        }
                    }
                }
            }
        } finally {
            try {
                dos.flush();
                dos.close();
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void _storeable_readState(InputStream is) throws IOException {
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(is);
            version = dis.readLong();
            lastAccessTime = dis.readLong();
            maxInactiveInterval = dis.readLong();
            dirtyBits = new boolean[]{true, true, true};

            boolean[] dirtyFlags = new boolean[3];
            for (int i = 0; i < dirtyFlags.length; i++) {
                dirtyFlags[i] = dis.readBoolean();
            }


            if (dirtyFlags[0]) {
                int len = dis.readInt();
                if (len > 0) {
                    state = new byte[len];
                    dis.readFully(state);
                }
            }

            if (dirtyFlags[1]) {
                int len = dis.readInt();
                if (len > 0) {
                    byte[] sd = new byte[len];
                    dis.readFully(sd);
                    stringExtraParam = new String(sd, Charset.defaultCharset());
                }
            }

            if (dirtyFlags[2]) {
                int entryCount = dis.readInt();
                for (int i = 0; i < entryCount; i++) {

                    int opNameLen = dis.readInt();
                    byte[] opnameData = new byte[opNameLen];
                    dis.readFully(opnameData);
                    String opName = new String(opnameData, Charset.defaultCharset());

                    int attrNameLen = dis.readInt();
                    if (attrNameLen > 0) {
                        byte[] sd = new byte[attrNameLen];
                        dis.readFully(sd);
                        String attrName = new String(sd, Charset.defaultCharset());

                        SessionAttributeMetadata.Operation smdOpcode = SessionAttributeMetadata.Operation.valueOf(opName);
                        switch (smdOpcode) {
                            case ADD:
                            case UPDATE:
                                int dataLen = dis.readInt();
                                byte[] attrData = new byte[dataLen];
                                dis.readFully(attrData);
                                attributesMap.put(attrName, new SessionAttributeMetadata(attrName, smdOpcode, attrData));
                                break;

                            case DELETE:
                                attributesMap.remove(attrName);
                                break;
                            default:
                                throw new IOException("Unknown operation");

                        }
                    }
                }
            }
        } finally {
            try {
                dis.close();
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public String toString() {
        return "CompositeMetadata{" +
                "version=" + version +
                ", maxInactiveInterval=" + maxInactiveInterval +
                ", lastAccessTime=" + lastAccessTime +
                ", state=" + (state ==null ? 0 : state.length) +
                ", _dirtyAttributeNames=" + _dirtyAttributeNames +
                '}';
    }

}

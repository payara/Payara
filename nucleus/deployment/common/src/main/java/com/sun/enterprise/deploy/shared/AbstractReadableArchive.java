/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deploy.shared;

import org.glassfish.api.deployment.archive.ReadableArchive;
import java.util.Map;
import java.util.HashMap;

/**
 * Common methods for ReadableArchive implementations
 */
public abstract class AbstractReadableArchive implements ReadableArchive {
    protected ReadableArchive parentArchive;
    protected Map<Class<?>, Object> extraData=new HashMap<Class<?>, Object>();
    protected Map<String, Object> archiveMetaData = new HashMap<String, Object>();


    /**
     * set the parent archive for this archive
     *
     * @param parentArchive the parent archive
     */
    public void setParentArchive(ReadableArchive parentArchive) {
        this.parentArchive = parentArchive;
    }

    /**
     * get the parent archive of this archive
     *
     * @return the parent archive
     */
    public ReadableArchive getParentArchive() {
        return parentArchive;
    }

    /**
     * Returns any data that could have been calculated as part of
     * the descriptor loading.
     *
     * @param dataType the type of the extra data
     * @return the extra data or null if there are not an instance of
     * type dataType registered.
     */
    public synchronized <U> U getExtraData(Class<U> dataType) {
        return dataType.cast(extraData.get(dataType));
    }

    public synchronized <U> void setExtraData(Class<U> dataType, U instance) {
        extraData.put(dataType, instance);
    }

    public synchronized <U> void removeExtraData(Class<U> dataType) {
        extraData.remove(dataType);
    }


    public void addArchiveMetaData(String metaDataKey, Object metaData) {
        if (metaData!=null) {
            archiveMetaData.put(metaDataKey, metaData);
        }
    }

    public <T> T getArchiveMetaData(String metaDataKey, Class<T> metadataType) {
        Object metaData = archiveMetaData.get(metaDataKey);
        if (metaData != null) {
            return metadataType.cast(metaData);
        }
        return null;
    }

    public void removeArchiveMetaData(String metaDataKey) {
        archiveMetaData.remove(metaDataKey);
    }
}

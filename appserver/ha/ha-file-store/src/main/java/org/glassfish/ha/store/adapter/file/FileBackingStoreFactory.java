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

package org.glassfish.ha.store.adapter.file;

import org.glassfish.ha.store.api.*;
import org.jvnet.hk2.annotations.Service;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mahesh Kannan
 */
@Service(name = "file")
public class FileBackingStoreFactory
        implements BackingStoreFactory {

    private static ThreadLocal<FileStoreTransaction> _current = new ThreadLocal<FileStoreTransaction>();

    private static ConcurrentHashMap<String, FileBackingStore> _stores
            = new ConcurrentHashMap<String, FileBackingStore>();


    static FileBackingStore getFileBackingStore(String storeName) {
        return _stores.get(storeName);
    }

    static void removemapping(String storeName) {
        _stores.remove(storeName);
    }

    @Override
    public <K extends Serializable, V extends Serializable> BackingStore<K, V> createBackingStore(
            BackingStoreConfiguration<K, V> conf)
                throws BackingStoreException {
        FileBackingStore<K, V> fs = new FileBackingStore<K, V>();
        fs.initialize(conf);
        fs.setFileBackingStoreFactory(this);
        _stores.put(conf.getStoreName(), fs);
        return fs;
    }

    @Override
    public BackingStoreTransaction createBackingStoreTransaction() {
        FileStoreTransaction tx = new FileStoreTransaction();
        _current.set(tx);
        return tx;
    }

    //package
    static final FileStoreTransaction getCurrent() {
        return _current.get();
    }
}

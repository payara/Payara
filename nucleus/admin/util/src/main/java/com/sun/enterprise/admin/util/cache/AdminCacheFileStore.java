/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.util.cache;

import com.sun.enterprise.security.store.AsadminSecurityUtil;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.logging.LogDomains;
import java.io.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

/** {@link AdminCahce} based on file system.<br/>
 *
 * @author mmares
 */
@Service(name=AdminCacheFileStore.SERVICE_NAME)
public class AdminCacheFileStore implements AdminCache {
    
    public static final String SERVICE_NAME = "file-store";
    private static final String DEFAULT_FILENAME = "#default#.cache";
    
    private static final Logger logger = LogDomains.getLogger(AdminCacheFileStore.class,
            LogDomains.ADMIN_LOGGER);
    
    @Inject
    private AdminCahceUtils adminCahceUtils;
    
    @Override
    public <A> A get(String key, Class<A> clazz) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Attribute key must be unempty.");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("Attribute clazz can not be null.");
        }
        DataProvider provider = adminCahceUtils.getProvider(clazz);
        if (provider == null) {
            return null;
        }
        return (A) provider.toInstance(get(key), clazz);
    }
    
    public byte[] get(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Attribute key must be unempty.");
        }
        if (!adminCahceUtils.validateKey(key)) {
            throw new IllegalArgumentException("Attribute key must be in form (([-_.a-zA-Z0-9]+/?)+)");
        }
        File f = getCacheFile(key);
        if (!f.exists()) {
            return null;
        }
        // @todo Java SE 7 - use try with resources
        InputStream is = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            is = new BufferedInputStream(new FileInputStream(f));
            FileUtils.copy(is, baos, 0);
        } catch (IOException ex) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Can not read admin cache file: {0}", f.getPath());
            }
            //File is not readable. Same as unexisting file => no result
            return null;
        } finally {
            try { is.close(); } catch (Exception ex) {}
            try { baos.close(); } catch (Exception ex) {}
        }
        return baos.toByteArray();
    }
    
    private File getCacheFile(String key) {
        File dir = AsadminSecurityUtil.getDefaultClientDir();
        int idx = key.lastIndexOf('/');
        if (idx > 0) {
            dir = new File(dir, key.substring(0, idx));
            if (!dir.exists()) {
                dir.mkdirs();
            }
            key = key.substring(idx + 1);
            if (key.isEmpty()) {
                key = DEFAULT_FILENAME;
            }
        }
        return new File(dir, key);
    }

    @Override
    public synchronized void put(String key, Object data) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Attribute key must be unempty.");
        }
        if (!adminCahceUtils.validateKey(key)) {
            throw new IllegalArgumentException("Attribute key must be in form (([-_.a-zA-Z0-9]+/?)+)");
        }
        if (data == null) {
            throw new IllegalArgumentException("Attribute data can not be null.");
        }
        DataProvider provider = adminCahceUtils.getProvider(data.getClass());
        if (provider == null) {
            throw new IllegalStateException("There is no data provider for " + data.getClass());
        }
        File cacheFile = getCacheFile(key);
        OutputStream os = null;
        try {
            File tempFile = File.createTempFile("temp", "cache", cacheFile.getParentFile());
            os = new FileOutputStream(tempFile);
            os.write(provider.toByteArray(data));
            os.close();
            cacheFile.delete();
            if (!tempFile.renameTo(cacheFile)) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING, "Can not write data to cache file: {0}", cacheFile.getPath());
                }
                tempFile.delete();
            }
        } catch (IOException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Can not write data to cache file: {0}", cacheFile.getPath());
            }
        } finally {
            try { os.close(); } catch (Exception ex) {}
        }
    }

    @Override
    public boolean contains(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Attribute key must be unempty.");
        }
        if (!adminCahceUtils.validateKey(key)) {
            throw new IllegalArgumentException("Attribute key must be in form (([-_.a-zA-Z0-9]+/?)+)");
        }
        File cacheFile = getCacheFile(key);
        return cacheFile.exists() && cacheFile.isFile();
    }

    @Override
    public Date lastUpdated(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Attribute key must be unempty.");
        }
        if (!adminCahceUtils.validateKey(key)) {
            throw new IllegalArgumentException("Attribute key must be in form (([-_.a-zA-Z0-9]+/?)+)");
        }
        File cacheFile = getCacheFile(key);
        if (!cacheFile.exists() || !cacheFile.isFile()) {
            return null;
        }
        return new Date(cacheFile.lastModified());
    }
    
}

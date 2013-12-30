/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.security.services.impl;

import com.sun.enterprise.security.store.PasswordAdapter;
import com.sun.enterprise.util.Utility;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.glassfish.api.admin.PasswordAliasStore;

/**
 * Provides the PasswordAliasStore behavior using a JCEKS keystore.
 * <p>
 * The keystore is actually managed by the PasswordAdapter, to which
 * this implementation currently delegates its work.  
 * <p>
 * Note that this service is currently per-lookup.  This is so that each
 * use of the alias store gets the current on-disk information.  Ideally we can change this
 * when we can use Java 7 features, including the WatchService feature.  
 * <p>
 * This class's methods are not
 * synchronized because the PasswordAdapter's methods are.  If this implementation
 * changes so that it no longer delegates to those synchronized PasswordAdapter
 * methods, then make sure that the implementation is thread-safe.
 * <p>
 * Note that the domain-scoped password alias store service class extends this
 * class.  As a service, that class will be instantiated using the no-args 
 * constructor.  So the actual initialization of the class occurs in the init
 * method.  The domain-scoped service class invokes the init method itself.
 * Any code that needs to create some other alias store can use the newInstance
 * method to provide the location of the alias store file and the password.
 * 
 * @author tjquinn
 */
public class JCEKSPasswordAliasStore implements PasswordAliasStore {

    private final static Charset utf8 = Charset.forName("UTF-8");
    
    private PasswordAdapter pa = null;
    private String pathToAliasStore;
    private char[] storePassword;
    
    protected final void init(final String pathToAliasStore, final char[] storePassword) {
        this.pathToAliasStore = pathToAliasStore;
        this.storePassword = storePassword;
    }
    
    private synchronized PasswordAdapter pa() {
        if (pa == null) {
            try {
                pa = new PasswordAdapter(pathToAliasStore, storePassword);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return pa;
    }
    
    public static JCEKSPasswordAliasStore newInstance(final String pathToAliasStore, final char[] storePassword) {
        final JCEKSPasswordAliasStore result = new JCEKSPasswordAliasStore();
        result.init(pathToAliasStore, storePassword);
        return result;
    }
    
    @Override
    public void clear() {
        try {
            for (Enumeration<String> aliasEnum = pa().getAliases(); aliasEnum.hasMoreElements(); ) {
                pa().removeAlias(aliasEnum.nextElement());
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void put(String alias, char[] password) {
        final CharBuffer charBuffer = CharBuffer.wrap(password);
        final ByteBuffer byteBuffer = utf8.encode(charBuffer);
        try {
            pa().setPasswordForAlias(alias, Utility.toByteArray(byteBuffer));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void putAll(PasswordAliasStore otherStore) {
        final Map<String,char[]> entries = new HashMap<String,char[]>();
        for (Iterator<String> aliasIt = otherStore.keys(); aliasIt.hasNext();) {
            final String alias = aliasIt.next();
            entries.put(alias, otherStore.get(alias));
        }
        putAll(entries);
    }

    @Override
    public void putAll(Map<String, char[]> settings) {
        for (Map.Entry<String,char[]> entry : settings.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
        settings.clear();
    }

    @Override
    public void remove(String alias) {
        try {
            pa().removeAlias(alias);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean containsKey(String alias) {
        try {
            return pa().aliasExists(alias);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public char[] get(String alias) {
        try {
            final SecretKey secretKey = pa().getPasswordSecretKeyForAlias(alias);
            final ByteBuffer byteBuffer = ByteBuffer.wrap(secretKey.getEncoded());
            return Utility.toCharArray(utf8.decode(byteBuffer));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Iterator<String> keys() {
        final List<String> keys = new ArrayList<String>();
        try {
            for (Enumeration<String> aliases = pa().getAliases(); aliases.hasMoreElements(); keys.add(aliases.nextElement())) {}
            return keys.iterator();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int size() {
        try {
            int size = 0;
            for (Enumeration<String> aliases = pa().getAliases(); aliases.hasMoreElements(); size++, aliases.nextElement() ) {}
            return size;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

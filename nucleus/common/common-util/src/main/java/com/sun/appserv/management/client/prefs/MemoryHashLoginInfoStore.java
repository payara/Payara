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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2017-2024] [Payara Foundation and/or its affiliates]
package com.sun.appserv.management.client.prefs;


import com.sun.enterprise.security.store.AsadminSecurityUtil;
import com.sun.enterprise.util.Utility;
import org.glassfish.security.common.FileProtectionUtility;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** A {@link LoginInfoStore} that reads the information from the default file ".gfclient/pass"
 * and stores it as a map in the memory. It is not guaranteed that the concurrent
 * modifications will yield consistent results. This class is <i> not </i> thread safe. The
 * serial access has to be ensured by the callers.
 * @since Appserver 9.0
 */
public class MemoryHashLoginInfoStore implements LoginInfoStore {

    private static final String DEFAULT_STORE_NAME = "pass";

    private static final Base64.Encoder encoder = Base64.getMimeEncoder();
    private static final Base64.Decoder decoder = Base64.getMimeDecoder();

    private Map<HostPortKey, LoginInfo> state;
    private final File store;
    /**
     * Creates a new instance of MemoryHashLoginInfoStore. A side effect of calling
     * this constructor is that if the default store does not exist, it will be created.
     * This does not pose any harm or surprises.
     */
    public MemoryHashLoginInfoStore() throws StoreException {
        try {
            final File dir = AsadminSecurityUtil.getDefaultClientDir();
            store = new File(dir, DEFAULT_STORE_NAME);

            if (store.createNewFile()) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(store, StandardCharsets.UTF_8))) {
                    FileMapTransform.writePreamble(bw);
                }
                state = new HashMap<> ();
            }
            else {
                try (BufferedReader br = new BufferedReader(new FileReader(store, StandardCharsets.UTF_8))) {
                    state = FileMapTransform.readAll(br);
                }
            }
        } catch(final Exception e) {
            throw new StoreException(e);
        }
        protect();
    }

    @Override
    public void store(final LoginInfo login) throws StoreException {
        this.store(login, false);
    }

    @Override
    public void store(final LoginInfo login, boolean overwrite) throws StoreException {
        if (login == null)
            throw new IllegalArgumentException("null_arg");
        final String host = login.getHost();
        final int port    = login.getPort();
        if (!overwrite && this.exists(host, port)) {
            throw new StoreException("Login exists for host: " + host + " port: " + port);
        }
        final HostPortKey key = new HostPortKey(host, port);
        final LoginInfo old   = state.get(key);
        state.put(key, login);
        commit(key, old);
        protect();
    }

    @Override
    public void remove(final String host, final int port) {
        final HostPortKey key = new HostPortKey(host, port);
        final LoginInfo gone  = state.remove(key);
        commit(key, gone);
    }

    @Override
    public LoginInfo read(String host, int port) {
        final HostPortKey key = new HostPortKey(host, port);
        return state.get(key);
    }

    @Override
    public boolean exists(String host, int port) {
        final HostPortKey key = new HostPortKey(host, port);
        return state.containsKey(key);
    }

    @Override
    public int size() {
        return state.size(); // no need to access disk
    }

    @Override
    public Collection<LoginInfo> list() {
        return (Collections.unmodifiableCollection(state.values())); // no need to access disk
    }

    @Override
    public String getName() {
        return store.getAbsoluteFile().getAbsolutePath();
    }

    ///// PRIVATE METHODS /////
    private void commit(final HostPortKey key, LoginInfo old) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(store, StandardCharsets.UTF_8))) {
            FileMapTransform.writeAll(state.values(), writer);
        } catch(final Exception e) {
            state.put(key, old); //try to roll back, first memory
            if (old != null) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(store, StandardCharsets.UTF_8))) { // then disk, if the old value is not null
                    FileMapTransform.writeAll(state.values(), writer);
                } catch (final Exception ae) {
                    throw new RuntimeException("catastrophe, can't write it to file");
                }//ignore, can't do much
            }
        }
    }

    private void protect()
    {
        /*
             note: if this is Windows we still try 'chmod' -- they may have MKS or
            some other UNIXy package for Windows.
            cacls is too dangerous to use because it requires a "Y" to be written to
            stdin of the cacls process.  If cacls doesn't exist or if they are using
            a non-NTFS file system we would hang here forever.
         */
        try
        {
            if(store == null || !store.exists())
                return;

            FileProtectionUtility.chmod0600(store);
        }
        catch(Exception e)
        {
            // we tried...
        }
    }

    private static class FileMapTransform {
        private FileMapTransform() {} //disallow
        static Map<HostPortKey, LoginInfo> readAll(final BufferedReader reader) throws IOException, URISyntaxException {
            String line;
            final Map<HostPortKey, LoginInfo> map = new HashMap<> ();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#"))
                    continue; //ignore comments
                final int si = line.indexOf(' '); //index of space
                if (si == -1)
                    throw new IOException("Error: invalid record: " + line);
                final URI uri         = new URI(line.substring(0, si));
                final String encp     = line.substring(si+1);
                final HostPortKey key = uri2Key(uri);
                final LoginInfo value = line2LoginInfo(uri, encp);
                map.put(key, value);
            }
            return ( map );
        }
        static void writeAll(final Collection<LoginInfo> logins, final BufferedWriter writer) throws IOException, URISyntaxException {
            writePreamble(writer);
            //write out sorted, because not more than 100 logins are expected to be there
            final List<LoginInfo> list = new ArrayList<>(logins);
            Collections.sort(list);
            for (LoginInfo login : list) {
                writeOne(login, writer);
            }
        }
        private static void writeOne(final LoginInfo login, final BufferedWriter writer) throws IOException, URISyntaxException {
            writer.write(login2Line(login));
            writer.newLine();
        }

        static HostPortKey uri2Key(final URI uri) {
            return new HostPortKey(uri.getHost(), uri.getPort());
        }

        static LoginInfo line2LoginInfo(final URI uri, final String encp) throws IOException {
            final String host     = uri.getHost();
            final int port        = uri.getPort();
            final String user     = uri.getUserInfo();
            final char[] password = Utility.convertByteArrayToCharArray(decoder.decode(encp), null);
            return new LoginInfo(host, port, user, password);
        }

        static String login2Line(final LoginInfo login) throws IOException, URISyntaxException {
            final String scheme   = "asadmin";
            final String host     = login.getHost();
            final int port        = login.getPort();
            final String user     = login.getUser();
            final URI uri         = new URI(scheme, user, host, port, null, null, null);
            final char[] password = login.getPassword();
            final String encp     = encoder.encodeToString(Utility.convertCharArrayToByteArray(password,null));
            return uri.toString() + ' ' + encp;
        }

        static void writePreamble(final BufferedWriter bw) throws IOException {
            final String preamble = "# Do not edit this file by hand. Use login interface instead.";
            bw.write(preamble);
            bw.newLine();
        }
    }

    private static class HostPortKey {
        private final String host;
        private final int port;
        HostPortKey(final String host, final int port) {
            this.host = host;
            this.port = port;
        }
        @Override
        public boolean equals(final Object other) {
            boolean same = false;
            if (other instanceof HostPortKey) {
                final HostPortKey that = (HostPortKey)other;
                same = this.host.equals(that.host) && this.port == that.port;
            }
            return ( same );
        }
        @Override
        public int hashCode() {
            return 53 * host.hashCode() + 31 * port;
        }
    }
}

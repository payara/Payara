/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.gfplugin;

import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFishException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.io.ByteArrayInputStream;

/**
 * @author bhavanishankar@java.net
 */

public class DeployerClient implements Deployer {

    CommandRunner cr;

    DeployerClient(CommandRunner commandRunner) {
        this.cr = commandRunner;
    }

    public String deploy(URI uri, String... strings) throws GlassFishException {
        return deploy(new File(uri), strings);
    }

    public String deploy(File file, String... strings) throws GlassFishException {
        // Use asadmin client to invoke.
        int size = strings != null ? strings.length + 1 : 1;
        String[] args = new String[size];
        if (size > 1) {
            System.arraycopy(strings, 0, args, 0, size - 1);
        }
        args[size - 1] = file.getAbsolutePath();
        CommandResult result = cr.run("deploy", args);
        return result.getOutput();
    }

    public String deploy(InputStream inputStream, String... strings)
            throws GlassFishException {
        try {
            return deploy(createFile(inputStream), strings);
        } catch (IOException e) {
            throw new GlassFishException(e);
        }
    }

    public void undeploy(String s, String... strings) throws GlassFishException {
        int size = strings != null ? strings.length + 1 : 1;
        String[] args = new String[size];
        if (size > 1) {
            System.arraycopy(strings, 0, args, 0, size - 1);
        }
        args[size - 1] = s;
        CommandResult result = cr.run("undeploy", args);
        System.out.println(result.getOutput());
    }

    public Collection<String> getDeployedApplications() throws GlassFishException {
        List<String> apps = new ArrayList();
        CommandResult result = cr.run("list-components");
        if (result.getOutput().indexOf("<") != -1) {
            Properties properties = new Properties();
            try {
                properties.load(new ByteArrayInputStream(result.getOutput().getBytes()));
                for (Object key : properties.keySet()) {
                    apps.add((String) key);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return apps;
    }

    private File createFile(InputStream in) throws IOException {
        File file;
        file = File.createTempFile("app", "tmp");
        file.deleteOnExit();
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            copyStream(in, out);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (out != null) {
                try {
                    out.close();
                } finally {
                    // ignore
                }
            }
        }
        return file;
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) >= 0) {
            out.write(buf, 0, len);
        }
    }

}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.cluster.ssh.util;

import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.SshAuth;
import com.sun.enterprise.config.serverbeans.SshConnector;
import com.sun.enterprise.universal.glassfish.TokenResolver;
import com.sun.enterprise.util.cluster.windows.process.WindowsCredentials;
import com.sun.enterprise.util.cluster.windows.process.WindowsException;
import com.sun.enterprise.util.cluster.windows.process.WindowsRemoteAsadmin;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.util.*;
import static com.sun.enterprise.util.StringUtils.ok;

/**
 * Put this ugly painful intricate parsing in one place to avoid code-bloat...
 * This class is guaranteed thread-safe and immutable
 * @author Byron Nevins
 */
public final class DcomInfo {
    private final WindowsCredentials credentials;
    private final Node node;
    private final String password;
    private final String host;
    private final String user;
    private final String windowsDomain;
    private final String remoteNodeRootDirectory;
    // CONFUSING PAIN POINT.  The "installdir" means the parent directory of glassfish
    // installroot means the glassfish dir.  E.g.
    // installdir == d:/glassfish4  and installroot == d:/glassfish4/glassfish
    //
    private final String remoteInstallRoot;
    private final String nadminPath;
    private final String nadminParentPath;

    public DcomInfo(Node theNode) throws WindowsException {
        // Create a resolver that can replace system properties in strings
        // System Properties can change at any time so do NOT cache this object
        TokenResolver resolver = new TokenResolver(
                new HashMap<String, String>((Map) (System.getProperties())));

        node = theNode;

        if (node == null)
            throw new WindowsException(
                    Strings.get("internal.error", "Node is null"));

        if (!isDcomNode(node))
            throw new WindowsException(Strings.get("not.dcom.node",
                    getNode().getName(), getNode().getType()));

        SshConnector conn = node.getSshConnector();
        if (conn == null)
            throw new WindowsException(Strings.get("no.password"));

        SshAuth auth = conn.getSshAuth();
        if (auth == null)
            throw new WindowsException(Strings.get("no.password"));

        String notFinal = auth.getPassword();
        if (!ok(notFinal))
            throw new WindowsException(Strings.get("no.password"));

        password = DcomUtils.resolvePassword(notFinal);

        notFinal = node.getNodeHost();
        if (!ok(notFinal))
            notFinal = conn.getSshHost();
        if (!ok(notFinal))
            throw new WindowsException(Strings.get("no.host"));
        host = resolver.resolve(notFinal);

        notFinal = auth.getUserName();
        if (!ok(notFinal))
            notFinal = System.getProperty("user.name");
        if (!ok(notFinal))
            throw new WindowsException(Strings.get("no.username"));
        user = resolver.resolve(notFinal);

        notFinal = node.getWindowsDomain();
        if (!ok(notFinal))
            notFinal = host;
        windowsDomain = resolver.resolve(notFinal);

        notFinal = node.getInstallDirUnixStyle();
        if (!ok(notFinal))
            throw new WindowsException(Strings.get("no.lib.dir"));

        if (!notFinal.endsWith("/"))
            notFinal += "/";

        notFinal += SystemPropertyConstants.getComponentName();
        remoteInstallRoot = StringUtils.quotePathIfNecessary(notFinal);
        notFinal += "/lib";
        notFinal = StringUtils.quotePathIfNecessary(notFinal);
        notFinal = notFinal.replace('/', '\\');
        nadminParentPath = notFinal;
        nadminPath = notFinal + "\\nadmin.bat";

        String notFinal2 = node.getNodeDirAbsolute();

        if (notFinal2 == null) {
            // no special nodedir -- use the defaults
            notFinal2 = remoteInstallRoot;  // e.g. "d:/glassfish4/glassfish"
            notFinal2 += "/nodes";
        }
        notFinal2 = notFinal2.replace('/', '\\');

        if (!notFinal2.endsWith("\\"))
            notFinal2 += '\\';

        remoteNodeRootDirectory = notFinal2 + node.getName();

        credentials = new WindowsCredentials(getHost(), getWindowsDomain(),
                getUser(), getPassword());
    }

    public String getRemoteNodeRootDirectory() {
        return remoteNodeRootDirectory;
    }

    public String getRemoteInstallRoot() {
        return remoteInstallRoot;
    }

    public String getNadminPath() {
        return nadminPath;
    }

    public String getNadminParentPath() {
        return nadminParentPath;
    }

    public WindowsCredentials getCredentials() {
        return credentials;
    }

    public Node getNode() {
        return node;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public String getUser() {
        return user;
    }

    public String getWindowsDomain() {
        return windowsDomain;
    }

    public WindowsRemoteAsadmin getAsadmin() {
        return new WindowsRemoteAsadmin(remoteInstallRoot, credentials);
    }

    private static boolean isDcomNode(Node node) {
        return "DCOM".equals(node.getType());
    }
}

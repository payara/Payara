/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

package fish.payara.micro.impl;

import fish.payara.micro.PayaraMicro;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RootDirLauncher {
    static final String BOOT_JAR_URL = "fish.payara.micro.BootJar";
    static final String ROOT_DIR_PATH = "fish.payara.micro.UnpackDir";
    private static final Logger LOGGER = Logger.getLogger("PayaraMicro");

    public static void main(String[] args) throws Exception {
        File bootJar = determineBootJar();
        String rootDir = bootJar.getParentFile().getAbsolutePath();
        System.setProperty(BOOT_JAR_URL, bootJar.toURI().toString());
        System.setProperty(ROOT_DIR_PATH, rootDir);
        PayaraMicroImpl.main(prepareArgs(args, rootDir));
    }

    private static String[] prepareArgs(String[] args, String rootDir) {
        String[] result = args;
        boolean containedRootDir = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--rootdir")) {
                LOGGER.warning("Overriding --rootdir with "+rootDir);
                args[i+1] = rootDir;
                containedRootDir = true;
            }
            if (args[i].equalsIgnoreCase("--addlibs")
                    || args[i].equalsIgnoreCase("--addjars")
                    || args[i].equals("--outputlauncher")) {
                LOGGER.log(Level.SEVERE, "Switch {0} cannot be used with launcher jar", args[i]);
                System.exit(-1);
            }
        }
        if (!containedRootDir) {
            result = Arrays.copyOf(args, args.length + 2);
            result[result.length - 2] = "--rootdir";
            result[result.length - 1] = rootDir;
        }
        return result;
    }

    private static File determineBootJar() {
        ProtectionDomain protectionDomain = PayaraMicro.class.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        if (codeSource != null) {
            try {
                URI sourceUri = codeSource.getLocation().toURI();
                return new File(sourceUri);
            } catch (RuntimeException | URISyntaxException e) {
                throw new IllegalStateException("Could not determine location of launcher jar", e);
            }
        } else {
            throw new IllegalStateException("Could not determine location of launcher.jar");
        }
    }
}

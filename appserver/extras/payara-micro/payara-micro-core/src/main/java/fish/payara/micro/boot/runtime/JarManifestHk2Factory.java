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

package fish.payara.micro.boot.runtime;

import com.sun.enterprise.module.ModuleDefinition;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.common_impl.AbstractFactory;
import com.sun.enterprise.module.common_impl.ModuleId;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.jar.JarFile;
import java.util.stream.Stream;

class JarManifestHk2Factory extends AbstractFactory {

    private final ClassLoader classLoader;
    private final JarManifestModuleRegistry modulesRegistry;

    static synchronized void initialize(ClassLoader cl, URI bootJar) throws IOException {
        // we need to get absolute paths of the jar our classpath is made of to make ClassPathModulesRegistry
        // reliably create modules
        URI[] locations = Stream.of(readClassPath(bootJar).split(" "))
                .map(bootJar::resolve)
                .toArray(URI[]::new);


        JarManifestHk2Factory factory = new JarManifestHk2Factory(cl, locations);
        Instance = factory;
        factory.modulesRegistry.initialize();
    }

    private static String readClassPath(URI bootJar) throws IOException {
        try (JarFile bootJarFile = new JarFile(new File(bootJar))) {
            return bootJarFile.getManifest().getMainAttributes().getValue("Class-Path");
        }
    }

    public JarManifestHk2Factory(ClassLoader cl, URI[] locations) throws IOException {
        this.classLoader = cl;
        this.modulesRegistry = new JarManifestModuleRegistry(cl, locations);
    }

    @Override
    public ModulesRegistry createModulesRegistry() {
        return modulesRegistry;
    }

    @Override
    public ModuleId createModuleId(String name, String version) {
        return new ModuleId(name);
    }

    @Override
    public ModuleId createModuleId(ModuleDefinition md) {
        return new ModuleId(md.getName());
    }
}

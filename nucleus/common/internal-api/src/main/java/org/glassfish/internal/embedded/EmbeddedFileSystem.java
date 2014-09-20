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

package org.glassfish.internal.embedded;

import org.glassfish.hk2.api.PreDestroy;

import java.io.File;
import java.util.logging.*;

/**
 * Abstraction for a virtual filesystem that be used by the server to store important files.
 *
 * @author Jerome Dochez
 */
public class EmbeddedFileSystem implements PreDestroy {

    /**
     * EmbeddedFileSystem builder class. Clients must use one these builder instances
     * to create an EmbeddedFileSystem instance.
     * 
     */
    public static class Builder {
        boolean autoDelete=false;
        boolean readOnly = true;
        boolean cookedMode=false;
        File configFile=null;
        File installRoot=null;
        File instanceRoot=null;

        // TODO : add some obvious file errors during the build()


        // todo : considering removing this call.

        /**
         * Sets the auto delete flag. If on, the embedded file system backing store will be
         * deleted once the embedded server is shutdown.
         *
         * @param b true to delete the instance root directory on server shutdown
         * @return itself
         */
        public Builder autoDelete(boolean b) {
            this.autoDelete = b;
            return this;
        }

        /**
         * Sets the location of the read-only domain.xml configuration file. The file can be named anything
         * but must have a valid domain.xml content. Any management operation will not be written to the
         * file.
         *
         * @param f location of the configuration file
         * @return itself
         */
        public Builder configurationFile(File f) {
            return configurationFile(f, true);

        }

        /**
         * Sets the location of the domain.xml configuration file. The file can be named anything
         * but must have a valid domain.xml content.
         *
         * @param f location of the configuration file
         * @param readOnly true if the file is readonly, false if management operations should be
         * persisted.
         * @return itself
         */
        public Builder configurationFile(File f, boolean readOnly) {
            this.configFile = f;
            this.readOnly = readOnly;
            return this;

        }

        /**
         * Sets the installation directory, using the installation module directory content
         * as the application server classpath. The classloader used to load this class will
         * be the parent class loader to the embedded server classloader  which will use the
         * modules located in the passed installation directory.
         *
         * @param f location of the glassfish installation
         * @return itself
         */
        public Builder installRoot(File f) {
            return installRoot(f, false);
        }

        // todo : replace cookedMode to...
        // todo :

        /**
         * Sets the installation directory and direct whether or not to use the installation's
         * module directory content in the embedded server classpath. If cookMode is on, the
         * embedded server will be loaded using the classloader used to load this class.
         *
         * @param f location of the installation
         * @param cookedMode true to use this class classloader, false to create a new classloader
         * with the installation modules directory content.
         * @return itself
         */
        public Builder installRoot(File f, boolean cookedMode) {
            this.installRoot = f;
            this.cookedMode = cookedMode;
            return this;
        }

        /**
         * Sets the location of the domain directory used to load this instance of the
         * embedded server.
         * @param f location of the domain directory
         * @return itself
         */
        public Builder instanceRoot(File f) {
            this.instanceRoot=f;
            if (this.configFile==null) {
                File tmp = new File(instanceRoot, "config");
                configFile = new File(tmp, "domain.xml");
                if (!configFile.exists()) {
                    configFile = null;
                }
            }
            return this;
        }

        /**
         * Builds a configured embedded file system instance that can be used to configure
         * an embedded server.
         *
         * @return an immutable configured instance of an EmbeddedFileSystem
         */
        public EmbeddedFileSystem build() {
            return new EmbeddedFileSystem(this);
        }

        @Override
        public String toString() {
            return "EmbeddedFileSystem>>installRoot = " + installRoot + ", instanceRoot=" +
                    instanceRoot + ",configFile=" + configFile + ",autoDelete=" + autoDelete;
        }

    }

    public final boolean autoDelete;
    public final boolean readOnlyConfigFile;
    public final boolean cookedMode;
    public final File installRoot;
    public final File instanceRoot;
    public final File configFile;

    private EmbeddedFileSystem(Builder builder) {
        autoDelete = builder.autoDelete;
        readOnlyConfigFile = builder.readOnly;
        installRoot = builder.installRoot;
        instanceRoot = builder.instanceRoot;
        configFile = builder.configFile;
        cookedMode = builder.cookedMode;
    }

    public void preDestroy() {
        Logger.getAnonymousLogger().finer("delete " + instanceRoot + " = " + autoDelete);
        if (autoDelete && instanceRoot != null) {
            // recursively delete instanceRoot directory
            Logger.getAnonymousLogger().finer("Deleting recursively" + instanceRoot);
            deleteAll(instanceRoot);
        }

    }

    private void deleteAll(File f) {
        for (File child : f.listFiles()) {
            if (child.isDirectory()) {
                deleteAll(child);
            } else {
                child.delete();
            }
        }
        f.delete();
    }

    void copy(Builder that) {
        that.autoDelete(autoDelete);
        that.configurationFile(configFile, readOnlyConfigFile);
        that.installRoot(installRoot, cookedMode);
        that.instanceRoot(instanceRoot);
    }

}

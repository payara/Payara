/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tests.kernel.deployment.container;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.deployment.DeploymentContext;

import java.lang.annotation.Annotation;
import java.util.logging.Logger;
import java.util.Map;
import java.io.IOException;

import com.sun.enterprise.module.Module;

/**
 * Created by IntelliJ IDEA.
 * User: dochez
 * Date: Mar 12, 2009
 * Time: 9:20:37 AM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class FakeSniffer implements Sniffer {

    public boolean handles(ReadableArchive source) {
        // I handle everything
        return true;
    }

    public boolean handles(DeploymentContext context) {
        // I handle everything
        return true;
    }

    public String[] getURLPatterns() {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Class<? extends Annotation>[] getAnnotationTypes() {
        return null;
    }

    public String[] getAnnotationNames(DeploymentContext context) {
        return null;
    }

    public String getModuleType() {
        return "fake";
    }

    public Module[] setup(String containerHome, Logger logger) throws IOException {
        return null;
    }

    public void tearDown() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String[] getContainersNames() {
        return new String[] { "FakeContainer" };
    }

    public boolean isUserVisible() {
        return false;
    }

   public boolean isJavaEE() {
        return false;
    }

    public Map<String, String> getDeploymentConfigurations(ReadableArchive source) throws IOException {
        return null;
    }

    public String[] getIncompatibleSnifferTypes() {
        return new String[0];
    }

    public boolean supportsArchiveType(ArchiveType archiveType) {
        return true;
    }

}

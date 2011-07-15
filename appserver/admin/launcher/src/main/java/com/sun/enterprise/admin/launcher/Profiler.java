/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2011 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.launcher;

import com.sun.enterprise.universal.glassfish.GFLauncherUtils;
import java.io.*;
import java.util.*;

/**
 * This class wraps the profiler element in java-config
 * Note that the V2 dtd says that there can be generic property elements in the
 * profiler element.  I don't know why anyone would use them -- but if they do I 
 * turn it into a "-D" System Property
 * @author Byron Nevins
 */
public class Profiler {
    Map<String, String> config;
    List<String> jvmOptions;

    Profiler(Map<String, String> config, List<String> jvmOptions, Map<String, String> sysProps) {
        this.config = config;
        enabled = Boolean.parseBoolean(this.config.get("enabled"));
        this.jvmOptions = jvmOptions;
        jvmOptions.addAll(getPropertiesAsJvmOptions(sysProps));
    }

    List<String> getJvmOptions() {
        if (!enabled) {
            return Collections.emptyList();
        }
        return jvmOptions;
    }

    Map<String, String> getConfig() {
        if (!enabled) {
            return Collections.emptyMap();
        }
        return config;
    }

    List<File> getClasspath() {
        if (!enabled) {
            return Collections.emptyList();
        }

        String cp = config.get("classpath");

        if (GFLauncherUtils.ok(cp)) {
            return GFLauncherUtils.stringToFiles(cp);
        }
        else {
            return Collections.emptyList();
        }
    }

    List<File> getNativePath() {
        if (!enabled) {
            return Collections.emptyList();
        }

        String cp = config.get("native-library-path");

        if (GFLauncherUtils.ok(cp)) {
            return GFLauncherUtils.stringToFiles(cp);
        }
        else {
            return Collections.emptyList();
        }
    }

    boolean isEnabled() {
        return enabled;
    }

    private List<String> getPropertiesAsJvmOptions(Map<String, String> props) {
        List<String> list = new ArrayList<String>();
        Set<Map.Entry<String, String>> entries = props.entrySet();

        for (Map.Entry<String, String> entry : entries) {
            String name = entry.getKey();
            String value = entry.getValue();

            if (value != null)
                list.add("-D" + name + "=" + value);
            else
                list.add("-D" + name);
        }
        return list;
    }
    private boolean enabled;
}

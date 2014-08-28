/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 * @author anilam
 */

package org.glassfish.admingui.util;

import com.sun.webui.jsf.model.Option;
import javax.faces.model.SelectItem;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

/**
 *
 * @author anilam
 */
/**
 * TODO:  Class.forName can cause problems under OSGi
 *        SUN_OPTION_CLASS = Class.forName("com.sun.webui.jsf.model.Option");
 * for the time being, we have to ensure that this class stays in the 'core' which is part of the war file so any plugin
 * modul can access it.
 * 
 */
public class SunOptionUtil {

    public static SelectItem[] getOptions(String[] values) {
        if (values == null) {
            SelectItem[] options = (SelectItem[]) Array.newInstance(SUN_OPTION_CLASS, 0);
            return options;
        }
        SelectItem[] options =
                (SelectItem[]) Array.newInstance(SUN_OPTION_CLASS, values.length);
        for (int i = 0; i < values.length; i++) {
            SelectItem option = getSunOption(values[i], values[i]);
            options[i] = option;
        }
        return options;
    }

    public static Option[] getOptionsArray(String[] values) {
        Option[] options =
                (Option[]) Array.newInstance(SUN_OPTION_CLASS, values.length);
        for (int i = 0; i < values.length; i++) {
            Option option = getOption(values[i], values[i]);
            options[i] = option;
        }
        return options;
    }

    public static Option getOption(String value, String label) {
        try {
            return (Option) SUN_OPTION_CONSTRUCTOR.newInstance(value, label);
        } catch (Exception ex) {
            return null;
        }
    }

    public static SelectItem[] getOptions(String[] values, String[] labels) {
        SelectItem[] options =
                (SelectItem[]) Array.newInstance(SUN_OPTION_CLASS, values.length);
        for (int i = 0; i < values.length; i++) {
            SelectItem option = getSunOption(values[i], labels[i]);
            options[i] = option;
        }
        return options;
    }

    public static SelectItem[] getModOptions(String[] values) {
        int size = (values == null) ? 1 : values.length + 1;
        SelectItem[] options =
                (SelectItem[]) Array.newInstance(SUN_OPTION_CLASS, size);
        options[0] = getSunOption("", "");
        for (int i = 0; i < size-1; i++) {
            SelectItem option = getSunOption(values[i], values[i]);
            options[i + 1] = option;
        }
        return options;
    }

    public static SelectItem getSunOption(String value, String label) {
        try {
            return (SelectItem) SUN_OPTION_CONSTRUCTOR.newInstance(value, label);
        } catch (Exception ex) {
            return null;
        }
    }

    private static Class SUN_OPTION_CLASS = null;
    private static Constructor SUN_OPTION_CONSTRUCTOR = null;
    

    static {
        try {
            // TODO: Class.forName can cause problems under OSGi
            SUN_OPTION_CLASS = Class.forName("com.sun.webui.jsf.model.Option");
            SUN_OPTION_CONSTRUCTOR = SUN_OPTION_CLASS.getConstructor(new Class[]{Object.class, String.class});
        } catch (Exception ex) {
            // Ignore exception here, NPE will be thrown when attempting to
            // use SUN_OPTION_CONSTRUCTOR.
        }
    }
}

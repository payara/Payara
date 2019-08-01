/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.admin.report;

import com.sun.enterprise.universal.collections.ManifestUtils;
import com.sun.enterprise.util.StringUtils;
import java.io.UnsupportedEncodingException;
import java.util.*;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.Map;

/**
 * Action reporter to a manifest file
 * @author Jerome Dochez
 */
@Service(name = "hk2-agent")
@PerLookup
public class PropsFileActionReporter extends ActionReporter {

    @Override
    public void setMessage(String message) {
        super.setMessage(ManifestUtils.encode(message));
    }

    @Override
    public void writeReport(OutputStream os) throws IOException {

        Manifest out = new Manifest();
        Attributes mainAttr = out.getMainAttributes();
        mainAttr.put(Attributes.Name.SIGNATURE_VERSION, "1.0");
        mainAttr.putValue("exit-code", exitCode.toString());
        mainAttr.putValue("use-main-children-attribute", Boolean.toString(useMainChildrenAttr));

        if (exitCode == ExitCode.FAILURE) {
            writeCause(mainAttr);
        }

        writeReport(null, topMessage, out, mainAttr);
        out.write(os);
    }

    public void writeReport(String prefix, MessagePart part, Manifest m, Attributes attr) {
        //attr.putValue("message", part.getMessage());
        StringBuilder sb = new StringBuilder();
        getCombinedMessages(this, sb);
        attr.putValue("message", sb.toString());
        if (part.getProps().size() > 0) {
            String keys = null;
            for (Map.Entry entry : part.getProps().entrySet()) {
                String key = fixKey(entry.getKey().toString());
                keys = (keys == null ? key : keys + ";" + key);
                attr.putValue(key + "_name", entry.getKey().toString());
                attr.putValue(key + "_value", ManifestUtils.encode(entry.getValue().toString()));
            }

            attr.putValue("keys", keys);
        }
        if (part.getChildren().size() > 0) {
            attr.putValue("children-type", part.getChildrenType());
            attr.putValue("use-main-children-attribute", "true");
            StringBuilder keys = null;
            for (MessagePart child : part.getChildren()) {
                // need to URL encode a ';' as %3B because it is used as a
                // delimiter
                String cm = child.getMessage();
                if (cm != null) {
                    try {
                        cm = URLEncoder.encode(cm, "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        // ignore - leave cm as it is
                    }
                }
                String newPrefix = (prefix == null ? cm : prefix + "." + cm);

                if(keys == null)
                    keys = new StringBuilder();
                else
                    keys.append(';');

                if(newPrefix != null)
                    keys.append(newPrefix);

                Attributes childAttr = new Attributes();
                m.getEntries().put(newPrefix, childAttr);
                writeReport(newPrefix, child, m, childAttr);
            }
            attr.putValue("children", keys.toString());
        }
    }

    private void writeCause(Attributes mainAttr) {
        Throwable t = getFailureCause();

        if (t == null) {
            return;
        }

        String causeMessage = t.toString();
        mainAttr.putValue("cause", causeMessage);
    }

    /* Issue 5918 Keep output sorted. If set to true ManifestManager will grab
     * "children" from main attributes. "children" is in original order of
     * output set by server-side
     */
    public void useMainChildrenAttribute(boolean useMainChildrenAttr) {
        this.useMainChildrenAttr = useMainChildrenAttr;
    }

    private String fixKey(String key) {
        // take a look at the  javadoc -- java.util.jar.Attributes.Name
        // < 70 chars in length and [a-zA-Z0-9_-]
        // then you can see in the code above that we take the key and add
        // _value to it.  So we simply hack it off at 63 characters.
        // We also replace "bad" characters with "_".  Note that asadmin will
        // display the correct real name.

        if (!StringUtils.ok(key)) {
            return key; // GIGO!
        }
        StringBuilder sb = new StringBuilder();
        boolean wasChanged = false;
        int len = key.length();

        if (len > LONGEST) {
            len = LONGEST;
            wasChanged = true;
        }

        for (int i = 0; i < len; i++) {
            char c = key.charAt(i);

            if (!isValid(c)) {
                wasChanged = true;
                sb.append('_');
            } else {
                sb.append(c);
            }
        }

        if (!wasChanged) {
            return key;
        }

        String fixedName = sb.toString();

        if (fixedNames.add(fixedName)) {
            return fixedName;
        }

        // perhaps they are using huge long names that differ just at the end?
        return doubleFixName(fixedName);
    }

    private String doubleFixName(String s) {
        // Yes, this is a nightmare!
        int len = s.length();

        if (len > LONGEST - 5) {
            s = s.substring(0, LONGEST - 5);
        }

        for (int i = 0; i < 10000; i++) {
            String num = String.format("%05d", i);
            String ret = s + num;

            if (fixedNames.add(ret)) {
                return ret;
            }
        }
        // Wow!!!
        throw new IllegalArgumentException("Could not come up with a unique name after 10000 attempts!!");
    }

    private static boolean isValid(char c) {
        return isAlpha(c) || isDigit(c) || c == '_' || c == '-';
    }

    private static boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean useMainChildrenAttr = false;
    private Set<String> fixedNames = new TreeSet<String>();
    private static final int LONGEST = 62;
}

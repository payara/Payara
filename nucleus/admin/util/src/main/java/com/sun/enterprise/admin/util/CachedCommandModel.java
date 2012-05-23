/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.util;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandModel;

/** Stores ETeg with command model.
 *
 * @author mmares
 */
public class CachedCommandModel extends CommandModelData {
    
    private String eTag;
    
    public CachedCommandModel(String name) {
        super(name);
    }
    
    public CachedCommandModel(String name, String eTag) {
        super(name);
        this.eTag = eTag;
    }

    public String getETag() {
        if (eTag == null) {
            eTag = computeETag(this);
        }
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }
    
    public static String computeETag(CommandModel cm) {
        try {
            Charset chs = Charset.forName("UTF-16");
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (cm.getCommandName() != null) {
                md.update(cm.getCommandName().getBytes(chs));
            }
            md.update((cm.unknownOptionsAreOperands()) ? (byte) 1 : (byte) 0);
            for (ParamModel paramModel : cm.getParameters()) {
                if (paramModel.getName() != null) {
                    md.update(paramModel.getName().getBytes(chs));
                }
                if (paramModel.getClass() != null) {
                    md.update(paramModel.getClass().getCanonicalName().getBytes(chs));
                }
                Param param = paramModel.getParam();
                if (param.optional()) {
                    if (param.obsolete()) {
                        md.update((byte) 4);
                    } else {
                        md.update((byte) 3);
                    }
                } else {
                    if (param.obsolete()) {
                        md.update((byte) 2);
                    } else {
                        md.update((byte) 1);
                    }
                }
                if (param.defaultValue() != null && !param.defaultValue().isEmpty()) {
                    md.update(param.defaultValue().getBytes(chs));
                }
                if (param.shortName() != null && !param.shortName().isEmpty()) {
                    md.update(param.shortName().getBytes(chs));
                }
                if (param.alias() != null && !param.shortName().isEmpty()) {
                    md.update(param.alias().getBytes(chs));
                }
            }
            return DatatypeConverter.printBase64Binary(md.digest());
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }
    
}

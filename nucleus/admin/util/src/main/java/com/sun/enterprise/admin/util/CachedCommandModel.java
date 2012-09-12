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
    private String usage;
    private boolean addedUploadOption = false;
    
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
    
    private static int strLen(String str) {
        if (str == null) {
            return 0;
        } else {
            return str.length();
        }
    }

    public String geteTag() {
        return eTag;
    }

    public void seteTag(String eTag) {
        this.eTag = eTag;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public boolean isAddedUploadOption() {
        return addedUploadOption;
    }

    public void setAddedUploadOption(boolean addedUploadOption) {
        this.addedUploadOption = addedUploadOption;
    }
    
    //TODO: This is very light algorithm. But have allways problem to find something - keep searching
    public static String computeETag(CommandModel cm) {
        if (cm instanceof CachedCommandModel) {
            String result = ((CachedCommandModel) cm).eTag;
            if (result != null && !result.isEmpty()) {
                return ((CachedCommandModel) cm).eTag;
            }
        }
        StringBuilder tag = new StringBuilder();
        tag.append("v1"); //Just symbol for loaders
        if (cm.supportsProgress()) {
            tag.append('p');
        }
        tag.append(strLen(cm.getCommandName()));
        if (cm.unknownOptionsAreOperands()) {
            tag.append('y');
        }
        if (cm.getParameters() != null) {
            int size = cm.getParameters().size();
            int totalStrings = 0;
            int totalAliasStrings = 0;
            int totalOptional = 0;
            boolean existPrimaty = false;
            boolean isPrimaryMultiple = false;
            int withShortName = 0;
            int totalObsoletes = 0;
            for (ParamModel paramModel : cm.getParameters()) {
                if ("upload".equals(paramModel.getName())) {
                    //Skip because potentialy generated on client side
                    size--;
                    continue;
                }
                totalStrings += strLen(paramModel.getName());
                Param param = paramModel.getParam();
                if (param.multiple()) {
                    isPrimaryMultiple = true;
                }
                if (param.optional()) {
                    totalOptional++;
                }
                if (param.primary()) {
                    existPrimaty = true;
                    continue;
                }
                if (param.obsolete()) {
                    totalObsoletes++;
                }
                if (param.shortName() != null && !param.shortName().isEmpty()) {
                    withShortName++;
                }
                totalAliasStrings += strLen(param.alias());
            }
            tag.append(size);
            tag.append(totalStrings);
            tag.append(totalAliasStrings);
            tag.append(totalOptional);
            tag.append(withShortName);
            tag.append(totalObsoletes);
            tag.append(totalStrings);
            if (existPrimaty) {
                tag.append(isPrimaryMultiple ? 'b' : 'a');
            } else {
                tag.append(isPrimaryMultiple ? 'c' : 'd');
            }
        }
        return tag.toString();
    }
    
    
//    public static String computeETag(CommandModel cm) {
//        if (cm instanceof CachedCommandModel) {
//            String result = ((CachedCommandModel) cm).eTag;
//            if (result != null && !result.isEmpty()) {
//                return ((CachedCommandModel) cm).eTag;
//            }
//        }
//        try {
//            Charset chs = Charset.forName("UTF-16");
//            MessageDigest md = MessageDigest.getInstance("MD5");
//            if (cm.getCommandName() != null) {
//                md.update(cm.getCommandName().getBytes(chs));
//            }
//            md.update((cm.unknownOptionsAreOperands()) ? (byte) 1 : (byte) 0);
//            for (ParamModel paramModel : cm.getParameters()) {
//                if (paramModel.getName() != null) {
//                    md.update(paramModel.getName().getBytes(chs));
//                }
////                if (paramModel.getClass() != null) {
////                    md.update(paramModel.getClass().getCanonicalName().getBytes(chs));
////                }
//                Param param = paramModel.getParam();
//                if (param.optional()) {
//                    md.update((byte) 9);
//                }
//                if (param.obsolete()) {
//                    md.update((byte) 7);
//                }
////                if (param.defaultValue() != null && !param.defaultValue().isEmpty()) {
////                    md.update(param.defaultValue().getBytes(chs));
////                }
//                if (param.shortName() != null && !param.shortName().isEmpty()) {
//                    md.update(param.shortName().getBytes(chs));
//                }
//                if (param.alias() != null && !param.shortName().isEmpty()) {
//                    md.update(param.alias().getBytes(chs));
//                }
//                if (param.primary()) {
//                    md.update((byte) 13);
//                }
//                if (param.multiple()) {
//                    md.update((byte) 12);
//                }
//            }
//            return DatatypeConverter.printBase64Binary(md.digest());
//        } catch (NoSuchAlgorithmException ex) {
//            return null;
//        }
//    }
    
}

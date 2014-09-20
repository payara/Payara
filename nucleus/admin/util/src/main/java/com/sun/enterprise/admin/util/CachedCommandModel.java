/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.StringUtils;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
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

    public static String computeETag(CommandModel cm) {
        if (cm instanceof CachedCommandModel) {
            String result = ((CachedCommandModel) cm).eTag;
            if (result != null && !result.isEmpty()) {
                return ((CachedCommandModel) cm).eTag;
            }
        }
        StringBuilder tag = new StringBuilder();
        tag.append(cm.getCommandName());
        if (cm.isManagedJob()) {
            tag.append('m');
        }
        if (cm.unknownOptionsAreOperands()) {
            tag.append('o');
        }
        if (cm.getParameters() != null) {
            //sort
            SortedSet<ParamModel> tree = new TreeSet<ParamModel>(new Comparator<ParamModel>() {
                    @Override
                    public int compare(ParamModel o1, ParamModel o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
            for (ParamModel paramModel : cm.getParameters()) {
                tree.add(paramModel);
            }
            //print
            for (ParamModel pm : tree) {
                if ("upload".equals(pm.getName())) {
                    continue;
                }
                tag.append(pm.getName());
                if (pm.getClass() != null) {
                    tag.append(pm.getClass().getCanonicalName());
                }
                Param param = pm.getParam();
                if (param.multiple()) {
                    tag.append('M');
                }
                if (param.optional()) {
                    tag.append('P');
                }
                if (param.primary()) {
                    tag.append('1');
                }
                if (param.obsolete()) {
                    tag.append('O');
                }
                if (param.shortName() != null && !param.shortName().isEmpty()) {
                    tag.append(param.shortName());
                }
                if (param.alias() != null && !param.alias().isEmpty()) {
                    tag.append(param.alias());
                }
                if (StringUtils.ok(param.defaultValue())) {
                    tag.append(param.defaultValue());
                    tag.append("A"); //TODO: removeit
                }
            }
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(tag.toString().getBytes("UTF-8"));
            return DatatypeConverter.printBase64Binary(md.digest());
        } catch (Exception ex) {
            return "v2" + tag.toString();
        }
    }

}

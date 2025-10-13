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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.admin.util.cache;

import com.sun.enterprise.admin.util.CachedCommandModel;
import com.sun.enterprise.admin.util.CommandModelData;
import com.sun.enterprise.admin.util.CommandModelData.ParamModelData;
import java.io.*;
import java.nio.charset.Charset;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandModel;
import org.jvnet.hk2.annotations.Service;

/**
 * Works with {@link com.sun.enterprise.admin.util.CachedCommandModel} and
 * {@link com.sun.enterprise.admin.util.CommandModelData).<br/>
 * This is <i>hand made</i> implementation which is focused on human readability
 * and fastness.
 *
 * @author mmares
 */
//It is ugly hand made code but fast and with readable result. Maybe rewrite to some JAX-B based on performance result.
@Service
public class CommandModelDataProvider implements DataProvider {
    private static final String ADDEDUPLOADOPTIONS_ELEMENT = "added-upload-options";
    private static final String ALIAS_ELEMENT = "alias";
    private static final String CLASS_ELEMENT = "class";
    private static final String DEFAULT_VALUE_ELEMENT = "default-value";
    private static final String PROMPT_ELEMENT = "prompt";
    private static final String PROMPT_AGAIN_ELEMENT = "prompt-again";
    private static final String ETAG_ELEMENT = "e-tag";
    private static final String NAME_ELEMENT = "name";
    private static final String OBSOLETE_ELEMENT = "obsolete";
    private static final String OPTIONAL_ELEMENT = "optional";
    private static final String PASSWORD_ELEMENT = "password";
    private static final String SHORTNAME_ELEMENT = "short-name";
    private static final String UNKNOWN_ARE_OPERANDS_ELEMENT = "unknown-are-operands";
    private static final String ROOT_ELEMENT = "command-model";
    private static final String PRIMARY_ELEMENT = "primary";
    private static final String MULTIPLE_ELEMENT = "multiple";
    private static final String USAGE_ELEMENT = "usage";

    private Charset charset;

    public CommandModelDataProvider() {
        try {
            charset = Charset.forName("UTF-8");
        } catch (Exception ex) {
            charset = Charset.defaultCharset();
        }
    }

    @Override
    public boolean accept(Class clazz) {
        return clazz == CommandModel.class ||
                clazz == CachedCommandModel.class ||
                clazz == CommandModelData.class;
    }

    @Override
    public void writeToStream(Object o, OutputStream stream) throws IOException {
        if (o == null) {
            return;
        }
        writeToStreamSimpleFormat((CommandModel) o, stream);
    }

    /** Super simple format possible because there can't be any problematic
     * symbol like EOL in attributes.
     *
     * @throws IOException
     */
    public void writeToStreamSimpleFormat(CommandModel cm, OutputStream stream) throws IOException {
        if (cm == null) {
            return;
        }

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(stream, charset))) {

            //command name
            String str = cm.getCommandName();
            if (str != null && !str.isEmpty()) {
                bw.write(ROOT_ELEMENT);
                bw.write(": ");
                bw.write(str);
                bw.newLine();
            }
            //ETag
            bw.write(ETAG_ELEMENT);
            bw.write(": ");
            bw.write(CachedCommandModel.computeETag(cm));
            bw.newLine();
            //unknown are operands
            if (cm.unknownOptionsAreOperands()) {
                bw.write(UNKNOWN_ARE_OPERANDS_ELEMENT);
                bw.write(": true");
                bw.newLine();
            }
            //CachedCommandModel specific staff
            if (cm instanceof CachedCommandModel) {
                CachedCommandModel ccm = (CachedCommandModel) cm;
                //unknown are operands
                if (ccm.isAddedUploadOption()) {
                    bw.write(ADDEDUPLOADOPTIONS_ELEMENT);
                    bw.write(": true");
                    bw.newLine();
                }
                //usage
                str = ccm.getUsage();
                if (str != null && !str.isEmpty()) {
                    bw.write(USAGE_ELEMENT);
                    bw.write(": ");
                    bw.write(escapeEndLines(str));
                    bw.newLine();
                }
            }
            //Parameters
            for (CommandModel.ParamModel paramModel : cm.getParameters()) {
                bw.newLine();
                //parameter / name
                bw.write(NAME_ELEMENT);
                bw.write(": ");
                bw.write(paramModel.getName());
                bw.newLine();
                //parameter / class
                if (paramModel.getType() != null) {
                    bw.write(CLASS_ELEMENT);
                    bw.write(": ");
                    bw.write(paramModel.getType().getName());
                    bw.newLine();
                }
                Param param = paramModel.getParam();
                //parameter / shortName
                str = param.shortName();
                if (str != null && !str.isEmpty()) {
                    bw.write(SHORTNAME_ELEMENT);
                    bw.write(": ");
                    bw.write(str);
                    bw.newLine();
                }
                //parameter / alias
                str = param.alias();
                if (str != null && !str.isEmpty()) {
                    bw.write(ALIAS_ELEMENT);
                    bw.write(": ");
                    bw.write(str);
                    bw.newLine();
                }
                //parameter / optional
                if (param.optional()) {
                    bw.write(OPTIONAL_ELEMENT);
                    bw.write(": true");
                    bw.newLine();
                }
                //parameter / obsolete
                if (param.obsolete()) {
                    bw.write(OBSOLETE_ELEMENT);
                    bw.write(": true");
                    bw.newLine();
                }
                //parameter / defaultValue
                str = param.defaultValue();
                if (str != null && !str.isEmpty()) {
                    bw.write(DEFAULT_VALUE_ELEMENT);
                    bw.write(": ");
                    bw.write(str);
                    bw.newLine();
                }
                //parameter / primary
                if (param.primary()) {
                    bw.write(PRIMARY_ELEMENT);
                    bw.write(": true");
                    bw.newLine();
                }
                //parameter / multiple
                if (param.multiple()) {
                    bw.write(MULTIPLE_ELEMENT);
                    bw.write(": true");
                    bw.newLine();
                }
                //parameter / password
                if (param.password()) {
                    bw.write(PASSWORD_ELEMENT);
                    bw.write(": true");
                    bw.newLine();
                }
                //parameter / prompt
                if (paramModel instanceof ParamModelData) {
                    str = ((ParamModelData) paramModel).getPrompt();
                    if (str != null && !str.isEmpty()) {
                        bw.write(PROMPT_ELEMENT);
                        bw.write(": ");
                        bw.write(escapeEndLines(str));
                        bw.newLine();
                    }
                    str = ((ParamModelData) paramModel).getPromptAgain();
                    if (str != null && !str.isEmpty()) {
                        bw.write(PROMPT_AGAIN_ELEMENT);
                        bw.write(": ");
                        bw.write(escapeEndLines(str));
                        bw.newLine();
                    }
                }
            }
        }
    }

    private String escapeEndLines(String str) {
        if (str == null) {
            return null;
        }
        return str.replace("\n", "${NL}").replace("\r", "${RC}");
    }

    private String resolveEndLines(String str) {
        if (str == null) {
            return null;
        }
        return str.replace("${NL}", "\n").replace("${RC}", "\r");
    }

    @Override
    public Object toInstance(InputStream stream, Class clazz) throws IOException {
        return toInstanceSimpleFormat(stream);
    }

    private CommandModel toInstanceSimpleFormat(InputStream stream) throws IOException {
        CachedCommandModel result = null;
        boolean inParam = false;
        String name = null;
        String eTag = null;
        boolean unknownAreOperands = false;
        String usage = null;
        boolean addedUploadOption = false;
        String pName = null;
        Class pCls = null;
        boolean pOptional = false;
        String pDefaultValue = null;
        String pShortName = null;
        boolean pObsolete = false;
        String pAlias = null;
        boolean pPrimary = false;
        boolean pMultiple = false;
        boolean pPassword = false;
        String pPrompt = null;
        String pPromptAgain = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int ind = line.indexOf(':');
                if (ind <= 0) {
                    continue;
                }
                String key = line.substring(0, ind);
                String value = line.substring(ind + 1).trim();
                
                if (inParam) {
                    switch (key) {
                        case NAME_ELEMENT:
                            //Add before parameter
                            CommandModelData.ParamModelData pmd =
                                    new CommandModelData.ParamModelData(pName,
                                            pCls, pOptional, pDefaultValue, pShortName,
                                            pObsolete, pAlias);
                            pmd.param._primary = pPrimary;
                            pmd.param._multiple = pMultiple;
                            pmd.param._password = pPassword;
                            pmd.prompt = pPrompt;
                            pmd.promptAgain = pPromptAgain;
                            result.add(pmd);
                            //Reset values
                            pCls = null;
                            pOptional = false;
                            pDefaultValue = null;
                            pShortName = null;
                            pObsolete = false;
                            pAlias = null;
                            pPrimary = false;
                            pMultiple = false;
                            pPassword = false;
                            pPrompt = null;
                            pPromptAgain = null;
                            //New param
                            pName = value;
                            break;
                        case CLASS_ELEMENT:
                            if (!value.isEmpty()) {
                                try {
                                    pCls = Class.forName(value);
                                } catch (Exception ex) {
                                }
                            }   break;
                        case OPTIONAL_ELEMENT:
                            pOptional = value.startsWith("t");
                            break;
                        case DEFAULT_VALUE_ELEMENT:
                            pDefaultValue = value;
                            break;
                        case SHORTNAME_ELEMENT:
                            pShortName = value;
                            break;
                        case OBSOLETE_ELEMENT:
                            pObsolete = value.startsWith("t");
                            break;
                        case ALIAS_ELEMENT:
                            pAlias = value;
                            break;
                        case PRIMARY_ELEMENT:
                            pPrimary = value.startsWith("t");
                            break;
                        case MULTIPLE_ELEMENT:
                            pMultiple = value.startsWith("t");
                            break;
                        case PASSWORD_ELEMENT:
                            pPassword = value.startsWith("t");
                            break;
                        case PROMPT_ELEMENT:
                            pPrompt = resolveEndLines(value);
                            break;
                        case PROMPT_AGAIN_ELEMENT:
                            pPromptAgain = resolveEndLines(value);
                            break;
                        default:
                            break;
                    }
                } else {
                    switch (key) {
                        case ROOT_ELEMENT:
                            name = value;
                            break;
                        case ETAG_ELEMENT:
                            eTag = value;
                            break;
                        case UNKNOWN_ARE_OPERANDS_ELEMENT:
                            unknownAreOperands = value.startsWith("t");
                            break;
                        case ADDEDUPLOADOPTIONS_ELEMENT:
                            addedUploadOption = value.startsWith("t");
                            break;
                        case USAGE_ELEMENT:
                            usage = resolveEndLines(value);
                            break;
                        case NAME_ELEMENT:
                            //Create base
                            result = new CachedCommandModel(name, eTag);
                            result.dashOk = unknownAreOperands;
                            result.setUsage(usage);
                            result.setAddedUploadOption(addedUploadOption);
                            //Continue in params
                            inParam = true;
                            pName = value;
                            break;
                        default:
                            break;
                    }
                }
            }
            if (inParam) {
                //Add parameter
                CommandModelData.ParamModelData pmd =
                        new CommandModelData.ParamModelData(pName,
                        pCls, pOptional, pDefaultValue, pShortName,
                        pObsolete, pAlias);
                pmd.param._primary = pPrimary;
                pmd.param._multiple = pMultiple;
                pmd.param._password = pPassword;
                pmd.prompt = pPrompt;
                pmd.promptAgain = pPromptAgain;
                result.add(pmd);
            } else if (result == null && name != null && !name.isEmpty()) {
                result = new CachedCommandModel(name, eTag);
                result.dashOk = unknownAreOperands;
                result.setUsage(usage);
                result.setAddedUploadOption(addedUploadOption);
            }
        }
        return result;
    }


}

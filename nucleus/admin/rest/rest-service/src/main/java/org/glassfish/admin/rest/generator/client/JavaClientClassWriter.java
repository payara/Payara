/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.generator.client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.logging.Level;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.RestLogging;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.admin.rest.client.RestClientBase;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.CommandModel.ParamModel;
import org.jvnet.hk2.config.ConfigModel;

/**
 *
 * @author jdlee
 */
public class JavaClientClassWriter implements ClientClassWriter {
    private String className;
    private BufferedWriter source;

    private static final String TMPL_CLASS_HEADER =
            "package " + Constants.CLIENT_JAVA_PACKAGE +";\n" +
            "import java.util.HashMap;\n" +
            "import java.util.Map;\n" +
            "import javax.ws.rs.client.Client;\n\n" +
            "public class CLASSNAME extends RestClientBase {\n";
    private static final String TMPL_CTOR_DOMAIN =
            "    private RestClient parent;\n" +
            "    public CLASSNAME (RestClient parent) {\n" +
            "        super(parent.client, null);\n" +
            "        this.parent = parent;\n" +
            "    }\n\n";
    private static final String TMPL_CTOR_OTHER_WITH_KEY = 
            "    private String name;\n" +
            "    protected CLASSNAME (Client c, RestClientBase p, String name) {\n" +
            "        super(c, p);\n" +
            "        this.name = name;\n" +
            "    }\n\n";
    private static final String TMPL_CTOR_OTHER_NO_KEY =
            "    protected  CLASSNAME (Client c, RestClientBase p) {\n" +
            "        super(c,p);\n" +
            "    }\n\n";
    private static final String TMPL_GET_REST_URL = // TODO: Test this code heavily
            "    @Override\n" +
            "    protected String getRestUrl() {\n" +
            "        return super.getRestUrl()HASKEY;\n" +
            "    }\n\n";
    private static final String TMPL_CTOR_SIMPLE =
            "package " + Constants.CLIENT_JAVA_PACKAGE +";\n" +
            "import javax.ws.rs.client.Client;\n\n" +
            "public class CLASSNAME extends PARENTCLASS {\n" +
            "    protected  CLASSNAME (Client c, RestClientBase p) {\n" +
            "        super(c,p);\n"+
            "    }\n\n";
    private static final String TMPL_GET_SEGMENT =
            "    @Override protected String getSegment() {\n" +
            "        return \"/TAGNAME\";\n" +
            "    }\n\n";
    private static final String TMPL_GETTERS_AND_SETTERS = 
            "    public TYPE getMETHOD() {\n" +
            "        return getValue(\"FIELDNAME\", TYPE.class);\n" +
            "    }\n\n" +
            "    public void setMETHOD(TYPE value) {\n" +
            "        setValue(\"FIELDNAME\", value);\n" +
            "    }\n\n";
    private static final String TMPL_COLLECTION_LEAF_RESOURCE =
            "    public CLASSNAME getCLASSNAME() {\n" +
            "        return new CLASSNAME (client, this);\n" +
            "    }\n\n";
    private static final String TMPL_COMMAND =
            "    public RestResponse METHODNAME(SIG1) {\n" +
            "        return METHODNAME(PARAMS new HashMap<String, Object>());\n" +
            "    }\n\n" +
            "    public RestResponse METHODNAME(SIG2 Map<String, Object> additional) {\n" +
            "METHODBODY" +
            "    }\n\n";
    private static final String TMPL_METHOD_BODY =
            "        Map<String, Object> payload = new HashMap<String, Object>();\n" +
            "PUTS" +
            "        payload.putAll(additional);\n" +
            "        return execute(Method.HTTPMETHOD, \"/RESOURCEPATH\", payload, NEEDSMULTIPART);\n";

    public JavaClientClassWriter(final ConfigModel model, final String className, Class parent, File baseDirectory) {
        this.className = className;


        File packageDir = new File(baseDirectory, Constants.CLIENT_JAVA_PACKAGE_DIR);
        packageDir.deleteOnExit();
        boolean success = packageDir.exists() || packageDir.mkdirs();
        if (!success) {
            throw new RuntimeException("Unable to create output directory"); // i18n
        }
        File classFile = new File(packageDir, className + ".java");
        try {
            boolean createSuccess = classFile.createNewFile();
            if (!createSuccess){
               RestLogging.restLogger.log(Level.SEVERE, RestLogging.FILE_CREATION_FAILED, classFile.getName());
            }
            classFile.deleteOnExit();
            source = new BufferedWriter(new FileWriter(classFile));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        if (parent.isAssignableFrom(RestClientBase.class)) {
            generateRestClientBaseChild(model);
        } else {
            generateSimpleCtor(parent.getName());
        }
    }
    
    protected final void generateRestClientBaseChild(ConfigModel model) {
        try {
            boolean hasKey = (Util.getKeyAttributeName(model) != null);
            boolean isDomain = className.equals("Domain");

            source.append(TMPL_CLASS_HEADER.replace("CLASSNAME", className));

            if (isDomain) {
                source.append(TMPL_CTOR_DOMAIN.replace("CLASSNAME", className));
            } else {
                if (hasKey) {
                    source.append(TMPL_CTOR_OTHER_WITH_KEY.replace("CLASSNAME", className));
                } else {
                    source.append(TMPL_CTOR_OTHER_NO_KEY.replace("CLASSNAME", className));
                }
            }

            if (hasKey || isDomain) {
                String method = TMPL_GET_REST_URL.replace("HASKEY", hasKey ? " + \"/\" + name" : "");
                if (isDomain) {
                    method = method.replace("super.getRestUrl()", "parent.getRestUrl() + getSegment()");
                }
                source.append(method);
            }
        } catch (IOException ex) {
            RestLogging.restLogger.log(Level.SEVERE, null, ex);
        }
    }

    // TODO: The next two generated ctors are identical, other than the parent class
    protected final void generateSimpleCtor(String parentClassName) {
        try {
            source.append(TMPL_CTOR_SIMPLE.replace("CLASSNAME", className).replace("PARENTCLASS", parentClassName));
        } catch (IOException ex) {
           RestLogging.restLogger.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void generateGetSegment(String tagName) {
        try {
            source.append(TMPL_GET_SEGMENT.replace("TAGNAME", tagName));
        } catch (IOException ex) {
            RestLogging.restLogger.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void generateCommandMethod(String methodName, String httpMethod, String resourcePath, CommandModel cm) {
        try {
            String parametersSignature = Util.getMethodParameterList(cm, true, false);
            boolean needsMultiPart = parametersSignature.contains("java.io.File");
            
            String parameters = Util.getMethodParameterList(cm, false, false);
            String method = TMPL_COMMAND
                    .replace("METHODNAME", methodName)
                    .replace("SIG1", parametersSignature)
                    .replace("PARAMS", !parameters.isEmpty() ? (parameters + ",") : "")
                    .replace("SIG2", !parametersSignature.isEmpty() ? (parametersSignature + ",") : "")
                    .replace("METHODBODY", generateMethodBody(cm, httpMethod, resourcePath, false, needsMultiPart));

            source.append(method);
        } catch (IOException ex) {
            RestLogging.restLogger.log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public String generateMethodBody(CommandModel cm, String httpMethod, String resourcePath, boolean includeOptional, boolean needsMultiPart) {
        StringBuilder sb = new StringBuilder();
        Collection<ParamModel> params = cm.getParameters();
        if ((params != null) && (!params.isEmpty())) {
            for (ParamModel model : params) {
                Param param = model.getParam();
                boolean include = true;
                if (param.optional() && !includeOptional) {
                    continue;
                }
                String key = (!param.alias().isEmpty()) ? param.alias() : model.getName();
                String paramName = Util.eleminateHypen(model.getName());
                String put = "        payload.put(\"" + key + "\", _" + paramName +");\n";
                sb.append(put);
            }
        }
        return TMPL_METHOD_BODY.replace("PUTS", sb.toString())
                .replace("HTTPMETHOD", httpMethod.toUpperCase(Locale.US))
                .replace("RESOURCEPATH", resourcePath)
                .replace("NEEDSMULTIPART", Boolean.toString(needsMultiPart));
    }

    @Override
    public void generateGettersAndSetters(String type, String methodName, String fieldName) {
        try {
            source.append(TMPL_GETTERS_AND_SETTERS
                .replace("METHOD", methodName)
                .replace("TYPE", type)
                .replace("FIELDNAME", fieldName));
        } catch (IOException ex) {
            RestLogging.restLogger.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void createGetChildResource(ConfigModel model, String elementName, String childResourceClassName) {
        try {
            final String TMPL_GET_CHILD_RESOURCE =
                    "    public CHILDRESOURCE getELEMENTNAME(HASKEY1) {\n" +
                    "        CHILDRESOURCE child = new CHILDRESOURCE(client, this HASKEY2);\n" +
                    "        child.initialize();\n" +
                    "        return (child.status == 200) ? child : null;\n" +
                    "    }\n";
            final boolean hasKey = Util.getKeyAttributeName(model) != null;
            source.append(TMPL_GET_CHILD_RESOURCE
                .replace("CHILDRESOURCE", childResourceClassName)
                .replace("HASKEY1", hasKey ? "String name" : "")
                .replace("HASKEY2", hasKey ? ", name" : "")
                .replace("ELEMENTNAME", elementName));
        } catch (IOException ex) {
            RestLogging.restLogger.log(Level.SEVERE, null, ex);
        }
    }

    // TODO: Merge generateCollectionLeafResourceGetter() and generateRestLeafGetter().  Must find a meaningful name first.
    @Override
    public void generateCollectionLeafResourceGetter(String className) {
        try {
            source.append(TMPL_COLLECTION_LEAF_RESOURCE.replace("CLASSNAME", className));
        } catch (IOException ex) {
            RestLogging.restLogger.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void generateRestLeafGetter(String className) {
        try {
            source.append(TMPL_COLLECTION_LEAF_RESOURCE.replace("CLASSNAME", className));
        } catch (IOException ex) {
            RestLogging.restLogger.log(Level.SEVERE, null, ex);
        }
    }

    public void done() {
        finishClass();
    }

    private void finishClass() {
        try {
            source.append("}");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            if (source != null) {
                try {
                    source.close();
                } catch (IOException ex) {
                    RestLogging.restLogger.log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}

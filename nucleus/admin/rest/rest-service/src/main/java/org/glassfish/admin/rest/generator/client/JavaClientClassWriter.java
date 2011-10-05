/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.Util;
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
            classFile.createNewFile();
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

            source.append("package ")
                    .append(Constants.CLIENT_JAVA_PACKAGE)
                    .append(";\n")
                    .append("import java.util.HashMap;\n")
                    .append("import java.util.Map;\n")
                    .append("import com.sun.jersey.api.client.Client;\n")
                    .append("\npublic class ")
                    .append(className)
                    .append(" extends RestClientBase {\n");

            // ctor
            if (isDomain) {
                source.append("    private RestClient parent;\n")
                        .append("    public ")
                        .append(className)
                        .append("(RestClient parent) {\n")
                        .append("        super(parent.client, null);\n")
                        .append("        this.parent = parent;\n    }\n");

            } else {
                if (hasKey) {
                    source.append("    private String name;\n")
                            .append("    protected ")
                            .append(className)
                            .append("(Client c, RestClientBase p, String name) {\n")
                            .append("        super(c, p);\n")
                            .append("        this.name = name;\n")
                            .append("    }\n");


                } else {
                    source.append("    protected ")
                            .append(className)
                            .append("(Client c, RestClientBase p) {\n")
                            .append("        super(c,p);\n")
                            .append("    }\n");

                }
            }

            if (hasKey || isDomain) {
                source.append("    @Override protected String getRestUrl() {\n");
                if (hasKey) {
                    source.append("        return super.getRestUrl() + (isNew() ? \"\" :  \"/\" + name);\n");
                } else if (isDomain) {
                    source.append("        return parent.getRestUrl() + getSegment();\n");
                }
                source.append("    }\n");
            }
        } catch (IOException ex) {
            Logger.getLogger(JavaClientClassWriter.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }

    // TODO: The next two generated ctors are identical, other than the parent class
    protected final void generateSimpleCtor(String parentClassName) {
        try {
            source.append("package org.glassfish.admin.rest.client;\n")
                    .append("import com.sun.jersey.api.client.Client;\n")
                    .append("\npublic class ")
                    .append(className)
                    .append(" extends ")
                    .append(parentClassName)
                    .append(" {\n")
                    .append("    protected ")
                    .append(className)
                    .append("(Client c, RestClientBase p) {\n")
                    .append("        super(c,p);\n")
                    .append("    }\n");
        } catch (IOException ex) {
            Logger.getLogger(JavaClientClassWriter.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void generateGetSegment(String tagName) {
        try {
            source.append("\n")
                    .append("    @Override protected String getSegment() {\n")
                    .append("        return \"/")
                    .append(tagName)
                    .append("\";\n    }\n");
        } catch (IOException ex) {
            Logger.getLogger(JavaClientClassWriter.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void generateCommandMethod(String methodName, String httpMethod, String resourcePath, CommandModel cm) {
        try {
            String parametersSignature = Util.getMethodParameterList(cm, true, false);
            boolean needsMultiPart = parametersSignature.contains("java.io.File");
            
            String parameters = Util.getMethodParameterList(cm, false, false);
            source.append("\n    public RestResponse ")
                    .append(methodName)
                    .append("(")
                    .append(parametersSignature)
                    .append(") {\n")
                    .append("        return ")
                    .append(methodName)
                    .append("(");
            if (!parameters.isEmpty()) {
                source.append(parameters)
                    .append(", ");
            }
            
            source.append("new HashMap<String, Object>());\n")
                    .append("    }\n");
            source.append("\n    public RestResponse ")
                    .append(methodName)
                    .append("(");
            
            if (!parametersSignature.isEmpty()) {
                source.append(parametersSignature)
                        .append(", ");
            }

            source.append("Map<String, Object> additional) {\n")
                    .append(generateMethodBody(cm, httpMethod, resourcePath, false, needsMultiPart))
                    .append("    }\n");
        } catch (IOException ex) {
            Logger.getLogger(JavaClientClassWriter.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public String generateMethodBody(CommandModel cm, String httpMethod, String resourcePath, boolean includeOptional, boolean needsMultiPart) {
        StringBuilder sb = new StringBuilder();
        sb.append("        Map<String, Object> payload = new HashMap<String, Object>();\n");
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
                sb.append("        payload.put(\"")
                        .append(key)
                        .append("\", _")
                        .append(paramName)
                        .append(");\n");
            }
        }
        sb.append("        payload.putAll(additional);\n");
        sb.append("        return execute(Method.")
                .append(httpMethod.toUpperCase(Locale.US))
                .append(", \"/")
                .append(resourcePath)
                .append("\"")
                .append(", payload,")
                .append(needsMultiPart)
                .append(");\n");


        return sb.toString();
    }

    public String generateMethodBody2(CommandModel cm, String httpMethod, String resourcePath, boolean includeOptional, boolean needsMultiPart) {
        StringBuilder sb = new StringBuilder();
        sb.append("        Map<String, Object> payload = new HashMap<String, Object>();\n");
        Map<String, String> paramNames = new HashMap<String, String>();
        Collection<ParamModel> params = cm.getParameters();
        if ((params != null) && (!params.isEmpty())) {
            for (ParamModel model : params) {
                Param param = model.getParam();
                boolean include = true;
                if (param.optional() && !includeOptional) {
                    include = false;
                }
                if (!include) {
                    continue;
                }
                paramNames.put((!param.alias().isEmpty()) ? param.alias() : model.
                        getName(), model.getName());
            }
        }
        for (Map.Entry<String, String> paramName : paramNames.entrySet()) {
            sb.append("        payload.put(\"")
                    .append(paramName.getKey())
                    .append("\", _")
                    .append(Util.eleminateHypen(paramName.getValue()))
                    .append(");\n");
        }
        sb.append("        return execute(Method.")
                .append(httpMethod.toUpperCase(Locale.US))
                .append(", \"/")
                .append(resourcePath)
                .append("\"")
                .append(", payload,")
                .append(needsMultiPart)
                .append(");\n");


        return sb.toString();
    }

    @Override
    public void generateGettersAndSetters(String type, String methodName, String fieldName) {
        try {
            // getter
            source.append("    public ")
                    .append(type)
                    .append(" get")
                    .append(methodName)
                    .append("() {\n        return getValue(\"")
                    .append(fieldName)
                    .append("\",")
                    .append(type)
                    .append(".class);\n    }\n\n");

            // setter
            source.append("    public void set")
                    .append(methodName).append("(")
                    .append(type)
                    .append(" value) {\n        setValue(\"")
                    .append(fieldName)
                    .append("\", value);\n    }\n\n");
        } catch (IOException ex) {
            Logger.getLogger(JavaClientClassWriter.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void createGetChildResource(ConfigModel model, String elementName, String childResourceClassName) {
        try {
            final boolean hasKey = Util.getKeyAttributeName(model) != null;
            source.append("    public ")
                    .append(childResourceClassName)
                    .append(" get")
                    .append(elementName)
                    .append("(");
            if (hasKey) {
                source.append("String name");
            }
            source.append(") {\n")
                    .append("        ")
                    .append(childResourceClassName)
                    .append(" child = new ")
                    .append(childResourceClassName)
                    .append("(client, this");
            if (hasKey) {
                source.append(", name");
            }
            source.append(");\n");
            source.append("        child.initialize();\n");
            source.append("        return (child.status == 200) ? child : null;\n");
            source.append("    }\n");
        } catch (IOException ex) {
            Logger.getLogger(JavaClientClassWriter.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }

    // TODO: Merge generateCollectionLeafResourceGetter() and
    // generateRestLeafGetter().  Must find a meaningful name first.
    @Override
    public void generateCollectionLeafResourceGetter(String className) {
        try {
            source.append("    public ")
                    .append(className)
                    .append(" get")
                    .append(className)
                    .append("() {\n")
                    .append("        return new ")
                    .append(className)
                    .append("(client, this);\n")
                    .append("    }\n");
        } catch (IOException ex) {
            Logger.getLogger(JavaClientClassWriter.class.getName()).
                    log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void generateRestLeafGetter(String className) {
        try {
            source.append("    public ")
                    .append(className)
                    .append(" get")
                    .append(className)
                    .append("() {\n")
                    .append("        return new ")
                    .append(className)
                    .append("(client, this);\n")
                    .append("    }\n");
        } catch (IOException ex) {
            Logger.getLogger(JavaClientClassWriter.class.getName()).
                    log(Level.SEVERE, null, ex);
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
                    Logger.getLogger(ClientGenerator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}

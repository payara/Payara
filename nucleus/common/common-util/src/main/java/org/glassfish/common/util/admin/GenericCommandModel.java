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

package org.glassfish.common.util.admin;

import com.sun.enterprise.util.LocalStringManager;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.CommandModel;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.DomDocument;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.glassfish.api.ParamDefaultCalculator;

public class GenericCommandModel extends CommandModel {

    final HashMap<String, ParamModel> params = new HashMap<String, ParamModel>();
    final String commandName;
    final Class<?> commandClass;
    final ExecuteOn cluster;
    final I18n i18n;
    final LocalStringManager localStrings;
    final boolean managedJob;

    /**
     * GenericCommandModel constructor.
     *
     * @param targetType	the ConfigBeanProxy class that may have
     * 				additional @Param annotations for the command,
     * @param useAnnotations	if true, use the annotations on the targetType
     * 				class to define the parameters
     * @param cluster		the @ExecuteOn annotation, if any
     * @param i18n		the @I18n annotation, if any
     * @param localStrings	where to find strings for the command
     * @param document		the DomDocument for the command
     * @param commandName	the name of the command
     * @param supportsProgress  {@code true} only if command working 
     *                          with ProgressStatus
     * @param extraTypes	any extra types that might also define
     * 				parameters for the command
     */
    public GenericCommandModel(Class<?> targetType,
                               boolean useAnnotations,
                               ExecuteOn cluster,
                               I18n i18n,
                               LocalStringManager localStrings,
                               DomDocument document,
                               String commandName,
                               boolean managedJob,
                               Class<?>... extraTypes) {
        this.commandName = commandName;
        this.commandClass = targetType;
        this.cluster = cluster;
        this.i18n = i18n;
        this.localStrings = localStrings;
        this.managedJob = managedJob;

        if (useAnnotations && targetType!=null &&
		ConfigBeanProxy.class.isAssignableFrom(targetType)) {
            ConfigModel cm = document.buildModel(targetType);
            for (Method m : targetType.getMethods()) {
                ConfigModel.Property prop = cm.toProperty(m);
                if (prop == null) continue;
                String attributeName = prop.xmlName;
                I18n paramI18n = m.getAnnotation(I18n.class);
                if (m.isAnnotationPresent(Param.class)) {
                    Param p = m.getAnnotation(Param.class);
                    if (p.name() != null && !p.name().isEmpty()) {
                        // GLASSFISH-18654: make sure password params are handled
                        String name = CommandModel.getParamName(p, m);
                        params.put(name, new ParamBasedModel(name, p, paramI18n));
                    } else if (m.isAnnotationPresent(Attribute.class)) {
                        Attribute attr = m.getAnnotation(Attribute.class);
                        if (attr.value() != null && !attr.value().isEmpty()) {
                            params.put(attr.value(), new AttributeBasedModel(attr.value(), attr, paramI18n));
                        } else {
                            params.put(attributeName, new AttributeBasedModel(attributeName, attr, paramI18n));
                        }
                    } else {
                            // use method name.
                            // GLASSFISH-18654: make sure password params are handled
                            String name = CommandModel.getParamName(p, m);
                            params.put(name, new ParamBasedModel(name, p, paramI18n));
                    }
                }
            }
        }

        if (extraTypes!=null) {
            for (Class extraType : extraTypes) {
                for (Map.Entry<String, ParamModel> e : CommandModelImpl.init(extraType, i18n, localStrings).entrySet()) {
                    if (!params.containsKey(e.getKey()))
                        params.put(e.getKey(), e.getValue());
                }

            }
        }
    }

    @Override
    public String getLocalizedDescription() {
        return localStrings.getLocalString(i18n.value(), "");
    }

    @Override
    public String getUsageText() {
        return localStrings.getLocalString(i18n.value()+".usagetext", null);
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public ParamModel getModelFor(String paramName) {
        return params.get(paramName);
    }

    @Override
    public Collection<String> getParametersNames() {
        return params.keySet();
    }

    @Override
    public Class<?> getCommandClass() {
        return commandClass;
    }

    @Override
    public ExecuteOn getClusteringAttributes() {
        return cluster;
    }

    @Override
    public void add(ParamModel model) {
        if (!params.containsKey(model.getName())) {
            params.put(model.getName(), model);
        }
    }

    @Override
    public boolean isManagedJob() {
        return managedJob;
    }

    private final class ParamBasedModel extends ParamModel {
        final String name;
        final Param param;
        final I18n i18n;

        private ParamBasedModel(String name, Param param, I18n i18n) {
            this.name = name;
            this.param = param;
            this.i18n = i18n;
        }

        private String getLocalizedString(String type) {
            if (i18n!=null) {
                return GenericCommandModel.this.localStrings.getLocalString(i18n.value() + type, "");
            } else {
                return GenericCommandModel.this.localStrings.getLocalString(
                        GenericCommandModel.this.i18n.value() + "." + name + type, "");
            }
        }
        
        @Override
        public String getLocalizedDescription() {
            return getLocalizedString("");
        }
        
        @Override
        public String getLocalizedPrompt() {
            return getLocalizedString(".prompt");
        }

        @Override
        public String getLocalizedPromptAgain() {
            return getLocalizedString(".promptAgain");
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Param getParam() {
            return param;
        }

        public I18n getI18n() {
            return i18n;
        }

        @Override
        public Class getType() {
            return String.class;
        }
    }

    private final class AttributeBasedModel extends ParamModel {
        final String name;
        final Attribute attr;
        final I18n i18n;

        private AttributeBasedModel(String name, Attribute attr, I18n i18n) {
            this.name = name;
            this.attr = attr;
            this.i18n = i18n;
        }

        @Override
        public String getName() {
            return name;
        }

        private String getLocalizedString(String type) {
            if (i18n!=null) {
                return GenericCommandModel.this.localStrings.getLocalString(i18n.value() + type, "");
            } else {
                return GenericCommandModel.this.localStrings.getLocalString(
                        GenericCommandModel.this.i18n.value() + "." + name + type, "");
            }
        }
        
        @Override
        public String getLocalizedDescription() {
            return getLocalizedString("");
        }
        
        @Override
        public String getLocalizedPrompt() {
            return getLocalizedString(".prompt");
        }

        @Override
        public String getLocalizedPromptAgain() {
            return getLocalizedString(".promptAgain");
        }
        
        @Override
        public Class getType() {
            return String.class;
        }

        @Override
        public Param getParam() {
            return new Param() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return Param.class;
                }

                @Override
                public String name() {
                    return name;
                }

                @Override
                public String acceptableValues() {
                    return null;
                }

                @Override
                public boolean optional() {
                    return !attr.key();

                }

                @Override
                public String shortName() {
                    return null;
                }

                @Override
                public boolean primary() {
                    return attr.key();
                }

                @Override
                public String defaultValue() {
                    return attr.defaultValue();
                }

                @Override
                public Class<ParamDefaultCalculator> defaultCalculator() {
                    return ParamDefaultCalculator.class;
                }

                @Override
                public boolean password() {
                    return false;
                }

                @Override
                public char separator() {
                    return ',';
                }

                @Override
                public boolean multiple() {
                    return false;
                }
                
                @Override
                public boolean obsolete() {
                    return false;
                }

                @Override
                public String alias() {
                    return "";
                }
            };
        }
    }
}

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
package org.glassfish.admin.rest.composite.metadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.logging.Level;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.OptionsCapable;
import org.glassfish.admin.rest.RestLogging;
import org.glassfish.admin.rest.composite.CompositeUtil;
import org.jvnet.hk2.config.Attribute;

/**
 *
 * @author jdlee
 */
public class ParamMetadata {
    private String name;
    private Type type;
    private String help;
    private Object defaultValue;
    private boolean readOnly = false;
    private boolean confidential = false;
    private boolean immutable = false;
    private boolean createOnly = false;
    private OptionsCapable context;

    public ParamMetadata() {

    }
    public ParamMetadata(OptionsCapable context, Type paramType, String name, Annotation[] annotations) {
        this.name = name;
        this.context = context;
        this.type = paramType;
        final CompositeUtil instance = CompositeUtil.instance();
        help = instance.getHelpText(annotations);
        defaultValue = getDefaultValue(annotations);

        for (Annotation a : annotations) {
            if (a.annotationType().equals(ReadOnly.class)) {
                readOnly = true;
            }
            if (a.annotationType().equals(Confidential.class)) {
                confidential = true;
            }
            if (a.annotationType().equals(Immutable.class)) {
                immutable = true;
            }
            if (a.annotationType().equals(CreateOnly.class)) {
                createOnly = true;
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String toString() {
        return "ParamMetadata{" + "name=" + name + ", type=" + getTypeString() + ", help=" + help + '}';
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
//        o.put("name", name);
        o.put("type", getTypeString());
        o.put("help", help);
        Object defVal = (defaultValue != null) ? defaultValue : JSONObject.NULL;
        o.put("default", defVal);
        o.put("readOnly", readOnly);
        o.put("confidential", confidential);
        o.put("immutable", immutable);
        o.put("createOnly", createOnly);
        return o;
    }
    
    protected String getTypeString() {
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType)type;
            StringBuilder sb = new StringBuilder(((Class<?>)pt.getRawType()).getSimpleName());
            sb.append("<");
            String sep = "";
            for (Type t : pt.getActualTypeArguments()) {
                sb.append(sep)
                        .append(((Class<?>)t).getSimpleName());
                sep = ";";
            }
            return sb.append(">").toString();
        } else {
            return ((Class<?>)type).getSimpleName();
        }
    }

    /**
     * This method will process the annotations for a field to try to determine the default value, if one has been specified.
     * @param annos
     * @return
     */
    private Object getDefaultValue(Annotation[] annos) {
        Object defval = null;
        if (annos != null) {
            for (Annotation annotation : annos) {
                 if (Default.class.isAssignableFrom(annotation.getClass())) {
                    try {
                        Default def = (Default)annotation;
                        Class clazz = def.generator();
                        if (def.useContext()) {
                            defval = ((DefaultsGenerator) context).getDefaultValue(name);
                        } else if (clazz != null && clazz != Void.class) {
                            if (DefaultsGenerator.class.isAssignableFrom(clazz)) {
                                defval = ((DefaultsGenerator) clazz.newInstance()).getDefaultValue(name);
                            } else {
                                RestLogging.restLogger.log(Level.SEVERE, RestLogging.DOESNT_IMPLEMENT_DEFAULTS_GENERATOR);
                            }
                        } else {
                            defval = parseValue(def.value());
                        }
                        break;
                    } catch (Exception ex) {
                        RestLogging.restLogger.log(Level.SEVERE, null, ex);
                    }
                } else if (Attribute.class.isAssignableFrom(annotation.getClass())) {
                    Attribute attr = (Attribute)annotation;
                    defval = attr.defaultValue();
                    break;
                }
            }
        }
        return defval;
    }

    private Object parseValue(String value) {
        Class<?> clazz = (Class<?>)type;
        try {
            if (clazz.equals(String.class)) {
                return value;
            }
            if (clazz.equals(Boolean.TYPE) || clazz.equals(Boolean.class)) {
                return Boolean.valueOf(value);
            }
            if (clazz.equals(Integer.TYPE) || clazz.equals(Integer.class)) {
                return new Integer(value);
            }
            if (clazz.equals(Long.TYPE) || clazz.equals(Long.class)) {
                return new Long(value);
            }
            if (clazz.equals(Double.TYPE) || clazz.equals(Double.class)) {
                return new Double(value);
            }
            if (clazz.equals(Float.TYPE) || clazz.equals(Float.class)) {
                return new Float(value);
            }
            // TBD - arrays/lists of values
            RestLogging.restLogger.log(Level.SEVERE, RestLogging.UNSUPPORTED_FIXED_VALUE);
        } catch (NumberFormatException e) {
            RestLogging.restLogger.log(Level.SEVERE, RestLogging.VALUE_DOES_NOT_MATCH_TYPE);
        }
        return null;
    }
}

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.File;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.validation.Validator;
import org.glassfish.api.ExecutionContext;
import org.glassfish.api.Param;
import org.glassfish.api.ParamDefaultCalculator;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.hk2.api.MultiException;
import org.jvnet.hk2.component.MultiMap;
import org.jvnet.hk2.config.InjectionResolver;


/**
 * An InjectionResolver that uses a ParameterMap object as the source of
 * the data to inject.
 */
public class MapInjectionResolver extends InjectionResolver<Param> {
    private final CommandModel model;
    private final ParameterMap parameters;
    private ExecutionContext context = null;

    private final MultiMap<String,File> optionNameToUploadedFileMap;

    public static final LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(MapInjectionResolver.class);
    private static Validator beanValidator = null;

    
    public MapInjectionResolver(CommandModel model,
					ParameterMap parameters) {
        this(model, parameters, null);
    }
    
    public MapInjectionResolver(CommandModel model,
					ParameterMap parameters,
                                        final MultiMap<String,File> optionNameToUploadedFileMap) {
	super(Param.class);
	this.model = model;
	this.parameters = parameters;
        this.optionNameToUploadedFileMap = optionNameToUploadedFileMap;
    }

    /**
     * Set the context that is passed to the DynamicParamImpl.defaultValue method.
     */
    public void setContext(ExecutionContext context) {
        this.context = context;
    }
    
    @Override
    public boolean isOptional(AnnotatedElement element, Param annotation) {
       String name = CommandModel.getParamName(annotation, element);
       CommandModel.ParamModel param = model.getModelFor(name);
       return param.getParam().optional();
    }

    @Override
    public <V> V getValue(Object component, AnnotatedElement target, Type genericType, Class<V> type) throws MultiException {
	// look for the name in the list of parameters passed.
	Param param = target.getAnnotation(Param.class);
	String paramName = CommandModel.getParamName(param, target);
	if (param.primary()) {
	    // this is the primary parameter for the command
            List<String> value = parameters.get("DEFAULT");
	    if (value != null && value.size() > 0) {
                /*
                 * If the operands are uploaded files, replace the
                 * client-provided values with the paths to the uploaded files.
                 * XXX - assume the lists are in the same order.
                 */
                final List<String> filePaths = getUploadedFileParamValues(
                        "DEFAULT",
                        type, optionNameToUploadedFileMap);
                if (filePaths != null) {
                    value = filePaths;
                    // replace the file name operands with the uploaded files
                    parameters.set("DEFAULT", value); 
                } else {
                    for (String s : value) {
                        checkAgainstAcceptableValues(target, s);
                    }
                    
                }
		// let's also copy this value to the cmd with a real name
		parameters.set(paramName, value);
                V paramValue = (V) convertListToObject(target, type, value);
                return paramValue;
	    }
	}
        if (param.multiple()) {
            List<String> value = parameters.get(paramName);
            if (value != null && value.size() > 0) {
                final List<String> filePaths = getUploadedFileParamValues(
                        paramName,
                        type, optionNameToUploadedFileMap);
                if (filePaths != null) {
                    value = filePaths;
                    // replace the file name operands with the uploaded files
                    parameters.set(paramName, value); 
                } else {
                    for (String s : value) {
                        checkAgainstAcceptableValues(target, s);
                    }
                }
            }
            parameters.set(paramName, value);
            V paramValue = (V) convertListToObject(target, type, value);
            return paramValue;
        }
	String paramValueStr = getParamValueString(parameters, param, target, context);

        /*
         * If the parameter is an uploaded file, replace the client-provided
         * value with the path to the uploaded file.
         */
        final String fileParamValueStr = getUploadedFileParamValue(
                paramName, type, optionNameToUploadedFileMap);
        if (fileParamValueStr != null) {
            paramValueStr = fileParamValueStr;
            parameters.set(paramName, paramValueStr);
        }
	checkAgainstAcceptableValues(target, paramValueStr);
        
        return paramValueStr != null ?
                (V) convertStringToObject(target, type, paramValueStr) :
                (V) getParamField(component, target);
    }

    /**
     * Returns the path to the uploaded file if the specified field is of type
     * File and if the field name is the same as one of the option names that
     * was associated with an uploaded File in the incoming command request.
     *
     * @param fieldName name of the field being injected
     * @param fieldType type of the field being injected
     * @param optionNameToFileMap map of field names to uploaded Files
     * @return absolute path of the uploaded file for the field;
     *          null if no file uploaded for this field
     */
    public static String getUploadedFileParamValue(final String fieldName,
                final Class fieldType,
                final MultiMap<String,File> optionNameToFileMap) {
        if (optionNameToFileMap == null) {
            return null;
        }
        final File uploadedFile = optionNameToFileMap.getOne(fieldName);
        if (uploadedFile != null && fieldType.isAssignableFrom(File.class)) {
            return uploadedFile.getAbsolutePath();
        } else {
            return null;
        }
    }

    /**
     * Returns the paths to the uploaded files if the specified field is of type
     * File[] and if the field name is the same as one of the option names that
     * was associated with an uploaded File in the incoming command request.
     *
     * @param fieldName name of the field being injected
     * @param fieldType type of the field being injected
     * @param optionNameToFileMap map of field names to uploaded Files
     * @return List of absolute paths of the uploaded files for the field;
     *          null if no file uploaded for this field
     */
    public static List<String> getUploadedFileParamValues(
                final String fieldName,
                final Class fieldType,
                final MultiMap<String,File> optionNameToFileMap) {
        if (optionNameToFileMap == null) {
            return null;
        }
        final List<File> uploadedFiles = optionNameToFileMap.get(fieldName);
        if (uploadedFiles != null && uploadedFiles.size() > 0 &&
                (fieldType.isAssignableFrom(File.class) ||
                 fieldType.isAssignableFrom(File[].class))) {
            final List<String> paths = new ArrayList(uploadedFiles.size());
            for (File f : uploadedFiles)
                paths.add(f.getAbsolutePath());
            return paths;
        } else {
            return null;
        }
    }

    /**
     * Get the param value.  Checks if the param (option) value
     * is defined on the command line (URL passed by the client)
     * by calling getParameterValue method.  If not, then check
     * for the shortName.  If param value is not given by the
     * shortName (short option) then if the default value is
     * defined return it.
     *
     * @param parameters parameters from the command line.
     * @param param from the annotated Param
     * @param target annotated element
     * @return param value
     */
    // package-private, for testing
    static String getParamValueString(final ParameterMap parameters,
                               final Param param,
                               final AnnotatedElement target,
                               final ExecutionContext context) {
        String paramValueStr = getParameterValue(parameters,
                                      CommandModel.getParamName(param, target),
                                      true);
        if (paramValueStr == null && param.alias().length() > 0) {
            // check for alias
            paramValueStr = getParameterValue(parameters, param.alias(), true);
        }
        if (paramValueStr == null) {
            // check for shortName
            paramValueStr = parameters.getOne(param.shortName());
        }
        
        // if paramValueStr is still null, then check to
        // see if the defaultValue is defined
        if (paramValueStr == null) {
            final String defaultValue = param.defaultValue();
            paramValueStr = defaultValue.equals("") ? null : defaultValue;
        }
        // if paramValueStr is still null, then check to see if a defaultCalculator
        // is defined, and if so, call the class to get the default value
        if (paramValueStr == null) {
            Class<? extends ParamDefaultCalculator> dc = param.defaultCalculator();
            if (dc != ParamDefaultCalculator.class) {
                try {
                    ParamDefaultCalculator pdc = dc.newInstance();
                    paramValueStr = pdc.defaultValue(context);
                } catch (InstantiationException ex) { // @todo Java SE 7 - use multi catch
                    Logger.getLogger(MapInjectionResolver.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(MapInjectionResolver.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return paramValueStr;
    }

    /**
     * Get the value of the field.  This value is defined in the
     * annotated Param declaration.  For example:
     * <code>
     * @Param(optional=true)
     * String name="server"
     * </code>
     * The Field, name's value, "server" is returned.
     *
     * @param component command class object
     * @param annotated annotated element
     * @return the annotated Field value
     */
    // package-private, for testing
    static Object getParamField(final Object component,
                         final AnnotatedElement annotated) {
        try {
            if (annotated instanceof Field) {
                final Field field = (Field)annotated;
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        field.setAccessible(true);
                        return null;
                    }
                });
                return ((Field) annotated).get(component);
            }
        } catch (Exception e) {
            // unable to get the field value, may not be defined
            // return null instead.
            return null;
        }
        return null;
    }

    /**
     * Searches for the parameter with the specified key in this parameter map.
     * The method returns null if the parameter is not found.
     *
     * @param params the parameter map to search in
     * @param key the property key
     * @param ignoreCase true to search the key ignoring case,
     *                   false otherwise
     * @return the value in this parameter map with the specified key value
     */
    // package-private, for testing
    static String getParameterValue(final ParameterMap params,
                            String key, final boolean ignoreCase) {
        if (ignoreCase) {
            for (Map.Entry<String,List<String>> entry : params.entrySet()) {
                final String paramName = entry.getKey();
                if (paramName.equalsIgnoreCase(key)) {
                    key = paramName;
                    break;
                }
            }
        }
        return params.getOne(key);
    }

    /**
     * Convert the String parameter to the specified type.
     * For example if type is Properties and the String
     * value is: name1=value1:name2=value2:...
     * then this api will convert the String to a Properties
     * class with the values {name1=name2, name2=value2, ...}
     *
     * @param target the target field
     * @param type the type of class to convert
     * @param paramValStr the String value to convert
     * @return Object
     */
    // package-private, for testing
    static Object convertStringToObject(AnnotatedElement target,
                                    Class type, String paramValStr) {
        Param param = target.getAnnotation(Param.class);
        Object paramValue = paramValStr;
        if (type.isAssignableFrom(String.class)) {
            paramValue = paramValStr;
        } else if (type.isAssignableFrom(Properties.class)) {
            paramValue =
                convertStringToProperties(paramValStr, param.separator());
        } else if (type.isAssignableFrom(List.class)) {
            paramValue = convertStringToList(paramValStr, param.separator());
        } else if (type.isAssignableFrom(Boolean.class) ||
                    type.isAssignableFrom(boolean.class)) {
            String paramName = CommandModel.getParamName(param, target);
            paramValue = convertStringToBoolean(paramName, paramValStr);
        } else if (type.isAssignableFrom(Integer.class) ||
                    type.isAssignableFrom(int.class)) {
            String paramName = CommandModel.getParamName(param, target);
            paramValue = convertStringToInteger(paramName, paramValStr);
        } else if (type.isAssignableFrom(String[].class)) {
            paramValue =
                convertStringToStringArray(paramValStr, param.separator());
        } else if (type.isAssignableFrom(File.class)) {
            return new File(paramValStr);
        }
        return paramValue;
    }

    /**
     * Convert the List<String> parameter to the specified type.
     *
     * @param target the target field
     * @param type the type of class to convert
     * @param paramValList the List of String values to convert
     * @return Object
     */
    // package-private, for testing
    public static Object convertListToObject(AnnotatedElement target,
                                    Class type, List<String> paramValList) {
        Param param = target.getAnnotation(Param.class);
        // does this parameter type allow multiple values?
        if (!param.multiple()) {
            if (paramValList.size() == 1)
                return convertStringToObject(target, type, paramValList.get(0));
            throw new UnacceptableValueException(
                localStrings.getLocalString("TooManyValues",
                    "Invalid parameter: {0}.  This parameter may not have " +
                    "more than one value.",
                    CommandModel.getParamName(param, target)));
        }

        Object paramValue = paramValList;
        if (type.isAssignableFrom(List.class)) {
            // the default case, nothing to do
        } else if (type.isAssignableFrom(String[].class)) {
            paramValue = paramValList.toArray(new String[paramValList.size()]);
        } else if (type.isAssignableFrom(File[].class)) {
            paramValue = convertListToFiles(paramValList);
        } else if (type.isAssignableFrom(Properties.class)) {
            paramValue = convertListToProperties(paramValList);
        }
        // XXX - could handle arrays of other types
        return paramValue;
    }

    /**
     * Convert a String to a Boolean.
     * null --> true
     * "" --> true
     * case insensitive "true" --> true
     * case insensitive "false" --> false
     * anything else --> throw Exception
     *
     * @param paramName the name of the param
     * @param s the String to convert
     * @return Boolean
     */
    private static Boolean convertStringToBoolean(String paramName, String s) {
        if (!ok(s))
            return Boolean.TRUE;

        if (s.equalsIgnoreCase(Boolean.TRUE.toString()))
            return Boolean.TRUE;

        if (s.equalsIgnoreCase(Boolean.FALSE.toString()))
            return Boolean.FALSE;

        String msg = localStrings.getLocalString("UnacceptableBooleanValue",
                "Invalid parameter: {0}.  This boolean option must be set " +
                    "(case insensitive) to true or false.  " +
                    "Its value was set to {1}",
                paramName, s);

        throw new UnacceptableValueException(msg);
    }

    /**
     * Convert a String to an Integer.
     *
     * @param paramName the name of the param
     * @param s the String to convert
     * @return Integer
     */
    private static Integer convertStringToInteger(String paramName, String s) {
        try {
            return new Integer(s);
        } catch (Exception ex) {
            String msg = localStrings.getLocalString("UnacceptableIntegerValue",
                "Invalid parameter: {0}.  This integer option must be set " +
                    "to a valid integer.\nIts value was set to {1}",
                    paramName, s);
            throw new UnacceptableValueException(msg);
        }
    }

    /**
     * Convert a String with the following format to Properties:
     * name1=value1:name2=value2:name3=value3:...
     * The Properties object contains elements:
     * {name1=value1, name2=value2, name3=value3, ...}
     * 
     * Whitespace around names is ignored.
     *
     * @param propsString the String to convert
     * @param sep the separator character
     * @return Properties containing the elements in String
     */
    // package-private, for testing
    static Properties convertStringToProperties(String propsString, char sep) {
        final Properties properties = new Properties();
        if (propsString != null) {
            ParamTokenizer stoken = new ParamTokenizer(propsString, sep);
            while (stoken.hasMoreTokens()) {
                String token = stoken.nextTokenKeepEscapes();
                final ParamTokenizer nameTok = new ParamTokenizer(token, '=');
                String name = null, value = null;
                if (nameTok.hasMoreTokens())
                    name = nameTok.nextToken().trim();
                if (nameTok.hasMoreTokens())
                    value = nameTok.nextToken();
                if (name == null)       // probably "::"
                    throw new IllegalArgumentException(
                        localStrings.getLocalString("PropertyMissingName",
                        "Invalid property syntax, missing property name",
                        propsString));
                if (value == null)
                    throw new IllegalArgumentException(
                        localStrings.getLocalString("PropertyMissingValue",
                        "Invalid property syntax, missing property value",
                        token));
                if (nameTok.hasMoreTokens())
                    throw new IllegalArgumentException(
                        localStrings.getLocalString("PropertyExtraEquals",
                        "Invalid property syntax, \"=\" in value", token));
                properties.setProperty(name, value);
            }
        }
        return properties;
    }

    /**
     * Convert a List of Strings to an array of File objects.
     *
     * @param filesList the List of Strings to convert
     * @return File[] containing the elements in the list
     */
    private static File[] convertListToFiles(List<String> filesList) {
        final File[] files = new File[filesList.size()];
        for (int i = 0; i < filesList.size(); i++)
            files[i] = new File(filesList.get(i));
        return files;
    }

    /**
     * Convert a List of Strings, each with the following format, to Properties:
     * name1=value1
     *
     * @param propsList the List of Strings to convert
     * @return Properties containing the elements in the list
     */
    private static Properties convertListToProperties(List<String> propsList) {
        final Properties properties = new Properties();
        if (propsList != null) {
            for (String prop : propsList) {
                final ParamTokenizer nameTok = new ParamTokenizer(prop, '=');
                String name = null, value = null;
                if (nameTok.hasMoreTokens())
                    name = nameTok.nextToken();
                if (nameTok.hasMoreTokens())
                    value = nameTok.nextToken();
                if (nameTok.hasMoreTokens() || name == null || value == null)
                    throw new IllegalArgumentException(
                        localStrings.getLocalString("InvalidPropertySyntax",
                            "Invalid property syntax.", prop));
                properties.setProperty(name, value);
            }
        }
        return properties;
    }

    /**
     * Convert a String with the following format to List<String>:
     * string1:string2:string3:...
     * The List object contains elements: string1, string2, string3, ...
     *
     * @param listString - the String to convert
     * @param sep the separator character
     * @return List containing the elements in String
     */
    // package-private, for testing
    static List<String> convertStringToList(String listString, char sep) {
        List<String> list = new java.util.ArrayList();
        if (listString != null) {
            final ParamTokenizer ptoken = new ParamTokenizer(listString, sep);
            while (ptoken.hasMoreTokens()) {
                String token = ptoken.nextToken();
                list.add(token);
            }
        }
        return list;
    }

    /**
     * convert a String with the following format to String Array:
     * string1,string2,string3,...
     * The String Array contains: string1, string2, string3, ...
     *
     * @param arrayString - the String to convert
     * @param sep the separator character
     * @return String[] containing the elements in String
     */
    // package-private, for testing
    static String[] convertStringToStringArray(String arrayString, char sep) {
        final ParamTokenizer paramTok = new ParamTokenizer(arrayString, sep);
        List<String> strs = new ArrayList<String>();
        while (paramTok.hasMoreTokens())
            strs.add(paramTok.nextToken());
        return strs.toArray(new String[strs.size()]);
    }

    /**
     * Check if the value string is one of the strings in the list of
     * acceptable values in the @Param annotation on the target.
     *
     * @param target the target field
     * @param paramValueStr the parameter value
     */
    private static void checkAgainstAcceptableValues(AnnotatedElement target,
                                                    String paramValueStr) {
        Param param = target.getAnnotation(Param.class);
        String acceptable = param.acceptableValues();
        String paramName = CommandModel.getParamName(param, target);

        if (ok(acceptable) && ok(paramValueStr)) {
            String[] ss = acceptable.split(",");

            for (String s : ss) {
                if (paramValueStr.equals(s.trim()))
                    return;     // matched, value is good
            }

            // didn't match any, error
            throw new UnacceptableValueException(
                localStrings.getLocalString("UnacceptableValue",
                    "Invalid parameter: {0}.  Its value is {1} " +
                        "but it isn''t one of these acceptable values: {2}",
                    paramName,
                    paramValueStr,
                    acceptable));
        }
    }

    private static boolean ok(String s) {
        return s != null && s.length() > 0;
    }
}

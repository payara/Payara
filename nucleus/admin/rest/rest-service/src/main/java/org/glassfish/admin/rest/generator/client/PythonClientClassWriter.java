/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package org.glassfish.admin.rest.generator.client;

import org.glassfish.admin.rest.Util;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandModel;
import org.jvnet.hk2.config.ConfigModel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 *
 * @author jdlee
 */
class PythonClientClassWriter implements ClientClassWriter {
    private String className;
    private StringBuilder source;
    private File packageDir;
    private static String TMPL_CTOR = "from restclientbase import *\n\nclass CLASS(RestClientBase):\n"+
            "    def __init__(self, connection, parent):\n" +
            "        RestClientBase.__init__(self, connection, parent)\n" +
            "        self.parent = parent\n" +
            "        self.connection = connection\n";
    private String TMPL_GET_SEGMENT = "    def getSegment(self):\n" + 
            "        return '/SEGMENT'\n";
    private static String TMPL_COMMAND_METHOD = "\n    def COMMAND(self PARAMS, optional={}):\n" + 
            "MERGE" +
            "        return self.execute('/PATH', 'METHOD', optional, MULTIPART)\n";
    private static String TMPL_GETTER_AND_SETTER = "\n    def getMETHOD(self):\n" + 
            "        return self.getValue('FIELD')\n\n" +
            "    def setMETHOD(self, value):\n" + 
            "        self.setValue('FIELD', value)\n";
    private static String TMPL_GET_CHILD_RESOURCE = "\n    def getELEMENT(self, name):\n" +
            "        from IMPORT import CHILD\n" +
            "        child = CHILD(self.connection, self, name)\n" +
            "        return child if (child.status == 200) else None\n";


    public PythonClientClassWriter(ConfigModel model, String className, Class parent, File baseDirectory) {
        this.className = className;

        packageDir = baseDirectory;
        packageDir.deleteOnExit();
        boolean success = packageDir.exists() || packageDir.mkdirs();
        if (!success) {
            throw new RuntimeException("Unable to create output directory"); // i18n
        }
        source = new StringBuilder();
        source.append(TMPL_CTOR.replace("CLASS", className));
    }

    @Override
    public void generateGetSegment(String tagName) {
        source.append(TMPL_GET_SEGMENT.replace("SEGMENT", tagName));
    }

    @Override
    public void generateCommandMethod(String methodName, String httpMethod, String resourcePath, CommandModel cm) {
        String parametersSignature = Util.getMethodParameterList(cm, true, false);
        Boolean needsMultiPart = parametersSignature.contains("java.io.File");
        String parameters = Util.getMethodParameterList(cm, false, false);
        if (!parameters.isEmpty()) {
            parameters = ", " + parameters;
        }

        StringBuilder merge = new StringBuilder();
        Collection<CommandModel.ParamModel> params = cm.getParameters();
        if ((params != null) && (!params.isEmpty())) {
            for (CommandModel.ParamModel model : params) {
                Param param = model.getParam();
                if (param.optional()) {
                    continue;
                }
                String key = (!param.alias().isEmpty()) ? param.alias() : model.getName();
                String paramName = Util.eleminateHypen(model.getName()); 
                merge.append("        optional['")
                        .append(key)
                        .append("'] = _")
                        .append(paramName)
                        .append("\n");
            }
        }
        
        source.append(TMPL_COMMAND_METHOD.replace("COMMAND", methodName)
                .replace("PARAMS", parameters)
                .replace("MERGE", merge.toString())
                .replace("PATH", resourcePath)
                .replace("METHOD", httpMethod)
                .replace("MULTIPART", Util.upperCaseFirstLetter(needsMultiPart.toString())));
    }

    @Override
    public String generateMethodBody(CommandModel cm, String httpMethod, String resourcePath, boolean includeOptional, boolean needsMultiPart) {
        return null;
    }

    @Override
    public void generateGettersAndSetters(String type, String methodName, String fieldName) {
        source.append(TMPL_GETTER_AND_SETTER.replace("METHOD", methodName).replace("FIELD", fieldName));
    }

    @Override
    public void createGetChildResource(ConfigModel model, String elementName, String childResourceClassName) {
        String method = TMPL_GET_CHILD_RESOURCE.replace("CHILD", childResourceClassName)
                .replace("IMPORT", childResourceClassName.toLowerCase(Locale.getDefault()))
                .replace("ELEMENT", elementName);
        if (Util.getKeyAttributeName(model) == null) {
            method = method.replace(", name", "");
        }
        source.append(method);
    }

    @Override
    public void generateCollectionLeafResourceGetter(String className) {
        source.append(TMPL_GET_CHILD_RESOURCE.replace("CHILD", className)
                .replace("IMPORT", className.toLowerCase(Locale.getDefault()))
                .replace("ELEMENT", className)
                .replace(", name", ""));
    }

    @Override
    public void generateRestLeafGetter(String className) {
        generateCollectionLeafResourceGetter(className);
    }

    @Override
    public void done() {
        File classFile = new File(packageDir, className.toLowerCase(Locale.getDefault()) + ".py");
        BufferedWriter writer = null;
        try {
            try {
                classFile.createNewFile();
                classFile.deleteOnExit();
                writer = new BufferedWriter(new FileWriter(classFile));
                writer.append(source.toString());
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                }
            }
        }

    }
}

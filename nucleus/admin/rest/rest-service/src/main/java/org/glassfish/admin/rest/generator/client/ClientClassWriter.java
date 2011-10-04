/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest.generator.client;

import java.util.Collection;
import org.glassfish.admin.rest.Util;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.CommandModel.ParamModel;
import org.jvnet.hk2.config.ConfigModel;

/**
 *
 * @author jdlee
 */
public abstract class ClientClassWriter {

    protected String getMethodParameterList(CommandModel cm, boolean withType, boolean includeOptional) {
        StringBuilder sb = new StringBuilder();
        Collection<ParamModel> params = cm.getParameters();
        if ((params != null) && (!params.isEmpty())) {
            String sep = "";
            for (ParamModel model : params) {
                Param param = model.getParam();
                boolean include = true;
                if (param.optional() && !includeOptional) {
                    continue;
                }

                sb.append(sep);
                if (withType) {
                    String type = model.getType().getName();
                    if (type.startsWith("java.lang")) {
                        type = model.getType().getSimpleName();
                    }
                    sb.append(type);
                }
                sb.append(" _").append(Util.eleminateHypen(model.getName()));
                sep = ", ";
            }
        }

        return sb.toString();
    }

    public abstract void generateGetSegment(String tagName);

    public abstract void generateCommandMethod(String methodName, String httpMethod, String resourcePath, CommandModel cm);

    public abstract String generateMethodBody(CommandModel cm, String httpMethod, String resourcePath, boolean includeOptional, boolean needsMultiPart);

    public abstract void generateGettersAndSetters(String type, String methodName, String fieldName);

    public abstract void createGetChildResource(ConfigModel model, String elementName, String childResourceClassName);

    public abstract void generateCollectionLeafResourceGetter(String className);

    public abstract void generateRestLeafGetter(String className);

    public abstract void done();
}

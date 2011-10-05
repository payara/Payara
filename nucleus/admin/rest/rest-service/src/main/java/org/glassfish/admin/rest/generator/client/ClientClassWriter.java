/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package org.glassfish.admin.rest.generator.client;

import org.glassfish.api.admin.CommandModel;
import org.jvnet.hk2.config.ConfigModel;

/**
 *
 * @author jdlee
 */
public interface ClientClassWriter {

    void generateGetSegment(String tagName);

    void generateCommandMethod(String methodName, String httpMethod, String resourcePath, CommandModel cm);

    String generateMethodBody(CommandModel cm, String httpMethod, String resourcePath, boolean includeOptional, boolean needsMultiPart);

    void generateGettersAndSetters(String type, String methodName, String fieldName);

    void createGetChildResource(ConfigModel model, String elementName, String childResourceClassName);

    void generateCollectionLeafResourceGetter(String className);

    void generateRestLeafGetter(String className);

    void done();
}

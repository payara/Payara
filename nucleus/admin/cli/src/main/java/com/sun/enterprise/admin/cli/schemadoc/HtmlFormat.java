/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.cli.schemadoc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.glassfish.api.admin.config.PropertyDesc;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.types.Property;

@Service(name = "html")
public class HtmlFormat implements SchemaOutputFormat {
    private PrintWriter tocWriter;
    private PrintWriter detail;
    private Set<ClassDef> toc = new HashSet<ClassDef>();
    private File dir;
    private Map<String, ClassDef> defs;

    @SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
    @Override
    public void output(Context context) {
        dir = context.getDocDir();
        defs = context.getClassDefs();
        try {
            try {
                tocWriter = new PrintWriter(new FileWriter(new File(dir, "toc.HTML")));
                detail = new PrintWriter(new FileWriter(new File(dir, "detail.HTML")));
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
            println(tocWriter,
                "<HTML><head><link rel=\"stylesheet\" type=\"text/css\" href=\"schemadoc.css\"><style>body{margin-left:-1em;}</style></head><body>");
            println(detail,
                "<HTML><head><link rel=\"stylesheet\" type=\"text/css\" href=\"schemadoc.css\"></head><body>");
            copyResources();
            buildToc(defs.get(context.getRootClassName()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            println(tocWriter, "</ul>");
            footer(tocWriter);
            footer(detail);
            if (tocWriter != null) {
                tocWriter.close();
            }
            if (detail != null) {
                detail.close();
            }
        }
    }

    private void buildDetail(ClassDef def) {
        println(detail, "<p><table><tr>");
        println(detail, "<a name=\"" + def.getXmlName() + "\">");
        println(detail, String.format("<th colspan=\"4\" class=\"TableHeadingColor entity %s\">%s%s",
            def.isDeprecated() ? "deprecated" : "",
            def.getXmlName(), def.isDeprecated() ? " - DEPRECATED" : ""));
        println(detail, "</th></tr>");
        println(detail, "<colgroup><col width=\"35%\"></colgroup>");
        printHeaderRow(detail, "attribute", "type", "default", "required");
        final Map<String, Attribute> map = def.getAttributes();
        if (map != null) {
            for (Entry<String, Attribute> entry : map.entrySet()) {
                println(detail, String.format("<tr><td class=\"TableSubHeadingColor\">%s</td>", entry.getKey()));
                printAttributeData(entry.getValue());
            }
        }
        println(detail, "</table>");
        printPropertyData(def);
    }

    private void println(final PrintWriter writer, final String text) {
        writer.println(text);
        writer.flush();
    }

    private void printAttributeData(final Attribute annotation) {
        printKeyValue(detail, annotation != null ? annotation.dataType().getName() : null);
        printKeyValue(detail, annotation != null ? annotation.defaultValue() : null);
        printKeyValue(detail, annotation != null && annotation.required());
    }

    private void printPropertyData(final ClassDef def) {
        final Set<PropertyDesc> properties = def.getProperties();
        if (properties != null && !properties.isEmpty()) {
            println(detail, "<tr><td colspan=\"2\">");
            println(detail, "<table>");
            println(detail, "<tr class=\"TableHeadingColor\">");
            println(detail, "<th colspan=\"4\">Properties</th>");
            println(detail, "</tr>");
            println(detail, "<tr class=\"TableHeadingColor\">");
            println(detail, "<th>name</th>");
            println(detail, "<th>default</th>");
            println(detail, "<th>values</th>");
            println(detail, "<th>description</th>");
            println(detail, "</tr>");
            for (PropertyDesc property : properties) {
                println(detail, "<tr>");
                println(detail, String.format("<td class=\"TableSubHeadingColor\">%s</td>", property.name()));
                println(detail, String.format("<td class=\"nobreak\">%s</td>", property.defaultValue()));
                println(detail, String.format("<td>%s</td>",
                    property.values().length == 0 ? "" : Arrays.toString(property.values())));
                println(detail, String.format("<td>%s</td>", property.description()));
                println(detail, "</tr>");
            }
            println(detail, "</table>");
            println(detail, "</td></tr>");
        }
    }

    private void buildToc(final ClassDef def)
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (def != null/* && !"Named".equals(def.getSimpleName())*/) {
            if (!toc.contains(def)) {
                buildDetail(def);
            }
            toc.add(def);
            println(tocWriter, "<ul>");
            println(tocWriter, "<li>" + link(def));
            for (Entry<String, String> aggType : def.getAggregatedTypes().entrySet()) {
                if (!Property.class.getName().equals(aggType.getValue()) &&
                        defs != null) {
                    buildToc(defs.get(aggType.getValue()));
                }
            }
            for (ClassDef subclass : def.getSubclasses()) {
                buildToc(subclass);
            }
            println(tocWriter, "</ul>");
        }
    }

    private String link(final ClassDef def) {
        return String.format("<a %s target=\"detail\" href=\"detail.html#%s\">%s</a>",
            def.isDeprecated() ? "class=\"deprecated\"" : "", def.getXmlName(), def.getXmlName());
    }

    private void footer(final PrintWriter writer) {
        writer.println("</body></HTML>");
        writer.flush();
        writer.close();
    }

    private void copyResources() throws IOException {
        copy("/schemadoc.css");
        copy("/index.html");
    }

    private void copy(final String resource) throws IOException {
        InputStreamReader reader = null;
        PrintWriter writer = null;
        try {
            try {
                InputStream stream = getClass().getClassLoader().getResourceAsStream(resource);
                reader = new InputStreamReader(stream);
                writer = new PrintWriter(new File(dir, resource));
                char[] bytes = new char[8192];
                int read;
                while ((read = reader.read(bytes)) != -1) {
                    writer.write(bytes, 0, read);
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void printKeyValue(final PrintWriter writer, Object value) {
        println(writer, "<td>");
        if (value != null) {
            if (value instanceof Class) {
                println(writer, ((Class) value).getSimpleName());
            } else {
                println(writer, value.toString().trim());
            }
        }
        println(writer, "</td>");
    }

    private void printHeaderRow(final PrintWriter writer, final String... values) {
        writer.println("<tr class=\"TableHeadingColor\">");
        for (String value : values) {
            writer.println(String.format("<th>%s</th>", value != null ? value.trim() : " "));
        }
        writer.println("</tr>");
    }
}

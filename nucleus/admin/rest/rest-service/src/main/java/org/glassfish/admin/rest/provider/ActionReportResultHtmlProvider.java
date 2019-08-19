/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package org.glassfish.admin.rest.provider;

import org.glassfish.api.ActionReport.ExitCode;
import com.sun.enterprise.admin.report.ActionReporter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.jvnet.hk2.config.ConfigBean;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import static org.glassfish.admin.rest.provider.ProviderUtil.getHtmlForComponent;
import static org.glassfish.admin.rest.provider.ProviderUtil.getHtmlRespresentationsForCommand;
import static org.glassfish.admin.rest.provider.ProviderUtil.getHint;
import org.glassfish.admin.rest.utils.ResourceUtil;

/**
 * @author Ludovic Champenois
 */
@Provider
@Produces(MediaType.TEXT_HTML)
public class ActionReportResultHtmlProvider extends BaseProvider<ActionReportResult> {
    
    private static final String ANCHOR_OPEN = "<a href=\"";
    private static final String HEADING_END = "</h3>";
    private static final String LIST_ITEM_END = "</li>";
    private static final String LIST_END = "</ul>";
    private static final String DELETE = "DELETE";
    
    public ActionReportResultHtmlProvider() {
        super(ActionReportResult.class, MediaType.TEXT_HTML_TYPE);
    }

    @Override
    public String getContent(ActionReportResult proxy) {
        RestActionReporter ar = (RestActionReporter) proxy.getActionReport();
        StringBuilder result = new StringBuilder(ProviderUtil.getHtmlHeader(getBaseUri()));
        final String message = ResourceUtil.encodeString(ar.getCombinedMessage());

        if (!message.isEmpty()) {
            result.append("<h3>").append(message).append(HEADING_END);
        }

        if (proxy.isError()) {
            result.append("<h2>").append(ar.getActionDescription()).append(" Error:</h2>")
                    .append(proxy.getErrorMessage());
        } else {
            final Map<String, String> childResources = (Map<String, String>) ar.getExtraProperties().get("childResources");
            final List<Map<String, String>> commands = (List<Map<String, String>>) ar.getExtraProperties().get("commands");
            final MethodMetaData postMetaData = proxy.getMetaData().getMethodMetaData("POST");
            final MethodMetaData deleteMetaData = proxy.getMetaData().getMethodMetaData(DELETE);
            final MethodMetaData getMetaData = proxy.getMetaData().getMethodMetaData("GET");
            final ConfigBean entity = proxy.getEntity();

            if ((proxy.getCommandDisplayName()!=null) &&(getMetaData!=null)) {//for commands, we want the output of the command before the form
                if (entity==null) {//show extra properties only for non entity pages
                    result.append(processReport(ar));
                }
            }

            if ((postMetaData != null) && (entity == null)) {
                String postCommand = getHtmlRespresentationsForCommand(postMetaData, "POST", ( proxy.getCommandDisplayName()==null )? "Create" : proxy.getCommandDisplayName(), uriInfo.get());
                result.append(getHtmlForComponent(postCommand, "Create " + ar.getActionDescription(), ""));
            }
            if ((deleteMetaData != null) && (entity == null)) {
                String deleteCommand = getHtmlRespresentationsForCommand(deleteMetaData, DELETE, ( proxy.getCommandDisplayName()==null )? "Delete" : proxy.getCommandDisplayName(), uriInfo.get());
                result.append(getHtmlForComponent(deleteCommand, "Delete " + ar.getActionDescription(), ""));
            }
            if ((getMetaData != null) && (entity == null) &&(proxy.getCommandDisplayName()!=null )) {
                String getCommand = getHtmlRespresentationsForCommand(getMetaData, "GET", ( proxy.getCommandDisplayName()==null )? "Get" : proxy.getCommandDisplayName(), uriInfo.get());
                result.append(getHtmlForComponent(getCommand, "Get " + ar.getActionDescription(), ""));
            }
            if (entity != null) {
                String attributes = ProviderUtil.getHtmlRepresentationForAttributes(proxy.getEntity(), uriInfo.get());
                result.append(ProviderUtil.getHtmlForComponent(attributes, ar.getActionDescription() + " Attributes", ""));

                String deleteCommand = ProviderUtil.getHtmlRespresentationsForCommand(proxy.getMetaData().getMethodMetaData(DELETE), DELETE, (proxy.getCommandDisplayName() == null) ? "Delete" : proxy.getCommandDisplayName(), uriInfo.get());
                result.append(ProviderUtil.getHtmlForComponent(deleteCommand, "Delete " + entity.model.getTagName(), ""));

            } else if (proxy.getLeafContent()!=null){ //it is a single leaf @Element
                String content =
                "<form action=\"" + uriInfo.get().getAbsolutePath().toString() +"\" method=\"post\">"+
                        "<dl><dt>"+
                        "<label for=\""+proxy.getLeafContent().name+"\">"+proxy.getLeafContent().name+":&nbsp;</label>"+
                                "</dt><dd>"+
                                "<input name=\""+proxy.getLeafContent().name+"\" value =\""+proxy.getLeafContent().value+"\" type=\"text\" >"+
                                "</dd><dt class=\"button\"></dt><dd class=\"button\"><input value=\"Update\" type=\"submit\"></dd></dl>"+
                                "</form><br><hr class=\"separator\"/";
                result.append(content);

            }
            else  { //This is a monitoring result!!!

                final Map vals = (Map) ar.getExtraProperties().get("entity");

                if ((vals != null) && (!vals.isEmpty())) {
                    result.append("<ul>");

                    for (Map.Entry entry : (Set<Map.Entry>) vals.entrySet()) {

                        Object object = entry.getValue();
                        if (object == null) {
                            //do nothing
                        } else if (object instanceof Collection) {
                            if (!((Collection) object).isEmpty()) {
                                Collection c = ((Collection) object);
                                Iterator i = c.iterator();
                                result.append("<li>").append(entry.getKey());
                                result.append("<ul>");

                                while (i.hasNext()) {
                                    result.append("<li>").append(getHtmlRepresentation(i.next())).append(LIST_ITEM_END);
                                }
                                result.append(LIST_END);
                                result.append(LIST_ITEM_END);

                            }
                        } else if (object instanceof Map) {
                            if (!((Map) object).isEmpty()) {
                                Map m = (Map) object;
                                if (vals.size() != 1) {//add a link if more than 1 child
                                    result.append("<li>").append(ANCHOR_OPEN).append(uriInfo.get().getAbsolutePath().toString()).append("/")
                                            .append(entry.getKey()).append("\">").append(entry.getKey()).append("</a>");
                                } else {
                                    result.append("<li>").append(entry.getKey());
                                }
                                result.append("<ul>");

                                for (Map.Entry anEntry : (Set<Map.Entry>) m.entrySet()) {
                                    final String htmlRepresentation = getHtmlRepresentation(anEntry.getValue());
                                    if (htmlRepresentation != null) {
                                        result.append("<li>").append(anEntry.getKey()).append(" : ").append(htmlRepresentation).append(LIST_ITEM_END);
                                    }
                                }
                                result.append(LIST_END);
                                result.append(LIST_ITEM_END);


                            }
                        } else {
                            result.append("<li>").append(entry.getKey()).append(" : ").append(object.toString()).append(LIST_ITEM_END);
                        }
                    }
                    result.append(LIST_END);

                } else {//no values to show... give an hint
                    if ((childResources == null) || (childResources.isEmpty())) {
                        if ((uriInfo !=null)&&(uriInfo.get().getPath().equalsIgnoreCase("domain"))) {
                            result.append(getHint(uriInfo.get(), MediaType.TEXT_HTML));
                        }
                    }

                }
            }

            if ((childResources != null)&&(!childResources.isEmpty())) {
                String childResourceLinks = getResourcesLinks(childResources);
                result.append(ProviderUtil.getHtmlForComponent(childResourceLinks, "Child Resources", ""));
            }

            if ((commands != null) &&(!commands.isEmpty())) {
                String commandLinks = getCommandLinks(commands);
                result.append(ProviderUtil.getHtmlForComponent(commandLinks, "Commands", ""));
            }

        }
        return result.append("</div></body></html>").toString();
    }

    protected String getBaseUri() {
        if ((uriInfo != null) && (uriInfo.get() != null)) {
            return uriInfo.get().getBaseUri().toASCIIString();
        }
        return "";
    }

    protected String getResourcesLinks(Map<String, String> childResources) {
        StringBuilder links = new StringBuilder("<div>");
        for (Map.Entry<String, String> link : childResources.entrySet()) {
            links.append(ANCHOR_OPEN)
                .append(link.getValue())
                .append("\">")
                .append(link.getKey())
                .append("</a><br>");

        }

        return links.append("</div><br/>").toString();
    }

    protected String getCommandLinks(List<Map<String, String>> commands) {
        StringBuilder result = new StringBuilder("<div>");
        boolean showHiddenCommands = canShowHiddenCommands();
        for (Map<String, String> commandList : commands) {
            String command = commandList.get("command");
            String path = commandList.get("path");
            if (path.startsWith("_") && (!showHiddenCommands)) {//hidden cli command name
                result.append("<!--");//hide the link in a comment
            }
            result.append(ANCHOR_OPEN)
                    .append(ProviderUtil.getElementLink(uriInfo.get(), command))
                    .append("\">")
                    .append(command)
                    .append("</a><br>");
            if (path.startsWith("_") && (!showHiddenCommands)) {//hidden cli
                result.append("-->");
            }
        }

        return result.append("</div><br>").toString();
    }

    protected String processReport(ActionReporter ar) {

        StringBuilder result = new StringBuilder();
        String des=ar.getActionDescription();
        //check for no description, make it blank
        if (des==null){
            des="";
        }
        final String message = ResourceUtil.encodeString((ar instanceof RestActionReporter) ? ((RestActionReporter)ar).getCombinedMessage() : ar.getMessage());
        if (message!=null){
            result.append("<h2>")
                .append(des)
                .append(" output:</h2><h3>")
                .append("<pre>")
                .append(message)
                .append("</pre>")
                .append(HEADING_END);
        }
        if (ar.getActionExitCode() != ExitCode.SUCCESS) {
            result.append("<h3>Exit Code: ").append(ar.getActionExitCode().toString()).append(HEADING_END);

        }

        Properties properties = ar.getTopMessagePart().getProps();
        if (!properties.isEmpty()) {
            result.append(processProperties(properties));
        }

        Properties extraProperties = ar.getExtraProperties();
        if ((extraProperties != null) && (!extraProperties.isEmpty())) {
            if ((extraProperties.size()==1)&&(extraProperties.get("methods")!=null)){
                //do not show only methods metadata in html, not really needed
            } else {
                result.append(getExtraProperties(extraProperties));
            }
        }

        List<ActionReport.MessagePart> children = ar.getTopMessagePart().getChildren();
        if (!children.isEmpty()) {
            result.append(processChildren(children));
        }

        List<ActionReporter> subReports = ar.getSubActionsReport();
        if (!subReports.isEmpty()) {
            result.append(processSubReports(subReports));
        }

        return result.toString();
    }

    protected String processProperties(Properties props) {
        StringBuilder result = new StringBuilder("<h3>Properties</h3>");
        result.append(getHtml(props));

        return result.append("</table>").toString();
    }

    protected String getExtraProperties(Properties props) {
        StringBuilder result = new StringBuilder("<h3>Extra Properties</h3>");
        result.append(getHtml(props));

        return result.toString();
    }

    protected String processChildren(List<ActionReport.MessagePart> parts) {
        StringBuilder result = new StringBuilder("<h3>Children</h3><ul>");

        for (ActionReport.MessagePart part : parts) {
            result.append("<li><table border=\"1\" style=\"border-collapse: collapse\">")
                    .append("<tr><td>Message</td>")
                    .append("<td>")
                    .append(part.getMessage())
                    .append("</td></tr><td>Properties</td><td>")
                    .append(getHtml(part.getProps()))
                    .append("</td></tr>");
            List<ActionReport.MessagePart> children = part.getChildren();
            if (!children.isEmpty()) {
                result.append("<tr><td>Children</td><td>")
                        .append(processChildren(part.getChildren()))
                        .append("</td></tr>");
            }
            result.append("</table></li>");
        }

        return result.append(LIST_END).toString();
    }

    protected String processSubReports(List<ActionReporter> subReports) {
        StringBuilder result = new StringBuilder("<h3>Sub Reports</h3><ul>");

        for (ActionReporter subReport : subReports) {
            result.append(processReport(subReport));
        }

        return result.append(LIST_END).toString();
    }

    protected String getHtmlRepresentation(Object object) {
        String result = null;
        if (object == null) {
            return "";
        } else if (object instanceof Collection) {
            if (!((Collection)object).isEmpty()) {
                result = getHtml((Collection) object);
            }
        } else if (object instanceof Map) {
            if (!((Map)object).isEmpty()) {
                result = getHtml((Map) object);
            }
        } else {
            result = object.toString();
        }

        return result;
    }

    protected String getHtml(Collection c) {
        StringBuilder result = new StringBuilder("<ul>");
        Iterator i = c.iterator();
        while (i.hasNext()) {
            result.append("<li>")
                    .append(getHtmlRepresentation(i.next()))
                    .append(LIST_ITEM_END);
        }

        return result.append("</li></ul>").toString();
    }

    protected String getHtml(Map map) {
        StringBuilder result = new StringBuilder();
        if (!map.isEmpty()) {
            result.append("<table border=\"1\" style=\"border-collapse: collapse\"><tr><th>key</th><th>value</th></tr>");

            for (Map.Entry entry : (Set<Map.Entry>) map.entrySet()) {
                final String htmlRepresentation = getHtmlRepresentation(entry.getValue());
                if (htmlRepresentation != null) {
                    result.append("<tr><td>")
                            .append(entry.getKey())
                            .append("</td><td>")
                            .append(htmlRepresentation)
                            .append("</td></tr>");
                }
            }

            result.append("</table>");
        }

        return result.toString();
    }
}

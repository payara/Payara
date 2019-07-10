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

// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.StringUtils;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import static com.sun.enterprise.util.StringUtils.ok;
import com.sun.enterprise.admin.report.ActionReporter;
import com.sun.enterprise.admin.report.PlainTextActionReporter;
import com.sun.enterprise.admin.report.PropsFileActionReporter;
import java.lang.reflect.Proxy;
import java.util.*;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.external.statistics.Statistic;
import org.glassfish.external.statistics.Stats;
import org.glassfish.external.statistics.impl.StatisticImpl;
import org.glassfish.internal.api.*;
import org.jvnet.hk2.annotations.Optional;

import org.jvnet.hk2.annotations.Service;
import static org.glassfish.api.ActionReport.ExitCode.FAILURE;
import static org.glassfish.api.ActionReport.ExitCode.SUCCESS;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.flashlight.MonitoringRuntimeDataRegistry;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

import javax.inject.Inject;

import static com.sun.enterprise.util.SystemPropertyConstants.MONDOT;
import static com.sun.enterprise.util.SystemPropertyConstants.SLASH;
import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 *
 * @author Byron Nevins First breathed life on November 6, 2010 The copyright
 * says 1997 because one method in here has code moved verbatim from
 * GetCommand.java which started life in 1997
 *
 * Note: what do you suppose is the worst possible name for a TreeNode class?
 * Correct! TreeNode! Clashing names is why we have to explicitly use this
 * ghastly name: org.glassfish.flashlight.datatree.TreeNode all over the
 * place...
 */
@Service(name = "MonitoringReporter")
@PerLookup
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
public class MonitoringReporter extends V2DottedNameSupport {

    private final TreeMap nodeTreeToProcess = new TreeMap(); // used for get
    private List<org.glassfish.flashlight.datatree.TreeNode> nodeListToProcess =
            new ArrayList<org.glassfish.flashlight.datatree.TreeNode>(); // used for list

    public enum OutputType {

        GET, LIST
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("\nPattern=[").append(pattern).append("]").append('\n');

        if (!targets.isEmpty()) {
            for (Server server : targets) {
                if (server != null)
                    sb.append("Server=[").append(server.getName()).append("]").append('\n');
            }
        }
        else
            sb.append("No Targets");

        return sb.toString();
    }
    ///////////////////////////////////////////////////////////////////////
    ////////////////////////  The API Methods  ///////////////////////////
    ///////////////////////////////////////////////////////////////////////

    public void prepareGet(AdminCommandContext c, String arg, Boolean data) {
        aggregateDataOnly = data;
        prepare(c, arg, OutputType.GET);
    }

    public Collection<? extends AccessCheck> getAccessChecksForGet() {
        final Collection<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        for (Object obj : nodeTreeToProcess.keySet()) {
            final String name = obj.toString().replace('.', '/');
            accessChecks.add(new AccessCheck(sanitizeResourceName(name), "read"));
        }
        return accessChecks;
    }

    public Collection<? extends AccessCheck> getAccessChecksForList() {
        final Collection<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        for (org.glassfish.flashlight.datatree.TreeNode tn1 : nodeListToProcess) {
            /*
             * doList discards nodes that do not have children, but we
             * include them here in building the access checks
             * because the user needs read access to the node
             * in order to find out that it does or does not have children.
             */
            String name = tn1.getCompletePathName().replace('.', '/');
            accessChecks.add(new AccessCheck(sanitizeResourceName(name), "read"));
        }
        return accessChecks;
    }

    private String sanitizeResourceName(final String resourceName) {
        return StringUtils.replace(resourceName, "[", "_ARRAY_");
    }
    public void prepareList(AdminCommandContext c, String arg) {
        prepare(c, arg, OutputType.LIST);
    }

    public void execute() {
        // TODO remove?  make it an exception???
        if (hasError())
            return;

        runLocally();
        runRemotely();
        if (targetIsMultiInstanceCluster && isInstanceRunning()) {
            runAggregate();
        }

    }

    private boolean isInstanceRunning() {
        boolean rs = false;
        int num = 0;

        List<Server> allServers = targetService.getAllInstances();
        for (Server server : allServers) {
            if (server.isRunning()) {
                num++;
            }
        }
        if (num >= 2)
            rs = true;

        return rs;
    }

    private void runAggregate() {
        List<String> list = getOutputLines();
        ActionReport aggregateReporter = null;
        if (aggregateDataOnly) {
            plainReporter = new PlainTextActionReporter();
            aggregateReporter = plainReporter.addSubActionsReport();
        }
        else
            aggregateReporter = reporter.addSubActionsReport();
        setClusterInfo(aggregateReporter, list);
        if (aggregateDataOnly) {
            reporter = plainReporter;
            context.setActionReport(plainReporter);
        }
    }

    private List<String> getOutputLines() {
        List<String> list = null;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            reporter.writeReport(os);
            String outputMessage = os.toString();
            String lines[] = outputMessage.split("\\n");
            list = Arrays.asList(lines);
        }
        catch (Exception e) {
        }
        return list;
    }

    private ArrayList<String> getKeyValuePair(String str, String instanceName) {
        ArrayList<String> list = new ArrayList<String>(2);
        String key = null;
        String value = null;
        if (str != null) {
            key = str.substring(0, str.lastIndexOf('='));
            key = (key.substring(instanceName.length() + 1)).trim();
            value = (str.substring(str.lastIndexOf('=') + 1, str.length())).trim();
        }
        list.add(0, key);
        list.add(1, value);
        return list;
    }

    private void setClusterInfo(ActionReport aggregateReporter, List<String> list) {
        List<HashMap> data = new ArrayList<HashMap>(targets.size());
        int i;
        for (i = 0; i < targets.size(); i++) {
            data.add(new HashMap<String, String>());
        }
        HashMap<String, String> clusterInfo = new HashMap<String, String>();
        int instanceCount = 0;
        for (Server server : targets) {
            String instanceName = server.getName();
            Map<String, String> instanceMap = data.get(instanceCount);
            String key = null;
            for (String str : list) {
                if (str.contains(instanceName) && str.contains("-count =")) {
                    ArrayList<String> kv = getKeyValuePair(str, instanceName);
                    key = (String) kv.get(0);
                    instanceMap.put((String) kv.get(0), kv.get(1));
                }
                if (key != null) {
                    String desc = key.substring(0, key.indexOf("-count")) + "-description";
                    if (str.contains(desc)) {
                        ArrayList<String> kv = getKeyValuePair(str, instanceName);
                        clusterInfo.put((String) kv.get(0), kv.get(1));
                    }
                    String lastSampleTime = key.substring(0, key.indexOf("-count")) + "-lastsampletime";
                    if (str.contains(lastSampleTime)) {
                        ArrayList<String> kv = getKeyValuePair(str, instanceName);
                        clusterInfo.put(instanceName + "." + (String) kv.get(0), kv.get(1));
                        key = null;
                    }
                }
            }
            instanceCount++;
        }

        List<Server> allServers = targetService.getAllInstances();
        String instanceListStr = "";
        i = 0;
        for (Server server : allServers) {
            if (server.isRunning()) {
                if (i == 0)
                    instanceListStr = server.getName();
                else
                    instanceListStr = instanceListStr + ", " + server.getName();
                i++;
            }
        }
        aggregateReporter.appendMessage("\nComputed Aggregate Data for " + i + " instances: " + instanceListStr + " in cluster " + targetName + " :\n");
        boolean noData = true;
        HashMap<String, String> h = data.get(0);
        Iterator it = h.keySet().iterator();

        while (it.hasNext()) {
            int total = 0, max = 0, min = 0, index = 0;
            float avg = 0;
            int[] values = new int[data.size()];
            boolean flag = false;
            String s = (String) it.next();
            for (HashMap hm : data) {
                String tmp = (String) hm.get(s);
                // if tmp is null then the string is not available in all the instances, so not required to add this in the cluster information
                if (tmp == null) {
                    flag = true;
                    break;
                }
                else {
                    int count = Integer.parseInt(tmp);
                    values[index++] = count;
                    total = total + count;
                }
            }
            if (!flag) {
                noData = false;
                Arrays.sort(values);
                min = values[0];
                max = values[values.length - 1];
                avg = (float) total / (float) data.size();
                String descKey = s.substring(0, s.length() - 5) + "description";
                aggregateReporter.appendMessage(targetName + "." + s + "-total = " + total + "\n");
                aggregateReporter.appendMessage(targetName + "." + s + "-avg = " + avg + "\n");
                aggregateReporter.appendMessage(targetName + "." + s + "-max = " + max + "\n");
                aggregateReporter.appendMessage(targetName + "." + s + "-min = " + min + "\n");
                aggregateReporter.appendMessage(targetName + "." + descKey + " = " + clusterInfo.get(descKey) + "\n");
                String lastSampleTimeKey = s.substring(0, s.length() - 5) + "lastsampletime";
                long sampletime = getLastSampleTime(clusterInfo, lastSampleTimeKey, data.size());
                aggregateReporter.appendMessage(targetName + "." + lastSampleTimeKey + " = " + sampletime + "\n");
            }
        }
        if (noData) {
            aggregateReporter.appendMessage("No aggregated cluster data to report\n");
        }
    }

    private long getLastSampleTime(HashMap<String, String> clusterInfo, String lastSampleTimeKey, int numofInstances) {
        long[] values = new long[numofInstances];
        int index = 0;
        for (Map.Entry e : clusterInfo.entrySet()) {
            String key = (String) e.getKey();
            String value = (String) e.getValue();
            if (key.contains(lastSampleTimeKey)) {
                values[index++] = Long.parseLong(value);
            }
        }
        Arrays.sort(values);
        return values[values.length - 1];
    }

    ///////////////////////////////////////////////////////////////////////
    ////////////////////////  ALL PRIVATE BELOW ///////////////////////////
    ///////////////////////////////////////////////////////////////////////
    private void prepare(AdminCommandContext c, String arg, OutputType type) {
        outputType = type;
        context = c;
        prepareReporter();
        // DAS runs the show on this command.  If we are running in an
        // instance -- that means we should call runLocally() AND it also
        // means that the pattern is already perfect!

        if (isDas())
            prepareDas(arg);
        else
            prepareInstance(arg);

        prepareNodesToProcess();
    }

    /**
     * The stock ActionReport we get is too inefficient. Replace it with
     * PlainText note that we might be called with HTML or XML or JSON or
     * others!
     */
    private void prepareReporter() {
        reporter = (ActionReporter) context.getActionReport();

        if (reporter instanceof PlainTextActionReporter) {
            // already setup correctly - don't change it!!
            plainReporter = (PlainTextActionReporter) reporter;
        }
        else if (reporter instanceof PropsFileActionReporter) {
            plainReporter = new PlainTextActionReporter();
            reporter = plainReporter;
            context.setActionReport(plainReporter);
        }
        else {
            plainReporter = null;
        }
    }

    private void prepareDas(String arg) {
        // TODO throw an exception if any errors????
        try {
            setSuccess();
            userarg = arg;

            if (!validate()) {
                return;
            }
        }
        catch (Exception e) {
            setError(Strings.get("admin.get.monitoring.unknown", e.getMessage()));
            reporter.setFailureCause(e);
        }
    }

    private void prepareInstance(String arg) {
        // TODO throw an exception if any errors!
        pattern = arg;
    }

    // mostly just copied over from old "get" implementation
    // That's why it is excruciatingly unreadable...
    private void prepareNodesToProcess() {

        // don't run if this is DAS **and** DAS is not in the server list.
        // otherwise we are in an instance and definitely want to run!
        if (isDas() && !dasIsInList())
            return;

        // say the pattern is "something" -->
        // we want "server.something" for DAS and "i1.server.something" for i1
        // Yes -- this is difficult to get perfect!!!  What if user entered
        //"server.something"?

        String localPattern = prependServerDot(pattern);
        org.glassfish.flashlight.datatree.TreeNode tn = datareg.get(serverEnv.getInstanceName());

        if (tn == null) {
            return;
        }

        List<org.glassfish.flashlight.datatree.TreeNode> ltn = tn.getNodes(localPattern);
        boolean singleStat = false;

        if (ltn == null || ltn.isEmpty()) {
            org.glassfish.flashlight.datatree.TreeNode parent = tn.getPossibleParentNode(localPattern);

            if (parent != null) {
                ltn = new ArrayList<org.glassfish.flashlight.datatree.TreeNode>(1);
                ltn.add(parent);
                singleStat = true;
            }
        }

        if (!singleStat) {
            localPattern = null; // signal to method call below.  localPattern was already used above...
        }

        if (outputType == OutputType.GET) {
            prepareNodeTreeToProcess(localPattern, ltn);
        }
        else if (outputType == OutputType.LIST) {
            nodeListToProcess = ltn;
        }
    }

    private void runLocally() {

        // don't run if this is DAS **and** DAS is not in the server list.
        // otherwise we are in an instance and definitely want to run!
        if (isDas() && !dasIsInList()) {
            return;
        }

        if (outputType == OutputType.GET) {
            doGet();
        }
        else if (outputType == OutputType.LIST) {
            doList();
        }

        if (plainReporter != null) {
            plainReporter.appendMessage(cliOutput.toString());
        }
    }

    private void prepareNodeTreeToProcess(final String pattern, final List<org.glassfish.flashlight.datatree.TreeNode> ltn) {
        for (org.glassfish.flashlight.datatree.TreeNode tn1 : sortTreeNodesByCompletePathName(ltn)) {
            if (!tn1.hasChildNodes()) {
                insertNameValuePairs(nodeTreeToProcess, tn1, pattern);
            }
        }
    }

    // Byron Nevins -- copied from original implementation
    private void doGet() {

        ActionReport.MessagePart topPart = reporter.getTopMessagePart();
        Iterator it = nodeTreeToProcess.keySet().iterator();

        while (it.hasNext()) {
            Object obj = it.next();
            String line = obj.toString();
            line = line.replace(SLASH, "/") + " = " + nodeTreeToProcess.get(obj);

            if (plainReporter != null)
                cliOutput.append(line).append('\n');
            else {
                ActionReport.MessagePart part = topPart.addChild();
                part.setMessage(line);
            }
        }
        setSuccess();
    }

    private void doList() {
        // list means only print things that have children.  Don't print the children.
        ActionReport.MessagePart topPart = reporter.getTopMessagePart();

        for (org.glassfish.flashlight.datatree.TreeNode tn1 : nodeListToProcess) {
            if (tn1.hasChildNodes()) {
                String line = tn1.getCompletePathName();

                if (plainReporter != null)
                    cliOutput.append(line).append('\n');
                else {
                    ActionReport.MessagePart part = topPart.addChild();
                    part.setMessage(line);
                }
            }
        }
        setSuccess();
    }

    /**
     * This can be a bit confusing. It is sort of like a recursive call.
     * GetCommand will be called on the instance. BUT -- the pattern arg will
     * just have the actual pattern -- the target name will NOT be in there! So
     * "runLocally" will be called on the instance. this method will ONLY run on
     * DAS (guaranteed!)
     */
    private void runRemotely() {
        if (!isDas())
            return;

        List<Server> remoteServers = getRemoteServers();

        if (remoteServers.isEmpty())
            return;

        try {
            ParameterMap paramMap = new ParameterMap();
            paramMap.set("monitor", "true");
            paramMap.set("DEFAULT", pattern);
            ClusterOperationUtil.replicateCommand("get", FailurePolicy.Error, FailurePolicy.Warn,
                    FailurePolicy.Ignore, remoteServers, context, paramMap, habitat);
        }
        catch (Exception ex) {
            setError(Strings.get("admin.get.monitoring.remote.error", getNames(remoteServers)));
        }
    }

    private String prependServerDot(String s) {
        // note -- we are now running in either DAS or an instance and we are going to gather up
        // data ONLY for this server.  I.e. the DAS dispatching has already happened.
        // we really need this pattern to start with the instance-name (DAS's instance-name is "server"

        // Issue#15054
        // this is pretty intricate but this is what we want to happen for these samples:
        // asadmin get -m network.thread-pool.totalexecutedtasks-count ==> ERROR no target
        // asadmin get -m server.network.thread-pool.totalexecutedtasks-count ==> OK, return DAS's data
        // asadmin get -m *.network.thread-pool.totalexecutedtasks-count ==> OK return DAS and instances' data
        // asadmin get -m i1.network.thread-pool.totalexecutedtasks-count ==> OK return data for i1

        final String namedot = serverEnv.getInstanceName() + ".";

        if (s.startsWith(namedot))
            return s;

        return namedot + s;
    }

    private boolean validate() {
        if (datareg == null) {
            setError(Strings.get("admin.get.no.monitoring"));
            return false;
        }

        if (!initPatternAndTargets())
            return false;

        return true;
    }

    /*
     * VERY VERY complicated to get this right!
     */
    private boolean initPatternAndTargets() {
        Server das = domain.getServerNamed("server");

        // no DAS in here!
        List<Server> allServers = targetService.getAllInstances();

        allServers.add(das);

        // 0 decode special things
        // \\ == literal backslash and \ is escaping next char
        userarg = handleEscapes(userarg); // too complicated to do in-line

        // MONDOT, SLASH should be replaced with literals
        userarg = userarg.replace(MONDOT, ".").replace(SLASH, "/");

        // double star makes no sense.  The loop gets rid of "***", "****", etc.
        while (userarg.indexOf("**") >= 0)
            userarg = userarg.replace("**", "*");

        // 1.  nothing
        // 2.  *
        // 3.  *.   --> which is a weird input but let's accept it anyway!
        // 4   .   --> very weird but we'll take it
        if (!ok(userarg)
                || userarg.equals("*")
                || userarg.equals(".")
                || userarg.equals("*.")) {
            // By definition this means ALL servers and ALL data
            targets = allServers;
            pattern = "*";
            return true;
        }

        // 5.   *..
        // 6.   *.<something>
        if (userarg.startsWith("*.")) {
            targets = allServers;

            // note: it can NOT be just "*." -- there is something at posn #2 !!
            pattern = userarg.substring(2);

            // "*.." is an error
            if (pattern.startsWith(".")) {
                String specificError = Strings.get("admin.get.monitoring.nodoubledot");
                setError(Strings.get("admin.get.monitoring.invalidpattern", specificError));
                return false;
            }
            return true;
        }

        // 7.  See 14685 for an example -->  "*jsp*"
        // 16313 for another example
        if (userarg.startsWith("*")) {
            targets = allServers;
            pattern = userarg;
            return true;
        }

        // Another example:
        // servername*something*
        // IT 14778
        // note we will NOT support serv*something getting resolved to server*something
        // that's too crazy.  They have to enter a reasonable name

        // we are looking for, e.g. instance1*foo.goo*
        // target is instance1  pattern is *foo.goo*
        // instance1.something is handled below
        String re = "[^\\.]+\\*.*";

        if (userarg.matches(re)) {
            int index = userarg.indexOf("*");

            if (index < 0) { // can't happen!!
                setError(Strings.get("admin.get.monitoring.invalidtarget", userarg));
                return false;
            }
            targetName = userarg.substring(0, index);
            pattern = userarg.substring(index);
        }

        if (targetName == null) {
            int index = userarg.indexOf(".");

            if (index >= 0) {
                targetName = userarg.substring(0, index);

                if (userarg.length() == index + 1) {
                    // 8. <servername>.
                    pattern = "*";
                }
                else
                    // 9. <servername>.<pattern>
                    pattern = userarg.substring(index + 1);
            }
            else {
                // no dots in userarg
                // 10. <servername>
                targetName = userarg;
                pattern = "*";
            }
        }

        // note that "server" is hard-coded everywhere in GF code.  We're stuck with it!!

        if (targetName.equals("server") || targetName.equals("server-config")) {
            targets.add(das);
            return true;
        }

        // targetName is either 1 instance or a cluster or garbage!
        targets = targetService.getInstances(targetName);

        if (targets.isEmpty()) {
            setError(Strings.get("admin.get.monitoring.invalidtarget", userarg));
            return false;
        }

        if (targetService.isCluster(targetName) && targets.size() > 1)
            targetIsMultiInstanceCluster = true;

        return true;
    }

    private void insertNameValuePairs(
            TreeMap map, org.glassfish.flashlight.datatree.TreeNode tn1, String exactMatch) {
        String name = tn1.getCompletePathName();
        Object value = tn1.getValue();
        if (tn1.getParent() != null) {
            map.put(tn1.getParent().getCompletePathName() + DOTTED_NAME,
                    tn1.getParent().getCompletePathName());
        }
        if (value instanceof Stats) {
            for (Statistic s : ((Stats) value).getStatistics()) {
                String statisticName = s.getName();
                if (statisticName != null) {
                    statisticName = s.getName().toLowerCase(Locale.getDefault());
                }
                addStatisticInfo(s, name + "." + statisticName, map);
            }
        }
        else if (value instanceof Statistic) {
            addStatisticInfo(value, name, map);
        }
        else {
            map.put(name, value);
        }

        // IT 8985 bnevins
        // Hack to get single stats.  The code above above would take a lot of
        // time to unwind.  For development speed we just remove unwanted items
        // after the fact...
        if (exactMatch != null) {
            NameValue nv = getIgnoreBackslash(map, exactMatch);
            map.clear();

            if (nv != null) {
                map.put(nv.name, nv.value);
            }
        }
    }

    /*
     * bnevins, 1-11-11
     * Note that we can not GUESS where to put the backslash into 'pattern'.
     * If so -- we could simply add it into pattern and do a get on the HashMap.
     * Instead we have to get each and every key in the map, remove backslashes
     * and compare.
     */
    private NameValue getIgnoreBackslash(TreeMap map, String pattern) {

        if (pattern == null)
            return null;

        Object match = map.get(pattern);

        if (match != null)
            return new NameValue(pattern, match);

        pattern = pattern.replace("\\", "");
        match = map.get(pattern);

        if (match != null)
            return new NameValue(pattern, match);

        // No easy match...

        Set<Map.Entry> elems = map.entrySet();

        for (Map.Entry elem : elems) {
            String key = elem.getKey().toString();

            if (!ok(key))
                continue;

            String name = key.replace("\\", "");

            if (pattern.equals(name))
                return new NameValue(key, elem.getValue());
        }
        return null;
    }

    private void addStatisticInfo(Object value, String name, TreeMap map) {
        Map<String, Object> statsMap;
        // Most likely we will get the proxy of the StatisticImpl,
        // reconvert that so you can access getStatisticAsMap method
        if (Proxy.isProxyClass(value.getClass())) {
            statsMap = ((StatisticImpl) Proxy.getInvocationHandler(value)).getStaticAsMap();
        }
        else {
            statsMap = ((StatisticImpl) value).getStaticAsMap();
        }
        for (Map.Entry<String,Object> entry : statsMap.entrySet()) {
            map.put(name + "-" + entry.getKey(), entry.getValue());
        }
    }

    private void setError(String msg) {
        reporter.setActionExitCode(FAILURE);
        appendStatusMessage(msg);
        clear();
    }

    private void setSuccess() {
        reporter.setActionExitCode(SUCCESS);
    }

    private void appendStatusMessage(String newMessage) {
        if (plainReporter != null)
            cliOutput.append(newMessage).append('\n');
        else {
            String oldMessage = reporter.getMessage();

            if (oldMessage == null)
                reporter.setMessage(newMessage);
            else
                reporter.appendMessage("\n" + newMessage);
        }
    }

    private boolean hasError() {
        //return reporter.hasFailures();
        return reporter.getActionExitCode() == FAILURE;
    }

    private void clear() {
        targets = Collections.emptyList();
        pattern = "";
    }

    private List<Server> getRemoteServers() {
        // only call on DAS !!!
        if (!isDas())
            throw new RuntimeException("Internal Error"); // todo?

        List<Server> notdas = new ArrayList<Server>(targets.size());
        String dasName = serverEnv.getInstanceName();

        for (Server server : targets) {
            if (!dasName.equals(server.getName()))
                notdas.add(server);
        }

        return notdas;
    }

    private boolean dasIsInList() {
        return getRemoteServers().size() != targets.size();
    }

    private String getNames(List<Server> list) {
        boolean first = true;
        StringBuilder sb = new StringBuilder();

        for (Server server : list) {
            if (first)
                first = false;
            else
                sb.append(", ");

            sb.append(server.getName());
        }
        return sb.toString();
    }

    private static String handleEscapes(String s) {
        // replace double backslash with backslash
        // simply remove single backslash
        // there is probably a much better, and very very complicated way to do
        // this with regexp.  I don't care - it is only done once for each time
        // a user runs a get -m comand.
        final String UNLIKELY_STRING = "___~~~~$$$$___";
        return s.replace("\\\\", UNLIKELY_STRING).replace("\\", "").replace(UNLIKELY_STRING, "\\");
    }

    private boolean isDas() {
        return serverEnv.isDas();
    }

    /*
     * Surprise!  The variables are down here.  All the variables are private.
     * That means they are an implementation detail and are hidden at the bottom
     * of the file.
     */
    List<Server> targets = new ArrayList<Server>();
    private PlainTextActionReporter plainReporter;
    private ActionReporter reporter;
    private AdminCommandContext context;
    private String pattern;
    private String userarg;
    @Inject
    @Optional
    private MonitoringRuntimeDataRegistry datareg;
    @Inject
    private Domain domain;
    @Inject
    private Target targetService;
    @Inject
    ServerEnvironment serverEnv;
    @Inject
    ServiceLocator habitat;
    private OutputType outputType;
    private final static String DOTTED_NAME = ".dotted-name";
    private final StringBuilder cliOutput = new StringBuilder();
    private boolean targetIsMultiInstanceCluster = false;
    private String targetName;
    private Boolean aggregateDataOnly = Boolean.FALSE;

    private static class NameValue {

        String name;
        Object value;

        private NameValue(String s, Object o) {
            name = s;
            value = o;
        }
    }
}

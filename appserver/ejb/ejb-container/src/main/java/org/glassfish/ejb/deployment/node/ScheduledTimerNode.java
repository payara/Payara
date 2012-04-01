/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.deployment.node;

import com.sun.enterprise.deployment.ScheduledTimerDescriptor;
import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import java.util.logging.Level;

import com.sun.enterprise.deployment.node.DeploymentDescriptorNode;
import com.sun.enterprise.deployment.node.MethodNode;
import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.xml.EjbTagNames;

import org.w3c.dom.Node;

import java.util.Map;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class ScheduledTimerNode extends DeploymentDescriptorNode {

    ScheduledTimerDescriptor descriptor = null;

    public ScheduledTimerNode() {
        super();
        
        registerElementHandler(new XMLElement(EjbTagNames.TIMEOUT_METHOD), MethodNode.class,
                "setTimeoutMethod");

    }

    /**
     * @return the Descriptor subclass that was populated  by reading
     * the source XML file
     */
    public Object getDescriptor() {
        if (descriptor == null) {
            descriptor = (ScheduledTimerDescriptor) new ScheduledTimerDescriptor();
        }
        return descriptor;
    }

    /**
     * all sub-implementation of this class can use a dispatch table to map xml element to
     * method name on the descriptor class for setting the element value.
     *
     * @return the map with the element name as a key, the setter method as a value
     */
    protected Map getDispatchTable() {
        // no need to be synchronized for now
        Map table = super.getDispatchTable();

        table.put(EjbTagNames.TIMER_SECOND, "setSecond");
        table.put(EjbTagNames.TIMER_MINUTE, "setMinute");
        table.put(EjbTagNames.TIMER_HOUR, "setHour");
        table.put(EjbTagNames.TIMER_DAY_OF_MONTH, "setDayOfMonth");
        table.put(EjbTagNames.TIMER_MONTH, "setMonth");
        table.put(EjbTagNames.TIMER_DAY_OF_WEEK, "setDayOfWeek");
        table.put(EjbTagNames.TIMER_YEAR, "setYear");

        table.put(EjbTagNames.TIMER_PERSISTENT, "setPersistent");
        table.put(EjbTagNames.TIMER_INFO,  "setInfo");
        table.put(EjbTagNames.TIMER_TIMEZONE, "setTimezone");

        
        return table;
    }

    /**
     * receives notiification of the value for a particular tag
     *
     * @param element the xml element
     * @param value it's associated value
     */
    public void setElementValue(XMLElement element, String value) {

        if (EjbTagNames.TIMER_START.equals(element.getQName())) {
            try {
                DatatypeFactory dFactory = DatatypeFactory.newInstance();

                XMLGregorianCalendar xmlGreg = dFactory.newXMLGregorianCalendar(value);
                GregorianCalendar cal = xmlGreg.toGregorianCalendar();
                descriptor.setStart(cal.getTime());
            } catch (Exception e) {
                DOLUtils.getDefaultLogger().warning(e.getMessage());
            }

        } else if(EjbTagNames.TIMER_END.equals(element.getQName())) {
            try {
                DatatypeFactory dFactory = DatatypeFactory.newInstance();

                XMLGregorianCalendar xmlGreg = dFactory.newXMLGregorianCalendar(value);
                GregorianCalendar cal = xmlGreg.toGregorianCalendar();
                descriptor.setEnd(cal.getTime());
            } catch (Exception e) {
                DOLUtils.getDefaultLogger().warning(e.getMessage());
            }

        } else {
            super.setElementValue(element, value);
        }

    }
        
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param node name for the root element of this xml fragment
     * @param the descriptor to write
     * @return the DOM tree top node
     */
    public Node writeDescriptor(Node parent, String nodeName, Descriptor descriptor) {
        if (! (descriptor instanceof ScheduledTimerDescriptor)) {
            throw new IllegalArgumentException(getClass() +
                    " cannot handles descriptors of type " + descriptor.getClass());
        }

        ScheduledTimerDescriptor desc = (ScheduledTimerDescriptor) descriptor;

        Node timerNode = super.writeDescriptor(parent, nodeName, descriptor);

        Node scheduleNode = appendChild(timerNode, EjbTagNames.TIMER_SCHEDULE);

        appendTextChild(scheduleNode, EjbTagNames.TIMER_SECOND, desc.getSecond());
        appendTextChild(scheduleNode, EjbTagNames.TIMER_MINUTE, desc.getMinute());
        appendTextChild(scheduleNode, EjbTagNames.TIMER_HOUR, desc.getHour());
        appendTextChild(scheduleNode, EjbTagNames.TIMER_DAY_OF_MONTH, desc.getDayOfMonth());
        appendTextChild(scheduleNode, EjbTagNames.TIMER_MONTH, desc.getMonth());
        appendTextChild(scheduleNode, EjbTagNames.TIMER_DAY_OF_WEEK, desc.getDayOfWeek());
        appendTextChild(scheduleNode, EjbTagNames.TIMER_YEAR, desc.getYear());

        try {
            DatatypeFactory dFactory = DatatypeFactory.newInstance();
            GregorianCalendar cal = new GregorianCalendar();

            if (desc.getStart() != null) {
                cal.setTime(desc.getStart());
                XMLGregorianCalendar xmlGreg = dFactory.newXMLGregorianCalendar(cal);
                appendTextChild(timerNode, EjbTagNames.TIMER_START, xmlGreg.toXMLFormat());
            }

            if (desc.getEnd() != null) {
                cal.setTime(desc.getEnd());
                XMLGregorianCalendar xmlGreg = dFactory.newXMLGregorianCalendar(cal);
                appendTextChild(timerNode, EjbTagNames.TIMER_END, xmlGreg.toXMLFormat());
            }
        } catch (Exception e) {
            DOLUtils.getDefaultLogger().log(Level.WARNING, e.getMessage(), e);
        }

        MethodNode methodNode = new MethodNode();

        methodNode.writeJavaMethodDescriptor(timerNode, EjbTagNames.TIMEOUT_METHOD,
                 desc.getTimeoutMethod());
        
        appendTextChild(timerNode, EjbTagNames.TIMER_PERSISTENT,
            Boolean.toString(desc.getPersistent()));


        String tz = desc.getTimezone();
        if( tz != null ) {
            appendTextChild(timerNode, EjbTagNames.TIMER_TIMEZONE, tz);
        }

        String info = desc.getInfo();
        if( info != null ) {
            appendTextChild(timerNode, EjbTagNames.TIMER_INFO, info);
        }

        return timerNode;
     }

}

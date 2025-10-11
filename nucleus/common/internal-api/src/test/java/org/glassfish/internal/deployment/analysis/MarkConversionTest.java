/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

package org.glassfish.internal.deployment.analysis;

import org.glassfish.internal.deployment.DeploymentTracing;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import static org.glassfish.internal.deployment.DeploymentTracing.ContainerMark.LOAD;
import static org.glassfish.internal.deployment.DeploymentTracing.ContainerMark.LOADED;
import static org.glassfish.internal.deployment.DeploymentTracing.ContainerMark.PREPARE;
import static org.glassfish.internal.deployment.DeploymentTracing.ContainerMark.PREPARED;
import static org.glassfish.internal.deployment.DeploymentTracing.Mark.*;
import static org.glassfish.internal.deployment.DeploymentTracing.ContainerMark.*;
import static org.glassfish.internal.deployment.DeploymentTracing.ModuleMark.*;


public class MarkConversionTest {
    final Instant initialTime = Instant.now();

    AtomicReference<Instant> time = new AtomicReference<>(initialTime);
    Clock testClock = new Clock() {

        @Override
        public ZoneId getZone() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return time.get();
        }
    };

    @Test
    public void sampleTrace() {

        StructuredDeploymentTracing structured = new StructuredDeploymentTracing(testClock, "ejb-invoker");


//        Let's reproduce simple actual deployment trace:
//
//        Loading application [ejb-invoker] at [/ejb-invoker]
        DeploymentTracing old = new DeploymentTracing(structured);
//        Mark ARCHIVE_OPENED at 0
        offset(0);
        old.addMark(ARCHIVE_OPENED);
        old.addMark(ARCHIVE_OPENED);
//        Mark ARCHIVE_HANDLER_OBTAINED at 1
        offset(1);
        old.addMark(ARCHIVE_HANDLER_OBTAINED);
//        Mark INITIAL_CONTEXT_CREATED at 1
        old.addMark(INITIAL_CONTEXT_CREATED);
        old.addMark(ARCHIVE_HANDLER_OBTAINED);
        old.addMark(INITIAL_CONTEXT_CREATED);
//        Mark APPINFO_PROVIDED at 3
        offset(3);
        old.addMark(APPINFO_PROVIDED);
//        Mark APPNAME_DETERMINED at 3
        old.addMark(APPNAME_DETERMINED);
//        Mark TARGET_VALIDATED at 8
        offset(8);
        old.addMark(TARGET_VALIDATED);
//        Mark CONTEXT_CREATED at 9
        offset(9);
        old.addMark(CONTEXT_CREATED);
//        Mark DEPLOY at 27
        offset(27);
        old.addMark(DEPLOY);
//        Mark ARCHIVE_HANDLER_OBTAINED at 28
        offset(28);
        old.addMark(ARCHIVE_HANDLER_OBTAINED);
//        Mark PARSING_DONE at 44
        offset(44);
        old.addMark(PARSING_DONE);
//        Mark CLASS_LOADER_HIERARCHY at 53
        offset(53);
        old.addMark(CLASS_LOADER_HIERARCHY);
//        Mark CLASS_LOADER_CREATED at 61
        offset(61);
        old.addMark(CLASS_LOADER_CREATED);
//        Container : com.sun.enterprise.security.ee.SecurityContainer Mark SNIFFER_DONE at 61
        old.addContainerMark(SNIFFER_DONE, "com.sun.enterprise.security.ee.SecurityContainer");
//        Container : com.sun.enterprise.security.ee.SecurityContainer Mark GOT_CONTAINER at 61
        old.addContainerMark(GOT_CONTAINER, "com.sun.enterprise.security.ee.SecurityContainer");
//        Container : com.sun.enterprise.security.ee.SecurityContainer Mark GOT_DEPLOYER at 61
        old.addContainerMark(GOT_DEPLOYER, "com.sun.enterprise.security.ee.SecurityContainer");
//        Container : com.sun.enterprise.web.WebContainer Mark SNIFFER_DONE at 61
        old.addContainerMark(SNIFFER_DONE, "com.sun.enterprise.web.WebContainer");
//        Container : com.sun.enterprise.web.WebContainer Mark GOT_CONTAINER at 61
        old.addContainerMark(GOT_CONTAINER, "com.sun.enterprise.web.WebContainer");
//        Container : com.sun.enterprise.web.WebContainer Mark GOT_DEPLOYER at 61
        old.addContainerMark(GOT_DEPLOYER, "com.sun.enterprise.web.WebContainer");
//        Mark CONTAINERS_SETUP_DONE at 113
        offset(113);
        old.addMark(CONTAINERS_SETUP_DONE);
//        Mark CLASS_LOADER_CREATED at 147
        offset(147);
        old.addMark(CLASS_LOADER_CREATED);
//        Module ejb-invoker Mark PREPARE at 147
        old.addModuleMark(DeploymentTracing.ModuleMark.PREPARE, "ejb-invoker");
//        Container : security Mark PREPARE at 147
        old.addContainerMark(PREPARE, "security");
//        Container : security Mark PREPARED at 148
        offset(148);
        old.addContainerMark(PREPARED, "security");
//        Container : web Mark PREPARE at 148
        old.addContainerMark(PREPARE, "web");
//        Container : web Mark PREPARED at 149
        offset(149);
        old.addContainerMark(PREPARED, "web");
//        Module ejb-invoker Mark PREPARE_EVENTS at 149
        old.addModuleMark(PREPARE_EVENTS, "ejb-invoker");
//        Module ejb-invoker Mark PREPARED at 149
        old.addModuleMark(DeploymentTracing.ModuleMark.PREPARED, "ejb-invoker");
//        Mark PREPARED at 151
        offset(151);
        old.addMark(DeploymentTracing.Mark.PREPARED);
//        Mark LOAD at 152
        offset(152);
        old.addMark(DeploymentTracing.Mark.LOAD);
//        Mark LOAD at 152
        old.addMark(DeploymentTracing.Mark.LOAD);
//        Mark LOAD_EVENTS at 152
        old.addMark(DeploymentTracing.Mark.LOAD_EVENTS);
//        Mark LOADED at 153
        offset(153);
        old.addMark(DeploymentTracing.Mark.LOADED);
//        Module ejb-invoker Mark LOAD at 153
        old.addModuleMark(DeploymentTracing.ModuleMark.LOAD, "ejb-invoker");
//        Mark LOAD at 153
        old.addMark(DeploymentTracing.Mark.LOAD);
//        Container : security Mark LOAD at 153
        old.addContainerMark(LOAD, "security");
//        Container : security Mark LOADED at 153
        old.addContainerMark(LOADED, "security");
//        Container : web Mark LOAD at 153
        old.addContainerMark(LOAD, "web");
//        Container : web Mark LOADED at 153
        old.addContainerMark(LOADED, "web");
//        Mark LOAD_EVENTS at 153
        old.addMark(LOAD_EVENTS);
//        Mark LOADED at 153
        old.addMark(DeploymentTracing.Mark.LOADED);
//        Module ejb-invoker Mark LOADED at 153
        old.addModuleMark(DeploymentTracing.ModuleMark.LOADED, "ejb-invoker");
//        Mark LOAD_EVENTS at 196
        offset(196);
        old.addMark(LOAD_EVENTS);
//        Mark LOADED at 204
        offset(204);
        old.addMark(DeploymentTracing.Mark.LOADED);
//        Mark START at 204
        old.addMark(DeploymentTracing.Mark.START);
//        Module ejb-invoker Mark START at 204
        old.addModuleMark(DeploymentTracing.ModuleMark.START, "ejb-invoker");
//        Container : security Mark START at 204
        old.addContainerMark(DeploymentTracing.ContainerMark.START, "security");
//        Container : security Mark STARTED at 205
        offset(205);
        old.addContainerMark(DeploymentTracing.ContainerMark.STARTED, "security");
//        Container : web Mark START at 205
        old.addContainerMark(DeploymentTracing.ContainerMark.START, "web");
//        Container : web Mark STARTED at 325
        offset(325);
        old.addContainerMark(DeploymentTracing.ContainerMark.STARTED, "web");
//        Module ejb-invoker Mark STARTED at 325
        old.addModuleMark(DeploymentTracing.ModuleMark.STARTED, "ejb-invoker");
//        Mark START_EVENTS at 325
        old.addMark(START_EVENTS);
//        Mark STARTED at 383
        offset(383);
        old.addMark(DeploymentTracing.Mark.STARTED);
//        Mark REGISTRATION at 510
        offset(510);
        old.addMark(REGISTRATION);
//        ejb-invoker was successfully deployed in 385 milliseconds.
        old.close();

        old.print(System.out);
    }

    void advance(int ms) {
        time.updateAndGet(instant -> instant.plusMillis(ms));
    }

    void offset(int ms) {
        time.set(initialTime.plusMillis(ms));
    }
}

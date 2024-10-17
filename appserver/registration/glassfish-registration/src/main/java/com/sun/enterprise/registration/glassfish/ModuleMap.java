/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2024] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.registration.glassfish;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ModuleMap {

    /*
     * A simple mapping of module names to locations in a bit field.  Used
     * to encode module usage information.
     */

     /* 
      * This mapping was generated for the 3.1.2 release.
      */
    public static final Map<String, Integer> CONSTANT_MAP = 
            Collections.unmodifiableMap(new HashMap<String, Integer>() {{ 
                put("GlassFish-Application-Common-Module", 1);
                put("com.sun.glassfish.admingui.console-custom-branding-plugin", 2);
                put("com.sun.glassfish.branding-fragment", 3);
                put("com.sun.glassfish.coherenceweb.glassfish-coherence-web-glue", 4);
                put("com.sun.glassfish.console-lbconfig-plugin", 5);
                put("com.sun.glassfish.dasrecovery.das-backup", 6);
                put("com.sun.glassfish.dasrecovery.das-backup-gui-plugin", 7);
                put("com.sun.glassfish.monitoring.scripting.server", 8);
                put("com.sun.glassfish.performance-tuner", 9);
                put("com.sun.glassfish.performance-tuner.console-performance-tuner-plugin", 10);
                put("com.sun.glassfish.performance-tuner.performance-tuner-extra-jre-packages", 11);
                put("com.sun.grizzly.comet", 12);
                put("com.sun.grizzly.config", 13);
                put("com.sun.grizzly.framework", 14);
                put("com.sun.grizzly.http", 15);
                put("com.sun.grizzly.http-ajp", 16);
                put("com.sun.grizzly.http-servlet", 17);
                put("com.sun.grizzly.lzma", 18);
                put("com.sun.grizzly.portunif", 19);
                put("com.sun.grizzly.rcm", 20);
                put("com.sun.grizzly.utils", 21);
                put("com.sun.grizzly.websockets", 22);
                put("com.sun.jersey.contribs.jersey-moxy", 23);
                put("com.sun.jersey.contribs.jersey-multipart", 24);
                put("com.sun.jersey.glassfish.statsproviders", 25);
                put("com.sun.jersey.glassfish.v3.osgi.jersey-gf-server", 26);
                put("com.sun.jersey.glassfish.v3.osgi.jersey-gf-servlet", 27);
                put("com.sun.jersey.jersey-client", 28);
                put("com.sun.jersey.jersey-core", 29);
                put("com.sun.jersey.jersey-grizzly", 30);
                put("com.sun.jersey.jersey-json", 31);
                put("org.glassfish.jsftemplating", 32);
                put("org.eclipse.angus.mail.jakarta.mail", 33);
                put("com.sun.pkg.client", 34);
                put("com.sun.xml.bind.jaxb-osgi", 35);
                put("commonj.sdo", 36);
                put("glassfish-corba-asm", 37);
                put("glassfish-corba-codegen", 38);
                put("glassfish-corba-csiv2-idl", 39);
                put("glassfish-corba-internal-api", 40);
                put("glassfish-corba-newtimer", 41);
                put("glassfish-corba-omgapi", 42);
                put("glassfish-corba-orb", 43);
                put("glassfish-corba-orbgeneric", 44);
                put("gmbal", 45);
                put("jackson-core-asl", 46);
                put("jackson-jaxrs", 47);
                put("jackson-mapper-asl", 48);
                put("jackson-xc", 49);
                put("jakarta.persistence", 50);
                put("jakarta.servlet-api", 51);
                put("javax.xml.jaxrpc-api-osgi", 52);
                put("jakarta.xml.bind-api", 53);
                put("org.glassfish.external.management-api", 54);
                put("org.apache.felix.configadmin", 55);
                put("org.apache.felix.eventadmin", 56);
                put("org.apache.felix.fileinstall", 57);
                put("org.apache.felix.gogo.command", 58);
                put("org.apache.felix.gogo.runtime", 59);
                put("org.apache.felix.gogo.shell", 60);
                put("org.apache.felix.scr", 61);
                put("org.apache.felix.shell", 62);
                put("org.apache.felix.shell.remote", 63);
                put("org.apache.felix.shell.tui", 64);
                //put("org.codehaus.jettison.jettison", 65);
//                put("org.eclipse.persistence.antlr", 66);
                put("org.eclipse.persistence.asm", 67);
                put("org.eclipse.persistence.core", 68);
                put("org.eclipse.persistence.jpa", 69);
                put("org.eclipse.persistence.jpa.modelgen", 70);
                put("org.eclipse.persistence.moxy", 71);
                put("org.eclipse.persistence.oracle", 72);
                put("org.eclipse.persistence.sdo", 73);
                put("org.glassfish.com.sun.faces", 74);
                put("org.glassfish.com.sun.faces", 75);
                put("org.glassfish.docs.help.console-cluster-plugin-help", 76);
                put("org.glassfish.docs.help.console-common-full-plugin-help", 77);
                put("org.glassfish.docs.help.console-common-help", 78);
                put("org.glassfish.docs.help.console-corba-plugin-help", 79);
                put("org.glassfish.docs.help.console-ejb-lite-plugin-help", 80);
                put("org.glassfish.docs.help.console-ejb-plugin-help", 81);
                put("org.glassfish.docs.help.console-jca-plugin-help", 82);
                put("org.glassfish.docs.help.console-jdbc-plugin-help", 83);
                put("org.glassfish.docs.help.console-jms-plugin-help", 84);
                put("org.glassfish.docs.help.console-jts-plugin-help", 85);
                put("org.glassfish.docs.help.console-web-plugin-help", 86);
                put("org.glassfish.fighterfish.osgi-cdi", 87);
                put("org.glassfish.fighterfish.osgi-ee-resources", 88);
                put("org.glassfish.fighterfish.osgi-ejb-container", 89);
                put("org.glassfish.fighterfish.osgi-http", 90);
                put("org.glassfish.fighterfish.osgi-javaee-base", 91);
                put("org.glassfish.fighterfish.osgi-jdbc", 92);
                put("org.glassfish.fighterfish.osgi-jpa", 93);
                put("org.glassfish.fighterfish.osgi-jpa-extension", 94);
                put("org.glassfish.fighterfish.osgi-jta", 95);
                put("org.glassfish.fighterfish.osgi-web-container", 96);
                put("org.glassfish.ha.ha-api", 97);
                put("org.glassfish.hk2.auto-depends", 98);
                put("org.glassfish.hk2.class-model", 99);
                put("org.glassfish.hk2.config", 100);
                put("org.glassfish.hk2.config-types", 101);
                put("org.glassfish.hk2.core", 102);
                //put("org.glassfish.hk2.external.asm-repackaged", 103);
                //put("org.glassfish.hk2.external.bean-validator", 104);
                put("org.glassfish.hk2.hk2", 105);
                put("org.glassfish.hk2.osgi-adapter", 106);
                put("org.glassfish.hk2.osgi-resource-locator", 107);
                put("fish.payara.server.internal.admin.backup", 108);
                put("fish.payara.server.internal.admin.cli", 109);
                put("fish.payara.server.internal.admin.cli-optional", 110);
                put("fish.payara.server.core.admin.config-api", 111);
                put("fish.payara.server.internal.admin.core", 112);
                put("fish.payara.server.internal.admin.javax.management.j2ee", 113);
                put("fish.payara.server.core.admin.launcher", 114);
                put("fish.payara.server.core.admin.monitoring-core", 115);
                put("fish.payara.server.internal.admin.rest-service", 116);
                put("fish.payara.server.internal.admin.server-mgmt", 117);
                put("fish.payara.server.internal.admin.util", 118);
                put("fish.payara.admingui.console-cluster-plugin", 119);
                put("fish.payara.admingui.console-common", 120);
                put("fish.payara.admingui.console-common-full-plugin", 121);
                put("fish.payara.admingui.console-payara-branding-plugin", 122);
                put("fish.payara.admingui.console-corba-plugin", 123);
                put("fish.payara.admingui.console-ejb-lite-plugin", 124);
                put("fish.payara.admingui.console-ejb-plugin", 125);
                put("fish.payara.admingui.console-jca-plugin", 126);
                put("fish.payara.admingui.console-jdbc-plugin", 127);
                put("fish.payara.admingui.console-jms-plugin", 128);
                put("fish.payara.admingui.console-jts-plugin", 129);
                put("fish.payara.admingui.console-plugin-service", 130);
                //put("fish.payara.admingui.console-updatecenter-plugin", 131);
                put("fish.payara.admingui.console-web-plugin", 132);
                put("fish.payara.admingui.dataprovider", 133);
                put("fish.payara.server.internal.appclient.client.acc-config", 134);
                put("fish.payara.server.internal.appclient.client.gf-client-module", 135);
                put("fish.payara.server.internal.appclient.server.appclient-connector", 136);
                put("fish.payara.server.internal.appclient.server.appclient-server-core", 137);
                put("fish.payara.server.internal.cluster.admin", 138);
                put("fish.payara.server.internal.cluster.cli", 139);
                put("fish.payara.server.internal.cluster.common", 140);
                put("fish.payara.server.internal.cluster.gms-adapter", 141);
                put("fish.payara.server.internal.cluster.gms-bootstrap", 142);
                put("fish.payara.server.internal.cluster.ssh", 143);
                put("fish.payara.server.internal.common.amx-all", 144);
                put("fish.payara.server.core.common.annotation-framework", 145);
                put("fish.payara.server.core.common.container-common", 146);
                put("fish.payara.server.core.common.glassfish-api", 147);
                put("fish.payara.server.core.common.glassfish-ee-api", 148);
                put("fish.payara.server.core.common.glassfish-mbeanserver", 149);
                put("fish.payara.server.core.common.glassfish-naming", 150);
                put("fish.payara.server.core.common.internal-api", 151);
                put("fish.payara.server.core.common.scattered-archive-api", 152);
                put("fish.payara.server.core.common.simple-glassfish-api", 153);
                put("fish.payara.server.core.common.stats77", 154);
                put("fish.payara.server.internal.common.util", 155);
                put("fish.payara.server.internal.connectors.admin", 156);
                put("fish.payara.server.internal.connectors.gf-connectors-connector", 157);
                put("fish.payara.server.internal.connectors.inbound-runtime", 158);
                put("fish.payara.server.internal.connectors.internal-api", 159);
                put("fish.payara.server.internal.connectors.jakarta.resource", 160);
                put("fish.payara.server.internal.connectors.runtime", 161);
                put("fish.payara.server.internal.connectors.work-management", 162);
                put("fish.payara.server.internal.core.branding", 163);
                put("fish.payara.server.core.nucleus.glassfish", 164);
                put("fish.payara.server.internal.core.glassfish-extra-jre-packages", 165);
                put("fish.payara.server.internal.core.jakartaee-kernel", 166);
                put("fish.payara.server.core.nucleus.kernel", 167);
                put("fish.payara.server.core.nucleus.logging", 168);
                put("fish.payara.server.internal.deployment.admin", 169);
                put("fish.payara.server.internal.deployment.autodeploy", 170);
                put("fish.payara.server.internal.deployment.common", 171);
                put("fish.payara.server.internal.deployment.deployment-client", 172);
                put("fish.payara.server.core.deployment.dol", 173);
                put("fish.payara.server.internal.deployment.javaee-core", 174);
                put("fish.payara.server.internal.deployment.javaee-full", 175);
                put("fish.payara.server.internal.deployment.javax.enterprise.deploy", 176);
                put("fish.payara.server.internal.ejb.ejb-container", 177);
                put("fish.payara.server.internal.ejb.gf-ejb-connector", 178);
                put("fish.payara.server.internal.ejb.internal-api", 179);
                put("fish.payara.server.internal.ejb.jakarta.ejb", 180);
                put("fish.payara.server.internal.repackage.ant", 181);
                put("fish.payara.server.internal.repackage.antlr-repackaged", 182);
                put("fish.payara.server.internal.repackage.dbschema-repackaged", 183);
                put("fish.payara.server.internal.repackage.j-interop-repackaged", 184);
                put("fish.payara.server.internal.repackage.jmxremote_optional-repackaged", 185);
                put("fish.payara.server.internal.repackage.ldapbp-repackaged", 186);
                put("fish.payara.server.internal.repackage.libpam4j-repackaged", 187);
                put("fish.payara.server.internal.repackage.schema2beans-repackaged", 188);
                put("fish.payara.server.internal.repackage.trilead-ssh2-repackaged", 189);
                put("fish.payara.server.internal.extras.grizzly-container", 190);
                put("fish.payara.server.internal.extras.osgi-container", 191);
                put("fish.payara.server.internal.flashlight.flashlight-extra-jdk-packages", 192);
                put("fish.payara.server.internal.flashlight.framework", 193);
                put("fish.payara.server.internal.ha.ha-file-store", 194);
                put("fish.payara.server.internal.ha.shoal-cache-bootstrap", 195);
                put("fish.payara.server.internal.ha.shoal-cache-store", 196);
                put("fish.payara.server.internal.javaee-api.jakarta.annotation", 197);
                put("fish.payara.server.internal.javaee-api.jakarta.jms", 198);
                put("fish.payara.server.internal.javaee-api.jakarta.servlet.jsp", 199);
                put("fish.payara.server.internal.javaee-api.jakarta.servlet.jsp.jstl", 200);
                put("fish.payara.server.internal.jdbc.admin", 201);
                put("fish.payara.server.internal.jms.admin", 202);
                put("fish.payara.server.internal.jms.core", 203);
                put("fish.payara.server.internal.loadbalancer.load-balancer-admin", 204);
                put("fish.payara.server.internal.orb.connector", 205);
                put("fish.payara.server.internal.orb.enabler", 206);
                put("fish.payara.server.internal.orb.iiop", 207);
                put("fish.payara.server.internal.persistence.cmp.ejb-mapping", 208);
                put("fish.payara.server.internal.persistence.cmp.enhancer", 209);
                put("fish.payara.server.internal.persistence.cmp.generator-database", 210);
                put("fish.payara.server.internal.persistence.cmp.internal-api", 211);
                put("fish.payara.server.internal.persistence.cmp.model", 212);
                put("fish.payara.server.internal.persistence.cmp.support-ejb", 213);
                put("fish.payara.server.internal.persistence.cmp.support-sqlstore", 214);
                put("fish.payara.server.internal.persistence.cmp.utility", 215);
                put("fish.payara.server.internal.persistence.common", 216);
                put("fish.payara.server.internal.persistence.glassfish-oracle-jdbc-driver-packages", 217);
                put("fish.payara.server.internal.persistence.jpa-connector", 218);
                put("fish.payara.server.internal.registration.glassfish-registration", 219);
                put("fish.payara.server.internal.registration.registration-api", 220);
                put("fish.payara.server.internal.registration.registration-impl", 221);
                put("fish.payara.server.internal.security", 222);
                put("fish.payara.server.internal.security.appclient.security", 223);
                put("fish.payara.server.internal.security.ejb.security", 224);
                put("fish.payara.server.internal.security.inmemory.jacc.provider", 225);
                put("fish.payara.server.core.security.jaspic.provider.framework", 226);
                put("fish.payara.server.internal.security.jakarta.security.auth.message", 227);
                put("fish.payara.server.internal.security.jakarta.security.jacc", 228);
                put("fish.payara.server.core.security.ssl-impl", 229);
                put("fish.payara.server.core.security.websecurity", 230);
                put("fish.payara.server.internal.security.webservices.security", 231);
                put("fish.payara.server.internal.transaction.internal-api", 232);
                put("fish.payara.server.internal.transaction.jakarta.transaction", 233);
                put("fish.payara.server.internal.transaction.jta", 234);
                put("fish.payara.server.internal.transaction.jts", 235);
                put("fish.payara.server.internal.web.cli", 236);
                put("fish.payara.server.internal.web.core", 237);
                put("fish.payara.server.core.web.gf-web-connector", 238);
                put("fish.payara.server.core.web.glue", 239);
                put("fish.payara.server.internal.web.gui-plugin-common", 240);
                put("fish.payara.server.internal.web.ha", 241);
                put("fish.payara.server.internal.web.jsf-connector", 242);
                put("fish.payara.server.internal.web.jspcaching-connector", 243);
                put("fish.payara.server.internal.web.jstl-connector", 244);
                put("fish.payara.server.internal.web.naming", 245);
                put("fish.payara.server.core.web.war-util", 246);
                put("fish.payara.server.core.web.web-embed.api", 247);
                put("fish.payara.server.internal.web.web-embed.impl", 248);
                put("fish.payara.server.internal.web.weld-integration", 249);
                put("fish.payara.server.internal.web.weld-integration-fragment", 250);
                put("fish.payara.server.internal.webservices.connector", 251);
                put("fish.payara.server.internal.webservices.jsr109-impl", 252);
                put("fish.payara.server.internal.webservices.metro-glue", 253);
                put("fish.payara.server.internal.webservices.soap-tcp", 254);
                put("org.glassfish.metro.webservices-api-osgi", 255);
                put("org.glassfish.metro.webservices-extra-jdk-packages", 256);
                put("org.glassfish.metro.webservices-osgi", 257);
                put("org.glassfish.web.el-impl", 258);
                put("org.glassfish.web.jsp-impl", 259);
                put("org.glassfish.web.jstl-impl", 260);
                put("org.jboss.weld.osgi-bundle", 261);
                put("org.jvnet.mimepull", 262);
                put("org.shoal.cache", 263);
                put("org.shoal.gms-api", 264);
                put("org.shoal.gms-impl", 265);
                put("stax2-api", 266);
                put("woodstox-core-asl", 267);
                put("com.fasterxml.jackson.dataformat.xml", 268);
                put("org.objectweb.asm", 269);
                put("org.objectweb.asm.commons", 270);
                put("org.objectweb.asm.tree", 271);
                put("org.objectweb.asm.tree.analysis", 272);
                put("org.objectweb.asm.util", 273);
            }});

    public ModuleMap() {
    }

    public static Map<String, Integer> getMap() {
        return CONSTANT_MAP;
    }
}

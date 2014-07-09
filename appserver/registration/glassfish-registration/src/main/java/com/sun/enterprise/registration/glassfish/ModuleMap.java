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
                put("com.sun.jsftemplating", 32);
                put("com.sun.mail.javax.mail", 33);
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
                put("javax.persistence", 50);
                put("javax.servlet-api", 51);
                put("javax.xml.jaxrpc-api-osgi", 52);
                put("jaxb-api", 53);
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
                put("org.codehaus.jettison.jettison", 65);
                put("org.eclipse.persistence.antlr", 66);
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
                put("org.glassfish.hk2.external.asm-all-repackaged", 103);
                put("org.glassfish.hk2.external.bean-validator", 104);
                put("org.glassfish.hk2.hk2", 105);
                put("org.glassfish.hk2.osgi-adapter", 106);
                put("org.glassfish.hk2.osgi-resource-locator", 107);
                put("org.glassfish.main.admin.backup", 108);
                put("org.glassfish.main.admin.cli", 109);
                put("org.glassfish.main.admin.cli-optional", 110);
                put("org.glassfish.main.admin.config-api", 111);
                put("org.glassfish.main.admin.core", 112);
                put("org.glassfish.main.admin.javax.management.j2ee", 113);
                put("org.glassfish.main.admin.launcher", 114);
                put("org.glassfish.main.admin.monitoring-core", 115);
                put("org.glassfish.main.admin.rest-service", 116);
                put("org.glassfish.main.admin.server-mgmt", 117);
                put("org.glassfish.main.admin.util", 118);
                put("org.glassfish.main.admingui.console-cluster-plugin", 119);
                put("org.glassfish.main.admingui.console-common", 120);
                put("org.glassfish.main.admingui.console-common-full-plugin", 121);
                put("org.glassfish.main.admingui.console-community-branding-plugin", 122);
                put("org.glassfish.main.admingui.console-corba-plugin", 123);
                put("org.glassfish.main.admingui.console-ejb-lite-plugin", 124);
                put("org.glassfish.main.admingui.console-ejb-plugin", 125);
                put("org.glassfish.main.admingui.console-jca-plugin", 126);
                put("org.glassfish.main.admingui.console-jdbc-plugin", 127);
                put("org.glassfish.main.admingui.console-jms-plugin", 128);
                put("org.glassfish.main.admingui.console-jts-plugin", 129);
                put("org.glassfish.main.admingui.console-plugin-service", 130);
                put("org.glassfish.main.admingui.console-updatecenter-plugin", 131);
                put("org.glassfish.main.admingui.console-web-plugin", 132);
                put("org.glassfish.main.admingui.dataprovider", 133);
                put("org.glassfish.main.appclient.client.acc-config", 134);
                put("org.glassfish.main.appclient.client.gf-client-module", 135);
                put("org.glassfish.main.appclient.server.appclient-connector", 136);
                put("org.glassfish.main.appclient.server.appclient-server-core", 137);
                put("org.glassfish.main.cluster.admin", 138);
                put("org.glassfish.main.cluster.cli", 139);
                put("org.glassfish.main.cluster.common", 140);
                put("org.glassfish.main.cluster.gms-adapter", 141);
                put("org.glassfish.main.cluster.gms-bootstrap", 142);
                put("org.glassfish.main.cluster.ssh", 143);
                put("org.glassfish.main.common.amx-all", 144);
                put("org.glassfish.main.common.annotation-framework", 145);
                put("org.glassfish.main.common.container-common", 146);
                put("org.glassfish.main.common.glassfish-api", 147);
                put("org.glassfish.main.common.glassfish-ee-api", 148);
                put("org.glassfish.main.common.glassfish-mbeanserver", 149);
                put("org.glassfish.main.common.glassfish-naming", 150);
                put("org.glassfish.main.common.internal-api", 151);
                put("org.glassfish.main.common.scattered-archive-api", 152);
                put("org.glassfish.main.common.simple-glassfish-api", 153);
                put("org.glassfish.main.common.stats77", 154);
                put("org.glassfish.main.common.util", 155);
                put("org.glassfish.main.connectors.admin", 156);
                put("org.glassfish.main.connectors.gf-connectors-connector", 157);
                put("org.glassfish.main.connectors.inbound-runtime", 158);
                put("org.glassfish.main.connectors.internal-api", 159);
                put("org.glassfish.main.connectors.javax.resource", 160);
                put("org.glassfish.main.connectors.runtime", 161);
                put("org.glassfish.main.connectors.work-management", 162);
                put("org.glassfish.main.core.branding", 163);
                put("org.glassfish.main.core.glassfish", 164);
                put("org.glassfish.main.core.glassfish-extra-jre-packages", 165);
                put("org.glassfish.main.core.javaee-kernel", 166);
                put("org.glassfish.main.core.kernel", 167);
                put("org.glassfish.main.core.logging", 168);
                put("org.glassfish.main.deployment.admin", 169);
                put("org.glassfish.main.deployment.autodeploy", 170);
                put("org.glassfish.main.deployment.common", 171);
                put("org.glassfish.main.deployment.deployment-client", 172);
                put("org.glassfish.main.deployment.dol", 173);
                put("org.glassfish.main.deployment.javaee-core", 174);
                put("org.glassfish.main.deployment.javaee-full", 175);
                put("org.glassfish.main.deployment.javax.enterprise.deploy", 176);
                put("org.glassfish.main.ejb.ejb-container", 177);
                put("org.glassfish.main.ejb.gf-ejb-connector", 178);
                put("org.glassfish.main.ejb.internal-api", 179);
                put("org.glassfish.main.ejb.javax.ejb", 180);
                put("org.glassfish.main.external.ant", 181);
                put("org.glassfish.main.external.antlr-repackaged", 182);
                put("org.glassfish.main.external.dbschema-repackaged", 183);
                put("org.glassfish.main.external.j-interop-repackaged", 184);
                put("org.glassfish.main.external.jmxremote_optional-repackaged", 185);
                put("org.glassfish.main.external.ldapbp-repackaged", 186);
                put("org.glassfish.main.external.libpam4j-repackaged", 187);
                put("org.glassfish.main.external.schema2beans-repackaged", 188);
                put("org.glassfish.main.external.trilead-ssh2-repackaged", 189);
                put("org.glassfish.main.extras.grizzly-container", 190);
                put("org.glassfish.main.extras.osgi-container", 191);
                put("org.glassfish.main.flashlight.flashlight-extra-jdk-packages", 192);
                put("org.glassfish.main.flashlight.framework", 193);
                put("org.glassfish.main.ha.ha-file-store", 194);
                put("org.glassfish.main.ha.shoal-cache-bootstrap", 195);
                put("org.glassfish.main.ha.shoal-cache-store", 196);
                put("org.glassfish.main.javaee-api.javax.annotation", 197);
                put("org.glassfish.main.javaee-api.javax.jms", 198);
                put("org.glassfish.main.javaee-api.javax.servlet.jsp", 199);
                put("org.glassfish.main.javaee-api.javax.servlet.jsp.jstl", 200);
                put("org.glassfish.main.jdbc.admin", 201);
                put("org.glassfish.main.jms.admin", 202);
                put("org.glassfish.main.jms.core", 203);
                put("org.glassfish.main.loadbalancer.load-balancer-admin", 204);
                put("org.glassfish.main.orb.connector", 205);
                put("org.glassfish.main.orb.enabler", 206);
                put("org.glassfish.main.orb.iiop", 207);
                put("org.glassfish.main.persistence.cmp.ejb-mapping", 208);
                put("org.glassfish.main.persistence.cmp.enhancer", 209);
                put("org.glassfish.main.persistence.cmp.generator-database", 210);
                put("org.glassfish.main.persistence.cmp.internal-api", 211);
                put("org.glassfish.main.persistence.cmp.model", 212);
                put("org.glassfish.main.persistence.cmp.support-ejb", 213);
                put("org.glassfish.main.persistence.cmp.support-sqlstore", 214);
                put("org.glassfish.main.persistence.cmp.utility", 215);
                put("org.glassfish.main.persistence.common", 216);
                put("org.glassfish.main.persistence.glassfish-oracle-jdbc-driver-packages", 217);
                put("org.glassfish.main.persistence.jpa-connector", 218);
                put("org.glassfish.main.registration.glassfish-registration", 219);
                put("org.glassfish.main.registration.registration-api", 220);
                put("org.glassfish.main.registration.registration-impl", 221);
                put("org.glassfish.main.security", 222);
                put("org.glassfish.main.security.appclient.security", 223);
                put("org.glassfish.main.security.ejb.security", 224);
                put("org.glassfish.main.security.inmemory.jacc.provider", 225);
                put("org.glassfish.main.security.jaspic.provider.framework", 226);
                put("org.glassfish.main.security.javax.security.auth.message", 227);
                put("org.glassfish.main.security.javax.security.jacc", 228);
                put("org.glassfish.main.security.ssl-impl", 229);
                put("org.glassfish.main.security.websecurity", 230);
                put("org.glassfish.main.security.webservices.security", 231);
                put("org.glassfish.main.transaction.internal-api", 232);
                put("org.glassfish.main.transaction.javax.transaction", 233);
                put("org.glassfish.main.transaction.jta", 234);
                put("org.glassfish.main.transaction.jts", 235);
                put("org.glassfish.main.web.cli", 236);
                put("org.glassfish.main.web.core", 237);
                put("org.glassfish.main.web.gf-web-connector", 238);
                put("org.glassfish.main.web.glue", 239);
                put("org.glassfish.main.web.gui-plugin-common", 240);
                put("org.glassfish.main.web.ha", 241);
                put("org.glassfish.main.web.jsf-connector", 242);
                put("org.glassfish.main.web.jspcaching-connector", 243);
                put("org.glassfish.main.web.jstl-connector", 244);
                put("org.glassfish.main.web.naming", 245);
                put("org.glassfish.main.web.war-util", 246);
                put("org.glassfish.main.web.web-embed.api", 247);
                put("org.glassfish.main.web.web-embed.impl", 248);
                put("org.glassfish.main.web.weld-integration", 249);
                put("org.glassfish.main.web.weld-integration-fragment", 250);
                put("org.glassfish.main.webservices.connector", 251);
                put("org.glassfish.main.webservices.jsr109-impl", 252);
                put("org.glassfish.main.webservices.metro-glue", 253);
                put("org.glassfish.main.webservices.soap-tcp", 254);
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
            }});

    public ModuleMap() {
    }

    public static Map<String, Integer> getMap() {
        return CONSTANT_MAP;
    }
}

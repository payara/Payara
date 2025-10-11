<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2018 Payara Foundation and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of either the GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    https://github.com/payara/Payara/blob/main/LICENSE.txt
    See the License for the specific
    language governing permissions and limitations under the License.

    When distributing the software, include this License Header Notice in each
    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.

    GPL Classpath Exception:
    The Payara Foundation designates this particular file as subject to the "Classpath"
    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
    file that accompanied this code.

    Modifications:
    If applicable, add the following below the License Header, with the fields
    enclosed by brackets [] replaced by your own identifying information:
    "Portions Copyright [year] [name of copyright owner]"

    Contributor(s):
    If you wish your version of this file to be governed by only the CDDL or
    only the GPL Version 2, indicate your decision by adding "[Contributor]
    elects to include this software in this distribution under the [CDDL or GPL
    Version 2] license."  If you don't indicate a single choice of license, a
    recipient has the option to distribute your version of this file under
    either the CDDL, the GPL Version 2 or to extend the choice of license to
    its licensees as provided above.  However, if you add GPL Version 2 code
    and therefore, elected the GPL Version 2 license, then the option applies
    only if the new code is made subject to such option by the copyright
    holder.
-->

<sun:tabSet id="microprofileTabs" immediate="$boolean{true}" selected="#{sessionScope.microprofileTabs}">
    <sun:tab id="microprofileConfigTab" immediate="$boolean{true}" 
             text="$resource{i18n_microprofile.microprofile.configuration.configTab}" 
             toolTip="$resource{i18n_microprofile.microprofile.configuration.configTab} Tab" >
        <!command
        setSessionAttribute(key="microprofileTabs" value="microprofileConfigTab");
        gf.redirect(page="#{request.contextPath}/microprofile/microprofile/specs/configConfiguration.jsf?configName=#{configName}");
        />
    </sun:tab>
    <sun:tab id="microprofileFaultToleranceTab" immediate="$boolean{true}" 
             text="$resource{i18n_microprofile.microprofile.configuration.faultToleranceTab}" 
             toolTip="$resource{i18n_microprofile.microprofile.configuration.faultToleranceTab} Tab" >
        <!command
        setSessionAttribute(key="microprofileTabs" value="microprofileFaultToleranceTab");
        gf.redirect(page="#{request.contextPath}/microprofile/microprofile/specs/faultToleranceConfiguration.jsf?configName=#{configName}");
        />
    </sun:tab>
   <sun:tab id="microprofileHealthCheckTab" immediate="$boolean{true}" 
             text="$resource{i18n_microprofile.microprofile.configuration.healthCheckTab}" 
             toolTip="$resource{i18n_microprofile.microprofile.configuration.healthCheckTab} Tab" >
        <!command
        setSessionAttribute(key="microprofileTabs" value="microprofileHealthCheckTab");
        gf.redirect(page="#{request.contextPath}/microprofile/microprofile/specs/healthCheckConfiguration.jsf?configName=#{configName}");
        />
    </sun:tab>
   <sun:tab id="microprofileMetricsTab" immediate="$boolean{true}" 
             text="$resource{i18n_microprofile.microprofile.configuration.metricsTab}" 
             toolTip="$resource{i18n_microprofile.microprofile.configuration.metricsTab} Tab" >
        <!command
        setSessionAttribute(key="microprofileTabs" value="microprofileMetricsTab");
        gf.redirect(page="#{request.contextPath}/microprofile/microprofile/specs/metricsConfiguration.jsf?configName=#{configName}");
        />
    </sun:tab>
   <sun:tab id="microprofileOpenAPITab" immediate="$boolean{true}" 
             text="$resource{i18n_microprofile.microprofile.configuration.openAPITab}" 
             toolTip="$resource{i18n_microprofile.microprofile.configuration.openAPITab} Tab" >
        <!command
        setSessionAttribute(key="microprofileTabs" value="microprofileOpenAPITab");
        gf.redirect(page="#{request.contextPath}/microprofile/microprofile/specs/openAPIConfiguration.jsf?configName=#{configName}");
        />
    </sun:tab>
   <sun:tab id="microprofileOpenTracingTab" immediate="$boolean{true}" 
             text="$resource{i18n_microprofile.microprofile.configuration.openTracingTab}" 
             toolTip="$resource{i18n_microprofile.microprofile.configuration.openTracingTab} Tab" >
        <!command
        setSessionAttribute(key="microprofileTabs" value="microprofileOpenTracingTab");
        gf.redirect(page="#{request.contextPath}/microprofile/microprofile/specs/openTracingConfiguration.jsf?configName=#{configName}");
        />
    </sun:tab>
</sun:tabSet>
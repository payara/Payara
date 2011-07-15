<#--
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
#
# The contents of this file are subject to the terms of either the GNU
# General Public License Version 2 only ("GPL") or the Common Development
# and Distribution License("CDDL") (collectively, the "License").  You
# may not use this file except in compliance with the License.  You can
# obtain a copy of the License at
# https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
# or packager/legal/LICENSE.txt.  See the License for the specific
# language governing permissions and limitations under the License.
#
# When distributing the software, include this License Header Notice in each
# file and include the License file at packager/legal/LICENSE.txt.
#
# GPL Classpath Exception:
# Oracle designates this particular file as subject to the "Classpath"
# exception as provided by Oracle in the GPL Version 2 section of the License
# file that accompanied this code.
#
# Modifications:
# If applicable, add the following below the License Header, with the fields
# enclosed by brackets [] replaced by your own identifying information:
# "Portions Copyright [year] [name of copyright owner]"
#
# Contributor(s):
# If you wish your version of this file to be governed by only the CDDL or
# only the GPL Version 2, indicate your decision by adding "[Contributor]
# elects to include this software in this distribution under the [CDDL or GPL
# Version 2] license."  If you don't indicate a single choice of license, a
# recipient has the option to distribute your version of this file under
# either the CDDL, the GPL Version 2 or to extend the choice of license to
# its licensees as provided above.  However, if you add GPL Version 2 code
# and therefore, elected the GPL Version 2 license, then the option applies
# only if the new code is made subject to such option by the copyright
# holder.
#
--> 

<#list sections as theSection>
  <#if (sections?size = 1)>
    <scrollpane border="none" ${sizeAttr}="${contentpanel_size_m2}" name="gContentComponent" horizontalscrollbarpolicy="JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED" verticalscrollbarpolicy="JScrollPane.VERTICAL_SCROLLBAR_NEVER" Layout="GridBagLayout">
    <gridbagconstraints gridx="0" gridy="0" weightx="1" weighty="1" gridwidth="1" gridheight="1" fill="GridBagConstraints.BOTH" anchor="GridBagConstraints.CENTER" insets="0,0,0,0"/>
      <scrpanel border="none" background="FFFFFF" ${sizeAttr}="${contentpanel_size_m2}" name="gContents" Layout="GridBagLayout" >
      <gridbagconstraints gridx="0" gridy="0" weightx="1" weighty="1" gridwidth="1" gridheight="1" fill="GridBagConstraints.BOTH" anchor="GridBagConstraints.CENTER" insets="0,0,0,0"/>
          ${.vars["${theSection}"]}
    </scrpanel>
    </scrollpane>
  </#if>
</#list>
<#if (sections?size > 1)>
  <tabbedpane border="none" background="FFFFFF" ${sizeAttr}="${contentpanel_size_m2}" name="gContentComponent" selectedIndex="0">
    <gridbagconstraints gridx="0" gridy="0" weightx="1" weighty="1" gridwidth="1" gridheight="1" fill="GridBagConstraints.BOTH" anchor="GridBagConstraints.CENTER" insets="0,0,0,0"/>
    <#list sections as theSection>
      <scrollpane border="none" ${sizeAttr}="${contentpanel_size_m3}" name="${theSection}" horizontalscrollbarpolicy="JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED" verticalscrollbarpolicy="JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED">
      <scrpanel border="none" Layout="GridBagLayout" Autoscrolls="true" ${sizeAttr}="${contentpanel_size_m4}" name="${theSection}" background="FFFFFF" >
       <gridbagconstraints gridx="0" gridy="0" weightx="0" weighty="0" fill="GridBagConstraints.NONE" anchor="GridBagConstraints.NORTHWEST" />
        ${.vars["${theSection}"]}
      </scrpanel>
      </scrollpane>
    </#list>
  </tabbedpane>
</#if>


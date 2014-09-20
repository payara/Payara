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

  <panel id="gMainPanel" name="gMainPanel" background="${mainpanel_bgcolor}" constraints="BorderLayout.CENTER" insets="12,12,12,12" Layout="GridBagLayout">

    <#--      L E F T   N A V I G A T I O N   P A N E L    -->
    <bgimagepanel id="gLeftPanelHolder" bgImage="${navigpanel_image}" Layout="GridBagLayout">
      <gridbagconstraints gridx="0" gridy="0" gridwidth="1" gridheight="4" weightx="0" weighty="1" fill="GridBagConstraints.VERTICAL" anchor="GridBagConstraints.NORTHWEST"/>
      <bgimagepanel name="topImagePanel"  bgImage="${top_left_image}" >
            <gridbagconstraints gridx="0" gridy="0" gridwidth="1" gridheight="1" weightx="0" weighty="0" insets="20,0,20,0" />
      </bgimagepanel>
      <#include "leftpanel.ftl">
      <bgimagepanel name="bottomImagePanel"  bgImage="${bottom_left_image}" >
            <gridbagconstraints gridx="0" gridy="3" gridwidth="1" gridheight="1" weightx="1" weighty="1" anchor="GridBagConstraints.SOUTH" insets="20,5,60,0" />
         </bgimagepanel>
    </bgimagepanel>

    <#--      T O P   L O G O   P A N E L    -->
    <bgimagepanel id="gLogoPanel" bgImage="${logopanel_image}" Layout="GridBagLayout">
      <gridbagconstraints gridx="1" gridy="0" gridwidth="1" gridheight="1" weightx="1" weighty="0" fill="GridBagConstraints.HORIZONTAL" anchor="GridBagConstraints.NORTHEAST"/>
      <smoothlabel id="gTitle1" Font="${title1_font}" Foreground="${title1_color}" text="${curPageTitle}">
        <gridbagconstraints gridx="1" gridy="0" weightx="1" weighty="1" anchor="GridBagConstraints.SOUTHWEST" insets="${contentpanel_insets}"/>
      </smoothlabel>
    </bgimagepanel>

    <#--      C O N T E N T   P A N E L    -->
    <shadowborderpanel id="gContentComponentHolder" name="gContentComponentHolder" <#if (sections?size = 1)> bordercolor="${shadow_border_color}" </#if> background="grey" minimumSize="${contentpanel_size}" preferredSize="${contentpanel_size}" Layout="GridBagLayout">
      <gridbagconstraints gridx="1" gridy="1" gridwidth="1" gridheight="2" weightx="1" weighty="1" fill="GridBagConstraints.BOTH" anchor="GridBagConstraints.EAST" insets="${contentpanel_insets}"/>
      <#include "contentpanel.ftl">
    </shadowborderpanel>

    <#--      B U T T O N S   P A N E L    -->
    <panel id="gNavButtonsPanelHolder" name="gNavButtonsPanelHolder" background="${btnpanel_bgcolor}" Layout="GridBagLayout">
      <gridbagconstraints gridx="1" gridy="3" gridwidth="1" gridheight="1" weightx="1" weighty="0" fill="GridBagConstraints.HORIZONTAL" anchor="GridBagConstraints.SOUTH" insets="${buttonpanel_insets}"/>
      <#include "buttonpanel.ftl">
    </panel>

  </panel>

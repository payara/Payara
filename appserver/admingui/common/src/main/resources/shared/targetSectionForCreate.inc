<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of either the GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
    or packager/legal/LICENSE.txt.  See the License for the specific
    language governing permissions and limitations under the License.

    When distributing the software, include this License Header Notice in each
    file and include the License file at packager/legal/LICENSE.txt.

    GPL Classpath Exception:
    Oracle designates this particular file as subject to the "Classpath"
    exception as provided by Oracle in the GPL Version 2 section of the License
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

<!-- shared/targetSectionForCreate.inc -->

<sun:propertySheet id="targetSection">
    <!beforeCreate
        gf.getChildrenNamesList(endpoint="#{sessionScope.REST_URL}/clusters/cluster", result="#{requestScope.clusterList}");
        gf.listInstances(optionKeys={"standaloneonly"} optionValues={"true"}, instances="#{pageSession.targetList}" );
        gf.listCombine(list="#{pageSession.targetList}",  list2="#{requestScope.clusterList}");
        listAdd(list="#{pageSession.targetList}", value="server" index="0");

        setAttribute(key="one" value="$int{1}");
	    setAttribute(key="targetSize" value="#{pageSession.targetList.size()}");
        //println("=========== #{requestScope.targetSize}");
        //println("===========#{pageSession.targetList}");

        if( #{requestScope.targetSize}=#{requestScope.one} ){
            setPageSessionAttribute(key="showTargetSection",  value="#{false}");
            if (#{pageSession.setDefaultTarget}){
                setPageSessionAttribute(key="selectedTargets" value=["server"] );
            }
        }
        if( #{requestScope.targetSize}>#{requestScope.one} ){
            setPageSessionAttribute(key="showTargetSection",  value="#{true}");
            //when issue#12596 is fixed.  we should not preselect target.
            //Because for the edit case, it has been set previously.
            //for create case, we should leave it blank and warn user to select one.
            if (#{pageSession.setDefaultTarget}){
                gf.containedIn(list="#{pageSession.targetList}" testStr="#{pageSession.targetValue}" contain=>$attribute{selectedTargetExists});
                if (!#{requestScope.selectedTargetExists}) {
		    convertListToArray(list={}, array="#{pageSession.selectedTargets}");
                }
                if (#{selectedTargetExists}) {
                    listAdd(value="#{pageSession.targetValue}" result=>$attribute{selectedTargetsList});
                    convertListToArray(list="#{selectedTargetsList}", array="#{pageSession.selectedTargets}");
                }
            }
        }
        setPageSessionAttribute(key="useCheckBox" value="#{true}");
        setPageSessionAttribute(key="useString" value="#{false}");

        gf.convertListToOptionArray(list="#{pageSession.targetList}", optionArray="#{pageSession.availableTargets}");
    />
    <sun:propertySheetSection id="targetSectionId" visible="#{pageSession.showTargetSection}" label="$resource{i18n.common.TargetTableTitle}"  >
        <sun:property id="addRemoveProp" noWrap="#{true}" overlapLabel="#{false}">
#include "/common/shared/commonAddRemove.inc"
        </sun:property>
        <!afterCreate
            getClientId(component="$this{component}" clientId="#{pageSession.targetSectionId}");
        />
    </sun:propertySheetSection>

</sun:propertySheet>


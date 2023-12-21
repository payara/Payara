Payara Platform 3rd Party Licenses

DO NOT TRANSLATE OR LOCALIZE.

=========================================

TABLE OF CONTENTS -
=========================================

I. Components

=========================================
I. COMPONENTS
=========================================

The following software (or certain identified files distributed with the
software) may be included in this product. Unless otherwise specified,
the software identified in this file is licensed under the licenses
described below. The disclaimers and copyright notices provided are
based on information made available to Payara Foundation by the third
party licensors listed.

****************************************

---------------------------------------------------
<#function licenseFormat licenses>
    <#assign result = ""/>
    <#list licenses as license>
        <#assign result = result + " (" + license + ")"/>
    </#list>
<#return result>
</#function>
<#function artifactFormat p>
    <#if p.name?index_of('Unnamed') &gt; -1>
<#return p.artifactId + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - " + (p.url!"no url defined") + ")">
    <#else>
<#return p.name + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - " + (p.url!"no url defined") + ")">
    </#if>
</#function>
<#if dependencyMap?size == 0>
    The project has no dependencies.
<#else>
    <#list dependencyMap as e >
        <#assign project = e.getKey()/>
        <#assign licenses = e.getValue()/>
${artifactFormat(project)}
${licenseFormat(licenses)}
    </#list>
</#if>
---------------------------------------------------
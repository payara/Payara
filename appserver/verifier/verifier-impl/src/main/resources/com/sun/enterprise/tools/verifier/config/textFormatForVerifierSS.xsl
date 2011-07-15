<?xml version="1.0" ?>
<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.

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

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	        xmlns:fo="http://www.w3.org/1999/XSL/Format"
		version="1.0" >
    <xsl:output method="text" indent="yes"/>
    
    <xsl:template match="/">
	<xsl:apply-templates />
    </xsl:template>
    <xsl:template match="static-verification">
          ---------------------------
          STATIC VERIFICATION RESULTS
          ---------------------------

	<xsl:apply-templates select="failure-count" />

        <xsl:apply-templates select="error" />
        
        <xsl:apply-templates select="application" />

        <xsl:apply-templates select="appclient" />
        
        <xsl:apply-templates select="ejb" />
	
        <xsl:apply-templates select="web" />
        
        <xsl:apply-templates select="connector" />
	
        <xsl:apply-templates select="other" />
        
          ----------------------------------
          END OF STATIC VERIFICATION RESULTS
          ----------------------------------
    </xsl:template>


    <!-- NOW LIST ALL THE PATTERN RULES-->
    <xsl:template match="application">

          -------------------------------------
          RESULTS FOR APPLICATION-RELATED TESTS
          -------------------------------------
	<xsl:apply-templates/>
    </xsl:template>


    <xsl:template match="appclient">

          -----------------------------------
          RESULTS FOR APPCLIENT-RELATED TESTS
          -----------------------------------
	<xsl:apply-templates/>
    </xsl:template>


    <xsl:template match="ejb">

          -----------------------------
          RESULTS FOR EJB-RELATED TESTS
          -----------------------------
	<xsl:apply-templates/>
    </xsl:template>

    
    <xsl:template match="web">

          -----------------------------
          RESULTS FOR WEB-RELATED TESTS
          -----------------------------
	<xsl:apply-templates/>
    </xsl:template>
    
    
    <xsl:template match="connector">

          -----------------------------------
          RESULTS FOR CONNECTOR-RELATED TESTS
          -----------------------------------
	<xsl:apply-templates/>
    </xsl:template>
    
    
    <xsl:template match="other">

          -----------------------------------
          RESULTS FOR OTHER XML-RELATED TESTS
          -----------------------------------
	<xsl:apply-templates/>
    </xsl:template>

    
    <xsl:template match="error">

          -----------------------------------------------------
          ERRORS THAT OCCURRED WHILE RUNNING STATIC VERIFICATION
          ----------------------------------------------------- 
	<xsl:apply-templates/>
    </xsl:template>


    <xsl:template match="error-name">
	Error Name : <xsl:value-of select="." />
    </xsl:template>


    <xsl:template match="error-description">
	Error Description : <xsl:value-of select="." />
    </xsl:template>


    <xsl:template match="failure-count">
	 ----------------------------------
	 NUMBER OF FAILURES/WARNINGS/ERRORS
	 ----------------------------------
	 # of Failures : <xsl:value-of select="failure-number" />
         # of Warnings : <xsl:value-of select="warning-number" />
	 # of Errors : <xsl:value-of select="error-number" />

    </xsl:template>

    
    <xsl:template match="failed">
	 --------------
	 FAILED TESTS : 
	 --------------
	<xsl:apply-templates/>
    </xsl:template>


    <xsl:template match="passed">
	 ---------------
	 PASSED TESTS :
	 ---------------
	<xsl:apply-templates/>
    </xsl:template>

    
    <xsl:template match="warning">
	 -----------
	 WARNINGS :
	 -----------
	<xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="not-applicable">
	 ---------------------
	 NOTAPPLICABLE TESTS :
	 ---------------------
	<xsl:apply-templates/>
    </xsl:template>

    
    <xsl:template match="test">
	 Test Name : <xsl:value-of select="test-name" />
	 Test Assertion : <xsl:value-of select="test-assertion" />
	 Test Description : <xsl:value-of select="test-description" />
    </xsl:template>

    
</xsl:stylesheet>

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

<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:Reporter="http://jse.east.sun.com/sqe/wspack/reporter/result.dtd"
	version="1.0">

<xsl:output method="html" indent="yes"/>
<xsl:strip-space elements="description"/>
<xsl:variable name="filler">&#xa0;</xsl:variable>
<xsl:variable name="description-filler">This Test Suite has no description</xsl:variable>

<xsl:variable name="passColor">silver</xsl:variable>
<xsl:variable name="failColor">red</xsl:variable>
<xsl:variable name="dnrColor">yellow</xsl:variable>
<xsl:variable name="testPassColor">0099CC</xsl:variable>

<xsl:template match="/">
	<html>
		<A NAME="TOP"/>
		<head>
			<title>GlassFish V3 Test Results</title>
		</head>
		<body BGCOLOR="white">
			<xsl:apply-templates/>
		</body>
	</html>
</xsl:template>

<xsl:template match="report">
	<CENTER>
		<br/>
		<H2>GlassFish V3 Test Result Report</H2>
		<xsl:apply-templates/>
	</CENTER>
</xsl:template>

<xsl:template match="date">
	<H3>Execution Date:<xsl:value-of select="."/></H3>
</xsl:template>


<xsl:template match="configuration">

	<TABLE BORDER="1">
		<TR>
			<TH COLSPAN="2">Configuration Information</TH>
		</TR>
		<TR>
			<TD>Machine Name</TD>
			<TD><xsl:value-of select="machineName"/></TD>
		</TR>
		<TR>
			<TD>OS</TD>
			<TD><xsl:value-of select="os"/></TD>
		</TR>
		<TR>
			<TD>JDK Version</TD>
			<TD><xsl:value-of select="jdkVersion"/></TD>
		</TR>
	</TABLE>

	<xsl:variable
		name="testSuiteCount"
		select="count(../testsuites/testsuite)"/>
	<xsl:variable
		name="testCount"
		select="count(../testsuites/testsuite/tests/test)"/>
	<xsl:variable
		name="passedTestCount"
		select="count(../testsuites/testsuite/tests/test/status[@value='pass'])"/>
	<xsl:variable
		name="failedTestCount"
		select="count(../testsuites/testsuite/tests/test/status[@value='fail'])"/>
	<xsl:variable
		name="dnrTestCount"
		select="count(../testsuites/testsuite/tests/test/status[@value='did_not_run'])"/>
	<xsl:variable
		name="testCaseCount"
		select="count(../testsuites/testsuite/tests/test/testcases/testcase)"/>
	<xsl:variable
		name="passedTestCaseCount"
		select="count(../testsuites/testsuite/tests/test/testcases/testcase/status[@value='pass'])"/>
	<xsl:variable
		name="failedTestCaseCount"
			select="count(../testsuites/testsuite/tests/test/testcases/testcase/status[@value='fail'])"/>
	<xsl:variable
		name="dnrTestCaseCount"
			select="count(../testsuites/testsuite/tests/test/testcases/testcase/status[@value='did_not_run'])"/>

	<HR/>
	<A NAME="Summary"/>
	<H3> Summary Test Results </H3>
	
	<TABLE BORDER="1">
		<TR>
			<TH>Item</TH>
			<TH>Total</TH>
			<TH>Pass</TH>
			<TH>Fail</TH>
			<TH>Did Not Run</TH>
		</TR>
		<TR>
			<TD ALIGN="CENTER">Test Suites</TD>
			<TD ALIGN="RIGHT"><xsl:value-of select="$testSuiteCount"/></TD>
			<TD ALIGN="RIGHT"><xsl:value-of select="$filler"/></TD>
			<TD ALIGN="RIGHT"><xsl:value-of select="$filler"/></TD>
			<TD ALIGN="RIGHT"><xsl:value-of select="$filler"/></TD>
		</TR>
		<TR>
			<TD ALIGN="CENTER">Test Cases</TD>
			<TD ALIGN="RIGHT"><xsl:value-of select="$testCaseCount"/></TD>
			<TD ALIGN="RIGHT"><xsl:value-of select="$passedTestCaseCount"/></TD>
			<TD ALIGN="RIGHT"><xsl:value-of select="$failedTestCaseCount"/></TD>
			<TD ALIGN="RIGHT"><xsl:value-of select="$dnrTestCaseCount"/></TD>
		</TR>
	</TABLE>
</xsl:template>


<xsl:template match="testsuites">
	<HR/>
	<A NAME="DetailedResults"/>
	<H2> Detailed Results </H2>
	<TABLE BORDER="1">
	<TR>
		<TD ALIGN="CENTER"> <B> Test Suite Link </B> </TD>
		<TD ALIGN="CENTER"> <B> Fail Count </B> </TD>
		<TD ALIGN="CENTER"> <B> Pass Count </B> </TD>
		<TD ALIGN="CENTER"> <B> Total Count </B> </TD>
	</TR>
		<xsl:for-each select="testsuite">
	         <xsl:variable
	        	name="testCount"
		       select="count(.//testcase)"/>

	         <xsl:variable
	        	name="failtestCount"
		       select="count(.//testcase/status[@value='fail'])"/>

	         <xsl:variable
	        	name="passtestCount"
		       select="count(.//testcase/status[@value='pass'])"/>


	<TR>
				<TD ALIGN="CENTER"> 
					<A HREF="#{generate-id(id)}"><xsl:value-of select="id"/></A> 
				</TD>
				<TD ALIGN="CENTER"> 
					<FONT COLOR="RED"><xsl:value-of select="$failtestCount"/></FONT> 
				</TD>
				<TD ALIGN="CENTER"> 
					<FONT COLOR="GREEN"><xsl:value-of select="$passtestCount"/></FONT> 
				</TD>
				<TD ALIGN="CENTER"> 
					<FONT COLOR="BLUE"><xsl:value-of select="$testCount"/></FONT> 
				</TD>

	</TR>
		</xsl:for-each>
	</TABLE>
	<HR/>
	<xsl:for-each select="testsuite">

		<xsl:variable name="myname">
			<xsl:choose>
				<xsl:when test="name!=''">
					<xsl:value-of select="name"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$filler"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:variable name="mydescription">
			<xsl:choose>
				<xsl:when test="description!=''">
					<xsl:value-of select="description"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$description-filler"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
                   
	         <xsl:variable
	        	name="mytestCount"
		       select="count(.//testcase)"/>

	         <xsl:variable
	        	name="myFailtestCount"
		       select="count(.//testcase/status[@value='fail'])"/>

	         <xsl:variable
	        	name="myPasstestCount"
		       select="count(.//testcase/status[@value='pass'])"/>

		<TABLE BORDER="0" BGCOLOR="silver">
			<TR>
				<TD>Testsuite Number</TD>
				<TD><A NAME="{generate-id(./id)}"><xsl:number/></A></TD>
			</TR>
			<TR>
				<TD>Testsuite Id</TD>
				<TD><xsl:value-of select="id"/></TD>
			</TR>
			<TR>
				<TD>Testsuite Name</TD>
				<TD><xsl:value-of select="$myname"/></TD>
			</TR>
			<TR>
				<TD>Testsuite Description</TD>
				<TD><xsl:value-of select="$mydescription"/></TD>
			</TR>
			<TR>
				<TD>Total Test Cases Run</TD>
				<TD><xsl:value-of select="$mytestCount"/></TD>
			</TR>
			<TR>
				<TD>Total Test Cases Failed</TD>
				<TD><xsl:value-of select="$myFailtestCount"/></TD>
			</TR>
			<TR>
				<TD>Total Test Cases Passed</TD>
				<TD><xsl:value-of select="$myPasstestCount"/></TD>
			</TR>
		</TABLE>

		<TABLE BORDER="1" BGCOLOR="silver"> 
			<TR VALIGN="TOP">
				<TH> Name </TH> 
				<TH> Status </TH> 
			</TR>

			<xsl:apply-templates select="tests"/>
		</TABLE>
		<BR/>
<P ALIGN="LEFT"> 
	<FONT SIZE="-2"> 
		[<A HREF="#DetailedResults"> Detailed Results </A>|
		<A HREF="#Summary"> Summary  </A>|
		<A HREF="#TOP"> Top </A>]
	</FONT>
</P>
	</xsl:for-each>
</xsl:template>

<xsl:template match="tests">
	<xsl:for-each select="test">
		<xsl:variable name="myname">
			<xsl:choose>
				<xsl:when test="name!=''">
					<xsl:value-of select="name"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$filler"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:if test="status/@value='pass'"> 
			<xsl:call-template name="test-row">
				<xsl:with-param name="color" select="$testPassColor"/>
			</xsl:call-template>
		</xsl:if> 

		<xsl:if test="status/@value='fail'">
			<xsl:call-template name="test-row">
				<xsl:with-param name="color" select="$failColor"/>
			</xsl:call-template>
		</xsl:if>

		<xsl:if test="status/@value='did_not_run'">
			<xsl:call-template name="test-row">
				<xsl:with-param name="color" select="$dnrColor"/>
			</xsl:call-template>
		</xsl:if>

		<xsl:apply-templates select="testcases"/>
	</xsl:for-each>	
</xsl:template>

<xsl:template name="test-row">
	<xsl:param name="color"/>

	<xsl:variable name="myname">
		<xsl:choose>
			<xsl:when test="name!=''">
				<xsl:value-of select="name"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$filler"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>

	<xsl:variable name="mystatus">
		<xsl:choose>
			<xsl:when test="status!=''">
				<xsl:value-of select="status"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$filler"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>

	<xsl:value-of select="concat('&lt;TR BGCOLOR=', $color, '&gt;')" disable-output-escaping="yes"/>
		<TD VALIGN="TOP"><xsl:value-of select="id"/> </TD>
		<xsl:value-of select="concat('&lt;TD  VALIGN=TOP BGCOLOR=', $color, '&gt;')" disable-output-escaping="yes"/>
		<xsl:value-of select="status/@value"/>
		<xsl:value-of select="concat('&lt;', '/TD', '&gt;')" disable-output-escaping="yes"/>
	<xsl:value-of select="concat('&lt;', '/TR', '&gt;')" disable-output-escaping="yes"/>
</xsl:template>

<xsl:template match="testcases">
	<xsl:for-each select="testcase">
		<xsl:if test="status/@value='pass'"> 
			<xsl:call-template name="testcase-row">
				<xsl:with-param name="color" select="$passColor"/>
			</xsl:call-template>
		</xsl:if> 

		<xsl:if test="status/@value='fail'">
			<xsl:call-template name="testcase-row">
				<xsl:with-param name="color" select="$failColor"/>
			</xsl:call-template>
		</xsl:if>

		<xsl:if test="status/@value='did_not_run'">
			<xsl:call-template name="testcase-row">
				<xsl:with-param name="color" select="$dnrColor"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:for-each>	
</xsl:template>

<xsl:template name="testcase-row">
	<xsl:param name="color"/>

	<xsl:variable name="myname">
		<xsl:choose>
			<xsl:when test="name!=''">
				<xsl:value-of select="name"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$filler"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>

	<TR>
		<TD VALIGN="TOP"><xsl:value-of select="id"/> </TD>
	<!--	<TD VALIGN="TOP"><xsl:value-of select="$myname"/> </TD> -->
		<xsl:value-of select="concat('&lt;TD  VALIGN=TOP BGCOLOR=', $color, '&gt;')" disable-output-escaping="yes"/>
		<xsl:value-of select="status/@value"/>
		<xsl:value-of select="concat('&lt;', '/TD', '&gt;')" disable-output-escaping="yes"/>
	</TR>
</xsl:template>

</xsl:stylesheet>

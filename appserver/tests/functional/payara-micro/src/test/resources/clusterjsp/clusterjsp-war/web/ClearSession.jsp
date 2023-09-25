<%--

    Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License v. 2.0, which is available at
    http://www.eclipse.org/legal/epl-2.0.

    This Source Code may also be made available under the following Secondary
    Licenses when the conditions for such availability set forth in the
    Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
    version 2 with the GNU Classpath Exception, which is available at
    https://www.gnu.org/software/classpath/license.html.

    SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0

--%>

<HTML>
<HEAD><TITLE>Cluster - Ha JSP Sample </TITLE></HEAD>
<BODY>

<% String action = request.getParameter("action");
   System.out.println("ClearSession.jsp: invalidating session");
   if (action != null && action.equals("CLEAR SESSION")) {
        session.invalidate();
   }
%>
<BR><BR><BR>
Served From Server: <b><%= request.getServerName() %></b>

<BR><BR>
<B>Instruction</B>
<UL>
<LI>Click on START NEW SESSION to start a new session</LI>
</UL>
<BR>
<A HREF="HaJsp.jsp" NAME="Link3">START NEW SESSION</A>
</BODY>
</HTML>

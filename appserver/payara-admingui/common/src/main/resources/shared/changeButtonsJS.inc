<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.

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

  #  When editing this file, make sure that there is backslash escape is at the END of the line

 " <script type="text/javascript">

  " function changeButtonAndDropDown(){   \
     var components = new Array();  \
     var gp = "$pageSession{topActionGroup}";  \
     components[0] = gp.concat(":button1");  \
     components[1] = gp.concat(":dropdown1");  \
     changeButtons(components,"$pageSession{tableId}");  \
       \
  }

" function changeOneDropdown(){   \
     var components = new Array();  \
     var gp = "$pageSession{topActionGroup}";  \
     components[1] = gp.concat(":dropdown1");  \
     changeButtons(components,"$pageSession{tableId}");  \
       \
  }
  
  "  function changeOneTableButton3(){   \
     var buttons = new Array();  \
     var gp = "$pageSession{topActionGroup}";  \
     buttons[0] = gp.concat(":button1");  \
     changeButtons(buttons,"$pageSession{tableId3}");  \
       \
  }
  
  "  function changeOneTableButton(){   \
     var buttons = new Array();  \
     var gp = "$pageSession{topActionGroup}";  \
     buttons[0] = gp.concat(":button1");  \
     changeButtons(buttons,"$pageSession{tableId}");  \
       \
  }
  
  " function changeTwoTableButtons(){   \
     var buttons = new Array();  \
     var gp = "$pageSession{topActionGroup}";  \
     buttons[0] = gp.concat(":button1");  \
     buttons[1] = gp.concat(":button2");  \
     changeButtons(buttons,"$pageSession{tableId}");  \
       \
  }
    
  " function changeThreeTableButtons(){    \
     var buttons = new Array();  \
     var gp = "$pageSession{topActionGroup}";  \
     buttons[0] = gp.concat(":button1");  \
     buttons[1] = gp.concat(":button2");  \
     buttons[2] = gp.concat(":button3");  \
     changeButtons(buttons,"$pageSession{tableId}");  \
  }

  " function changeThreeTableButtons2(){  \
     var buttons = new Array();  \
     var gp = "$pageSession{topActionGroup2}";  \
     buttons[0] = gp.concat(":button1");  \
     buttons[1] = gp.concat(":button2");  \
     buttons[2] = gp.concat(":button3");  \
     changeButtons(buttons,"$pageSession{tableId2}");  \
  }

  " function changeButtons(buttons,tableId){   \
    var table = document.getElementById(tableId);  \
    var selections = table.getAllSelectedRowsCount();  \
    var disabled = (selections > 0) ? false : true;  \
    for (count=0; count < buttons.length; count++) {  \
       var element = document.getElementById(buttons[count]);  \
       if (element) {  \
         if (element.setDisabled) {  \
           element.setDisabled(disabled);  \
         }  \
         else {  \
           element.disabled = disabled;  \
         }  \
       }  \
     }  \
  }

  " function initAllRows() {  \
    var table = document.getElementById("$pageSession{tableId}");  \
    table.initAllRows();  \
  }

  " function initAllRows2() {  \
    var table = document.getElementById("$pageSession{tableId2}");  \
    table.initAllRows();  \
  }

  " function initAllRows3() {  \
    var table = document.getElementById("$pageSession{tableId3}");  \
    table.initAllRows();  \
  }

  " function initAllRows4() {  \
    var table = document.getElementById("$pageSession{tableId4}");  \
    table.initAllRows();  \
  }

 " </script>

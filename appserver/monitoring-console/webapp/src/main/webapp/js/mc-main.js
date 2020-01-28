/*
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
  
   The contents of this file are subject to the terms of either the GNU
   General Public License Version 2 only ("GPL") or the Common Development
   and Distribution License("CDDL") (collectively, the "License").  You
   may not use this file except in compliance with the License.  You can
   obtain a copy of the License at
   https://github.com/payara/Payara/blob/master/LICENSE.txt
   See the License for the specific
   language governing permissions and limitations under the License.
  
   When distributing the software, include this License Header Notice in each
   file and include the License file at glassfish/legal/LICENSE.txt.
  
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
*/

/*jshint esversion: 8 */

Chart.defaults.global.defaultFontColor = "#007FFF";
Chart.defaults.global.tooltips.enabled = false;

/**
 * The different parts of the Monitoring Console are added as the below properties by the individual files.
 */
const MonitoringConsole =  {

  /**
   * Static configuration data for page presets.
   */
  Data: {},

   /**
    * Functions of manipulate the model of the MC (often returns a layout that is applied to the View)
    **/ 
  Model: {},

   /**
    * Functions to update the actual HTML page of the MC
    **/
	View: {},

   /**
    * API functions to talk to the server.
    **/ 
  Controller: {},

   /**
    * Functions specifically to take the data and prepare the display particular chart type using the underlying charting library.
    *
    * Each of the type objects below shares the same public API that is used by Model and View to apply the model to the chart to update the view properly.
    **/
	Chart: {

    /**
     * A collection of general adapter functions for the underlying chart library
     */ 
    Common: {},
   /**
    * Line chart adapter API for monitoring series data
    **/
    Line: {},
   /**
    * Bar chart adapter API for monitoring series data
    **/
    Bar: {},

    /**
     * Trace 'gantt chart' like API, this is not a strict adapter API as the other two as the data to populate this is specific to traces
     */
    Trace: {},

  }
};
MonitoringConsole.Chart.getAPI = function(widget) {
  switch (widget.type) {
    default:
    case 'line': return MonitoringConsole.Chart.Line;
    case 'bar': return MonitoringConsole.Chart.Bar;
    case 'alert': return MonitoringConsole.Chart.Line;
  }
};

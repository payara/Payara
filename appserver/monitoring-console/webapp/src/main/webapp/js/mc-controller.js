/*
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
  
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

/**
 * API to talk to the server.
 *
 * Main purpose is isolate and document the API between client and server.
 **/
MonitoringConsole.Controller = (function() {

   function requestWithJsonBody(method, url, queryData, onSuccess, onFailure) {
      $.ajax({
         url: url,
         type: method,
         data: JSON.stringify(queryData),
         contentType:"application/json; charset=utf-8",
         dataType:"json",
      }).done(onSuccess).fail(onFailure);
   }

   function requestWithoutBody(method, url, onSuccess, onFailure) {
      $.ajax({ type: method, url: url }).done(onSuccess).fail(onFailure);
   }

   /**
    * @param {array}    queries   - a JS array with query objects as expected by the server API (object corresponds to java class SeriesQuery)
    * @param {function} onSuccess - a callback function with one argument accepting the response object as send by the server (java class SeriesResponse)
    * @param {function} onFailure - a callback function with no arguments
    */
   function requestListOfSeriesData(queries, onSuccess, onFailure) {
      requestWithJsonBody('POST', 'api/series/data/', { queries: queries }, onSuccess, onFailure);
   }

   /**
    * @param {function} onSuccess - a function with one argument accepting an array of series names
    */
   function requestListOfSeriesNames(onSuccess) {
      $.getJSON("api/series/", onSuccess);
   }

   /**
    * @param {string}   series    - name of the metric series
    * @param {function} onSuccess - a function with one argument accepting an array request traces as returned by the server (each trace object corresponds to java class RequestTraceResponse)
    */
   function requestListOfRequestTraces(series, onSuccess) {
      $.getJSON("api/trace/data/" + series, onSuccess);
   }

   /**
    * @param {function} onSuccess - a callback function with no arguments
    */
   function requestListOfWatches(onSuccess) {
      $.getJSON("api/watches/data/", (response) => onSuccess(response.watches));
   }

   /**
    * @param {object}   watch     - a JS watch object as expected by the server API (object corresponds to java class WatchData)
    * @param {function} onSuccess - a callback function with no arguments
    * @param {function} onFailure - a callback function with no arguments
    */
   function requestCreateWatch(watch, onSuccess, onFailure) {
      requestWithJsonBody('PUT', 'api/watches/data/', watch, onSuccess, onFailure);
   }

   /**
    * @param {string}   name      - name of the watch to delete
    * @param {function} onSuccess - a callback function with no arguments
    * @param {function} onFailure - a callback function with no arguments
    */
   function requestDeleteWatch(name, onSuccess, onFailure) {
      requestWithoutBody('DELETE', 'api/watches/data/' + name + '/', onSuccess, onFailure);
   }

   /**
    * @param {string}   name      - name of the watch to disable
    * @param {function} onSuccess - a callback function with no arguments
    * @param {function} onFailure - a callback function with no arguments
    */
   function requestDisableWatch(name, onSuccess, onFailure) {
      requestWithoutBody('PATCH', 'api/watches/data/' + name + '/?disable=true', onSuccess, onFailure);
   }

   /**
    * @param {string}   name      - name of the watch to enable
    * @param {function} onSuccess - a callback function with no arguments
    * @param {function} onFailure - a callback function with no arguments
    */
   function requestEnableWatch(name, onSuccess, onFailure) {
      requestWithoutBody('PATCH', 'api/watches/data/' + name + '/?disable=false', onSuccess, onFailure);
   }

   /**
    * @param {number}   serial    - serial of the alert to ackknowledge
    * @param {function} onSuccess - a callback function with no arguments
    * @param {function} onFailure - a callback function with no arguments
    */
   function requestAcknowledgeAlert(serial, onSuccess, onFailure) {
      requestWithoutBody('POST', 'api/alerts/ack/' + serial + '/', onSuccess, onFailure);
   }

   /**
    * Public API to talk to the server.
    * 
    * Note that none of the functions have a direct return value.
    * All function "return" data by calling their "onSuccess" callback with the result
    * or the "onFailure" callback in case the equest failed.
    */ 
   return {
      requestListOfSeriesData: requestListOfSeriesData,
      requestListOfSeriesNames: requestListOfSeriesNames,
      requestListOfRequestTraces: requestListOfRequestTraces,
      requestListOfWatches: requestListOfWatches,
      requestCreateWatch: requestCreateWatch,
      requestDeleteWatch: requestDeleteWatch,
      requestDisableWatch: requestDisableWatch,
      requestEnableWatch: requestEnableWatch,
      requestAcknowledgeAlert: requestAcknowledgeAlert,
   };
})();
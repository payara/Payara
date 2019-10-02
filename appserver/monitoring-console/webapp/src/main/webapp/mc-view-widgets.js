/*
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
  
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
 *
 **/
MonitoringConsole.View.Widgets = (function() {

   const Selection = MonitoringConsole.Model.Page.Widgets.Selection;

   function element(fn) {
      let e = $.isFunction(fn) ? fn() : fn;
      return (typeof e === 'string') ? document.createTextNode(e) : e;
   }

   let Settings = (function() {

      function emptyPanel() {
         return $('#panel-settings').empty();
      }

      function createHeaderRow(caption) {
         return $('<tr/>').append($('<th/>', {colspan: 2})
             .html(caption)
             .click(function() {
                 let tr = $(this).closest('tr').next();
                 let toggleAll = tr.children('th').length > 0;
                 while (tr.length > 0 && (toggleAll || tr.children('th').length == 0)) {
                     if (tr.children('th').length == 0) {
                         tr.children().toggle();                    
                     }
                     tr = tr.next();
                 }
         }));
      }

      function createCheckboxRow(label, checked, onChange) {
         return createRow(label, () => createCheckbox(checked, onChange));
      }

      function createTable(id, caption) {
         let table = $('<table />', { 'class': 'settings', id: id });
         if (caption)
            table.append(createHeaderRow(caption));
         return table;
      }

      function createRow(label, createInput) {
         return $('<tr/>').append($('<td/>').text(label)).append($('<td/>').append(element(createInput)));   
      }

      /**
      * Creates a checkbox to configure the attributes of a widget.
      *
      * @param {boolean} isChecked - if the created checkbox should be checked
      * @param {function} onChange - a function accepting two arguments: the updated widget and the checked state of the checkbox after a change
      */
      function createCheckbox(isChecked, onChange) {
         return $("<input/>", { type: 'checkbox', checked: isChecked })
             .on('change', function() {
                 let checked = this.checked;
                 Selection.configure((widget) => onChange(widget, checked));
             });
      }

      function createSliderRow(label, min, max, value, onChange) {
         return createRow(label, () => $('<input/>', {type: 'number', min:min, max:max, value: value})
             .on('input change', function() {  
                 let val = this.valueAsNumber;
                 MonitoringConsole.View.onPageUpdate(Selection.configure((widget) => onChange(widget, val)));
             }));
      }

      function createDropdownRow(label, options, value, onChange) {
         let dropdown = $('<select/>');
         Object.keys(options).forEach(option => dropdown.append($('<option/>', {text:options[option], value:option, selected: value === option})));
         dropdown.change(() => MonitoringConsole.View.onPageUpdate(Selection.configure((widget) => onChange(widget, dropdown.val()))));
         return createRow(label, () => dropdown);
      }
      return {
         emptyPanel: emptyPanel,
         createTable: createTable,
         createRow: createRow,
         createHeaderRow: createHeaderRow,
         createCheckboxRow: createCheckboxRow,
         createSliderRow: createSliderRow,
         createDropdownRow: createDropdownRow,
      };
    })();

    /*
     * Public API below:
     */
    return {
      Settings: Settings,
    };
})();
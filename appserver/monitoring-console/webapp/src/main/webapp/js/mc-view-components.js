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
 * Data/Model driven view components.
 *
 * Each of them gets passed a model which updates the view of the component to the model state.
 **/
MonitoringConsole.View.Components = (function() {

   const Units = MonitoringConsole.View.Units;
   const Selection = MonitoringConsole.Model.Page.Widgets.Selection;

   /**
    * This is the side panel showing the details and settings of widgets
    */
   let Settings = (function() {

      function emptyPanel() {
         return $('#Settings').empty();
      }

      function createHeaderRow(model) {
         let caption = model.label;
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

      function createTable(model) {
         let table = $('<table />', { id: model.id });
         if (model.caption)
            table.append(createHeaderRow({ label: model.caption }));
         return table;
      }

      function createRow(model, inputs) {
         let components = $.isFunction(inputs) ? inputs() : inputs;
         if (typeof components === 'string') {
            components = document.createTextNode(components);
         }
         return $('<tr/>').append($('<td/>').text(model.label)).append($('<td/>').append(components));   
      }

      function createCheckboxInput(model) {
         return $("<input/>", { type: 'checkbox', checked: model.value })
             .on('change', function() {
                 let checked = this.checked;
                 Selection.configure((widget) => model.onChange(widget, checked));
             });
      }

      function createRangeInput(model) {
         let attributes = {type: 'number', value: model.value};
         if (model.min)
            attributes.min = model.min;
         if (model.max)
            attributes.max = model.max;         
         return $('<input/>', attributes)
             .on('input change', function() {  
                 let val = this.valueAsNumber;
                 MonitoringConsole.View.onPageUpdate(Selection.configure((widget) => model.onChange(widget, val)));
             });
      }

      function createDropdownInput(model) {
         let dropdown = $('<select/>');
         Object.keys(model.options).forEach(option => dropdown.append($('<option/>', {text:model.options[option], value:option, selected: model.value === option})));
         dropdown.change(() => MonitoringConsole.View.onPageUpdate(Selection.configure((widget) => model.onChange(widget, dropdown.val()))));
         return dropdown;
      }

      function createValueInput(model) {
         if (model.unit === 'percent')
            return createRangeInput({min: 0, max: 100, value: model.value, onChange: model.onChange });
         return createTextInput(model, Units.converter(model.unit));
      }

      function createTextInput(model, converter) {
         if (!converter)
            converter = { format: (str) => str, parse: (str) => str };
         let input = $('<input/>', {type: 'text', value: converter.format(model.value) });
         input.on('input change', function() {
            let val = converter.parse(this.value);
            MonitoringConsole.View.onPageUpdate(Selection.configure((widget) => model.onChange(widget, val)));
         });
         return input;
      }

      function createInput(model) {
         switch (model.type) {
            case 'checkbox': return createCheckboxInput(model);
            case 'dropdown': return createDropdownInput(model);
            case 'range'   : return createRangeInput(model);
            case 'value'   : return createValueInput(model);
            case 'text'    : return createTextInput(model);
            default        : return model.input;
         }
      }

      function onUpdate(model) {
         let panel = emptyPanel();
         for (let t = 0; t < model.length; t++) {
            let group = model[t];
            let table = createTable(group);
            panel.append(table);
            for (let r = 0; r < group.entries.length; r++) {
               let entry = group.entries[r];
               let type = entry.type;
               let auto = type === undefined;
               let input = entry.input;
               if (type == 'header' || auto && input === undefined) {
                  table.append(createHeaderRow(entry));
               } else if (!auto) {
                  table.append(createRow(entry, createInput(entry)));
               } else {
                  if (Array.isArray(input)) {
                     let multiInput = $('<div/>');
                     for (let i = 0; i < input.length; i++) {
                        multiInput.append(createInput(input[i]));
                        if (input[i].label) {
                           multiInput.append($('<label/>').html(input[i].label));
                        }                        
                     }
                     input = multiInput;
                  }
                  table.append(createRow(entry, input));
               }
            }
         }
      }

      return { onUpdate: onUpdate };
   })();

   /**
   * Legend is a generic component showing a number of current values annotated with label and color.
   */ 
   let Legend = (function() {

      function createItem(model) {
         let label = model.label;
         let value = model.value;
         let color = model.color;
         let assessments = model.assessments;
         let strong = value;
         let normal = '';
         if (typeof value === 'string' && value.indexOf(' ') > 0) {
            strong = value.substring(0, value.indexOf(' '));
            normal = value.substring(value.indexOf(' '));
         }
         let attrs = { style: 'border-color: ' + color + ';' };
         if (assessments && assessments.status)
            attrs.class = 'status-' + assessments.status;
         if (label === 'server') { // special rule for DAS
            label = 'DAS'; 
            attrs.title = "Data for the Domain Administration Server (DAS); plain instance name is 'server'";
         }
         return $('<li/>', attrs)
               .append($('<span/>').text(label))
               .append($('<strong/>').text(strong))
               .append($('<span/>').text(normal));
      }

      function onCreation(model) {
         let legend = $('<ol/>',  {'class': 'Legend'});
         for (let i = 0; i < model.length; i++) {
            legend.append(createItem(model[i]));
         }
         return legend;
      }

      return { onCreation: onCreation };
   })();

  /**
  * Component to navigate pages. More a less a dropdown.
  */
  let Navigation = (function() {

    function onUpdate(model) {
       let dropdown = $('<select/>');
       dropdown.change(() => model.onChange(dropdown.val()));
       for (let i = 0; i < model.pages.length; i++) {
          let pageModel = model.pages[i];
          dropdown.append($('<option/>', {value: pageModel.id, text: pageModel.label, selected: pageModel.active }));
          if (pageModel.active) {
             dropdown.val(pageModel.id);
          }
       }
       let nav = $("#Navigation"); 
       nav.empty();
       nav.append(dropdown);
       return dropdown;
    }

    return { onUpdate: onUpdate };
  })();

  /**
   * Component drawn for each widget legend item to indicate data status.
   */
  let Indicator = (function() {

    function onCreation(model) {
       if (!model.text) {
          return $('<div/>', {'class': 'Indicator', style: 'display: none;'});
       }
       let html = model.text.replace(/\*([^*]+)\*/g, '<b>$1</b>').replace(/_([^_]+)_/g, '<i>$1</i>');
       return $('<div/>', { 'class': 'Indicator status-' + model.status }).html(html);
    }

    return { onCreation: onCreation };
  })();

  /**
   * Component for any of the text+icon menus/toolbars.
   */
  let Menu = (function() {

    function onCreation(model) {
      let attrs = { 'class': 'Menu' };
      if (model.id)
        attrs.id = model.id;
      let menu = $('<span/>', attrs);
      let groups = model.groups;
      for (let g = 0; g < groups.length; g++) {
        let group = groups[g];
        if (group.items) {
          let groupBox = $('<span/>', { class: 'Group' });
          let groupButton = $('<span/>', { class: 'Item' })
            .append($('<a/>').html(createText(group)))
            .append(groupBox)
            ;
          menu.append(groupButton);
          for (let b = 0; b < group.items.length; b++) {
            groupBox.append(createButton(group.items[b]));
          }          
        } else {
          menu.append(createButton(group).addClass('Item'));
        }
      }
      return menu;
    }

    function createText(button) {
      let text = '';
      if (button.icon)
        text += '<strong>'+button.icon+'</strong>';
      if (button.label)
        text += button.label;
      return text;
    }

    function createButton(button) {
      return $('<button/>', { title: button.description })
            .html(createText(button))
            .click(button.onClick);
    }

    return { onCreation: onCreation };
  })();

  /*
  * Public API below:
  */
  return {
      /**
       * Call to update the settings side panel with the given model
       */
      onSettingsUpdate: (model) => Settings.onUpdate(model),
      /**
       * Call to update the top page navigation with the given model
       */
      onNavigationUpdate: (model) => Navigation.onUpdate(model),
      /**
       * Returns a jquery legend element reflecting the given model to be inserted into the DOM
       */
      onLegendCreation: (model) => Legend.onCreation(model),
      /**
       * Returns a jquery indicator element reflecting the given model to be inserted into the DOM
       */
      onIndicatorCreation: (model) => Indicator.onCreation(model),
      /**
       * Returns a jquery menu element reflecting the given model to inserted into the DOM
       */
      onMenuCreation: (model) => Menu.onCreation(model),
      //TODO add id to model and make it an update?
  };

})();
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
 * Each of them gets passed a model creates a DOM structure in form of a jQuery object 
 * that should be inserted into the page DOM using jQuery.
 *
 * Besides general encapsulation the idea is to benefit of a function approch 
 * that utilises pure functions to compute the page context from a fixed state input.
 * This makes the code much easier to understand and maintain as it is free of overall page state.
 **/
MonitoringConsole.View.Components = (function() {

  const Controller = MonitoringConsole.Controller;
  const Units = MonitoringConsole.View.Units;
  const Colors = MonitoringConsole.View.Colors;
  const Selection = MonitoringConsole.Model.Page.Widgets.Selection;

   /**
    * This is the side panel showing the details and settings of widgets
    */
   let Settings = (function() {

      function createHeaderRow(model) {
        let caption = model.label;
        let config = {colspan: 2};
        if (model.description)
          config.title = model.description;
        let th = $('<th/>', config);
        let showHide = function() {
          let tr = th.closest('tr').next();
          let toggleAll = tr.children('th').length > 0;
          while (tr.length > 0 && (toggleAll || tr.children('th').length == 0)) {
              if (tr.children('th').length == 0) {
                  tr.toggle();                    
              }
              tr = tr.next();
          }
        };
        return $('<tr/>').append(
            th.html(caption).click(showHide));
      }

      function createTable(model) {
        let table = $('<table />', { id: model.id });
        if (model.caption)
          table.append(createHeaderRow({ label: model.caption, description: model.description, collapsed: model.collapsed }));
        return table;
      }

      function createRow(model, inputs) {
        let components = $.isFunction(inputs) ? inputs() : inputs;
        if (typeof components === 'string')
            components = document.createTextNode(components);
        let config = {};
        if (model.description)
          config.title = model.description;
        let tr = $('<tr/>');
        tr.append($('<td/>', config).text(model.label)).append($('<td/>').append(components));
        if (model.collapsed)
          tr.css('display', 'none');
        return tr;
      }

      function enhancedOnChange(onChange, updatePage) {
        if (onChange.length == 2) {
          return (checked) => {
            let layout = Selection.configure((widget) => onChange(widget, checked));
            if (updatePage) {
              MonitoringConsole.View.onPageUpdate(layout);
            }
          };
        }
        return onChange;
      }

      function createCheckboxInput(model) {
        let config = { id: model.id, type: 'checkbox', checked: model.value };
        if (model.description && !model.label)
          config.title = model.description;
        let onChange = enhancedOnChange(model.onChange);
        return $("<input/>", config)
          .on('change', function() {
            let checked = this.checked;
            onChange(checked);
          });
      }

      function createRangeInput(model) {
        let config = { id: model.id, type: 'number', value: model.value};
        if (model.min)
          config.min = model.min;
        if (model.max)
          config.max = model.max;
        if (model.description && !model.label)
          config.title = model.description;
        let onChange = enhancedOnChange(model.onChange, true);
        return $('<input/>', config)
          .on('input change', function() {  
            let val = this.valueAsNumber;
            if (Number.isNaN(val))
              val = undefined;
            onChange(val);
          });
      }

      function createDropdownInput(model) {
        let config = { id: model.id };
        if (model.description && !model.label)
          config.title = description;
        let dropdown = $('<select/>',  );
        if (Array.isArray(model.options)) {
          model.options.forEach(option => dropdown.append($('<option/>',
            { text: option, value: option, selected: model.value === option})));
        } else {
          Object.keys(model.options).forEach(option => dropdown.append($('<option/>', 
            { text:model.options[option], value:option, selected: model.value === option})));          
        }
        let onChange = enhancedOnChange(model.onChange, true);
        dropdown.change(() => onChange(dropdown.val()));
        return dropdown;
      }

      function createValueInput(model) {
        let unit = model.unit;
        if (typeof unit === 'string') {
          if (unit === 'percent')
            return createRangeInput({id: model.id, min: 0, max: 100, value: model.value, onChange: model.onChange });
          if (unit === 'count')
            return createRangeInput(model);
        }
        return createTextInput(model);
      }

      function createTextInput(model) {
        function getConverter() {
          if (model.unit === undefined)
            return { format: (str) => str, parse: (str) => str };
          if (typeof model.unit === 'function')
            return Units.converter(model.unit());
          return Units.converter(model.unit);
        }
        let converter = getConverter();
        let config = { 
          id: model.id,
          type: 'text', 
          value: converter.format(model.value), 
          'class': 'input-' + model.type,
        };
        if (model.description && !model.label)
          config.title = description;
        let readonly = model.onChange === undefined;
        if (!readonly && typeof model.unit === 'string') {
          if (converter.pattern !== undefined)
            config.pattern = converter.pattern();
          if (converter.patternHint !== undefined)
            config.title = (config.title ? config.title + ' ' : '') + converter.patternHint();
        }
        let input = $('<input/>', config);
        if (!readonly) {
          let onChange = enhancedOnChange(model.onChange, true);
          input.on('input change paste', function() {
            let val = getConverter().parse(this.value);
            onChange(val);
          });          
        } else {
          input.prop('readonly', true);
        }
        return input;
      }

      function createColorInput(model) {
        let value = model.value;
        if (value === undefined && model.defaultValue !== undefined)
          value = model.defaultValue;
        if (Array.isArray(value))
          return createMultiColorInput(model);
        let config = { id: model.id, type: 'color', value: value };
        if (model.description && !model.label)
          config.title = model.description;
        let onChange = enhancedOnChange(model.onChange);
        let input = $('<input/>', config)
          .on('input change', function() { 
            let val = this.value;
            if (val === model.defaultValue)
              val = undefined;
            onChange(val);
          });
        let wrapper = $('<span/>', {'class': 'color-picker'}).append(input);
        if (model.defaultValue === undefined)
          return wrapper;
        return $('<span/>').append(wrapper).append($('<input/>', { 
          type: 'button', 
          value: ' ',
          title: 'Reset to default color', 
          style: 'background-color: ' + model.defaultValue,
          'class': 'color-reset',
        }).click(() => {
          onChange(undefined);
          input.val(model.defaultValue);
        }));
      }

      function createMultiColorInput(model) {
        let value = model.value;
        if (value === undefined && model.defaultValue !== undefined)
          value = model.defaultValue;
        if (!Array.isArray(value))
          value = [value];
        let list = $('<span/>');
        //TODO model id goes where?
        let colors = [...value];
        let onChange = enhancedOnChange(model.onChange);
        for (let i = 0; i < value.length; i++) {
          list.append(createMultiColorItemInput(colors, i, onChange));
        }
        let add = $('<button/>', {text: '+', 'class': 'color-list'});
        add.click(() => {
          colors.push(Colors.random(colors));
          createMultiColorItemInput(colors, colors.length-1, onChange).insertBefore(add);
          onChange(colors);
        });
        let remove = $('<button/>', {text: '-', 'class': 'color-list'});
        remove.click(() => {
          if (colors.length > 1) {
            colors.length -= 1;
            list.children('.color-picker').last().remove();
            onChange(colors);
          }
        });
        list.append(add);
        list.append(remove);
        return list;
      }

      function createMultiColorItemInput(colors, index, onChange) {
        return createColorInput({ value: colors[index], onChange: (val) => {
          colors[index] = val;
          onChange(colors);
        }});
      }

      function createInput(model) {
         switch (model.type) {
            case 'checkbox': return createCheckboxInput(model);
            case 'dropdown': return createDropdownInput(model);
            case 'range'   : return createRangeInput(model);
            case 'value'   : return createValueInput(model);
            case 'text'    : return createTextInput(model);
            case 'color'   : return createColorInput(model);
            default        : return model.input;
         }
      }

      function createComponent(model) {
         let panel = $('<div/>', { id: model.id });
         let syntheticId = 0;
         let collapsed = false;
         for (let t = 0; t < model.groups.length; t++) {
            let group = model.groups[t];
            let table = createTable(group);
            collapsed = group.collapsed === true;
            panel.append(table);
            for (let r = 0; r < group.entries.length; r++) {
               syntheticId++;
               let entry = group.entries[r];
               let type = entry.type;
               let auto = type === undefined;
               let input = entry.input;
               if (entry.id === undefined)
                 entry.id = 'setting_' + syntheticId;
               entry.collapsed = collapsed;
               if (type == 'header' || auto && input === undefined) {
                  collapsed = entry.collapsed === true;
                  table.append(createHeaderRow(entry));
               } else if (!auto) {
                  table.append(createRow(entry, createInput(entry)));
               } else {
                  if (Array.isArray(input)) {
                    let [innerInput, innerSyntheticId] = createMultiInput(input, syntheticId, 'x-input');
                    input = innerInput;
                    syntheticId = innerSyntheticId;
                  }
                  table.append(createRow(entry, input));
               }
            }
         }
         return panel;
      }

      function createMultiInput(inputs, syntheticId, css) {
        let box = $('<div/>', {'class': css});
        for (let i = 0; i < inputs.length; i++) {
          let entry = inputs[i];
          if (Array.isArray(entry)) {
            let [innerBox, innerSyntheticId] = createMultiInput(entry, syntheticId);
            box.append(innerBox);
            syntheticId = innerSyntheticId;
          } else {
            syntheticId++;
            if (entry.id === undefined)
              entry.id = 'setting_' + syntheticId;
            let input = createInput(entry);
            if (entry.label) {
              let config = { 'for': entry.id };
              if (entry.description)
                config.title = entry.description;
              box.append($('<span/>').append(input).append($('<label/>', config).html(entry.label))).append("\n");
            } else {
              box.append(input);
            }
          }                    
        }
        return [box, syntheticId];
      }

      return { 
        createComponent: createComponent,
        createInput: createInput,
      };
   })();

  /**
  * Legend is a generic component showing a number of current values annotated with label and color.
  */ 
  let Legend = (function() {

    function createItem(item) {
      let label = item.label;
      let value = item.value;
      let color = item.color;
      let strong = value;
      let normal = '';
      if (typeof value === 'string' && value.indexOf(' ') > 0) {
        strong = value.substring(0, value.indexOf(' '));
        normal = value.substring(value.indexOf(' '));
      }
      let attrs = { style: 'border-color: ' + color + ';' };
      if (item.status)
        attrs.class = 'status-' + item.status;
      let label0 = Array.isArray(label) ? label[0] : label;
      if (label0 === 'server') { // special rule for DAS
        label0 = 'DAS'; 
        attrs.title = "Data for the Domain Administration Server (DAS); plain instance name is 'server'";
      }
      let textAttrs = {};
      if (item.highlight)
       textAttrs.style = 'color: '+ item.highlight + ';';
      let mainLabel = $('<span/>').text(label0);
      if (Array.isArray(label) && label.length > 1) {
        for (let i = 1; i < label.length; i++) {
          mainLabel.append(' - ' + label[i]);
        }
      }
      return $('<li/>', attrs)
        .append(mainLabel)
        .append($('<strong/>', textAttrs).text(strong))
        .append($('<span/>').text(normal));
    }

    function createComponent(model) {
      let legend = $('<ol/>',  {'class': 'Legend'});
      for (let item of model) {
        legend.append(createItem(item));
      }
      return legend;
    }

    return { createComponent: createComponent };
  })();

  /**
   * Component drawn for each widget legend item to indicate data status.
   */
  const Indicator = (function() {

    function createComponent(model) {
       if (!model.text) {
          return $('<div/>', {'class': 'Indicator', style: 'display: none;'});
       }
       let html = model.text.replace(/\*([^*]+)\*/g, '<b>$1</b>').replace(/_([^_]+)_/g, '<i>$1</i>');
       return $('<div/>', { 'class': 'Indicator status-' + model.status, style: 'color: ' + model.color + ';' }).html(html);
    }

    return { createComponent: createComponent };
  })();

  /**
   * Component for any of the text+icon menus/toolbars.
   */
  const Menu = (function() {

    function createComponent(model) {
      let attrs = { 'class': 'Menu' };
      if (model.id)
        attrs.id = model.id;
      let menu = $('<span/>', attrs);
      let groups = model.groups;
      for (let g = 0; g < groups.length; g++) {
        let group = groups[g];
        if (group.items) {
          let groupBox = $('<span/>', { class: 'Group' });
          let groupLabel = $('<a/>').html(createText(group));
          let groupItem = $('<span/>', { class: 'Item' })
            .append(groupLabel)
            .append(groupBox)
            ;
          if (group.clickable) {
            groupLabel
              .click(group.items.find(e => e.hidden !== true && e.disabled !== true).onClick)
              .addClass('clickable');
          }
          menu.append(groupItem);
          for (let i = 0; i < group.items.length; i++) {
            let item = group.items[i];
            if (item.hidden !== true)
              groupBox.append(createButton(item));
          }          
        } else {
          if (group.hidden !== true)
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
      if (button.label && button.items)
        text += " &#9013;";
      return text;
    }

    function createButton(button) {
      let attrs = { title: button.description };
      if (button.disabled)
        attrs.disabled = true;
      return $('<button/>', attrs)
            .html(createText(button))
            .click(button.onClick)
            .addClass('clickable');
    }

    return { createComponent: createComponent };
  })();



  /**
   * An alert table is a widget that shows a table of alerts that have occured for the widget series.
   */
  let AlertTable = (function() {

    function createComponent(model) {
      let items = model.items === undefined ? [] : model.items.sort(sortMostUrgetFirst);
      config = { 'class': 'AlertTable' };
      if (model.id)
        config.id = model.id;
      if (items.length == 0)
        config.style = 'display: none';
      let table = $('<div/>', config);
      for (let i = 0; i < items.length; i++) {
        table.append(createAlertRow(items[i], model.verbose));
      }
      return table;
    }

    function createAlertRow(item, verbose) {
      item.frames = item.frames.sort(sortMostRecentFirst); //NB. even though sortMostUrgetFirst does this as well we have to redo it here - JS...
      let endFrame = item.frames[0];
      let ongoing = endFrame.until === undefined;
      let level = endFrame.level;
      let color = ongoing ? endFrame.color : Colors.hex2rgba(endFrame.color, 0.6);
      let box = $('<div/>', { style: 'border-color:' + color + ';' });
      box.append($('<input/>', { type: 'checkbox', checked: item.acknowledged, disabled: item.acknowledged })
        .change(() => acknowledge(item)));
      box.append(createGeneralGroup(item, verbose));
      box.append(createStatisticsGroup(item, verbose));
      if (ongoing && verbose)
        box.append(createConditionGroup(item));
      if (verbose && Array.isArray(item.annotations) && item.annotations.length > 0)
        box.append(createAnnotationGroup(item));
      let row = $('<div/>', { id: 'Alert-' + item.serial, class: 'Item ' + level, style: 'border-color:'+item.color+';' });
      row.append(box);
      return row;
    }

    function acknowledge(item) {
      Controller.requestAcknowledgeAlert(item.serial);
    }

    function createAnnotationGroup(item) {
      let id = 'Alert-' + item.serial + '-Annotations';
      let element = $('#' + id); 
      let display = element.length == 0 || element.is(":visible") ? 'block' : 'none';
      let groups = $('<div/>', { id: id, style: 'display: ' + display + ';' });
      let baseColor = item.frames[0].color;
      for (let i = 0; i < item.annotations.length; i++) {
        let annotation = item.annotations[i];
        groups.append(AnnotationTable.createEntry({
          unit: item.unit,
          time: annotation.time,
          value: annotation.value,
          attrs: annotation.attrs,
          color: Colors.hex2rgba(baseColor, 0.45),
          fields: annotation.fields,
        }));
      }
      let label = display == 'none' ? '[ + ]' : '[ - ]';
      let group = $('<div/>');
      appendProperty(group, 'Annotations', $('<a/>').text(label).click(() => groups.toggle()));
      group.append(groups);
      return group;
    }

    function createConditionGroup(item) {
      let endFrame = item.frames[0];
      let circumstance = item.watch[endFrame.level];
      let group = $('<div/>', { 'class': 'Group' });
      appendProperty(group, 'Start', formatCondition(circumstance.start, item.unit));
      if (circumstance.stop) {
        appendProperty(group, 'Stop', formatCondition(circumstance.stop, item.unit));
      }
      return group;
    }

    function createStatisticsGroup(item, verbose) {
        let endFrame = item.frames[0];
        let startFrame = item.frames[item.frames.length - 1];
        let duration = durationMs(startFrame, endFrame);
        let amberStats = computeStatistics(item, 'amber');
        let redStats = computeStatistics(item, 'red');
        let group = $('<div/>', { 'class': 'Group' });
        appendProperty(group, 'Since', Units.formatTime(startFrame.since));
        appendProperty(group, 'For', formatDuration(duration));
        if (redStats.count > 0 && verbose)
          appendProperty(group, 'Red', redStats.text);
        if (amberStats.count > 0 && verbose)
          appendProperty(group, 'Amber', amberStats.text);
      return group;
    }

    function createGeneralGroup(item, verbose) {
      let group = $('<div/>', { 'class': 'Group' });
      appendProperty(group, 'Alert', item.serial);
      appendProperty(group, 'Watch', item.name);
      if (item.series)
        appendProperty(group, 'Series', item.series);
      if (item.instance && verbose)
        appendProperty(group, 'Instance', item.instance === 'server' ? 'DAS' : item.instance);
      return group;
    }

    function computeStatistics(item, level) {
      const reduceCount = (count, frame) => count + 1;
      const reduceDuration = (duration, frame) => duration + durationMs(frame, frame);
      let frames = item.frames;
      let matches = frames.filter(frame => frame.level == level);
      let count = matches.reduce(reduceCount, 0);
      let duration = matches.reduce(reduceDuration, 0);
      return { 
        count: count,
        duration: duration,
        text: formatDuration(duration) + ' x' + count, 
      };
    }

    function durationMs(frame0, frame1) {
      return (frame1.until === undefined ? new Date() : frame1.until) - frame0.since;
    }

    function formatDuration(ms) {
      return Units.converter('sec').format(Math.round(ms/1000));
    }

    /**
     * Sorts alerts starting with ongoing most recent red and ending with ended most past amber.
     */
    function sortMostUrgetFirst(a, b) {
      a.frames = a.frames.sort(sortMostRecentFirst);
      b.frames = b.frames.sort(sortMostRecentFirst);
      let aFrame = a.frames[0]; // most recent frame is not at 0
      let bFrame = b.frames[0];
      let aOngoing = aFrame.until === undefined;
      let bOngoing = bFrame.until === undefined;
      if (aOngoing != bOngoing)
        return aOngoing ? -1 : 1;
      let aLevel = aFrame.level;
      let bLevel = bFrame.level;
      if (aLevel != bLevel)
        return aLevel === 'red' ? -1 : 1;
      return bFrame.since - aFrame.since; // start with most recent item
    }

    function sortMostRecentFirst(a, b) {
      return b.since - a.since; // sort most recent frame first 
    }

    return { createComponent: createComponent };
  })();


  /**
   * The annotation table is shown for widgets of type 'annotation'.
   * Alert lists with annotations visible also use the list entry to add annotations to alert entries.
   * 
   * The interesting part with annotations is that the attributes can be any set of string key-value pairs.
   * Still, the values should be formatted meaningfully. Therefore formatters can be set which analyse each
   * key-value-pair to determine how to display them.
   *
   * The annotation table can either display a list style similar to alert table or an actual table with
   * rows and columns. An that case all items are expected to have the same fields value.
   */
  let AnnotationTable = (function() {

    function inRange(x, min, max) {
      return x >= min && x <= max;
    }

    let SQLValueFormatter = {
      applies: (item, attrKey, attrValue) => attrValue.includes(' ') && attrValue.trim().endsWith(';'),
      format:  (item, attrValue) => attrValue,
      type: 'pre',
    };

    let TimeValueFormatter = {
      applies: (item, attrKey, attrValue) => inRange(new Date().getTime() - Number(attrValue), 0, 86400000), // 1 day in millis
      format:  (item, attrValue) => Units.formatTime(attrValue),  
    };

    let UnitValueFormatter = (function(names) {
      return {
        applies: (item, attrKey, attrValue) => names.indexOf(attrKey) >= 0 && !Number.isNaN(parseInt(attrValue)),
        format: (item, attrValue) => Units.converter(item.unit).format(Number(attrValue)),
      };
    });

    let SeriesValueFormatter = {
      applies: (item, attrKey, attrValue) => attrKey == 'Series' || attrValue.startsWith('ns:'),
      format: (item, attrValue) => attrValue,
      type: 'code',
    };

    let PlainValueFormatter = {
      applies: (item, attrKey, attrValue) => true,
      format: (item, attrValue) => attrValue,
    };

    let DEFAULT_FORMATTERS = [
      TimeValueFormatter,
      UnitValueFormatter('Threshold'),
      SeriesValueFormatter,
      SQLValueFormatter,
      PlainValueFormatter,
    ];

    function createComponent(model) {
      let items = model.items || [];
      config = { 'class': 'AnnotationTable' };
      if (model.id)
        config.id = model.id;
      if (items.length == 0)
        config.style = 'display: none';
      let isTable = model.mode === 'table';
      let tag = isTable ? '<table/>' : '<div/>';
      let table = $(tag, config);
      if (model.sort === undefined && isTable || model.sort == 'value')
        items = items.sort((a, b) => b.value - a.value);
      if (model.sort == 'time')
        items = items.sort((a, b) => b.time - a.time);
      for (let item of items) {
        if (isTable) {
          if (table.children().length == 0) {
            let tr = $('<tr/>');
            for (let key of Object.keys(createAttributesModel(item)))
              tr.append($('<th/>').text(key));
            table.append(tr);
          }
          table.append(createTableEntry(item));  
        } else {
          table.append(createListEntry(item));  
        }
      }
      return table;
    }

    function createListEntry(item) {      
      let attrs = createAttributesModel(item);
      let group = $('<div/>', { 'class': 'Group Annotation', style: 'border-color:' + item.color + ';' });
      for (let [key, entry] of Object.entries(attrs)) {
        appendProperty(group, key, entry.value, entry.type);
      }      
      return group;
    }

    function createTableEntry(item) {
      let attrs = createAttributesModel(item);
      let row = $('<tr/>', { 'class': 'Annotation' });
      let style = { 'style': 'border-left-color: ' + item.color + ';' };
      for (let entry of Object.values(attrs)) {
        let td = $('<td/>', style);
        style = {}; // clear after 1. column
        if (entry.type) {
          td.append($('<' + entry.type + '/>').append(entry.value));
        } else {
          td.text(entry.value);
        }
        row.append(td);
      }
      return row;
    }

    function createAttributesModel(item) {
      let attrs = {}; // new object is both sorted by default order and accessible by key
      if (item.series)
        attrs.Series = { value: item.series, type: 'code' };        
      if (item.instance)
        attrs.Instance = { value: item.instance };
      attrs.When = { value: Units.formatTime(item.time) };
      attrs.Value = { value: Units.converter(item.unit).format(item.value) };
      for (let [key, value] of Object.entries(item.attrs)) {
        let formatter = selectFormatter(item, key, value, item.formatters);
        attrs[key] = { value: formatter.format(item, value), type: formatter.type };        
      }
      if (!item.fields)
        return attrs;
      let model = {};
      for (let field of item.fields) {
        let entry = attrs[field];
        if (entry)
          model[field] = entry;
      }
      return model;
    }

    function selectFormatter(item, attrKey, attrValue, formatters) {
      if (formatters === undefined)
        return selectFormatter(item, attrKey, attrValue, DEFAULT_FORMATTERS);
      for (let formatter of formatters) 
        if (formatter.applies(item, attrKey, attrValue))
          return formatter;
      if (formatters !== DEFAULT_FORMATTERS)
        return selectFormatter(item, attrKey, attrValue, DEFAULT_FORMATTERS);
      return PlainValueFormatter;
    }

    return { 
      createComponent: createComponent,
      createEntry: createListEntry, 
    };
  })();

  /**
   * Lists existing watches to explain their logic to the user.
   */
  const WatchList = (function() {

    function createComponent(model) {
      const config = { 'class': 'WatchList' };
      if (model.id)
        config.id = model.id;
      const list = $('<div/>', config);
      for (let item of model.items) {
        list.append(createItem(item, model.colors, model.actions));
      }
      return list;
    }

    function createItem(item, colors, actions) {
      const watch = $('<div/>', { 'class': 'WatchItem ' + 'state-' + (item.disabled || item.stopped ? 'disabled' : 'enabled') });
      const disableButton = Settings.createInput({ type: 'checkbox', value: !item.disabled, onChange: (checked) => {
        if (checked) {
          actions.onEnable(item.name);
        } else {
          actions.onDisable(item.name);
        }
      }});
      const menu = [
        { icon: '&#128295;', label: item.programmatic ? 'Duplicate' : 'Edit', onClick: () => actions.onEdit(item) }
      ];
      if (item.disabled) {
        menu.push({ icon: '&#9745;', label: 'Enable', onClick: () => actions.onEnable(item.name) });        
      } else {
        menu.push({ icon: '&#9746;', label: 'Disable', onClick: () => actions.onDisable(item.name) });        
      }
      if (!item.programmatic) {
        menu.push({ icon: '&times;', label: 'Delete', onClick: () => actions.onDelete(item.name) });
      }
      const general = $('<h3/>').append(disableButton).append(item.name + (item.stopped ? ' (stopped)' : '')).click(() => actions.onEdit(item));
      watch
        .append(Menu.createComponent({ groups: [{ icon: '&#9881;', items: menu }]}))
        .append(general);
      for (let level of ['red', 'amber', 'green'])
        if (item[level])
          watch.append(createCircumstance(level, item[level], item.unit, item.series, colors[level]));
      return watch;
    }

    function createCircumstance(level, model, unit, series, color) {
      function plainText(condition) {
        let text = condition.text();
        return text.substring(text.indexOf('value') + 5);
      }
      const circumstance = $('<div/>', { 'class': 'WatchCondition', style: 'color: '+ color +';'});
      let levelText = paddedLeftWith('&nbsp;', Units.Alerts.name(level), 'Unhealthy'.length);
      let text = '<b>' + levelText + ':</b> <em>If</em> ' + series + ' <em>in</em> ' + Units.names()[unit] + ' <em>is</em> ';
      text += plainText(formatCondition(model.start, unit));
      if (model.suppress)
        text += ' <em>unless</em> ' + model.surpressingSeries + ' ' + plainText(formatCondition(model.suppress, modelsurpressingUnit));
      if (model.stop)
        text += ' <em>until</em> ' + plainText(formatCondition(model.stop, unit));
      return circumstance.html(text);
    }

    return { createComponent: createComponent };
  })();

  /**
   * A component that creates the form to compose a single new watch
   */
  const WatchBuilder = (function() {
    
    function createComponent(model, watch) {
      const config = { 'class': 'WatchBuilder WatchItem' };
      if (model.id)
        config.id = model.id;
      let editedWatch = watch || { unit: 'count', name: 'New Watch' };
      if (editedWatch.programmatic) {
        editedWatch = JSON.parse(JSON.stringify(watch));
        editedWatch.name = 'Copy of ' + watch.name;
        editedWatch.programmatic = false;
      }
      const builder = $('<div/>', config);
      const nameInput = Settings.createInput({ type: 'text', value: editedWatch.name, onChange: (name) => editedWatch.name = name });
      builder.append($('<h3/>').append(nameInput));
      const unitDropdowns = [];
      const seriesInputs = [];
      for (let level of ['red', 'amber', 'green']) {
        builder.append(createLevelBuilder(level, editedWatch, model.colors[level], unitDropdowns, seriesInputs));
      }
      builder.append($('<button/>').text('Save or Update').click(() => model.actions.onCreate(editedWatch)));
      return builder;
    }

    function createLevelBuilder(level, editedWatch, color, unitDropdowns, seriesInputs) {
      const editedCircumstance = editedWatch[level] || { level: level };
      const editedStartCondition = editedCircumstance.start || { operator: '>', forTimes: 1 };
      const editedStopCondition = editedCircumstance.stop || { operator: '<', forTimes: 1 };
      const defined = editedWatch[level] !== undefined;
      const levelBox = $('<span/>', defined ? {} : { style: 'display: none;'});
      let enableCheckbox = Settings.createInput({type: 'checkbox', value: defined, onChange: (checked) => {
        if (checked) {
          levelBox.show();
          editedWatch[level] = editedCircumstance;
          editedCircumstance.start = editedStartCondition;
        } else {
          levelBox.hide();
          editedWatch[level] = undefined;
        }
      }});
      const unitDropdown = Settings.createInput({ type: 'dropdown', value: editedWatch.unit, options: Units.names(), onChange: (selected) => {
        editedWatch.unit = selected;
        unitDropdowns.forEach(dropdown => dropdown.val(selected));
      }});
      unitDropdowns.push(unitDropdown);
      const seriesInput = Settings.createInput({ type: 'text', value: editedWatch.series, onChange: (series) => {
        editedWatch.series = series;
        seriesInputs.forEach(input => input.val(series));
      }});
      seriesInputs.push(seriesInput);
      const isUntilDefined = editedCircumstance.stop !== undefined;
      const untilBox = $('<span/>', isUntilDefined ? {} : { style: 'display: none;'})
        .append(createConditionBuilder(editedWatch, editedCircumstance, editedStopCondition));      
      const untilCheckbox = Settings.createInput({ type: 'checkbox', value: isUntilDefined, onChange: (checked) => {
        if (checked) {
          untilBox.show();
          editedCircumstance.stop = editedStopCondition;
        } else {
          untilBox.hide();
          editedCircumstance.stop = undefined;
        }
      }});
      levelBox
        .append(' <em>If</em> ').append(seriesInput)
        .append(' <em>in</em> ').append(unitDropdown)
        .append(' <em>is<em/> ').append(createConditionBuilder(editedWatch, editedCircumstance, editedStartCondition))
        .append(' <em>until</em> ').append(untilCheckbox).append(untilBox);
      return $('<div/>', {'class': 'WatchCondition', style: 'color: ' + color + ';' })
        .append(enableCheckbox).append('<b>' + paddedLeftWith('&nbsp;', Units.Alerts.name(level), 'Unhealthy'.length) + ':</b>')
        .append(levelBox);
    }

    function createConditionBuilder(editedWatch, editedCircumstance, editedCondition) {
      function getKind() {
        if (editedCondition.forTimes === 0 || editedCondition.forMillis === 0)
          return 'inSample';
        if (editedCondition.forTimes < 0 || editedCondition.forMillis < 0)
          return 'inLast';
        if (editedCondition.onAverage)
          return 'forAvgOfLast';
        return 'forLast';
      }
      const kindOptions = {
        forLast: 'for last', 
        forAvgOfLast: 'for average of last', 
        inLast: 'in last', 
        inSample: 'in sample'
      };
      const forInBox = $('<span/>', getKind() != 'inSample' ? {} : { style: 'display: none;' });
      function updateEditedCondition(selectedKind, forLastInputValue) {
        if (selectedKind === undefined)
          selectedKind = getKind();
        const isCount = forLastInputValue === undefined 
          ? editedCondition.forTimes !== undefined
          : /^[0-9]+$/i.test(forLastInputValue);
        const forLast = forLastInputValue === undefined
          ? editedCondition.forTimes !== undefined ? Math.abs(editedCondition.forTimes) : Math.abs(editedCondition.forMillis)
          : Units.converter(isCount ? 'count' : 'ms').parse(forLastInputValue);
        editedCondition.onAverage = selectedKind === 'forAvgOfLast';
        if (selectedKind == 'forLast' || selectedKind == 'forAvgOfLast') {
          editedCondition.forTimes = isCount ? Math.abs(forLast) : undefined;
          editedCondition.forMillis = isCount ? undefined : Math.abs(forLast);
        } else if (selectedKind == 'inLast') {
          editedCondition.forTimes = isCount ? - Math.abs(forLast) : undefined;
          editedCondition.forMillis = isCount ? undefined : - Math.abs(forLast);
        }
        if (selectedKind == 'inSample') {
          forInBox.hide();
          editedCondition.forTimes = 0;
          editedCondition.forMillis = undefined;
        } else {
          forInBox.show();
        }        
      }
      const forInValue = editedCondition.forTimes !== undefined 
        ? Math.abs(editedCondition.forTimes) 
        : editedCondition.forMillis !== undefined ? Units.converter('ms').format(Math.abs(editedCondition.forMillis)) : 1;
      const operatorDropdown = Settings.createInput({ type: 'dropdown', value: editedCondition.operator, options: ['<', '<=', '=', '>', '>='], onChange: (selected) => editedCondition.operator = selected});
      const thresholdInput = Settings.createInput({ type: 'value', unit: () => editedWatch.unit, value: editedCondition.threshold, onChange: (value) => editedCondition.threshold = value});
      const forInInput = Settings.createInput({ type: 'text', value: forInValue, onChange: (value) => updateEditedCondition(undefined, value)});     
      const kindDropdown = Settings.createInput({ type: 'dropdown', value: getKind(), options: kindOptions, onChange: (selected) => updateEditedCondition(selected, undefined)});
      return $('<span/>')
        .append(operatorDropdown)
        .append(thresholdInput)
        .append(kindDropdown)
        .append(forInBox.append(forInInput));
    }

    return { createComponent: createComponent };

  })();

  /**
   * Combines the WatchList and WatchBuilder into one component to list and create watches.
   */ 
  const WatchManager = (function() {

    function createComponent(model) {
      const config = { 'class': 'WatchManager' };
      if (model.id)
        config.id = model.id;
      const manager = $('<div/>', config);
      model.id = undefined; // id should not be set by sub-components
      let builder = WatchBuilder.createComponent(model);
      const list = WatchList.createComponent(model);
      model.actions.onEdit = (watch) => {
        const newBuilder = WatchBuilder.createComponent(model, watch);
        builder.replaceWith(newBuilder);
        builder = newBuilder;
      };
      manager.append(list);
      manager.append(builder);
      return manager;
    }

    return { createComponent: createComponent };
  })();


  /*
   * Shared functions
   */

  function appendProperty(parent, label, value, tag = "strong") {
    parent.append($('<span/>')
      .append($('<small>', { text: label + ':' }))
      .append($('<' + tag + '/>').append(value))
    ).append('\n'); // so browser will line break;
  }

  function formatCondition(condition, unit) {
    if (condition === undefined)
      return '';
    const forTimes = condition.forTimes;
    const forMillis = condition.forMillis;
    let any = forTimes === 0 || forMillis === 0;
    let anyN = forTimes < 0 || forMillis < 0;
    let threshold = Units.converter(unit).format(condition.threshold);
    let text = ''; 
    let forText = '';
    let forValue;
    text += 'value ' + condition.operator + ' ' + threshold;
    if (forTimes !== undefined || forMillis !== undefined) {
      if (any) {
        forText += ' in sample';
      } else if (anyN) {
        forText += ' in last ';
      } else if (condition.onAverage) {
        forText += ' for average of last ';
      } else {
        forText += ' for last ';
      }
    }
    if (forTimes !== undefined && forTimes !== 0)
      forValue = Math.abs(condition.forTimes) + 'x';
    if (forMillis !== undefined && forMillis !== 0)
      forValue = Units.converter('ms').format(Math.abs(condition.forMillis));
    let desc = $('<span/>').append(text);
    if (forText != '')
      desc.append($('<small/>', { text: forText})).append(forValue);
    return desc;
  }

  function paddedLeftWith(char, text, length) {
    let n = length - text.length;
    for (let i = 0; i < n; i++)
      text = char + text;
    return text;
  }

  /*
  * Public API below:
  *
  * All methods return a jquery element reflecting the given model to be inserted into the DOM using jQuery
  */
  return {
      createSettings: (model) => Settings.createComponent(model),
      createLegend: (model) => Legend.createComponent(model),
      createIndicator: (model) => Indicator.createComponent(model),
      createMenu: (model) => Menu.createComponent(model),
      createAlertTable: (model) => AlertTable.createComponent(model),
      createAnnotationTable: (model) => AnnotationTable.createComponent(model),
      createWatchManager: (model) => WatchManager.createComponent(model),
  };

})();
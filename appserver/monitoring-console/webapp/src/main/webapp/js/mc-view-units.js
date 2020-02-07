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

/**
 * Utilities to convert values given in units from and to string.
 **/
MonitoringConsole.View.Units = (function() {

   const PERCENT_FACTORS = {
      '%': 1
   };

   /**
    * Factors used for any time unit to milliseconds
    */
   const SEC_FACTORS = {
      h: 60 * 60, hours: 60 * 60,
      m: 60, min: 60, mins: 60,
      s: 1, sec: 1, secs: 1,
      _: [['h', 'm', 's'], ['m', 's'], ['h', 'm']]
   };

   /**
    * Factors used for any time unit to milliseconds
    */
   const MS_FACTORS = {
      h: 60 * 60 * 1000, hours: 60 * 60 * 1000,
      m: 60 * 1000, min: 60 * 1000, mins: 60 * 1000,
      s: 1000, sec: 1000, secs: 1000,
      ms: 1,
      us: 1/1000, μs: 1/1000,
      ns: 1/1000000,
      _: [['m', 's', 'ms'], ['s', 'ms'], ['h', 'm', 's'], ['m', 's'], ['h', 'm']]
   };

   /**
    * Factors used for any time unit to nanoseconds
    */
   const NS_FACTORS = {
      h: 60 * 60 * 1000 * 1000000, hours: 60 * 60 * 1000 * 1000000,
      m: 60 * 1000 * 1000000, min: 60 * 1000 * 1000000, mins: 60 * 1000 * 1000000,
      s: 1000 * 1000000, sec: 1000 * 1000000, secs: 1000 * 1000000,
      ms: 1000000,
      us: 1000, μs: 1000,
      ns: 1,
      _: [['m', 's', 'ms'], ['s', 'ms'], ['h', 'm', 's'], ['m', 's'], ['h', 'm']]
   };

   /**
    * Factors used for any memory unit to bytes
    */
   const BYTES_FACTORS = {
      kB: 1024, kb: 1024,
      MB: 1024 * 1024, mb: 1024 * 1024,
      GB: 1024 * 1024 * 1024, gb: 1024 * 1024 * 1024,
      TB: 1024 * 1024 * 1024 * 1024, tb: 1024 * 1024 * 1024 * 1024,
   };

   /**
    * Factors used for usual (unit less) count values
    */
   const COUNT_FACTORS = {
      K: 1000,
      M: 1000 * 1000
   };

   /**
    * Factors by unit
    */
   const FACTORS = {
      count: COUNT_FACTORS,
      sec: SEC_FACTORS,
      ms: MS_FACTORS,
      ns: NS_FACTORS,
      bytes: BYTES_FACTORS,
      percent: PERCENT_FACTORS,
   };

   const UNIT_NAMES = {
      count: 'Count', 
      ms: 'Milliseconds', 
      ns: 'Nanoseconds', 
      bytes: 'Bytes', 
      percent: 'Percentage'
   };

   const ALERT_STATUS_NAMES = { 
      white: 'Normal', 
      green: 'Healthy', 
      amber: 'Degraded', 
      red: 'Unhealthy' 
   };

   function parseNumber(valueAsString, factors) {
      if (!valueAsString || typeof valueAsString === 'string' && valueAsString.trim() === '')
         return undefined;
      let valueAsNumber = Number(valueAsString);
      if (!Number.isNaN(valueAsNumber))
         return valueAsNumber;
      let valueAndUnit = valueAsString.split(/(-?[0-9]+\.?[0-9]*)/);
      let sum = 0;
      for (let i = 1; i < valueAndUnit.length; i+=2) {
         valueAsNumber = Number(valueAndUnit[i].trim());
         let unit = valueAndUnit[i+1].trim().toLowerCase();
         let factor = factors[unit];
         sum += valueAsNumber * factor;               
      }
      return sum;
   }

   function formatDecimal(valueAsNumber) {
      if (!hasDecimalPlaces(valueAsNumber)) {
         return Math.round(valueAsNumber).toString();
      } 
      let text = valueAsNumber.toFixed(1);
      return text.endsWith('.0') ? Math.round(valueAsNumber).toString() : text;
   }

   function formatNumber(valueAsNumber, factors, useDecimals) {
      if (valueAsNumber === undefined)
         return undefined;
      if (valueAsNumber === 0)
         return '0';
      if (!factors)
         return formatDecimal(valueAsNumber);
      if (valueAsNumber < 0)
         return '-' + formatNumber(-valueAsNumber, factors, useDecimals);
      let largestFactorUnit;
      let largestFactor = 0;
      let factor1Unit = '';
      for (let [unit, factor] of Object.entries(factors)) {
         if (unit != '_' && (valueAsNumber >= 0 && factor > 0 || valueAsNumber < 0 && factor < 0)
            && (useDecimals && (valueAsNumber / factor) >= 1 || !hasDecimalPlaces(valueAsNumber / factor))
            && factor > largestFactor) {
            largestFactor = factor;
            largestFactorUnit = unit;
         }
         if (factor === 1)
            factor1Unit = unit;
      }
      if (!largestFactorUnit)
         return formatDecimal(valueAsNumber) + factor1Unit;
      if (useDecimals)
         return formatDecimal(valueAsNumber / largestFactor) + largestFactorUnit;
      let valueInUnit = Math.round(valueAsNumber / largestFactor);
      if (factors._) {
         for (let i = 0; i < factors._.length; i++) {
            let combination = factors._[i];
            let rest = valueAsNumber;
            let text = '';
            if (combination[combination.length - 1] == largestFactorUnit) {
               for (let j = 0; j < combination.length; j++) {
                  let unit = combination[j];
                  let factor = factors[unit];
                  let times = Math.floor(rest / factor);
                  if (times === 0)
                     break;
                  rest -= times * factor;
                  text += times + unit;                      
               }
            }
            if (rest === 0) {
               return text;
            }
         }
      }
      // TODO look for a combination that has the found unit as its last member
      // try if value can eben be expressed as the combination, otherwise try next that might fulfil firs condition
      return valueInUnit + largestFactorUnit;
   }

   function hasDecimalPlaces(number) {
      return number % 1 != 0;
   }

   function formatTime(hourOrDateOrTimestamp, minute, second, millis) {
      if (typeof hourOrDateOrTimestamp === 'string')
         hourOrDateOrTimestamp = Number(hourOrDateOrTimestamp);
      if (typeof hourOrDateOrTimestamp === 'number' && hourOrDateOrTimestamp > 24) { // assume timestamp
         hourOrDateOrTimestamp = new Date(hourOrDateOrTimestamp);
      }
      if (typeof hourOrDateOrTimestamp === 'object') { // assume Date
         minute = hourOrDateOrTimestamp.getMinutes();
         second = hourOrDateOrTimestamp.getSeconds();
         millis = hourOrDateOrTimestamp.getMilliseconds();
         hourOrDateOrTimestamp = hourOrDateOrTimestamp.getHours();
      }
      let str = as2digits(hourOrDateOrTimestamp);
      str += ':' + as2digits(minute ? minute : 0);
      if (second)
         str += ':' +  as2digits(second);
      if (millis)
         str += '.' + as3digits(millis);
      return str;
   }

   function as2digits(number) {
      return number.toString().padStart(2, '0');
   }

   function as3digits(number) {
      return number.toString().padStart(3, '0');
   }

   const DECIMAL_NUMBER_PATTERN = '([0-9]+\.)?[0-9]+';

   function pattern(factors) {
      if (!factors)
         return DECIMAL_NUMBER_PATTERN;
      return '(' + DECIMAL_NUMBER_PATTERN + '(' + Object.keys(factors).filter(unit => unit != '_').join('|') + ')? *)+';
   }

   function patternHint(factors) {
      if (!factors)
         return 'a integer or decimal number';
      return 'a integer or decimal number, optionally followed by a unit: ' + Object.keys(factors).filter(unit => unit != '_').join(', ');
   }

   function maxAlertLevel(a, b) {
      const table = ['white', 'normal', 'green', 'alarming', 'amber', 'critical', 'red'];
      return table[Math.max(0, Math.max(table.indexOf(a), table.indexOf(b)))];
   }

   /**
    * Public API below:
    */
   return {
      Alerts: {
         maxLevel: maxAlertLevel,
         name: (level) => ALERT_STATUS_NAMES[level == undefined ? 'white' : level],
      },
      
      names: () => UNIT_NAMES,

      formatTime: formatTime,
      formatNumber: formatNumber,
      formatMilliseconds: (valueAsNumber) => formatNumber(valueAsNumber, MS_FACTORS),
      formatNanoseconds: (valueAsNumber) => formatNumber(valueAsNumber, NS_FACTORS),
      formatBytes: (valueAsNumber) => formatNumber(valueAsNumber, BYTES_FACTORS),
      parseNumber: parseNumber,
      parseMilliseconds: (valueAsString) => parseNumber(valueAsString, MS_FACTORS),
      parseNanoseconds: (valueAsString) => parseNumber(valueAsString, NS_FACTORS),
      parseBytes: (valueAsString) => parseNumber(valueAsString, BYTES_FACTORS),
      converter: function(unit) {
         let factors = FACTORS[unit];
         return {
            format: (valueAsNumber, useDecimals) => formatNumber(valueAsNumber, factors, useDecimals),
            parse: (valueAsString) => parseNumber(valueAsString, factors),
            pattern: () =>  pattern(factors),
            patternHint: () => patternHint(factors),
         };
      }
   };
})();
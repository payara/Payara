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
 * Utilities to convert color values and handle color schemes.
 *
 * Color schemes are named sets of colors that are applied to the model.
 * This is build purely on top of the model. 
 * The model does not remember a scheme, 
 * schemes are just a utility to reset color configurations to a certain set.
 **/
MonitoringConsole.View.Colors = (function() {

   const Colors = MonitoringConsole.Model.Colors;

   const SCHEMES = {
      Payara: {
         name: 'Payara',
         palette: [ '#F0981B', '#008CC4', '#8B79BC', '#87BC25', '#FF70DA' ],
         opacity: 20,
         defaults:  { 
            waterline: '#00ffff', alarming: '#FFD700', critical: '#dc143c',
            white: '#ffffff', green: '#87BC25', amber: '#f0981b', red: '#dc143c',
          }
      },

      a: {
         name: '80s',
         opacity: 30,
         palette: [ '#ff48c4', '#2bd1fc', '#f3ea5f', '#c04df9', '#ff3f3f'],
         defaults:  { waterline: '#2bd1fc', alarming: '#f3ea5f', critical: '#ff3f3f' }
      },

      b: {
         name: '80s Pastel',
         opacity: 40,
         palette: [ '#bd6283', '#96c0bc', '#dbd259', '#d49e54', '#b95f51'],
         defaults:  { waterline: '#96c0bc', alarming: '#dbd259', critical: '#b95f51' }
      },

      c: {
         name: '80s Neon',
         opacity: 15,
         palette: [ '#cb268b', '#f64e0c', '#eff109', '#6cf700', '#00aff3'],
         defaults:  { waterline: '#00aff3', alarming: '#eff109', critical: '#f64e0c' }
      },

      d: {
         name: 'VaporWave',
         opacity: 20,
         palette: [ '#05ffa1', '#b8a9df', '#01cdfe', '#b967ff', '#fffb96'],
         defaults:  { waterline: '#01cdfe', alarming: '#fffb96', critical: '#FB637A' }
      },

      e: {
         name: 'Solarized',
         opacity: 20,
         palette: [ '#b58900', '#cb4b16', '#dc322f', '#d32682', '#c671c4', '#268bd2', '#2aa198', '#859900'],
         defaults:  { waterline: '#268bd2', alarming: '#b58900', critical: '#dc322f' }
      }
   };

   /**
    * Object used as map to remember the colors by coloring stratgey.
    * Each strategy leads to an object map that remebers the key as fields 
    * and the index associated with the key as value.
    * This is a mapping from e.g. the name 'DAS' to index 0. 
    * The index is then used to pick a color form the palette.
    * This makes sure that same key, for example the instance name,
    * always uses the same color accross widgets and pages.
    */
   let colorIndexMaps = {};

   function lookup(coloring, key, palette) {
      let mapName = coloring || 'instance';
      let map = colorIndexMaps[mapName];
      if (map === undefined) {
         map = {};
         colorIndexMaps[mapName] = map;
      }
      let index = map[key];
      if (index === undefined) {
         index = Object.keys(map).length;
         map[key] = index;
      }
      return derive(palette, index);
   }

   /**
    * Returns a palette color.
    * If index is outside of the palette given a color is derived from the palette.
    * The derived colors are intentionally biased towards light non-grayish colors.
    */
   function derive(palette, index = 1) {
      let color = palette[index % palette.length];
      if (index < palette.length)
         return color;
      let [r, g, b] = hex2rgb(color);
      let offset = index - palette.length + 1;
      let shift = (offset * 110) % 255;
      r = (r + shift) % 255;
      g = (g + shift) % 255;
      b = (b + shift) % 255;
      let variant = offset % 6;
      if (variant == 0 || variant == 3 || variant == 4)
         r = Math.round(r / 2);
      if (variant == 1 || variant == 3 || variant == 5)
         g = Math.round(g / 2);
      if (variant == 2 || variant == 4 || variant == 5)
         b = Math.round(b / 2);
      if (r + g + b < 380 && r < 120) r = 255 - r;
      if (r + g + b < 380 && g < 120) g = 255 - g;
      if (r + g + b < 380 && b < 120) b = 255 - b;
      return rgb2hex(r, g, b);
   }

   function random(palette) {
      if (Array.isArray(palette))
         return derive([palette[0]], palette.length); 
      const letters = '0123456789ABCDEF';
      let color = '#';
      for (let i = 0; i < 6; i++) {
         color += letters[Math.floor(Math.random() * 16)];
      }
      return color;
   }

   function hex2rgb(hex) {
      return hex.match(/\w\w/g).map(x => parseInt(x, 16));
   }

   function rgb2hex(r, g, b) {
      return "#" + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
   }

   function hex2rgba(hex, alpha = 1) {
      const [r, g, b] = hex2rgb(hex);
      return `rgba(${r},${g},${b},${alpha})`;
   }

   function schemeOptions() {
      return Object.keys(SCHEMES).reduce(function(result, key) {
         result[key] = SCHEMES[key].name;
         return result;
      }, { _: '(Select to apply)' });
   }

   function applyScheme(name, override = true) {
      let scheme = SCHEMES[name];
      if (scheme) {
         if (override || Colors.palette() === undefined)
            Colors.palette(scheme.palette);
         if (override || Colors.opacity() === undefined)
            Colors.opacity(scheme.opacity);
         if (scheme.defaults) {
            for (let [name, color] of Object.entries(scheme.defaults)) {
               if (override || Colors.default(name) === undefined)
                  Colors.default(name, color);
            }
         }            
      }
   }

   /**
    * Public API of the color utility module.
    */
   return {
      lookup: lookup,
      random: random,
      hex2rgba: hex2rgba,
      schemes: schemeOptions,
      scheme: applyScheme,
   };
})();
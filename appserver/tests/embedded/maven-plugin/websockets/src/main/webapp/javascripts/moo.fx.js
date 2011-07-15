/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

var Fx = fx = {};

Fx.Base = function(){};
Fx.Base.prototype = {

	setOptions: function(options){
		this.options = Object.extend({
			onStart: function(){},
			onComplete: function(){},
			transition: Fx.Transitions.sineInOut,
			duration: 500,
			unit: 'px',
			wait: true,
			fps: 50
		}, options || {});
	},

	step: function(){
		var time = new Date().getTime();
		if (time < this.time + this.options.duration){
			this.cTime = time - this.time;
			this.setNow();
		} else {
			setTimeout(this.options.onComplete.bind(this, this.element), 10);
			this.clearTimer();
			this.now = this.to;
		}
		this.increase();
	},

	setNow: function(){
		this.now = this.compute(this.from, this.to);
	},

	compute: function(from, to){
		var change = to - from;
		return this.options.transition(this.cTime, from, change, this.options.duration);
	},

	clearTimer: function(){
		clearInterval(this.timer);
		this.timer = null;
		return this;
	},

	_start: function(from, to){
		if (!this.options.wait) this.clearTimer();
		if (this.timer) return;
		setTimeout(this.options.onStart.bind(this, this.element), 10);
		this.from = from;
		this.to = to;
		this.time = new Date().getTime();
		this.timer = setInterval(this.step.bind(this), Math.round(1000/this.options.fps));
		return this;
	},

	custom: function(from, to){
		return this._start(from, to);
	},

	set: function(to){
		this.now = to;
		this.increase();
		return this;
	},

	hide: function(){
		return this.set(0);
	},

	setStyle: function(e, p, v){
		if (p == 'opacity'){
			if (v == 0 && e.style.visibility != "hidden") e.style.visibility = "hidden";
			else if (e.style.visibility != "visible") e.style.visibility = "visible";
			if (window.ActiveXObject) e.style.filter = "alpha(opacity=" + v*100 + ")";
			e.style.opacity = v;
		} else e.style[p] = v+this.options.unit;
	}

};

Fx.Style = Class.create();
Fx.Style.prototype = Object.extend(new Fx.Base(), {

	initialize: function(el, property, options){
		this.element = $(el);
		this.setOptions(options);
		this.property = property.camelize();
	},

	increase: function(){
		this.setStyle(this.element, this.property, this.now);
	}

});

Fx.Styles = Class.create();
Fx.Styles.prototype = Object.extend(new Fx.Base(), {

	initialize: function(el, options){
		this.element = $(el);
		this.setOptions(options);
		this.now = {};
	},

	setNow: function(){
		for (p in this.from) this.now[p] = this.compute(this.from[p], this.to[p]);
	},

	custom: function(obj){
		if (this.timer && this.options.wait) return;
		var from = {};
		var to = {};
		for (p in obj){
			from[p] = obj[p][0];
			to[p] = obj[p][1];
		}
		return this._start(from, to);
	},

	increase: function(){
		for (var p in this.now) this.setStyle(this.element, p, this.now[p]);
	}

});

//Transitions (c) 2003 Robert Penner (http://www.robertpenner.com/easing/), BSD License.

Fx.Transitions = {
	linear: function(t, b, c, d) { return c*t/d + b; },
	sineInOut: function(t, b, c, d) { return -c/2 * (Math.cos(Math.PI*t/d) - 1) + b; }
};
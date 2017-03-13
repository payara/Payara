webpackJsonp([1,4],{

/***/ 103:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_http__ = __webpack_require__(149);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_toPromise__ = __webpack_require__(163);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_toPromise___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_toPromise__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return LoginService; });
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
/**
 *
 * Service to login to the Zendesk platform
 * Author : Daniel Contreras Aladro
 * Date : 2017-02-21
 *
 */



var LoginService = (function () {
    /**
     * constructor - Constructor of the service
     */
    function LoginService(http) {
        this.http = http;
    }
    /**
     * getUserData - Method to call to the API to make login
     *
     * @param {string}  email String with the email to make te login
     *
     * @return {Promise<User>} Returns the response promise
     */
    LoginService.prototype.getUserData = function (email) {
        this.headers = new __WEBPACK_IMPORTED_MODULE_1__angular_http__["c" /* Headers */]();
        this.headers.append('Authorization', 'Basic ' + btoa(email + '/token:' + this.connectionData.token));
        this.headers.append('Content-Type', 'application/json');
        return this.http.get(this.connectionData.zendeskUrl + 'users/me.json', { headers: this.headers })
            .toPromise()
            .then(function (response) { return response.json().user; });
    };
    return LoginService;
}());
LoginService = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["e" /* Injectable */])(),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_1__angular_http__["b" /* Http */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_http__["b" /* Http */]) === "function" && _a || Object])
], LoginService);

var _a;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/login.service.js.map

/***/ }),

/***/ 226:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return Ticket; });
var Ticket = (function () {
    function Ticket() {
    }
    return Ticket;
}());

//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/ticket.js.map

/***/ }),

/***/ 227:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return User; });
/**
 *
 * Class to define the fields of a user
 * Author : Daniel Contreras Aladro
 * Date : 2017-02-21
 *
 */
var User = (function () {
    function User() {
    }
    return User;
}());

//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/user.js.map

/***/ }),

/***/ 228:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return environment; });
// The file contents for the current environment will overwrite these during build.
// The build system defaults to the dev environment which uses `environment.ts`, but if you do
// `ng build --env=prod` then `environment.prod.ts` will be used instead.
// The list of which env maps to which file can be found in `angular-cli.json`.
// The file contents for the current environment will overwrite these during build.
var environment = {
    production: true,
    zendesk: {
        admin: 'support@payara.co',
        pwd: 'P4y4r4$upp0rt',
        token: 'ypxD1iQz4gC3CXNZa60y9GC39izttZmxQXMoxtt0',
        baseUrl: 'https://payara.zendesk.com/api/v2/'
    },
    payara: {
        baseUrl: '/management/domain/configs/config/server-config/zendesk-support-configuration/'
    }
};
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/environment.js.map

/***/ }),

/***/ 348:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_http__ = __webpack_require__(149);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_toPromise__ = __webpack_require__(163);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_toPromise___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_toPromise__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return PayaraService; });
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
/**
 *
 * Service to login to the Zendesk platform
 * Author : Daniel Contreras Aladro
 * Date : 2017-02-21
 *
 */



var PayaraService = (function () {
    /**
     * constructor - Constructor of the service
     */
    function PayaraService(http) {
        this.http = http;
    }
    /**
     * getStoredEmail - Method to call to the API to get the Zendesk Support Stored Email
     *
     * @return {Promise<string>} Returns the response promise
     */
    PayaraService.prototype.getStoredEmail = function () {
        if (this.email !== undefined && this.email !== null && this.email !== '') {
            return Promise.resolve(this.email);
        }
        else {
            this.headers = new __WEBPACK_IMPORTED_MODULE_1__angular_http__["c" /* Headers */]();
            this.headers.append('Content-Type', 'application/json');
            return this.http.get(this.connectionData.payaraURL + 'get-zendesk-support-configuration.json', { headers: this.headers })
                .toPromise()
                .then(function (response) { return response.json().extraProperties.zendeskSupportConfiguration.emailAddress; });
        }
    };
    /**
     * setStoredEmail - Method to call to the API to set the Zendesk Support Stored Email
     *
     * @param {string}  email String with the email to set inside domain.xml
     *
     * @return {Promise<string>} Returns the response promise
     */
    PayaraService.prototype.setStoredEmail = function (email) {
        this.headers = new __WEBPACK_IMPORTED_MODULE_1__angular_http__["c" /* Headers */]();
        this.headers.append('accept', 'application/json');
        this.headers.append('X-Requested-By', 'payara');
        this.headers.append('Content-Type', 'application/json');
        return this.http.post(this.connectionData.payaraURL + 'set-zendesk-support-configuration', JSON.stringify({ emailAddress: email }), { headers: this.headers })
            .toPromise()
            .then(function (response) { return response.json(); });
    };
    return PayaraService;
}());
PayaraService = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["e" /* Injectable */])(),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_1__angular_http__["b" /* Http */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_http__["b" /* Http */]) === "function" && _a || Object])
], PayaraService);

var _a;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/payara.service.js.map

/***/ }),

/***/ 506:
/***/ (function(module, exports) {

function webpackEmptyContext(req) {
	throw new Error("Cannot find module '" + req + "'.");
}
webpackEmptyContext.keys = function() { return []; };
webpackEmptyContext.resolve = webpackEmptyContext;
module.exports = webpackEmptyContext;
webpackEmptyContext.id = 506;


/***/ }),

/***/ 507:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser_dynamic__ = __webpack_require__(594);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__environments_environment__ = __webpack_require__(228);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__app_app_module__ = __webpack_require__(625);




if (__WEBPACK_IMPORTED_MODULE_2__environments_environment__["a" /* environment */].production) {
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_1__angular_core__["a" /* enableProdMode */])();
}
__webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_platform_browser_dynamic__["a" /* platformBrowserDynamic */])().bootstrapModule(__WEBPACK_IMPORTED_MODULE_3__app_app_module__["a" /* AppModule */]);
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/main.js.map

/***/ }),

/***/ 53:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_http__ = __webpack_require__(149);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_toPromise__ = __webpack_require__(163);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_toPromise___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_toPromise__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ZendeskService; });
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
/**
 *
 * Service to operate against Zendesk platform API
 * Author : Daniel Contreras Aladro
 * Date : 2017-02-21
 *
 */



var ZendeskService = (function () {
    /**
     * constructor - Constructor of the service
     */
    function ZendeskService(http) {
        this.http = http;
    }
    /**
     * setHeaders - Method to set the security headers of the request
     *
     * @param {boolean}  file Boolean to set headers for the file requests
     * @param {boolean}  admin Boolean to set headers for the admin requests
     */
    ZendeskService.prototype.setHeaders = function (file, admin) {
        this.headers = new __WEBPACK_IMPORTED_MODULE_1__angular_http__["c" /* Headers */]();
        if (admin) {
            this.headers.append('Authorization', 'Basic ' + btoa(this.connectionData.admin + ':' + this.connectionData.pwd));
        }
        else {
            this.headers.append('Authorization', 'Basic ' + btoa(this.username + '/token:' + this.connectionData.token));
        }
        if (file) {
            this.headers.append('Content-Type', 'application/binary');
        }
        else {
            this.headers.append('Content-Type', 'application/json');
        }
    };
    /**
     * getTicketsOrganizationRequested - Method to get the tickets openend by the organization of the user
     *
     * @param {number}  organization Number with the organization id
     * @return {Promise<Ticket[]>} Returns the response promise
     */
    ZendeskService.prototype.getTicketsOrganizationRequested = function (organization) {
        this.setHeaders(false, false);
        if (this.ticketsOrganization !== undefined && this.ticketsOrganization !== null && this.ticketsOrganization.length > 0) {
            return Promise.resolve(this.ticketsOrganization);
        }
        else {
            return this.http.get(this.connectionData.zendeskUrl + 'organizations/' + organization + '/tickets.json', { headers: this.headers })
                .toPromise()
                .then(function (response) { return response.json().tickets; });
        }
    };
    /**
     * getTicketsUserRequested - Method to get the tickets openend by the user
     *
     * @param {number}  user Number with the user id
     *
     * @return {Promise<Ticket[]>} Returns the response promise
     */
    ZendeskService.prototype.getTicketsUserRequested = function (user) {
        this.setHeaders(false, false);
        if (this.ticketsUser !== undefined && this.ticketsUser !== null && this.ticketsUser.length > 0) {
            return Promise.resolve(this.ticketsUser);
        }
        else {
            return this.http.get(this.connectionData.zendeskUrl + 'users/' + user + '/requests.json', { headers: this.headers })
                .toPromise()
                .then(function (response) { return response.json().requests; });
        }
    };
    /**
     * getUserIdentity - Method to get the user identity
     *
     * @param {string}  user String with the user id
     *
     * @return {Promise} Returns the response promise
     */
    ZendeskService.prototype.getUserIdentity = function (user) {
        this.setHeaders(false, false);
        return this.http.get(this.connectionData.zendeskUrl + 'users/' + user + '.json', { headers: this.headers })
            .toPromise()
            .then(function (response) { return response.json().user; });
    };
    /**
     * getGenericCustomFields - Method to get the data of the generic fields
     *
     * @return {Promise<Field[]>} Returns the response promise
     */
    ZendeskService.prototype.getGenericCustomFields = function () {
        this.setHeaders(false, true);
        return this.http.get(this.connectionData.zendeskUrl + 'ticket_fields.json', { headers: this.headers })
            .toPromise()
            .then(function (response) { return response.json().ticket_fields; });
    };
    /**
     * getCustomField - Method to get the data of a custom field
     *
     * @param {number}  field Number with the field id
     *
     * @return {Promise} Returns the response promise
     */
    ZendeskService.prototype.getCustomField = function (field) {
        this.setHeaders(false, true);
        return this.http.get(this.connectionData.zendeskUrl + 'ticket_fields/' + field + '.json', { headers: this.headers })
            .toPromise()
            .then(function (response) { return response.json().ticket_field; });
    };
    /**
     * getTicketComments - Method to get the comnents of a ticket
     *
     * @param {number}  ticket Number with the ticket id
     *
     * @return {Promise<Comment[]>} Returns the response promise
     */
    ZendeskService.prototype.getTicketComments = function (ticket) {
        this.setHeaders(false, false);
        return this.http.get(this.connectionData.zendeskUrl + 'requests/' + ticket + '/comments.json', { headers: this.headers })
            .toPromise()
            .then(function (response) { return response.json().comments; });
    };
    /**
     * addNewComment - Method to add a comment to a ticket
     *
     * @param {Ticket}  ticket Object with the data of the ticket
     *
     * @return {Promise} Returns the response promise
     */
    ZendeskService.prototype.addNewComment = function (ticket) {
        this.setHeaders(false, true);
        this.http.put(this.connectionData.zendeskUrl + 'tickets/' + ticket.id + '.json', JSON.stringify({ ticket: ticket }), { headers: this.headers })
            .toPromise()
            .then(function () { return ticket; });
    };
    /**
     * addNewFile - Method to upload new file
     *
     * @param {Object}  input Object with the data of the file
     * @param {string}  filename String with the email to make te login
     *
     * @return {Promise} Returns the response promise
     */
    ZendeskService.prototype.addNewFile = function (input, filename) {
        this.setHeaders(true, false);
        return this.http
            .post(this.connectionData.zendeskUrl + 'uploads.json?filename=' + filename, input, { headers: this.headers })
            .toPromise()
            .then(function (res) { return res.json().upload; });
    };
    /**
     * createNewTicket - Method to create a new ticket
     *
     * @param {Ticket}  ticketData Object with the data of a ticket
     *
     * @return {Promise} Returns the response promise
     */
    ZendeskService.prototype.createNewTicket = function (ticketData) {
        this.setHeaders(false, false);
        return this.http
            .post(this.connectionData.zendeskUrl + 'requests.json', JSON.stringify({ request: ticketData }), { headers: this.headers })
            .toPromise()
            .then(function (res) { return res.json().request; });
    };
    return ZendeskService;
}());
ZendeskService = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["e" /* Injectable */])(),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_1__angular_http__["b" /* Http */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_http__["b" /* Http */]) === "function" && _a || Object])
], ZendeskService);

var _a;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/zendesk.service.js.map

/***/ }),

/***/ 624:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(77);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_moment__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_moment___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_moment__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__environments_environment__ = __webpack_require__(228);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__services_login_service__ = __webpack_require__(103);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__services_zendesk_service__ = __webpack_require__(53);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__services_payara_service__ = __webpack_require__(348);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__classes_user__ = __webpack_require__(227);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AppComponent; });
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
/**
 *
 * Main Component of the App
 * Author : Daniel Contreras Aladro
 * Date : 2017-02-21
 *
 */









var AppComponent = (function () {
    /**
     * constructor - Constructor of the component
     */
    function AppComponent(translate, router, route, zendeskService, loginService, payaraService) {
        this.translate = translate;
        this.router = router;
        this.route = route;
        this.zendeskService = zendeskService;
        this.loginService = loginService;
        this.payaraService = payaraService;
        /**
         * Properties and objects of the component
         */
        this.environment = __WEBPACK_IMPORTED_MODULE_3__environments_environment__["a" /* environment */];
        this.user = new __WEBPACK_IMPORTED_MODULE_8__classes_user__["a" /* User */]();
        translate.setDefaultLang(translate.getBrowserLang());
        translate.use(translate.getBrowserLang());
        __WEBPACK_IMPORTED_MODULE_2_moment__["locale"](translate.getBrowserLang());
        //translate.use('es');moment.locale('es');
        this.loginService.connectionData = {
            zendeskUrl: this.environment.zendesk.baseUrl,
            token: this.environment.zendesk.token
        };
        this.zendeskService.connectionData = {
            zendeskUrl: this.environment.zendesk.baseUrl,
            token: this.environment.zendesk.token,
            pwd: this.environment.zendesk.pwd,
            admin: this.environment.zendesk.admin
        };
        this.payaraService.connectionData = {
            payaraURL: this.environment.payara.baseUrl
        };
    }
    /**
     * ngOnInit - OnInit method of the component
     */
    AppComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.zendeskService.ticketsUser = [];
        this.zendeskService.ticketsOrganization = [];
        this.user = new __WEBPACK_IMPORTED_MODULE_8__classes_user__["a" /* User */]();
        var globalPort = window["globalPort"];
        if (globalPort !== undefined && globalPort !== null) {
            this.payaraService.connectionData = {
                payaraURL: 'http://localhost:' + globalPort + this.environment.payara.baseUrl
            };
            this.payaraService.getStoredEmail()
                .then(function (responseData) {
                if (responseData !== undefined && responseData !== null) {
                    var email = responseData;
                    var regExpEmail = /^[a-z0-9]+(\.[_a-z0-9]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,15})$/i;
                    if (email !== '' && regExpEmail.test(email)) {
                        _this.loginService.getUserData(email)
                            .then(function (responseData) {
                            if (responseData !== undefined && responseData !== null && responseData.id !== null) {
                                _this.user = responseData;
                                _this.loginService.user = _this.user;
                                _this.router.navigate(['/list']);
                            }
                            else {
                                _this.user = null;
                                _this.loginService.user = _this.user;
                            }
                        }, function (error) {
                            _this.user = null;
                            _this.loginService.user = _this.user;
                        });
                    }
                    else {
                        _this.user = null;
                        _this.loginService.user = _this.user;
                    }
                }
                else {
                    _this.user = null;
                    _this.loginService.user = _this.user;
                }
            }, function (error) {
                _this.user = null;
                _this.loginService.user = _this.user;
            });
        }
    };
    /**
     * isCurrentRoute - Check if the current route is equals a parameter passed
     *
     * @param {string}  route String wih the name of the route to check against the current
     */
    AppComponent.prototype.isCurrentRoute = function (route) {
        return this.router.url === '/' + route;
    };
    /**
     * logout - Disconnect the user to the Zendesk API
     */
    AppComponent.prototype.logout = function () {
        this.user = null;
        this.loginService.user = this.user;
        this.zendeskService.ticketsUser = [];
        this.zendeskService.ticketsOrganization = [];
        this.zendeskService.genericFields = [];
        this.zendeskService.username = null;
        this.router.navigate(['/login']);
        this.payaraService.setStoredEmail('');
    };
    return AppComponent;
}());
AppComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_5" /* Component */])({
        selector: 'app-root',
        template: __webpack_require__(702),
        styles: [__webpack_require__(691)],
        providers: [__WEBPACK_IMPORTED_MODULE_5__services_login_service__["a" /* LoginService */], __WEBPACK_IMPORTED_MODULE_6__services_zendesk_service__["a" /* ZendeskService */], __WEBPACK_IMPORTED_MODULE_7__services_payara_service__["a" /* PayaraService */]]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["b" /* Router */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["b" /* Router */]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["c" /* ActivatedRoute */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["c" /* ActivatedRoute */]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_6__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_6__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_5__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_5__services_login_service__["a" /* LoginService */]) === "function" && _e || Object, typeof (_f = typeof __WEBPACK_IMPORTED_MODULE_7__services_payara_service__["a" /* PayaraService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_7__services_payara_service__["a" /* PayaraService */]) === "function" && _f || Object])
], AppComponent);

var _a, _b, _c, _d, _e, _f;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/app.component.js.map

/***/ }),

/***/ 625:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser__ = __webpack_require__(153);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_forms__ = __webpack_require__(319);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_http__ = __webpack_require__(149);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__angular_router__ = __webpack_require__(77);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__angular_common__ = __webpack_require__(44);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__ngx_translate_core__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__ngx_translate_http_loader__ = __webpack_require__(635);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__environments_environment__ = __webpack_require__(228);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__app_component__ = __webpack_require__(624);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10__components_login_login_component__ = __webpack_require__(631);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_11__components_list_tickets_list_tickets_component__ = __webpack_require__(630);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_12__components_new_ticket_new_ticket_component__ = __webpack_require__(632);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_13__components_detailed_ticket_detailed_ticket_component__ = __webpack_require__(629);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_14__components_add_file_add_file_component__ = __webpack_require__(627);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_15__components_ticket_data_ticket_data_component__ = __webpack_require__(633);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_16__components_comment_data_comment_data_component__ = __webpack_require__(628);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_17__pipes_day_time_pipe__ = __webpack_require__(634);
/* unused harmony export createTranslateLoader */
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AppModule; });
/**
 *
 * Main Module to generate the App
 * Author : Daniel Contreras Aladro
 * Date : 2017-02-21
 *
 */
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
/**
 * Generic imports
 */









/**
 * Component imports
 */








/**
 * Pipe imports
 */

/**
 * Routes declared to navigate intothe app
 */
var appRoutes = [
    {
        path: '',
        redirectTo: '/login',
        pathMatch: 'full'
    },
    {
        path: 'login',
        component: __WEBPACK_IMPORTED_MODULE_10__components_login_login_component__["a" /* LoginComponent */]
    },
    {
        path: 'list',
        component: __WEBPACK_IMPORTED_MODULE_11__components_list_tickets_list_tickets_component__["a" /* ListTicketsComponent */]
    },
    {
        path: 'detail/:id',
        component: __WEBPACK_IMPORTED_MODULE_13__components_detailed_ticket_detailed_ticket_component__["a" /* DetailedTicketComponent */]
    },
    {
        path: 'new',
        component: __WEBPACK_IMPORTED_MODULE_12__components_new_ticket_new_ticket_component__["a" /* NewTicketComponent */]
    },
    { path: '**', redirectTo: '/login' }
];
function createTranslateLoader(http) {
    return new __WEBPACK_IMPORTED_MODULE_7__ngx_translate_http_loader__["a" /* TranslateHttpLoader */](http, './assets/i18n/', '.json');
}
if (__WEBPACK_IMPORTED_MODULE_8__environments_environment__["a" /* environment */].production) {
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_1__angular_core__["a" /* enableProdMode */])();
}
var AppModule = (function () {
    function AppModule() {
    }
    return AppModule;
}());
AppModule = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_1__angular_core__["b" /* NgModule */])({
        declarations: [
            __WEBPACK_IMPORTED_MODULE_9__app_component__["a" /* AppComponent */],
            __WEBPACK_IMPORTED_MODULE_10__components_login_login_component__["a" /* LoginComponent */],
            __WEBPACK_IMPORTED_MODULE_11__components_list_tickets_list_tickets_component__["a" /* ListTicketsComponent */],
            __WEBPACK_IMPORTED_MODULE_12__components_new_ticket_new_ticket_component__["a" /* NewTicketComponent */],
            __WEBPACK_IMPORTED_MODULE_13__components_detailed_ticket_detailed_ticket_component__["a" /* DetailedTicketComponent */],
            __WEBPACK_IMPORTED_MODULE_14__components_add_file_add_file_component__["a" /* AddFileComponent */],
            __WEBPACK_IMPORTED_MODULE_15__components_ticket_data_ticket_data_component__["a" /* TicketDataComponent */],
            __WEBPACK_IMPORTED_MODULE_16__components_comment_data_comment_data_component__["a" /* CommentDataComponent */],
            __WEBPACK_IMPORTED_MODULE_17__pipes_day_time_pipe__["a" /* DayTimePipe */]
        ],
        imports: [
            __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser__["a" /* BrowserModule */],
            __WEBPACK_IMPORTED_MODULE_2__angular_forms__["a" /* FormsModule */],
            __WEBPACK_IMPORTED_MODULE_2__angular_forms__["b" /* ReactiveFormsModule */],
            __WEBPACK_IMPORTED_MODULE_3__angular_http__["a" /* HttpModule */],
            __WEBPACK_IMPORTED_MODULE_4__angular_router__["a" /* RouterModule */].forRoot(appRoutes),
            __WEBPACK_IMPORTED_MODULE_6__ngx_translate_core__["a" /* TranslateModule */].forRoot({
                loader: {
                    provide: __WEBPACK_IMPORTED_MODULE_6__ngx_translate_core__["b" /* TranslateLoader */],
                    useFactory: (createTranslateLoader),
                    deps: [__WEBPACK_IMPORTED_MODULE_3__angular_http__["b" /* Http */]]
                }
            })
        ],
        exports: [__WEBPACK_IMPORTED_MODULE_4__angular_router__["a" /* RouterModule */], __WEBPACK_IMPORTED_MODULE_17__pipes_day_time_pipe__["a" /* DayTimePipe */], __WEBPACK_IMPORTED_MODULE_6__ngx_translate_core__["a" /* TranslateModule */]],
        providers: [__WEBPACK_IMPORTED_MODULE_5__angular_common__["a" /* DatePipe */], __WEBPACK_IMPORTED_MODULE_17__pipes_day_time_pipe__["a" /* DayTimePipe */]],
        bootstrap: [__WEBPACK_IMPORTED_MODULE_9__app_component__["a" /* AppComponent */]]
    })
], AppModule);

//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/app.module.js.map

/***/ }),

/***/ 626:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return Comment; });
var Comment = (function () {
    function Comment() {
    }
    return Comment;
}());

//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/comment.js.map

/***/ }),

/***/ 627:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__ngx_translate_core__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__services_zendesk_service__ = __webpack_require__(53);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AddFileComponent; });
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
/**
 *
 * Component to attach files to the storage
 * Author : Daniel Contreras Aladro
 * Date : 2017-02-21
 *
 */



var AddFileComponent = (function () {
    /**
     * constructor - Constructor of the component
     */
    function AddFileComponent(translate, zendeskService) {
        this.translate = translate;
        this.zendeskService = zendeskService;
        /**
         * Properties and objects of the component
         */
        this.saved = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["v" /* EventEmitter */]();
    }
    /**
     * ngOnInit - OnInit method ofthe component
     */
    AddFileComponent.prototype.ngOnInit = function () {
    };
    /**
     * onChange - Event method for make actions when a file is added
     *
     * @param {Event}  event Object with the event data
     */
    AddFileComponent.prototype.onChange = function (event) {
        var _this = this;
        var fileList = event.target.files;
        if (fileList.length > 0) {
            var file = fileList[0];
            var formData = new FormData();
            formData.append('upload', file, file.name);
            this.zendeskService.addNewFile(formData, file.name)
                .then(function (responseData) {
                if (responseData !== undefined && responseData !== null) {
                    _this.saved.emit(responseData);
                }
            }, function (error) {
                _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                    _this.errorMessage = res;
                });
            });
        }
    };
    return AddFileComponent;
}());
__decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_2" /* Output */])(),
    __metadata("design:type", Object)
], AddFileComponent.prototype, "saved", void 0);
AddFileComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_5" /* Component */])({
        selector: 'app-add-file',
        template: __webpack_require__(703),
        styles: [__webpack_require__(692)]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_1__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_2__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _b || Object])
], AddFileComponent);

var _a, _b;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/add-file.component.js.map

/***/ }),

/***/ 628:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common__ = __webpack_require__(44);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__ = __webpack_require__(53);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__classes_ticket__ = __webpack_require__(226);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__classes_comment__ = __webpack_require__(626);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return CommentDataComponent; });
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
/**
 *
 * Component to show the data of the comments of a ticket
 * Author : Daniel Contreras Aladro
 * Date : 2017-02-21
 *
 */






var CommentDataComponent = (function () {
    /**
     * constructor - Constructor of the component
     */
    function CommentDataComponent(translate, zendeskService, datePipe) {
        this.translate = translate;
        this.zendeskService = zendeskService;
        this.datePipe = datePipe;
        /**
         * Properties and objects of the component
         */
        this.saved = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["v" /* EventEmitter */]();
    }
    /**
     * ngOnInit - OnInit method ofthe component
     */
    CommentDataComponent.prototype.ngOnInit = function () {
        this.newAttachments = [];
        this.getComments();
    };
    /**
     * getComments - Method to recover the comments of a ticket from the API
     */
    CommentDataComponent.prototype.getComments = function () {
        var _this = this;
        this.zendeskService.getTicketComments(this.ticket.id)
            .then(function (responseData) {
            if (responseData !== undefined && responseData !== null) {
                _this.comments = responseData;
            }
        }, function (error) {
            _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                _this.errorMessage = res;
            });
        });
    };
    /**
     * keyUpEvent - Event method to remove errorMEssage while theuser is writing
     *
     * @param {any}  event Angular event related with the action
     */
    CommentDataComponent.prototype.keyUpEvent = function (event) {
        this.errorMessage = '';
    };
    /**
     * onSavedAttachment - Method to add new files to a files array
     *
     * @param {Attachment}  newAttachment Object with a new file to attach
     */
    CommentDataComponent.prototype.onSavedAttachment = function (newAttachment) {
        this.newAttachments.push(newAttachment);
    };
    /**
     * saveComment - Method to save new comments
     */
    CommentDataComponent.prototype.saveComment = function () {
        var _this = this;
        if (this.newCommentText !== undefined && this.newCommentText !== null && this.newCommentText !== '') {
            var newComment = new __WEBPACK_IMPORTED_MODULE_5__classes_comment__["a" /* Comment */]();
            newComment.body = this.newCommentText;
            if (this.newAttachments !== undefined && this.newAttachments.length > 0) {
                newComment.uploads = [];
                newComment.uploads.push(this.newAttachments[0].token);
                newComment.attachments = [];
                newComment.attachments.push(this.newAttachments[0].attachment);
            }
            newComment.created_at = this.datePipe.transform(new Date(), 'yyyy-MM-ddTHH:mm:ss');
            this.comments.push(newComment);
            this.saved.emit(newComment);
            this.newCommentText = '';
            this.newAttachments = [];
        }
        else {
            this.translate.get('Empty comment').subscribe(function (res) {
                _this.errorMessage = res;
            });
        }
    };
    return CommentDataComponent;
}());
__decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_2" /* Output */])(),
    __metadata("design:type", Object)
], CommentDataComponent.prototype, "saved", void 0);
__decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["l" /* Input */])(),
    __metadata("design:type", typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_4__classes_ticket__["a" /* Ticket */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__classes_ticket__["a" /* Ticket */]) === "function" && _a || Object)
], CommentDataComponent.prototype, "ticket", void 0);
__decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_2" /* Output */])(),
    __metadata("design:type", String)
], CommentDataComponent.prototype, "newCommentText", void 0);
CommentDataComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_5" /* Component */])({
        selector: 'app-comment-data',
        template: __webpack_require__(704),
        styles: [__webpack_require__(693)]
    }),
    __metadata("design:paramtypes", [typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_1__angular_common__["a" /* DatePipe */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_common__["a" /* DatePipe */]) === "function" && _d || Object])
], CommentDataComponent);

var _a, _b, _c, _d;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/comment-data.component.js.map

/***/ }),

/***/ 629:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(77);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__(44);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_add_operator_switchMap__ = __webpack_require__(477);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_add_operator_switchMap___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_rxjs_add_operator_switchMap__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__services_zendesk_service__ = __webpack_require__(53);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return DetailedTicketComponent; });
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
/**
 *
 * Component to show the detail of a ticket, with the comments and the attachments
 * Author : Daniel Contreras Aladro
 * Date : 2017-02-21
 *
 */





var DetailedTicketComponent = (function () {
    /**
     * constructor - Constructor of the component
     */
    function DetailedTicketComponent(route, router, zendeskService, datePipe) {
        this.route = route;
        this.router = router;
        this.zendeskService = zendeskService;
        this.datePipe = datePipe;
    }
    /**
     * ngOnInit - OnInit method of the component
     */
    DetailedTicketComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.id = +this.route.snapshot.params['id'];
        if (this.zendeskService.organization) {
            this.ticket = this.zendeskService.ticketsOrganization.filter(function (ticket) { return ticket.id === _this.id; })[0];
        }
        else {
            this.ticket = this.zendeskService.ticketsUser.filter(function (ticket) { return ticket.id === _this.id; })[0];
        }
    };
    /**
     * onSavedComment - Gets the comment data and add this to the ticket
     *
     * @param {Ticket}  ticket      Object with the data of a ticket
     * @param {Comment}  newComment Object with the data of a comment
     */
    DetailedTicketComponent.prototype.onSavedComment = function (ticket, newComment) {
        ticket.comment = newComment;
        ticket.updated = this.datePipe.transform(new Date(), 'yyyy-MM-ddTHH:mm:ss');
        this.zendeskService.addNewComment(ticket);
    };
    return DetailedTicketComponent;
}());
DetailedTicketComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_5" /* Component */])({
        selector: 'app-detailed-ticket',
        template: __webpack_require__(705),
        styles: [__webpack_require__(694)]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["c" /* ActivatedRoute */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["c" /* ActivatedRoute */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["b" /* Router */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["b" /* Router */]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_4__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_2__angular_common__["a" /* DatePipe */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__angular_common__["a" /* DatePipe */]) === "function" && _d || Object])
], DetailedTicketComponent);

var _a, _b, _c, _d;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/detailed-ticket.component.js.map

/***/ }),

/***/ 630:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(77);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__ = __webpack_require__(53);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__services_login_service__ = __webpack_require__(103);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return ListTicketsComponent; });
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
/**
 *
 * Component to show thelist of tickets
 * Author : Daniel Contreras Aladro
 * Date : 2017-02-21
 *
 */





var ListTicketsComponent = (function () {
    /**
     * constructor - Constructor of the component
     */
    function ListTicketsComponent(translate, route, router, zendeskService, loginService) {
        this.translate = translate;
        this.route = route;
        this.router = router;
        this.zendeskService = zendeskService;
        this.loginService = loginService;
    }
    /**
     * ngOnInit - OnInit method ofthe component
     */
    ListTicketsComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.sort = {
            column: 'id',
            descending: true
        };
        this.userBool = true;
        this.query = '';
        this.statusFilter = 'any';
        this.user = this.loginService.user;
        if (this.user !== undefined) {
            this.zendeskService.getGenericCustomFields()
                .then(function (responseData) {
                if (responseData !== undefined && responseData !== null) {
                    _this.zendeskService.genericFields = responseData;
                    _this.statusFields = _this.zendeskService.genericFields.filter(function (field) { return field.title_in_portal === 'Status'; })[0].system_field_options;
                }
            });
            this.ticketsUser();
        }
        else {
            this.router.navigate(['/login']);
        }
    };
    /**
     * callbackTickets - Method to store in the internal properties the API response with the tickets
     *
     * @param {Ticket[]}  responseData Array of tickets fromthe API
     */
    ListTicketsComponent.prototype.callbackTickets = function (responseData) {
        if (responseData !== undefined && responseData !== null) {
            responseData.sort(function (a, b) {
                if (a['id'] < b['id'])
                    return -1;
                else if (a['id'] > b['id'])
                    return 1;
                else
                    return 0;
            });
            this.tickets = responseData;
            this.zendeskService.organization = !this.userBool;
            if (this.userBool) {
                this.zendeskService.ticketsUser = this.tickets;
            }
            else {
                this.zendeskService.ticketsOrganization = this.tickets;
            }
        }
        this.query = '';
        this.statusFilter = 'any';
    };
    /**
     * ticketsUser - Method to search the user tickets from the API
     */
    ListTicketsComponent.prototype.ticketsUser = function () {
        var _this = this;
        this.zendeskService.getTicketsUserRequested(this.user.id)
            .then(function (responseData) { return _this.callbackTickets(responseData); }, function (error) {
            _this.tickets = [];
            _this.zendeskService.ticketsUser = _this.tickets;
            _this.zendeskService.ticketsOrganization = _this.tickets;
            _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                _this.errorMessage = res;
            });
        });
    };
    /**
     * ticketsOrganization - Method to search the organization tickets from the API
     */
    ListTicketsComponent.prototype.ticketsOrganization = function () {
        var _this = this;
        this.zendeskService.getTicketsOrganizationRequested(this.user.organization_id)
            .then(function (responseData) { return _this.callbackTickets(responseData); }, function (error) {
            _this.tickets = [];
            _this.zendeskService.ticketsUser = _this.tickets;
            _this.zendeskService.ticketsOrganization = _this.tickets;
            _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                _this.errorMessage = res;
            });
        });
    };
    /**
     * ticketClicked - Method to redirect to the ticket selected detailed data
     *
     * @param {Ticket}  ticket Object with the ticket data
     */
    ListTicketsComponent.prototype.ticketClicked = function (ticket) {
        this.router.navigate(['/detail', ticket.id]);
    };
    /**
     * updateTickets - Method that updates the list between user tickets or organization tickets
     *
     * @param {Boolean}  userBool Boolean value to select
     */
    ListTicketsComponent.prototype.updateTickets = function (userBool) {
        this.userBool = userBool;
        if (this.userBool) {
            this.ticketsUser();
        }
        else {
            this.ticketsOrganization();
        }
    };
    /**
     * filterStatus - Method to filter the list data by a status selected by the user
     */
    ListTicketsComponent.prototype.filterStatus = function () {
        var _this = this;
        if (this.statusFilter !== 'any') {
            if (this.userBool) {
                this.tickets = this.zendeskService.ticketsUser.filter(function (ticket) { return ticket.status === _this.statusFilter; });
            }
            else {
                this.tickets = this.zendeskService.ticketsOrganization.filter(function (ticket) { return ticket.status === _this.statusFilter; });
            }
        }
        else {
            if (this.userBool) {
                this.tickets = this.zendeskService.ticketsUser;
            }
            else {
                this.tickets = this.zendeskService.ticketsOrganization;
            }
        }
    };
    /**
     * filter - Method to filter the list data by a string writed by the user
     */
    ListTicketsComponent.prototype.filter = function () {
        var _this = this;
        if (this.query !== '') {
            if (this.userBool) {
                this.tickets = this.zendeskService.ticketsUser.filter(function (ticket) { return ticket.subject.toLowerCase().indexOf(_this.query.toLowerCase()) >= 0 || ticket.id.toString().indexOf(_this.query) >= 0; });
            }
            else {
                this.tickets = this.zendeskService.ticketsOrganization.filter(function (ticket) { return ticket.subject.toLowerCase().indexOf(_this.query.toLowerCase()) >= 0 || ticket.id.toString().indexOf(_this.query) >= 0; });
            }
        }
        else {
            if (this.userBool) {
                this.tickets = this.zendeskService.ticketsUser;
            }
            else {
                this.tickets = this.zendeskService.ticketsOrganization;
            }
        }
    };
    /**
     * changeSorting - Method to sort data by a column
     *
     * @param {string}  columnName String with the column to sort for
     */
    ListTicketsComponent.prototype.changeSorting = function (columnName) {
        var sort = this.sort;
        if (sort.column == columnName) {
            sort.descending = !sort.descending;
            if (sort.descending) {
                this.tickets.sort(function (a, b) {
                    if (a[columnName] < b[columnName])
                        return 1;
                    else if (a[columnName] > b[columnName])
                        return -1;
                    else
                        return 0;
                });
            }
            else {
                this.tickets.sort(function (a, b) {
                    if (a[columnName] < b[columnName])
                        return -1;
                    else if (a[columnName] > b[columnName])
                        return 1;
                    else
                        return 0;
                });
            }
        }
        else {
            sort.column = columnName;
            sort.descending = false;
            this.tickets.sort(function (a, b) {
                if (a[columnName] < b[columnName])
                    return -1;
                else if (a[columnName] > b[columnName])
                    return 1;
                else
                    return 0;
            });
        }
    };
    return ListTicketsComponent;
}());
ListTicketsComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_5" /* Component */])({
        selector: 'app-list-tickets',
        template: __webpack_require__(706),
        styles: [__webpack_require__(695)]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["c" /* ActivatedRoute */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["c" /* ActivatedRoute */]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["b" /* Router */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["b" /* Router */]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_4__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__services_login_service__["a" /* LoginService */]) === "function" && _e || Object])
], ListTicketsComponent);

var _a, _b, _c, _d, _e;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/list-tickets.component.js.map

/***/ }),

/***/ 631:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(77);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__ = __webpack_require__(53);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__services_login_service__ = __webpack_require__(103);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__services_payara_service__ = __webpack_require__(348);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__classes_user__ = __webpack_require__(227);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return LoginComponent; });
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
/**
 *
 * Component to show the login form
 * Author : Daniel Contreras Aladro
 * Date : 2017-02-21
 *
 */







var LoginComponent = (function () {
    /**
     * constructor - Constructor of the component
     */
    function LoginComponent(translate, router, zendeskService, loginService, payaraService) {
        this.translate = translate;
        this.router = router;
        this.zendeskService = zendeskService;
        this.loginService = loginService;
        this.payaraService = payaraService;
        /**
         * Properties and objects of the component
         */
        this.user = new __WEBPACK_IMPORTED_MODULE_6__classes_user__["a" /* User */]();
    }
    /**
     * ngOnInit - OnInit method ofthe component
     */
    LoginComponent.prototype.ngOnInit = function () {
    };
    /**
     * ngOnDestroy - OnDestroy method of the component
     */
    LoginComponent.prototype.ngOnDestroy = function () {
        this.loginService.user = this.user;
    };
    /**
     * loginToZendesk - Submit method to login a user to the API and redirect to the ticket list
     *
     * @param {User}  user Object with the data of the user to login
     */
    LoginComponent.prototype.loginToZendesk = function (user) {
        var _this = this;
        var regExpEmail = /^[a-z0-9]+(\.[_a-z0-9]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,15})$/i;
        if (user.email !== undefined && user.email !== '' && regExpEmail.test(user.email)) {
            this.loginService.getUserData(user.email)
                .then(function (responseData) {
                if (responseData !== undefined && responseData !== null && responseData.id !== null) {
                    _this.user = responseData;
                    _this.loginService.user = _this.user;
                    _this.zendeskService.username = _this.user.email;
                    _this.payaraService.setStoredEmail(_this.user.email)
                        .then(function (responseData) {
                        if (responseData !== undefined && responseData !== null && responseData.exit_code === "SUCCESS") {
                            _this.router.navigate(['/list']);
                        }
                        else {
                            _this.user = new __WEBPACK_IMPORTED_MODULE_6__classes_user__["a" /* User */]();
                            _this.loginService.user = _this.user;
                            _this.zendeskService.username = '';
                            _this.translate.get('Error! User stored but bad response').subscribe(function (res) {
                                _this.errorMessage = res;
                            });
                        }
                    }, function (error) {
                        _this.user = new __WEBPACK_IMPORTED_MODULE_6__classes_user__["a" /* User */]();
                        _this.loginService.user = _this.user;
                        _this.zendeskService.username = '';
                        _this.translate.get('Error! User not stored').subscribe(function (res) {
                            _this.errorMessage = res;
                        });
                    });
                }
                else {
                    _this.user = new __WEBPACK_IMPORTED_MODULE_6__classes_user__["a" /* User */]();
                    _this.loginService.user = _this.user;
                    _this.zendeskService.username = '';
                    _this.translate.get('Error! User not found').subscribe(function (res) {
                        _this.errorMessage = res;
                    });
                }
            }, function (error) {
                _this.user = new __WEBPACK_IMPORTED_MODULE_6__classes_user__["a" /* User */]();
                _this.loginService.user = _this.user;
                _this.zendeskService.username = null;
                _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                    _this.errorMessage = res;
                });
            });
        }
        else {
            this.translate.get('invalid-email', { value: user.email }).subscribe(function (res) {
                _this.errorMessage = res;
            });
        }
    };
    /**
     * cleanError - Method to clean the error message when a key is pressed on the input box
     *
     * @param {any}  event Object with the event data
     */
    LoginComponent.prototype.cleanError = function (event) {
        this.errorMessage = "";
    };
    return LoginComponent;
}());
LoginComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_5" /* Component */])({
        selector: 'app-login',
        template: __webpack_require__(707),
        styles: [__webpack_require__(696)]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["b" /* Router */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["b" /* Router */]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_4__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__services_login_service__["a" /* LoginService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_5__services_payara_service__["a" /* PayaraService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_5__services_payara_service__["a" /* PayaraService */]) === "function" && _e || Object])
], LoginComponent);

var _a, _b, _c, _d, _e;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/login.component.js.map

/***/ }),

/***/ 632:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(77);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__(44);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_forms__ = __webpack_require__(319);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_rxjs_Rx__ = __webpack_require__(712);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_rxjs_Rx___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_5_rxjs_Rx__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__services_zendesk_service__ = __webpack_require__(53);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__services_login_service__ = __webpack_require__(103);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__classes_ticket__ = __webpack_require__(226);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__classes_user__ = __webpack_require__(227);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return NewTicketComponent; });
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
/**
 *
 * Component to show the ticket form to create new tickets
 * Author : Daniel Contreras Aladro
 * Date : 2017-02-21
 *
 */










var NewTicketComponent = (function () {
    /**
     * constructor - Constructor of the component
     */
    function NewTicketComponent(translate, fb, router, zendeskService, loginService, datePipe) {
        this.translate = translate;
        this.fb = fb;
        this.router = router;
        this.zendeskService = zendeskService;
        this.loginService = loginService;
        this.datePipe = datePipe;
        this.user = new __WEBPACK_IMPORTED_MODULE_9__classes_user__["a" /* User */]();
        this.newTicket = new __WEBPACK_IMPORTED_MODULE_8__classes_ticket__["a" /* Ticket */]();
    }
    /**
     * ngOnInit - OnInit method of the component
     */
    NewTicketComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.initTicket();
        this.user = this.loginService.user;
        this.genericFields = this.zendeskService.genericFields;
        this.genericFields.forEach(function (field) {
            _this.zendeskService.getCustomField(field.id)
                .then(function (responseData) {
                if (responseData !== undefined && responseData !== null) {
                    field.title_in_portal = responseData.title_in_portal;
                    field.custom_field_options = responseData.custom_field_options;
                    field.system_field_options = responseData.system_field_options;
                }
            }, function (error) {
                _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                    _this.errorMessage = res;
                });
            });
        });
        this.newAttachments = [];
    };
    /**
     * ngOnDestroy - OnDestroy method of the component
     */
    NewTicketComponent.prototype.ngOnDestroy = function () {
        this.initTicket();
        this.errorMessage = "";
        this.newAttachments = [];
    };
    /**
     * initTicket - Method to initiate the new ticket data
     */
    NewTicketComponent.prototype.initTicket = function () {
        this.ticketForm = this.fb.group({
            subject: ['', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["c" /* Validators */].required]],
            description: ['', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["c" /* Validators */].required]],
            type: ['problem', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["c" /* Validators */].required]],
            environment: ['prod', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["c" /* Validators */].required]],
            priority: ['normal', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["c" /* Validators */].required]],
            version: ['', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["c" /* Validators */].required]]
        });
    };
    /**
     * discardChanges - Method to discard the new ticket data and return to the list of tickets
     */
    NewTicketComponent.prototype.discardChanges = function () {
        this.router.navigate(['/list']);
    };
    /**
     * onSavedAttachment - Method to add new files to a files array
     *
     * @param {Attachment}  newAttachment Object with the new file attached data
     */
    NewTicketComponent.prototype.onSavedAttachment = function (newAttachment) {
        this.newAttachments.push(newAttachment);
    };
    /**
     * checkData - Submit method to check the data and send to the API
     *
     * @param {FormGroup}  form Angular form group with the data filled in the form
     */
    NewTicketComponent.prototype.checkData = function (form) {
        var _this = this;
        if (form.value) {
            var ticketData = form.value;
            ticketData.submiter_id = this.loginService.user.id;
            ticketData.comment = new Comment();
            ticketData.comment.body = ticketData.description;
            if (this.newAttachments.length > 0) {
                ticketData.comment.uploads = [];
                ticketData.comment.uploads.push(this.newAttachments[0].token);
                ticketData.comment.attachments = [];
                ticketData.comment.attachments.push(this.newAttachments[0].attachment);
            }
            ticketData.comment.created_at = this.datePipe.transform(new Date(), 'yyyy-MM-ddTHH:mm:ss');
            this.zendeskService.createNewTicket(ticketData)
                .then(function (responseData) {
                if (responseData !== undefined && responseData !== null) {
                    _this.zendeskService.ticketsUser.push(responseData);
                    _this.router.navigate(['/list']);
                }
            }, function (error) {
                _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                    _this.errorMessage = res;
                });
            });
        }
        else {
            this.successMessage = "";
            this.translate.get('Please fill the data of the form before continue.').subscribe(function (res) {
                _this.errorMessage = res;
            });
            var timer = __WEBPACK_IMPORTED_MODULE_5_rxjs_Rx__["Observable"].timer(5000, 1000);
            timer.subscribe(function (t) { return _this.errorMessage = ""; });
        }
    };
    return NewTicketComponent;
}());
NewTicketComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_5" /* Component */])({
        selector: 'app-new-ticket',
        template: __webpack_require__(708),
        styles: [__webpack_require__(697)]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_3__angular_forms__["d" /* FormBuilder */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_3__angular_forms__["d" /* FormBuilder */]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["b" /* Router */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["b" /* Router */]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_6__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_6__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_7__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_7__services_login_service__["a" /* LoginService */]) === "function" && _e || Object, typeof (_f = typeof __WEBPACK_IMPORTED_MODULE_2__angular_common__["a" /* DatePipe */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__angular_common__["a" /* DatePipe */]) === "function" && _f || Object])
], NewTicketComponent);

var _a, _b, _c, _d, _e, _f;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/new-ticket.component.js.map

/***/ }),

/***/ 633:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__ngx_translate_core__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__services_zendesk_service__ = __webpack_require__(53);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__services_login_service__ = __webpack_require__(103);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__classes_ticket__ = __webpack_require__(226);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return TicketDataComponent; });
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
/**
 *
 * Component to show the properties of a ticket
 * Author : Daniel Contreras Aladro
 * Date : 2017-02-21
 *
 */





var TicketDataComponent = (function () {
    /**
     * constructor - Constructor of the component
     */
    function TicketDataComponent(translate, zendeskService, loginService) {
        this.translate = translate;
        this.zendeskService = zendeskService;
        this.loginService = loginService;
    }
    /**
     * ngOnInit - OnInit method ofthe component
     */
    TicketDataComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.ticket.submitter_name = this.loginService.user.name;
        this.ticket.custom_fields.forEach(function (custom_field) {
            _this.zendeskService.getCustomField(custom_field.id)
                .then(function (responseData) {
                if (responseData !== undefined && responseData !== null) {
                    custom_field.title_in_portal = responseData.title_in_portal;
                    custom_field.custom_field_options = responseData.custom_field_options;
                    custom_field.system_field_options = responseData.system_field_options;
                }
            }, function (error) {
                _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                    _this.errorMessage = res;
                });
            });
        });
    };
    /**
     * getValue - Method to return a readable value of a field
     *
     * @param {any}  fieldData Object with the custom field data
     *
     * @return {string}  Returns a string to show in the screen to the user
     */
    TicketDataComponent.prototype.getValue = function (fieldData) {
        if (fieldData.custom_field_options) {
            var valueData = fieldData.custom_field_options.filter(function (field) { return field.value === fieldData.value; });
            return valueData[0] !== undefined ? valueData[0].name : "Not specified";
        }
        else if (fieldData.system_field_options) {
            var valueData = fieldData.system_field_options.filter(function (field) { return field.value === fieldData.value; });
            return valueData[0] !== undefined ? valueData[0].name : "Not specified";
        }
        else {
            var valueReturn = fieldData.value !== null ? fieldData.value : "Not specified";
            if (typeof (valueReturn) === "boolean") {
                if (valueReturn) {
                    valueReturn = 'YES';
                }
                else {
                    valueReturn = 'NO';
                }
            }
            return valueReturn;
        }
    };
    return TicketDataComponent;
}());
__decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["l" /* Input */])(),
    __metadata("design:type", typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_4__classes_ticket__["a" /* Ticket */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__classes_ticket__["a" /* Ticket */]) === "function" && _a || Object)
], TicketDataComponent.prototype, "ticket", void 0);
TicketDataComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["_5" /* Component */])({
        selector: 'app-ticket-data',
        template: __webpack_require__(709),
        styles: [__webpack_require__(698)]
    }),
    __metadata("design:paramtypes", [typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__ngx_translate_core__["c" /* TranslateService */]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_2__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_3__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_3__services_login_service__["a" /* LoginService */]) === "function" && _d || Object])
], TicketDataComponent);

var _a, _b, _c, _d;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/ticket-data.component.js.map

/***/ }),

/***/ 634:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_moment__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_moment___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1_moment__);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return DayTimePipe; });
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
/**
 *
 * Pipe to show dates in a specific format
 * Author : Daniel Contreras Aladro
 * Date : 2017-02-21
 *
 */


var DayTimePipe = (function () {
    function DayTimePipe() {
    }
    DayTimePipe.prototype.transform = function (value, args) {
        if (value) {
            var d = __WEBPACK_IMPORTED_MODULE_1_moment__(new Date(value));
            var now = __WEBPACK_IMPORTED_MODULE_1_moment__(new Date());
            var std = d.format('DD/MM/YYYY HH:mm');
            var oneMonthAgo = __WEBPACK_IMPORTED_MODULE_1_moment__(new Date()).subtract(1, 'month');
            if (d.isBefore(oneMonthAgo)) {
                std = d.fromNow();
            }
            else {
                var oneWeekAgo = __WEBPACK_IMPORTED_MODULE_1_moment__(new Date()).subtract(1, 'week');
                if (d.isBefore(oneWeekAgo)) {
                    std = d.format('D MMMM');
                }
                else {
                    var oneDayAgo = __WEBPACK_IMPORTED_MODULE_1_moment__(new Date()).subtract(1, 'day');
                    if (d.isBefore(oneDayAgo)) {
                        std = d.format('dddd HH:mm');
                    }
                    else {
                        if (d.isSame(now, 'day')) {
                            var oneHourAgo = __WEBPACK_IMPORTED_MODULE_1_moment__(new Date()).subtract(1, 'hour');
                            if (d.isBefore(oneHourAgo)) {
                                std = d.format('HH:mm');
                            }
                            else {
                                std = d.fromNow();
                            }
                        }
                    }
                }
            }
            return std;
        }
    };
    return DayTimePipe;
}());
DayTimePipe = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["d" /* Pipe */])({
        name: 'dayTime'
    })
], DayTimePipe);

//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/day-time.pipe.js.map

/***/ }),

/***/ 691:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(28)();
// imports


// module
exports.push([module.i, ".logout {\n  margin-top: -1.75rem;\n}\n\n.appTittle {\n  margin-left: 1.5rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 692:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(28)();
// imports


// module
exports.push([module.i, "span.glyphicon.glyphicon-paperclip{\n  background-color: #E6E6E6;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 693:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(28)();
// imports


// module
exports.push([module.i, ".new_comment{\n  resize: none;\n  outline: none;\n  width: 100%;\n  padding: 10px;\n  border: none;\n  height: 100%;\n  border-radius: 5px;\n  background-color: #ffffff;\n  margin-bottom: .5rem;\n}\n\n.comment_box{\n  background-color: #ffffff;\n  margin-bottom: .25rem;\n  padding: .25rem;\n}\n\npre code {\n    padding: 0;\n    font-size: .75rem;\n    margin-left: -1.5rem;\n}\n\n.addFile{\n  margin:.5rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 694:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(28)();
// imports


// module
exports.push([module.i, ".returnBack {\n  margin-top: -.5rem;\n}\n\nbutton.btn.btn-sm.pull-right.returnBack{\n  max-width: 2rem;\n  max-height: 1.75rem\n}\n\nspan.glyphicon.glyphicon-chevron-left{\n  background-color: transparent;\n}\n\nspan.glyphicon.glyphicon-chevron-left::before{\n    margin-left: -.15rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 695:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(28)();
// imports


// module
exports.push([module.i, ".addTicket {\n  margin-top: -.5rem;\n}\n\nbutton.btn.btn-sm.pull-right.addTicket{\n  max-width: 2rem;\n  max-height: 1.75rem\n}\n\nspan.glyphicon.glyphicon-chevron-up,\nspan.glyphicon.glyphicon-chevron-down,\nspan.glyphicon.glyphicon-plus{\n  background-color: transparent;\n}\n\nspan.glyphicon.glyphicon-plus::before{\n  margin-left: -.15rem;\n}\n\ntable.table-responsive.table-striped.table-md.table-inverse.table-sortable{\n  margin-top:3rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 696:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(28)();
// imports


// module
exports.push([module.i, "button.btn.btn-sm.btn-primary.btn-block{\n  margin-top:.75rem;\n}\n\n#inputEmail{\n  margin-top:.5rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 697:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(28)();
// imports


// module
exports.push([module.i, ".form-group.required .control-label:after {\n    color: #d00;\n    content: \"*\";\n    margin-left: .5rem;\n}\n\n.form-group .control-label,\n.form-group.required .control-label {\n  margin-bottom:.25rem;\n}\n\n.discardChanges {\n  margin-top: -.5rem;\n}\n\nbutton.btn.btn-sm.pull-right.discardChanges{\n  max-width: 2rem;\n  max-height: 1.75rem\n}\n\nspan.glyphicon.glyphicon-remove-circle{\n  background-color: transparent;\n}\n\nspan.glyphicon.glyphicon-remove-circle::before{\n    margin-left: -.15rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 698:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(28)();
// imports


// module
exports.push([module.i, ".detailContent {\n  border-left: thin solid #f0981b;\n  margin-left:1.5rem;\n  margin-top:1rem;\n}\n\n.detailData{\n  margin:.5rem;\n  padding: .25rem;\n  letter-spacing: .05rem;\n  display:-webkit-box;\n  display:-ms-flexbox;\n  display:flex;\n}\n\n.titleData{\n  margin-top:.25rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 700:
/***/ (function(module, exports, __webpack_require__) {

var map = {
	"./af": 364,
	"./af.js": 364,
	"./ar": 370,
	"./ar-dz": 365,
	"./ar-dz.js": 365,
	"./ar-ly": 366,
	"./ar-ly.js": 366,
	"./ar-ma": 367,
	"./ar-ma.js": 367,
	"./ar-sa": 368,
	"./ar-sa.js": 368,
	"./ar-tn": 369,
	"./ar-tn.js": 369,
	"./ar.js": 370,
	"./az": 371,
	"./az.js": 371,
	"./be": 372,
	"./be.js": 372,
	"./bg": 373,
	"./bg.js": 373,
	"./bn": 374,
	"./bn.js": 374,
	"./bo": 375,
	"./bo.js": 375,
	"./br": 376,
	"./br.js": 376,
	"./bs": 377,
	"./bs.js": 377,
	"./ca": 378,
	"./ca.js": 378,
	"./cs": 379,
	"./cs.js": 379,
	"./cv": 380,
	"./cv.js": 380,
	"./cy": 381,
	"./cy.js": 381,
	"./da": 382,
	"./da.js": 382,
	"./de": 384,
	"./de-at": 383,
	"./de-at.js": 383,
	"./de.js": 384,
	"./dv": 385,
	"./dv.js": 385,
	"./el": 386,
	"./el.js": 386,
	"./en-au": 387,
	"./en-au.js": 387,
	"./en-ca": 388,
	"./en-ca.js": 388,
	"./en-gb": 389,
	"./en-gb.js": 389,
	"./en-ie": 390,
	"./en-ie.js": 390,
	"./en-nz": 391,
	"./en-nz.js": 391,
	"./eo": 392,
	"./eo.js": 392,
	"./es": 394,
	"./es-do": 393,
	"./es-do.js": 393,
	"./es.js": 394,
	"./et": 395,
	"./et.js": 395,
	"./eu": 396,
	"./eu.js": 396,
	"./fa": 397,
	"./fa.js": 397,
	"./fi": 398,
	"./fi.js": 398,
	"./fo": 399,
	"./fo.js": 399,
	"./fr": 402,
	"./fr-ca": 400,
	"./fr-ca.js": 400,
	"./fr-ch": 401,
	"./fr-ch.js": 401,
	"./fr.js": 402,
	"./fy": 403,
	"./fy.js": 403,
	"./gd": 404,
	"./gd.js": 404,
	"./gl": 405,
	"./gl.js": 405,
	"./he": 406,
	"./he.js": 406,
	"./hi": 407,
	"./hi.js": 407,
	"./hr": 408,
	"./hr.js": 408,
	"./hu": 409,
	"./hu.js": 409,
	"./hy-am": 410,
	"./hy-am.js": 410,
	"./id": 411,
	"./id.js": 411,
	"./is": 412,
	"./is.js": 412,
	"./it": 413,
	"./it.js": 413,
	"./ja": 414,
	"./ja.js": 414,
	"./jv": 415,
	"./jv.js": 415,
	"./ka": 416,
	"./ka.js": 416,
	"./kk": 417,
	"./kk.js": 417,
	"./km": 418,
	"./km.js": 418,
	"./ko": 419,
	"./ko.js": 419,
	"./ky": 420,
	"./ky.js": 420,
	"./lb": 421,
	"./lb.js": 421,
	"./lo": 422,
	"./lo.js": 422,
	"./lt": 423,
	"./lt.js": 423,
	"./lv": 424,
	"./lv.js": 424,
	"./me": 425,
	"./me.js": 425,
	"./mi": 426,
	"./mi.js": 426,
	"./mk": 427,
	"./mk.js": 427,
	"./ml": 428,
	"./ml.js": 428,
	"./mr": 429,
	"./mr.js": 429,
	"./ms": 431,
	"./ms-my": 430,
	"./ms-my.js": 430,
	"./ms.js": 431,
	"./my": 432,
	"./my.js": 432,
	"./nb": 433,
	"./nb.js": 433,
	"./ne": 434,
	"./ne.js": 434,
	"./nl": 436,
	"./nl-be": 435,
	"./nl-be.js": 435,
	"./nl.js": 436,
	"./nn": 437,
	"./nn.js": 437,
	"./pa-in": 438,
	"./pa-in.js": 438,
	"./pl": 439,
	"./pl.js": 439,
	"./pt": 441,
	"./pt-br": 440,
	"./pt-br.js": 440,
	"./pt.js": 441,
	"./ro": 442,
	"./ro.js": 442,
	"./ru": 443,
	"./ru.js": 443,
	"./se": 444,
	"./se.js": 444,
	"./si": 445,
	"./si.js": 445,
	"./sk": 446,
	"./sk.js": 446,
	"./sl": 447,
	"./sl.js": 447,
	"./sq": 448,
	"./sq.js": 448,
	"./sr": 450,
	"./sr-cyrl": 449,
	"./sr-cyrl.js": 449,
	"./sr.js": 450,
	"./ss": 451,
	"./ss.js": 451,
	"./sv": 452,
	"./sv.js": 452,
	"./sw": 453,
	"./sw.js": 453,
	"./ta": 454,
	"./ta.js": 454,
	"./te": 455,
	"./te.js": 455,
	"./tet": 456,
	"./tet.js": 456,
	"./th": 457,
	"./th.js": 457,
	"./tl-ph": 458,
	"./tl-ph.js": 458,
	"./tlh": 459,
	"./tlh.js": 459,
	"./tr": 460,
	"./tr.js": 460,
	"./tzl": 461,
	"./tzl.js": 461,
	"./tzm": 463,
	"./tzm-latn": 462,
	"./tzm-latn.js": 462,
	"./tzm.js": 463,
	"./uk": 464,
	"./uk.js": 464,
	"./uz": 465,
	"./uz.js": 465,
	"./vi": 466,
	"./vi.js": 466,
	"./x-pseudo": 467,
	"./x-pseudo.js": 467,
	"./yo": 468,
	"./yo.js": 468,
	"./zh-cn": 469,
	"./zh-cn.js": 469,
	"./zh-hk": 470,
	"./zh-hk.js": 470,
	"./zh-tw": 471,
	"./zh-tw.js": 471
};
function webpackContext(req) {
	return __webpack_require__(webpackContextResolve(req));
};
function webpackContextResolve(req) {
	var id = map[req];
	if(!(id + 1)) // check for number
		throw new Error("Cannot find module '" + req + "'.");
	return id;
};
webpackContext.keys = function webpackContextKeys() {
	return Object.keys(map);
};
webpackContext.resolve = webpackContextResolve;
module.exports = webpackContext;
webpackContext.id = 700;


/***/ }),

/***/ 702:
/***/ (function(module, exports) {

module.exports = "<hr>\n\n<h2>\n  <p class=\"appTittle\">{{'Payara Support' | translate}}</p>\n  <button class=\"btn btn-default pull-right logout\" *ngIf=\"!isCurrentRoute('login')\" (click)=\"logout()\">\n    <span class=\"glyphicon glyphicon-off\" aria-hidden=\"true\">\n    </span>\n  </button>\n</h2>\n\n<hr>\n\n<router-outlet></router-outlet>\n"

/***/ }),

/***/ 703:
/***/ (function(module, exports) {

module.exports = "<label class=\"btn btn-sm btn-file btn-block\">\n    <span class=\"glyphicon glyphicon-paperclip\" aria-hidden=\"true\"></span>\n    {{'Add file' | translate}}\n    <input type=\"file\" class=\"hidden\" (change)=\"onChange($event)\">\n</label>\n"

/***/ }),

/***/ 704:
/***/ (function(module, exports) {

module.exports = "<div class=\"commentContainer\" *ngFor=\"let comment of comments\">\n  <div class=\"row\">\n    <div class=\"col-md-9\">\n      <pre class=\"comment_box\">\n        <code>{{comment.body}}</code>\n      </pre>\n    </div>\n    <div class=\"col-md-3\">\n      <p>{{comment.created_at | dayTime}}</p>\n    </div>\n  </div>\n  <div class=\"row\" *ngIf=\"comment.attachments !== undefined && comment.attachments.length>0\">\n    <div class=\"col-md-12\" *ngFor=\"let file of comment.attachments\">\n      <a href=\"{{file.url}}\">\n        <span class=\"glyphicon glyphicon-file\" aria-hidden=\"true\">\n          {{file.file_name}}\n        </span>\n      </a>\n    </div>\n  </div>\n  <hr>\n</div>\n\n<div class=\"newCommentContainer\" *ngIf=\"ticket.status!=='closed' && ticket.status!=='solved'\">\n  <div class=\"row\">\n    <div class=\"col-md-8\">\n      <textarea class=\"new_comment\" [(ngModel)]=\"newCommentText\" (keyup)=\"keyUpEvent($event)\"></textarea>\n    </div>\n    <div class=\"col-md-3\">\n      <!--<app-add-file class=\"addFile\" (saved)=\"onSavedAttachment($event)\"></app-add-file>-->\n    </div>\n    <div class=\"col-md-1\">\n      <button class=\"btn btn-sm btn-default btn-block\" type=\"submit\" (click)=\"saveComment()\">\n        <span class=\"glyphicon glyphicon-ok\" aria-hidden=\"true\">\n        </span>\n      </button>\n    </div>\n  </div>\n</div>\n\n<div class=\"row\">\n  <div *ngIf=\"errorMessage\" class=\"alert alert-warning\" role=\"alert\">{{errorMessage}}</div>\n</div>\n"

/***/ }),

/***/ 705:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\">\n  <div class=\"panel-heading\">\n    <h4>\n      {{'Request' | translate}} #{{ticket.id}} <strong>{{ticket.subject}}</strong>\n      <button class=\"btn btn-sm pull-right returnBack\" routerLink=\"/list\">\n        <span class=\"glyphicon glyphicon-chevron-left\" aria-hidden=\"true\">\n        </span>\n      </button>\n    </h4>\n  </div>\n  <div class=\"panel-body\">\n    <div class=\"row\">\n      <div class=\"col-sm-8\">\n        <app-comment-data [(ticket)]=\"ticket\" (saved)=\"onSavedComment(ticket,$event)\"></app-comment-data>\n      </div>\n      <div class=\"col-sm-4\">\n        <app-ticket-data [(ticket)]=\"ticket\"></app-ticket-data>\n      </div>\n  </div>\n</div>\n"

/***/ }),

/***/ 706:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\" *ngIf=\"tickets\">\n  <div class=\"panel-heading\">\n    <h4>\n      {{'My requests' | translate}}\n      <button class=\"btn btn-sm pull-right addTicket\" routerLink=\"/new\">\n        <span class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\">\n        </span>\n      </button>\n    </h4>\n  </div>\n  <div class=\"panel-body\">\n      <div class=\"col-sm-6\">\n        <input type=\"text\" class=\"form-control\" [(ngModel)]=\"query\" (keyup)=\"filter()\" placeholder=\"{{'Filter' | translate}}\" />\n      </div>\n      <div class=\"col-sm-4\">\n        <div class=\"btn-group pull-right\" data-toggle=\"buttons\">\n          <label class=\"btn btn-primary\" [ngClass]=\"{active: userBool}\">\n            <input type=\"radio\" autocomplete=\"off\" (click)=\"updateTickets(true)\">{{'User' | translate}}\n          </label>\n          <label class=\"btn btn-primary\" [ngClass]=\"{active: !userBool}\">\n            <input type=\"radio\" autocomplete=\"off\" (click)=\"updateTickets(false)\">{{'Organization' | translate}}\n          </label>\n        </div>\n      </div>\n      <div class=\"col-sm-2 pull-right\">\n       <select class=\"form-control\" id=\"statusFilter\" [(ngModel)]=\"statusFilter\" (change)=\"filterStatus()\">\n         <option value=\"any\">{{'Any' | translate}}</option>\n         <option *ngFor=\"let statusOption of statusFields\" value=\"{{statusOption.value}}\">{{statusOption.name | translate}}</option>\n       </select>\n     </div>\n    <table class=\"table table-responsive table-striped table-md table-inverse table-sortable\">\n      <thead>\n        <tr>\n          <th (click)=\"changeSorting('id')\">Id\n            <span *ngIf=\"sort.column === 'id' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'id' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th (click)=\"changeSorting('subject')\">{{'Subject' | translate}}\n            <span *ngIf=\"sort.column === 'subject' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'subject' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th (click)=\"changeSorting('created_at')\">{{'Created' | translate}}\n            <span *ngIf=\"sort.column === 'created_at' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'created_at' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th (click)=\"changeSorting('updated_at')\">{{'Last activity' | translate}}\n            <span *ngIf=\"sort.column === 'updated_at' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'updated_at' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th (click)=\"changeSorting('status')\">{{'Status' | translate}}\n            <span *ngIf=\"sort.column === 'status' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'status' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n        </tr>\n      </thead>\n      <tbody>\n        <tr *ngFor=\"let ticket of tickets\" (click)=\"ticketClicked(ticket)\">\n          <th scope=\"row\">{{ticket.id}}</th>\n          <td>{{ticket.subject}}</td>\n          <td>{{ticket.created_at | dayTime}}</td>\n          <td>{{ticket.updated_at | dayTime}}</td>\n          <td style=\"text-align: center;\">\n              <span\n              [ngClass]=\"{\n                              'ticketOpen': ticket.status==='open',\n                              'ticketNew': ticket.status==='new',\n                              'ticketClosed': ticket.status==='closed',\n                              'ticketSolved': ticket.status==='solved',\n                              'ticketPending': ticket.status==='pending',\n                              'ticketHold': ticket.status==='hold'}\"\n              class=\"glyphicon glyphicon-flag\" aria-hidden=\"true\"> {{ticket.status | translate}}</span>\n          </td>\n        </tr>\n      </tbody>\n    </table>\n  </div>\n</div>\n\n<div *ngIf=\"!tickets && errorMessage\" class=\"alert alert-danger\" role=\"alert\">{{errorMessage}}</div>\n"

/***/ }),

/***/ 707:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\">\n  <div class=\"panel-heading\">{{'Login' | translate}}</div>\n  <div class=\"panel-body\">\n    <form class=\"form-signin\" id=\"login\">\n      <h4 class=\"form-signin-heading\">{{'Please sign in' | translate}}</h4>\n      <label for=\"inputEmail\" class=\"sr-only\">{{'Email address' | translate}}</label>\n      <input type=\"email\" id=\"inputEmail\" class=\"form-control\" placeholder=\"{{'Email address' | translate}}\" required autofocus [(ngModel)]=\"user.email\" name=\"email\" (keypress)=\"cleanError($event)\">\n      <button class=\"btn btn-sm btn-primary btn-block\" type=\"submit\" [disabled]=\"!user.email\" (click)=\"loginToZendesk(user)\">{{'Sign in' | translate}}</button>\n    </form>\n  </div>\n</div>\n\n<div *ngIf=\"errorMessage\" class=\"alert alert-danger\" role=\"alert\">{{errorMessage}}</div>\n"

/***/ }),

/***/ 708:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\">\n  <div *ngIf=\"errorMessage\" class=\"alert alert-danger alert-dismissible\" role=\"alert1\">\n    {{errorMessage}}\n  </div>\n  <div *ngIf=\"successMessage\" class=\"alert alert-success alert-dismissible\" role=\"alert2\">\n    {{successMessage}}\n  </div>\n  <div class=\"panel-heading\">\n    <h4>\n      {{'Submit a request' | translate}}\n      <button class=\"btn btn-sm pull-right discardChanges\" (click)=\"discardChanges()\">\n        <span class=\"glyphicon glyphicon-remove-circle\" aria-hidden=\"true\">\n        </span>\n      </button>\n    </h4>\n  </div>\n  <div class=\"panel-body\">\n    <form class=\"form-ticket form-vertical\" [formGroup]=\"ticketForm\">\n      <div class=\"form-group required\">\n         <label class=\"col-md-2 control-label\">{{genericFields[0].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <input class=\"form-control\" id=\"subject\" formControlName=\"subject\" required type=\"text\"/>\n           <small *ngIf=\"!ticketForm.controls.subject.valid &&\n                         (ticketForm.controls.subject.dirty ||\n                         ticketForm.controls.subject.touched)\"\n                        class=\"badge text-warning\">\n             {{genericFields[0].title_in_portal | translate}} {{'is required' | translate}}\n           </small>\n         </div>\n      </div>\n      <div class=\"form-group required\">\n         <label class=\"col-md-2 control-label\">{{genericFields[1].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <textarea class=\"form-control\" rows=\"5\" id=\"description\" formControlName=\"description\"></textarea>\n           <small *ngIf=\"!ticketForm.controls.description.valid &&\n                         (ticketForm.controls.description.dirty ||\n                         ticketForm.controls.description.touched)\"\n                        class=\"badge text-warning\">\n             {{genericFields[0].title_in_portal | translate}} {{'is required' | translate}}\n           </small>\n         </div>\n      </div>\n      <div class=\"form-group\">\n         <label class=\"col-md-2 control-label\">{{genericFields[3].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <select class=\"form-control\" id=\"type\" formControlName=\"type\">\n             <option *ngFor=\"let typeOption of genericFields[3].system_field_options\" value=\"{{typeOption.value}}\">{{typeOption.name | translate}}</option>\n           </select>\n        </div>\n      </div>\n      <div class=\"form-group required\">\n         <label class=\"col-md-2 control-label\">{{genericFields[4].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <select class=\"form-control\" id=\"environment\" formControlName=\"environment\" required>\n              <option *ngFor=\"let environmentOption of genericFields[4].custom_field_options\" value=\"{{environmentOption.value}}\">{{environmentOption.name | translate}}</option>\n           </select>\n        </div>\n      </div>\n      <div class=\"form-group required\">\n         <label class=\"col-md-2 control-label\">{{genericFields[5].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <select class=\"form-control\" id=\"priority\" formControlName=\"priority\" required>\n              <option *ngFor=\"let priorityOption of genericFields[5].system_field_options\" value=\"{{priorityOption.value}}\">{{priorityOption.name | translate}}</option>\n           </select>\n        </div>\n      </div>\n      <div class=\"form-group required\">\n         <label class=\"col-md-2 control-label\">{{genericFields[15].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <input class=\"form-control\" id=\"version\" formControlName=\"version\" required type=\"text\"/>\n           <small *ngIf=\"!ticketForm.controls.version.valid &&\n                         (ticketForm.controls.version.dirty ||\n                         ticketForm.controls.version.touched)\"\n                        class=\"badge text-warning\">\n             {{genericFields[15].title_in_portal | translate}} {{'is required' | translate}}\n           </small>\n         </div>\n      </div>\n      <!--\n      <div class=\"form-group\">\n         <label class=\"col-md-2 control-label\">{{'Attachments' | translate}}</label>\n         <div class=\"col-md-4\">\n           <app-add-file (saved)=\"onSavedAttachment($event)\"></app-add-file>\n         </div>\n      </div>\n    -->\n      <button class=\"btn btn-sm btn-primary btn-block\" type=\"submit\" [disabled]=\"!ticketForm.valid\" (click)=\"checkData(ticketForm)\">{{'Submit' | translate}}</button>\n    </form>\n  </div>\n</div>\n"

/***/ }),

/***/ 709:
/***/ (function(module, exports) {

module.exports = "  <div class=\"col-md-11 detailContent\">\n    <h5>\n      <strong>{{ticket.submitter_name}}</strong> {{'submitted this request' | translate}}\n    </h5>\n    <hr>\n    <p class=\"detailData\">\n      <strong>{{'Status' | translate}}</strong>\n      &nbsp;\n      <span\n      [ngClass]=\"{\n                      'ticketOpen': ticket.status==='open',\n                      'ticketNew': ticket.status==='new',\n                      'ticketClosed': ticket.status==='closed',\n                      'ticketSolved': ticket.status==='solved',\n                      'ticketPending': ticket.status==='pending',\n                      'ticketHold': ticket.status==='hold'}\"\n      class=\"glyphicon glyphicon-flag\" aria-hidden=\"true\"> {{ticket.status | translate}}</span>\n    </p>\n    <p class=\"detailData\">\n      <strong>{{'Type' | translate}}</strong>\n      &nbsp;\n      {{ticket.type | translate}}\n    </p>\n    <p class=\"detailData\">\n      <strong>{{'Priority' | translate}}</strong>\n      &nbsp;\n      {{ticket.priority | translate}}\n    </p>\n    <div *ngFor=\"let field of ticket.custom_fields\">\n      <div class=\"detailData\">\n        <strong>{{field.title_in_portal | translate}}</strong>\n        &nbsp;\n        <p>{{getValue(field) | translate}}</p>\n      </div>\n    </div>\n  </div>\n"

/***/ }),

/***/ 973:
/***/ (function(module, exports, __webpack_require__) {

module.exports = __webpack_require__(507);


/***/ })

},[973]);
//# sourceMappingURL=main.bundle.js.map
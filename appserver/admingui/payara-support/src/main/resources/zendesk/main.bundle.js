webpackJsonp([1,5],{

/***/ 1002:
/***/ (function(module, exports, __webpack_require__) {

module.exports = __webpack_require__(520);


/***/ }),

/***/ 158:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return environment; });
var environment = {
    production: true,
    zendesk: {
        baseUrl: 'https://api.payara.fish/api/zendesk/',
        filesUrl: 'https://payara.zendesk.com/api/v2/'
    },
    payara: {
        baseUrl: '/management/domain/',
        shopUrl: 'http://www.payara.fish/support'
    }
};
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/environment.js.map

/***/ }),

/***/ 230:
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

/***/ 231:
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

/***/ 232:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_http__ = __webpack_require__(152);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_toPromise__ = __webpack_require__(166);
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
            return this.http.get(this.connectionData.payaraURL + 'configs/config/server-config/zendesk-support-configuration/get-zendesk-support-configuration.json', { headers: this.headers })
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
        return this.http.post(this.connectionData.payaraURL + 'configs/config/server-config/zendesk-support-configuration/set-zendesk-support-configuration', JSON.stringify({ emailAddress: email }), { headers: this.headers })
            .toPromise()
            .then(function (response) { return response.json(); });
    };
    /**
     * getFile - Method to call to the API to get Payara server files
     *
     * @param {string}  url String with the url to call
     *
     * @return {Promise<any>} Returns the response promise
     */
    PayaraService.prototype.getFile = function (url) {
        this.headers = new __WEBPACK_IMPORTED_MODULE_1__angular_http__["c" /* Headers */]();
        this.headers.append('Content-Type', 'application/json');
        return this.http.get(this.connectionData.payaraURL + url, this.headers)
            .toPromise()
            .then(function (response) { return response; });
    };
    /**
     * getServerInstances - Method to call to the API to get Payara server instances
     *
     * @return {Promise<any>} Returns the response promise
     */
    PayaraService.prototype.getServerInstances = function () {
        this.headers = new __WEBPACK_IMPORTED_MODULE_1__angular_http__["c" /* Headers */]();
        this.headers.append('Content-Type', 'application/json');
        return this.http.get(this.connectionData.payaraURL + 'servers/server.json', this.headers)
            .toPromise()
            .then(function (response) { return response.json().extraProperties.childResources; });
    };
    return PayaraService;
}());
PayaraService = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])(),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_1__angular_http__["b" /* Http */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_http__["b" /* Http */]) === "function" && _a || Object])
], PayaraService);

var _a;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/payara.service.js.map

/***/ }),

/***/ 353:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__environments_environment__ = __webpack_require__(158);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_tinymce_tinymce__ = __webpack_require__(996);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_tinymce_tinymce___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_tinymce_tinymce__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_tinymce_themes_modern_theme__ = __webpack_require__(995);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_tinymce_themes_modern_theme___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_tinymce_themes_modern_theme__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_tinymce_plugins_paste_plugin__ = __webpack_require__(993);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_tinymce_plugins_paste_plugin___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_4_tinymce_plugins_paste_plugin__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_tinymce_plugins_searchreplace_plugin__ = __webpack_require__(994);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_tinymce_plugins_searchreplace_plugin___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_5_tinymce_plugins_searchreplace_plugin__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__ngx_translate_core__ = __webpack_require__(55);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7_jquery_dist_jquery__ = __webpack_require__(58);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_7_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__services_zendesk_service__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__services_payara_service__ = __webpack_require__(232);
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




// Plugins






var AddFileComponent = (function () {
    /**
     * constructor - Constructor of the component
     */
    function AddFileComponent(translate, zendeskService, payaraService) {
        this.translate = translate;
        this.zendeskService = zendeskService;
        this.payaraService = payaraService;
        /**
         * Properties and objects of the component
         */
        this.environment = __WEBPACK_IMPORTED_MODULE_1__environments_environment__["a" /* environment */];
        this.tinymce = __WEBPACK_IMPORTED_MODULE_2_tinymce_tinymce___default.a;
        this.staticFileButtons = [
            {
                title: 'Monitoring Data',
                url: 'get-monitoring-configuration.json',
                loaded: 'no',
                type: 'json'
            },
            {
                title: 'Domain Log',
                url: 'view-log',
                loaded: 'no',
                type: 'log'
            },
        ];
        this.fileButtons = [];
        this.filesLoaded = false;
        this.filesSaved = false;
        this.saved = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.elementId = 'domainEditor';
    }
    /**
     * ngAfterViewInit - AfterViewInit method of the component
     */
    AddFileComponent.prototype.ngAfterViewInit = function () {
        var _this = this;
        var initObject = {
            relative_urls: false,
            remove_script_host: false,
            document_base_url: null,
            skin_url: '../assets/skins/lightgray',
            selector: '#' + this.elementId,
            plugins: ['paste', 'searchreplace'],
            elementpath: false,
            height: 300,
            menubar: false,
            toolbar: false,
            statusbar: false,
            forced_root_block: false,
            setup: function (editor) { _this.editor = editor; },
        };
        if (this.environment.production) {
            initObject.relative_urls = false;
            initObject.remove_script_host = false;
            initObject.document_base_url = 'http://localhost:' + window["globalPort"] + '/resource/payara_support/zendesk/';
            initObject.skin_url = './assets/skins/lightgray';
        }
        this.tinymce.init(initObject);
        this.discardXml();
    };
    /**
     * ngOnDestroy - OnDestroy method of the component
     */
    AddFileComponent.prototype.ngOnDestroy = function () {
        __WEBPACK_IMPORTED_MODULE_7_jquery_dist_jquery__('.ng-tool-tip-content').hide();
    };
    /**
     * ngOnInit - OnInit method of the component
     */
    AddFileComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.fileButtons = Object.assign([], this.staticFileButtons);
        this.payaraService.getServerInstances()
            .then(function (responseData) {
            if (responseData !== undefined && responseData !== null) {
                _this.filesLoaded = true;
                var responseArray = Object.keys(responseData);
                for (var prop in responseArray) {
                    _this.fileButtons.push({
                        title: _this.translate.instant('JVM Report') + ': ' + responseArray[prop],
                        url: 'servers/server/' + responseArray[prop] + '/generate-jvm-report.json?type=summary',
                        loaded: 'no',
                        type: 'json'
                    });
                    _this.fileButtons.push({
                        title: _this.translate.instant('Thread Dump') + ': ' + responseArray[prop],
                        url: 'servers/server/' + responseArray[prop] + '/generate-jvm-report.json?type=thread',
                        loaded: 'no',
                        type: 'json'
                    });
                    _this.fileButtons.push({
                        title: 'Log: ' + responseArray[prop],
                        url: 'logs_' + responseArray[prop] + '.zip?contentSourceId=LogFiles&target=' + responseArray[prop] + '&restUrl=http://localhost:4848/management/domain',
                        file: 'logs_' + responseArray[prop] + '.zip',
                        loaded: 'no',
                        type: 'zip'
                    });
                }
            }
        }, function (error) {
            _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                _this.errorMessage = res;
            });
        });
    };
    /**
    * searchFile - Event to get the data for the file when the button is pressed
    *
    * @param {any}  fileButton Object with the data of the fileButton
    */
    AddFileComponent.prototype.searchFile = function (fileButton) {
        var _this = this;
        fileButton.loaded = 'loading';
        this.loadingMessage = "Loading file ...";
        if (fileButton.url && fileButton.url !== '') {
            switch (fileButton.type) {
                case 'log':
                    this.getFile(fileButton.url, false, function (response) {
                        _this.addFile(JSON.stringify(response['_body']), fileButton.title.replace(' ', '_') + '.log', 'application/octet-stream', function (response) {
                            _this.saved.emit(response);
                            fileButton.loaded = 'yes';
                            _this.filesSaved = true;
                            _this.loadingMessage = null;
                        });
                    });
                    break;
                case 'json':
                    this.getFile(fileButton.url, false, function (response) {
                        _this.addFile(JSON.stringify(response.json().message), fileButton.title.replace(' ', '_') + '.txt', 'application/octet-stream', function (response) {
                            _this.saved.emit(response);
                            fileButton.loaded = 'yes';
                            _this.filesSaved = true;
                            _this.loadingMessage = null;
                        });
                    });
                    break;
                case 'zip':
                    this.getFile(fileButton.url, true, function (response2) {
                        _this.addFile(new File([response2], fileButton.file, { type: 'application/zip' }), fileButton.file, 'application/zip', function (result) {
                            _this.saved.emit(result);
                            fileButton.loaded = 'yes';
                            _this.filesSaved = true;
                            _this.loadingMessage = null;
                        });
                    });
                    break;
            }
        }
    };
    /**
    * cleanFiles - Method to clean the add file buttons and enable it
    */
    AddFileComponent.prototype.cleanFiles = function () {
        this.filesSaved = false;
        this.filesLoaded = false;
        for (var _i = 0, _a = this.fileButtons; _i < _a.length; _i++) {
            var button = _a[_i];
            button.loaded = 'no';
        }
    };
    /**
    * searchFile - Event to get the data for the file when the button is pressed
    *
    * @param {any}      content Object with file's data
    * @param {string}   name String with file's name
    * @param {string}   type String with file's mimetype
    * @param {any}      back Callback function to return sync value
    */
    AddFileComponent.prototype.addFile = function (content, name, type, back) {
        var _this = this;
        this.zendeskService.addNewFile(content, name, type)
            .then(function (responseData) {
            if (responseData !== undefined && responseData !== null) {
                back(JSON.parse(responseData).upload);
            }
        }, function (error) {
            _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                _this.errorMessage = res;
            });
        });
    };
    /**
    * searchFile - Event to get the data for the file when the button is pressed
    *
    * @param {string}   url String with the endpoint to call
    * @param {boolean}  parse Boolean to know if parse data is needed
    * @param {any}      back Callback function to return sync value
    */
    AddFileComponent.prototype.getFile = function (url, parse, back) {
        var _this = this;
        if (parse) {
            JSZipUtils.getBinaryContent(this.payaraService.connectionData.filesURL + url, function (err, data) {
                if (err) {
                    _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                        _this.errorMessage = res;
                    });
                }
                back(data);
            });
        }
        else {
            this.payaraService.getFile(url)
                .then(function (responseData) {
                if (responseData !== undefined && responseData !== null) {
                    back(responseData);
                }
            }, function (error) {
                _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                    _this.errorMessage = res;
                });
            });
        }
    };
    /**
     * discardXml - Function to discard changes made to xml file shown
     */
    AddFileComponent.prototype.discardXml = function () {
        var _this = this;
        this.getFile('configs/config/server-config/zendesk-support-configuration/get-domain-xml.json', false, function (response) {
            if (response !== undefined && response !== null) {
                _this.tinymce.activeEditor.setContent(response.json().message.replace(/>/g, '&gt;').replace(/</g, '<br>&lt;').replace('<br>', ''));
            }
        });
    };
    /**
     * saveXml - Function to save changes made to xml file shown
     */
    AddFileComponent.prototype.saveXml = function () {
        var _this = this;
        this.loadingMessageXml = "Loading domain.xml file ...";
        this.addFile(this.editor.getContent().replace(/&gt;/g, '>').replace(/&lt;/g, '<').replace(/<br \/>/g, '\n'), 'Domain.xml', 'application/binary', function (response) {
            _this.saved.emit(response);
            _this.filesSaved = true;
            _this.tinymce.activeEditor.setMode('readonly');
            _this.loadingMessageXml = null;
        });
    };
    return AddFileComponent;
}());
__decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
    __metadata("design:type", Object)
], AddFileComponent.prototype, "saved", void 0);
AddFileComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
        selector: 'app-add-file',
        template: __webpack_require__(728),
        styles: [__webpack_require__(710)],
        animations: [
            __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["trigger"])('state', [
                __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["state"])('no', __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({
                    backgroundColor: 'transparent',
                    color: '#002c3e'
                })),
                __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["state"])('loading', __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({
                    backgroundColor: '#f0981b',
                    color: '#ffffff'
                })),
                __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["state"])('yes', __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({
                    backgroundColor: '#47B404',
                    color: '#E6E6E6'
                })),
                __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["transition"])('loading => yes', [
                    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["animate"])(300, __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["keyframes"])([
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1)', offset: 0 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.025)', offset: 0.1 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.05)', offset: 0.2 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.075)', offset: 0.3 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.1)', offset: 0.4 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.125)', offset: 0.5 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.1)', offset: 0.6 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.075)', offset: 0.7 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.05)', offset: 0.8 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.025)', offset: 0.9 }),
                    ]))
                ]),
                __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["transition"])('no => loading', [
                    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["animate"])(900, __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["keyframes"])([
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1)', offset: 0 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.025)', offset: 0.1 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.05)', offset: 0.2 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.075)', offset: 0.3 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.1)', offset: 0.4 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.125)', offset: 0.5 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.1)', offset: 0.6 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.075)', offset: 0.7 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.05)', offset: 0.8 }),
                        __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["style"])({ transform: 'scale(1.025)', offset: 0.9 }),
                    ]))
                ])
            ]),
        ]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_6__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_6__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_8__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_8__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_9__services_payara_service__["a" /* PayaraService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_9__services_payara_service__["a" /* PayaraService */]) === "function" && _c || Object])
], AddFileComponent);

var _a, _b, _c;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/add-file.component.js.map

/***/ }),

/***/ 354:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(41);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__(29);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__ = __webpack_require__(58);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_rxjs_add_operator_switchMap__ = __webpack_require__(490);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_rxjs_add_operator_switchMap___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_4_rxjs_add_operator_switchMap__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__ = __webpack_require__(54);
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
    function DetailedTicketComponent(route, zendeskService, datePipe) {
        this.route = route;
        this.zendeskService = zendeskService;
        this.datePipe = datePipe;
    }
    /**
     * ngOnDestroy - OnDestroy method of the component
     */
    DetailedTicketComponent.prototype.ngOnDestroy = function () {
        __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__('.ng-tool-tip-content').hide();
    };
    /**
     * ngOnInit - OnInit method of the component
     */
    DetailedTicketComponent.prototype.ngOnInit = function () {
        var ticketId = +this.route.snapshot.params['id'];
        if (this.zendeskService.organization) {
            this.ticket = this.zendeskService.ticketsOrganization.filter(function (ticket) { return ticket.id === ticketId; })[0];
        }
        else {
            this.ticket = this.zendeskService.ticketsUser.filter(function (ticket) { return ticket.id === ticketId; })[0];
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
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
        selector: 'app-detailed-ticket',
        template: __webpack_require__(730),
        styles: [__webpack_require__(712)]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_2__angular_common__["DatePipe"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__angular_common__["DatePipe"]) === "function" && _c || Object])
], DetailedTicketComponent);

var _a, _b, _c;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/detailed-ticket.component.js.map

/***/ }),

/***/ 355:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(41);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__ = __webpack_require__(55);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__ = __webpack_require__(58);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__services_zendesk_service__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__services_login_service__ = __webpack_require__(69);
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
     * ngOnDestroy - OnDestroy method of the component
     */
    ListTicketsComponent.prototype.ngOnDestroy = function () {
        __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__('.ng-tool-tip-content').hide();
    };
    /**
     * ngOnInit - OnInit method of the component
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
        if (this.loginService.user !== undefined) {
            this.user = this.loginService.user;
        }
        else {
            if (localStorage.getItem('currentUser') !== undefined && localStorage.getItem('currentUser') !== null) {
                this.user = JSON.parse(localStorage.getItem('currentUser'));
            }
        }
        if (this.user !== undefined) {
            this.zendeskService.OAuthToken = this.user.token;
            this.loginService.connectionData.OauthToken = this.user.token;
            this.loginService.user = this.user;
            this.zendeskService.getGenericCustomFields()
                .then(function (responseData) {
                if (responseData !== undefined && responseData !== null) {
                    _this.zendeskService.genericFields = responseData;
                    var customFields = _this.zendeskService.genericFields.filter(function (field) { return field.title_in_portal === 'Status'; })[0];
                    _this.statusFields = customFields !== undefined ? customFields.system_field_options : [];
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
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
        selector: 'app-list-tickets',
        template: __webpack_require__(731),
        styles: [__webpack_require__(713)]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_4__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_5__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_5__services_login_service__["a" /* LoginService */]) === "function" && _e || Object])
], ListTicketsComponent);

var _a, _b, _c, _d, _e;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/list-tickets.component.js.map

/***/ }),

/***/ 356:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(41);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__ = __webpack_require__(55);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__ = __webpack_require__(58);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_crypto_js_crypto_js__ = __webpack_require__(373);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_crypto_js_crypto_js___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_4_crypto_js_crypto_js__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__services_login_service__ = __webpack_require__(69);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__services_payara_service__ = __webpack_require__(232);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__classes_user__ = __webpack_require__(231);
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
    function LoginComponent(translate, route, router, zendeskService, loginService, payaraService) {
        this.translate = translate;
        this.route = route;
        this.router = router;
        this.zendeskService = zendeskService;
        this.loginService = loginService;
        this.payaraService = payaraService;
        /**
         * Properties and objects of the component
         */
        this.user = new __WEBPACK_IMPORTED_MODULE_8__classes_user__["a" /* User */]();
    }
    /**
     * ngOnInit - OnInit method of the component
     */
    LoginComponent.prototype.ngOnInit = function () {
        this.user == null;
        this.loginService.user = this.user;
        localStorage.setItem('currentUser', JSON.stringify(this.user));
    };
    /**
     * ngOnDestroy - OnDestroy method of the component
     */
    LoginComponent.prototype.ngOnDestroy = function () {
        __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__('.ng-tool-tip-content').hide();
        if (this.user.id !== undefined) {
            this.loginService.user = this.user;
        }
    };
    /**
     * loginToZendesk - Submit method to login a user to the API and redirect to the ticket list
     *
     * @param {User}  user Object with the data of the user to login
     */
    LoginComponent.prototype.loginToZendesk = function (user) {
        var _this = this;
        var regExpEmail = /^[a-z0-9]+(\.[_a-z0-9]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,15})$/i;
        if (user.email !== undefined && user.email !== '' && regExpEmail.test(user.email) && user.password !== undefined && user.password !== '') {
            this.loginService.getOauthToken(user.email, user.password)
                .then(function (responseData) {
                if (responseData !== undefined && responseData !== null && responseData !== '') {
                    var encryptedData_1 = __WEBPACK_IMPORTED_MODULE_4_crypto_js_crypto_js__["AES"].encrypt(user.email + '|' + responseData, 'payara').toString();
                    _this.loginService.connectionData.OauthToken = responseData;
                    _this.loginService.getUserData()
                        .then(function (responseData) {
                        if (responseData !== undefined && responseData !== null && responseData.id !== null) {
                            _this.user = responseData;
                            _this.user.token = _this.loginService.connectionData.OauthToken;
                            _this.loginService.user = _this.user;
                            localStorage.setItem('currentUser', JSON.stringify(_this.user));
                            _this.zendeskService.OAuthToken = _this.loginService.connectionData.OauthToken;
                            _this.payaraService.setStoredEmail(encryptedData_1)
                                .then(function (responseData) {
                                if (responseData !== undefined && responseData !== null && responseData.exit_code === "SUCCESS") {
                                    _this.router.navigate(['/list']);
                                }
                                else {
                                    _this.user = new __WEBPACK_IMPORTED_MODULE_8__classes_user__["a" /* User */]();
                                    _this.loginService.user = _this.user;
                                    _this.zendeskService.OAuthToken = '';
                                    _this.translate.get('Error! User stored but bad response').subscribe(function (res) {
                                        _this.errorMessage = res;
                                    });
                                }
                            }, function (error) {
                                _this.user = new __WEBPACK_IMPORTED_MODULE_8__classes_user__["a" /* User */]();
                                _this.loginService.user = _this.user;
                                _this.zendeskService.OAuthToken = '';
                                _this.translate.get('Error! User not stored').subscribe(function (res) {
                                    _this.errorMessage = res;
                                });
                            });
                        }
                        else {
                            _this.user = new __WEBPACK_IMPORTED_MODULE_8__classes_user__["a" /* User */]();
                            _this.loginService.user = _this.user;
                            _this.zendeskService.OAuthToken = '';
                            _this.translate.get('Error! User not found').subscribe(function (res) {
                                _this.errorMessage = res;
                            });
                        }
                    }, function (error) {
                        _this.user = new __WEBPACK_IMPORTED_MODULE_8__classes_user__["a" /* User */]();
                        _this.loginService.user = _this.user;
                        _this.zendeskService.OAuthToken = null;
                        _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                            _this.errorMessage = res;
                        });
                    });
                }
            }, function (error) {
                _this.user = new __WEBPACK_IMPORTED_MODULE_8__classes_user__["a" /* User */]();
                _this.loginService.user = _this.user;
                _this.zendeskService.OAuthToken = null;
                _this.translate.get('Error! User or password invalid').subscribe(function (res) {
                    _this.errorMessage = res;
                });
            });
        }
        else {
            if (!regExpEmail.test(user.email)) {
                this.translate.get('invalid-email', { value: user.email }).subscribe(function (res) {
                    _this.errorMessage = res;
                });
            }
            else {
                this.translate.get('Error! User or password invalid').subscribe(function (res) {
                    _this.errorMessage = res;
                });
            }
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
    /**
     * shopSupport - Method to send the user to the support buy website
     */
    LoginComponent.prototype.shopSupport = function () {
        window.open(this.loginService.connectionData.shopURL);
    };
    return LoginComponent;
}());
LoginComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
        selector: 'app-login',
        template: __webpack_require__(732),
        styles: [__webpack_require__(714)]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_6__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_6__services_login_service__["a" /* LoginService */]) === "function" && _e || Object, typeof (_f = typeof __WEBPACK_IMPORTED_MODULE_7__services_payara_service__["a" /* PayaraService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_7__services_payara_service__["a" /* PayaraService */]) === "function" && _f || Object])
], LoginComponent);

var _a, _b, _c, _d, _e, _f;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/login.component.js.map

/***/ }),

/***/ 357:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(41);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__(29);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_forms__ = __webpack_require__(323);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__ = __webpack_require__(55);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_jquery_dist_jquery__ = __webpack_require__(58);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_5_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_rxjs_Rx__ = __webpack_require__(737);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_rxjs_Rx___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_6_rxjs_Rx__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__services_login_service__ = __webpack_require__(69);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__classes_ticket__ = __webpack_require__(230);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10__classes_user__ = __webpack_require__(231);
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
        this.user = new __WEBPACK_IMPORTED_MODULE_10__classes_user__["a" /* User */]();
        this.newTicket = new __WEBPACK_IMPORTED_MODULE_9__classes_ticket__["a" /* Ticket */]();
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
        __WEBPACK_IMPORTED_MODULE_5_jquery_dist_jquery__('.ng-tool-tip-content').hide();
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
            var timer = __WEBPACK_IMPORTED_MODULE_6_rxjs_Rx__["Observable"].timer(5000, 1000);
            timer.subscribe(function (t) { return _this.errorMessage = ""; });
        }
    };
    return NewTicketComponent;
}());
NewTicketComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
        selector: 'app-new-ticket',
        template: __webpack_require__(733),
        styles: [__webpack_require__(715)]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_3__angular_forms__["d" /* FormBuilder */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_3__angular_forms__["d" /* FormBuilder */]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_8__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_8__services_login_service__["a" /* LoginService */]) === "function" && _e || Object, typeof (_f = typeof __WEBPACK_IMPORTED_MODULE_2__angular_common__["DatePipe"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__angular_common__["DatePipe"]) === "function" && _f || Object])
], NewTicketComponent);

var _a, _b, _c, _d, _e, _f;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/new-ticket.component.js.map

/***/ }),

/***/ 358:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(41);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__services_login_service__ = __webpack_require__(69);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AuthGuard; });
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
 * Guard to control the login inside the different parts of the App
 * Author : Daniel Contreras Aladro
 * Date : 2017-03-16
 *
 */



var AuthGuard = (function () {
    /**
     * constructor - Constructor of the service
     */
    function AuthGuard(router, loginService) {
        this.router = router;
        this.loginService = loginService;
    }
    /**
     * canActivate - Method to call to the API to make login
     *
     * @param {ActivatedRouteSnapshot}  route Current Route
     * @param {RouterStateSnapshot}  state Route to activate
     *
     * @return {boolean} Returns if the route can be activated or not
     */
    AuthGuard.prototype.canActivate = function (route, state) {
        if (localStorage.getItem('currentUser')) {
            // logged in so return true
            return true;
        }
        // not logged in so redirect to login page with the return url
        this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
        return false;
    };
    return AuthGuard;
}());
AuthGuard = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])(),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_2__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__services_login_service__["a" /* LoginService */]) === "function" && _b || Object])
], AuthGuard);

var _a, _b;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/auth.guard.js.map

/***/ }),

/***/ 519:
/***/ (function(module, exports) {

function webpackEmptyContext(req) {
	throw new Error("Cannot find module '" + req + "'.");
}
webpackEmptyContext.keys = function() { return []; };
webpackEmptyContext.resolve = webpackEmptyContext;
module.exports = webpackEmptyContext;
webpackEmptyContext.id = 519;


/***/ }),

/***/ 520:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser_dynamic__ = __webpack_require__(613);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__environments_environment__ = __webpack_require__(158);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__app_app_module__ = __webpack_require__(647);




if (__WEBPACK_IMPORTED_MODULE_2__environments_environment__["a" /* environment */].production) {
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_1__angular_core__["enableProdMode"])();
}
__webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_platform_browser_dynamic__["a" /* platformBrowserDynamic */])().bootstrapModule(__WEBPACK_IMPORTED_MODULE_3__app_app_module__["a" /* AppModule */]);
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/main.js.map

/***/ }),

/***/ 54:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_http__ = __webpack_require__(152);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_jquery__ = __webpack_require__(58);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_add_operator_toPromise__ = __webpack_require__(166);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_add_operator_toPromise___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_rxjs_add_operator_toPromise__);
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
     */
    ZendeskService.prototype.setHeaders = function () {
        this.headers = new __WEBPACK_IMPORTED_MODULE_1__angular_http__["c" /* Headers */]();
        console.log('ss:' + this.OAuthToken);
        this.headers.append('ZendeskAuth', this.OAuthToken);
        this.headers.append('Content-Type', 'application/json');
    };
    /**
     * getTicketsOrganizationRequested - Method to get the tickets openend by the organization of the user
     *
     * @param {number}  organization Number with the organization id
     * @return {Promise<Ticket[]>} Returns the response promise
     */
    ZendeskService.prototype.getTicketsOrganizationRequested = function (organization) {
        this.setHeaders();
        /*
        if (this.ticketsOrganization !== undefined && this.ticketsOrganization !== null && this.ticketsOrganization.length > 0) {
          return Promise.resolve(this.ticketsOrganization);
        } else {
          */
        return this.http.get(this.connectionData.zendeskUrl + 'organizations/' + organization + '/tickets.json', { headers: this.headers })
            .toPromise()
            .then(function (response) { return response.json().tickets; });
        //}
    };
    /**
     * getTicketsUserRequested - Method to get the tickets openend by the user
     *
     * @param {number}  user Number with the user id
     *
     * @return {Promise<Ticket[]>} Returns the response promise
     */
    ZendeskService.prototype.getTicketsUserRequested = function (user) {
        this.setHeaders();
        /*
        if (this.ticketsUser !== undefined && this.ticketsUser !== null && this.ticketsUser.length > 0) {
          return Promise.resolve(this.ticketsUser);
        } else {
          */
        return this.http.get(this.connectionData.zendeskUrl + 'users/' + user + '/requests.json', { headers: this.headers })
            .toPromise()
            .then(function (response) { return response.json().requests; });
        //}
    };
    /**
     * getUserIdentity - Method to get the user identity
     *
     * @param {string}  user String with the user id
     *
     * @return {Promise} Returns the response promise
     */
    ZendeskService.prototype.getUserIdentity = function (user) {
        this.setHeaders();
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
        this.setHeaders();
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
        this.setHeaders();
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
        this.setHeaders();
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
        this.setHeaders();
        this.http.put(this.connectionData.zendeskUrl + 'tickets/' + ticket.id + '.json', JSON.stringify({ ticket: ticket }), { headers: this.headers })
            .toPromise()
            .then(function () { return ticket; });
    };
    /**
     * addNewFile - Method to upload new file
     *
     * @param {Object}  input Object with the data of the file
     * @param {string}  filename String with the email to make te login
     * @param {string}  contentType String with the contentType of the file
     *
     * @return {Promise} Returns the response promise
     */
    ZendeskService.prototype.addNewFile = function (input, filename, contentType) {
        var settings = {
            "async": true,
            "crossDomain": true,
            "url": this.connectionData.filesUrl + 'uploads.json?filename=' + filename,
            "method": "POST",
            "headers": {
                "authorization": "Bearer " + this.OAuthToken,
                "content-type": contentType
            },
            "data": input,
            "processData": false,
            "contentType": contentType
        };
        return __WEBPACK_IMPORTED_MODULE_2_jquery__["ajax"](settings).done();
    };
    /**
     * createNewTicket - Method to create a new ticket
     *
     * @param {Ticket}  ticketData Object with the data of a ticket
     *
     * @return {Promise} Returns the response promise
     */
    ZendeskService.prototype.createNewTicket = function (ticketData) {
        this.setHeaders();
        return this.http
            .post(this.connectionData.zendeskUrl + 'requests.json', JSON.stringify({ request: ticketData }), { headers: this.headers })
            .toPromise()
            .then(function (res) { return res.json().request; });
    };
    return ZendeskService;
}());
ZendeskService = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])(),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_1__angular_http__["b" /* Http */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_http__["b" /* Http */]) === "function" && _a || Object])
], ZendeskService);

var _a;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/zendesk.service.js.map

/***/ }),

/***/ 646:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(41);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_moment__ = __webpack_require__(2);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_moment___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_moment__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__environments_environment__ = __webpack_require__(158);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__ = __webpack_require__(55);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_crypto_js_crypto_js__ = __webpack_require__(373);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_crypto_js_crypto_js___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_5_crypto_js_crypto_js__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__services_login_service__ = __webpack_require__(69);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__services_payara_service__ = __webpack_require__(232);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__classes_user__ = __webpack_require__(231);
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
        this.user = new __WEBPACK_IMPORTED_MODULE_9__classes_user__["a" /* User */]();
        translate.setDefaultLang(translate.getBrowserLang());
        translate.use(translate.getBrowserLang());
        __WEBPACK_IMPORTED_MODULE_2_moment__["locale"](translate.getBrowserLang());
        //translate.use('es');moment.locale('es');
        this.loginService.connectionData = {
            zendeskUrl: this.environment.zendesk.baseUrl,
            shopURL: this.environment.payara.shopUrl
        };
        this.zendeskService.connectionData = {
            zendeskUrl: this.environment.zendesk.baseUrl,
            filesUrl: this.environment.zendesk.filesUrl
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
        this.user = new __WEBPACK_IMPORTED_MODULE_9__classes_user__["a" /* User */]();
        var globalPort = window["globalPort"] !== undefined ? window["globalPort"] : '5000';
        if (globalPort !== undefined && globalPort !== null) {
            this.payaraService.connectionData = {
                payaraURL: window.location.protocol + '//' + window.location.hostname + ':' + globalPort + this.environment.payara.baseUrl,
                filesURL: window.location.protocol + '//' + window.location.hostname + ':' + globalPort + '/download/'
            };
            this.payaraService.getStoredEmail()
                .then(function (responseData) {
                if (responseData !== undefined && responseData !== null) {
                    var decryptedData = __WEBPACK_IMPORTED_MODULE_5_crypto_js_crypto_js__["AES"].decrypt(responseData, 'payara').toString(__WEBPACK_IMPORTED_MODULE_5_crypto_js_crypto_js__["enc"].Utf8);
                    var email = decryptedData.split('|')[0];
                    var regExpEmail = /^[a-z0-9]+(\.[_a-z0-9]+)*@[a-z0-9-]+(\.[a-z0-9-]+)*(\.[a-z]{2,15})$/i;
                    if (email !== '' && regExpEmail.test(email) && decryptedData.split('|').length === 2) {
                        _this.loginService.connectionData.OauthToken = decryptedData.split('|')[1];
                        _this.loginService.getUserData()
                            .then(function (responseData) {
                            if (responseData !== undefined && responseData !== null && responseData.id !== null) {
                                _this.user = responseData;
                                _this.user.token = _this.loginService.connectionData.OauthToken;
                                _this.loginService.user = _this.user;
                                localStorage.setItem('currentUser', JSON.stringify(_this.user));
                                _this.zendeskService.OAuthToken = _this.loginService.connectionData.OauthToken;
                                _this.router.navigate(['/list']);
                            }
                            else {
                                _this.user = null;
                                _this.zendeskService.OAuthToken = '';
                                _this.loginService.user = _this.user;
                            }
                        }, function (error) {
                            _this.user = null;
                            _this.zendeskService.OAuthToken = '';
                            _this.loginService.user = _this.user;
                        });
                    }
                    else {
                        _this.user = null;
                        _this.zendeskService.OAuthToken = '';
                        _this.loginService.user = _this.user;
                    }
                }
                else {
                    _this.user = null;
                    _this.zendeskService.OAuthToken = '';
                    _this.loginService.user = _this.user;
                }
            }, function (error) {
                _this.user = null;
                _this.zendeskService.OAuthToken = '';
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
        this.zendeskService.OAuthToken = null;
        this.router.navigate(['/login']);
        this.payaraService.setStoredEmail('');
    };
    return AppComponent;
}());
AppComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
        selector: 'app-root',
        template: __webpack_require__(727),
        styles: [__webpack_require__(709)],
        providers: [__WEBPACK_IMPORTED_MODULE_6__services_login_service__["a" /* LoginService */], __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__["a" /* ZendeskService */], __WEBPACK_IMPORTED_MODULE_8__services_payara_service__["a" /* PayaraService */]]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_6__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_6__services_login_service__["a" /* LoginService */]) === "function" && _e || Object, typeof (_f = typeof __WEBPACK_IMPORTED_MODULE_8__services_payara_service__["a" /* PayaraService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_8__services_payara_service__["a" /* PayaraService */]) === "function" && _f || Object])
], AppComponent);

var _a, _b, _c, _d, _e, _f;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/app.component.js.map

/***/ }),

/***/ 647:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser__ = __webpack_require__(104);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_forms__ = __webpack_require__(323);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_http__ = __webpack_require__(152);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__angular_router__ = __webpack_require__(41);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__angular_common__ = __webpack_require__(29);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__ngx_translate_core__ = __webpack_require__(55);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__ngx_translate_http_loader__ = __webpack_require__(653);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8_ngx_modal__ = __webpack_require__(719);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8_ngx_modal___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_8_ngx_modal__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9_angular2_tooltip__ = __webpack_require__(643);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10__environments_environment__ = __webpack_require__(158);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_11__app_component__ = __webpack_require__(646);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_12__app_routing__ = __webpack_require__(648);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_13__components_login_login_component__ = __webpack_require__(356);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_14__components_list_tickets_list_tickets_component__ = __webpack_require__(355);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_15__components_new_ticket_new_ticket_component__ = __webpack_require__(357);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_16__components_detailed_ticket_detailed_ticket_component__ = __webpack_require__(354);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_17__components_add_file_add_file_component__ = __webpack_require__(353);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_18__components_ticket_data_ticket_data_component__ = __webpack_require__(651);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_19__components_comment_data_comment_data_component__ = __webpack_require__(650);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_20__services_login_service__ = __webpack_require__(69);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_21__guards_auth_guard__ = __webpack_require__(358);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_22__pipes_day_time_pipe__ = __webpack_require__(652);
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

function createTranslateLoader(http) {
    return new __WEBPACK_IMPORTED_MODULE_7__ngx_translate_http_loader__["a" /* TranslateHttpLoader */](http, './assets/i18n/', '.json');
}
if (__WEBPACK_IMPORTED_MODULE_10__environments_environment__["a" /* environment */].production) {
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_1__angular_core__["enableProdMode"])();
}
var AppModule = (function () {
    function AppModule() {
    }
    return AppModule;
}());
AppModule = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_1__angular_core__["NgModule"])({
        declarations: [
            __WEBPACK_IMPORTED_MODULE_11__app_component__["a" /* AppComponent */],
            __WEBPACK_IMPORTED_MODULE_13__components_login_login_component__["a" /* LoginComponent */],
            __WEBPACK_IMPORTED_MODULE_14__components_list_tickets_list_tickets_component__["a" /* ListTicketsComponent */],
            __WEBPACK_IMPORTED_MODULE_15__components_new_ticket_new_ticket_component__["a" /* NewTicketComponent */],
            __WEBPACK_IMPORTED_MODULE_16__components_detailed_ticket_detailed_ticket_component__["a" /* DetailedTicketComponent */],
            __WEBPACK_IMPORTED_MODULE_17__components_add_file_add_file_component__["a" /* AddFileComponent */],
            __WEBPACK_IMPORTED_MODULE_18__components_ticket_data_ticket_data_component__["a" /* TicketDataComponent */],
            __WEBPACK_IMPORTED_MODULE_19__components_comment_data_comment_data_component__["a" /* CommentDataComponent */],
            __WEBPACK_IMPORTED_MODULE_22__pipes_day_time_pipe__["a" /* DayTimePipe */]
        ],
        imports: [
            __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser__["a" /* BrowserModule */],
            __WEBPACK_IMPORTED_MODULE_2__angular_forms__["a" /* FormsModule */],
            __WEBPACK_IMPORTED_MODULE_2__angular_forms__["b" /* ReactiveFormsModule */],
            __WEBPACK_IMPORTED_MODULE_3__angular_http__["a" /* HttpModule */],
            __WEBPACK_IMPORTED_MODULE_8_ngx_modal__["ModalModule"],
            __WEBPACK_IMPORTED_MODULE_9_angular2_tooltip__["a" /* ToolTipModule */],
            __WEBPACK_IMPORTED_MODULE_12__app_routing__["a" /* routing */],
            __WEBPACK_IMPORTED_MODULE_6__ngx_translate_core__["a" /* TranslateModule */].forRoot({
                loader: {
                    provide: __WEBPACK_IMPORTED_MODULE_6__ngx_translate_core__["b" /* TranslateLoader */],
                    useFactory: (createTranslateLoader),
                    deps: [__WEBPACK_IMPORTED_MODULE_3__angular_http__["b" /* Http */]]
                }
            })
        ],
        exports: [
            __WEBPACK_IMPORTED_MODULE_4__angular_router__["RouterModule"],
            __WEBPACK_IMPORTED_MODULE_22__pipes_day_time_pipe__["a" /* DayTimePipe */],
            __WEBPACK_IMPORTED_MODULE_6__ngx_translate_core__["a" /* TranslateModule */]
        ],
        providers: [
            __WEBPACK_IMPORTED_MODULE_5__angular_common__["DatePipe"],
            __WEBPACK_IMPORTED_MODULE_22__pipes_day_time_pipe__["a" /* DayTimePipe */],
            __WEBPACK_IMPORTED_MODULE_21__guards_auth_guard__["a" /* AuthGuard */],
            __WEBPACK_IMPORTED_MODULE_20__services_login_service__["a" /* LoginService */]
        ],
        bootstrap: [__WEBPACK_IMPORTED_MODULE_11__app_component__["a" /* AppComponent */]]
    })
], AppModule);

//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/app.module.js.map

/***/ }),

/***/ 648:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_router__ = __webpack_require__(41);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__components_login_login_component__ = __webpack_require__(356);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__components_list_tickets_list_tickets_component__ = __webpack_require__(355);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__components_new_ticket_new_ticket_component__ = __webpack_require__(357);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__components_detailed_ticket_detailed_ticket_component__ = __webpack_require__(354);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__guards_auth_guard__ = __webpack_require__(358);
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return routing; });
/**
 *
 * Module to control Routing inside the App
 * Author : Daniel Contreras Aladro
 * Date : 2017-03-16
 *
 */






var appRoutes = [
    { path: '', component: __WEBPACK_IMPORTED_MODULE_2__components_list_tickets_list_tickets_component__["a" /* ListTicketsComponent */], canActivate: [__WEBPACK_IMPORTED_MODULE_5__guards_auth_guard__["a" /* AuthGuard */]] },
    { path: 'detail/:id', component: __WEBPACK_IMPORTED_MODULE_4__components_detailed_ticket_detailed_ticket_component__["a" /* DetailedTicketComponent */], canActivate: [__WEBPACK_IMPORTED_MODULE_5__guards_auth_guard__["a" /* AuthGuard */]] },
    { path: 'new', component: __WEBPACK_IMPORTED_MODULE_3__components_new_ticket_new_ticket_component__["a" /* NewTicketComponent */], canActivate: [__WEBPACK_IMPORTED_MODULE_5__guards_auth_guard__["a" /* AuthGuard */]] },
    { path: 'login', component: __WEBPACK_IMPORTED_MODULE_1__components_login_login_component__["a" /* LoginComponent */] },
    // otherwise redirect to list of tickets
    { path: '**', redirectTo: '' }
];
var routing = __WEBPACK_IMPORTED_MODULE_0__angular_router__["RouterModule"].forRoot(appRoutes);
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/app.routing.js.map

/***/ }),

/***/ 649:
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

/***/ 650:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common__ = __webpack_require__(29);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__ = __webpack_require__(55);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__ = __webpack_require__(58);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__services_zendesk_service__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__add_file_add_file_component__ = __webpack_require__(353);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__classes_ticket__ = __webpack_require__(230);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__classes_comment__ = __webpack_require__(649);
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
        this.saved = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
    }
    /**
     * ngOnDestroy - OnDestroy method of the component
     */
    CommentDataComponent.prototype.ngOnDestroy = function () {
        __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__('.ng-tool-tip-content').hide();
    };
    /**
     * ngOnInit - OnInit method of the component
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
        if (newAttachment.attachment !== null) {
            this.newAttachments.push(newAttachment);
            this.newCommentText = (this.newCommentText !== undefined ? this.newCommentText : "") + '\n - File: ' + newAttachment['attachment'].file_name + ' added!\n';
        }
        else {
            this.addFilecomponent.cleanFiles();
            this.newAttachments = [];
            this.newCommentText = this.newCommentText.split('- File')[0];
        }
    };
    /**
     * saveComment - Method to save new comments
     */
    CommentDataComponent.prototype.saveComment = function () {
        var _this = this;
        if (this.newCommentText !== undefined && this.newCommentText !== null && this.newCommentText !== '') {
            var newComment_1 = new __WEBPACK_IMPORTED_MODULE_7__classes_comment__["a" /* Comment */]();
            newComment_1.body = this.newCommentText;
            if (this.newAttachments !== undefined && this.newAttachments.length > 0) {
                newComment_1.uploads = [];
                newComment_1.attachments = [];
                this.newAttachments.forEach(function (file) {
                    newComment_1.uploads.push(file.token);
                    newComment_1.attachments.push(file.attachment);
                });
            }
            newComment_1.created_at = this.datePipe.transform(new Date(), 'yyyy-MM-ddTHH:mm:ss');
            this.comments.push(newComment_1);
            this.saved.emit(newComment_1);
            this.addFilecomponent.cleanFiles();
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
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["ViewChild"])(__WEBPACK_IMPORTED_MODULE_5__add_file_add_file_component__["a" /* AddFileComponent */]),
    __metadata("design:type", typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_5__add_file_add_file_component__["a" /* AddFileComponent */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_5__add_file_add_file_component__["a" /* AddFileComponent */]) === "function" && _a || Object)
], CommentDataComponent.prototype, "addFilecomponent", void 0);
__decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
    __metadata("design:type", Object)
], CommentDataComponent.prototype, "saved", void 0);
__decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
    __metadata("design:type", typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_6__classes_ticket__["a" /* Ticket */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_6__classes_ticket__["a" /* Ticket */]) === "function" && _b || Object)
], CommentDataComponent.prototype, "ticket", void 0);
__decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
    __metadata("design:type", String)
], CommentDataComponent.prototype, "newCommentText", void 0);
CommentDataComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
        selector: 'app-comment-data',
        template: __webpack_require__(729),
        styles: [__webpack_require__(711)]
    }),
    __metadata("design:paramtypes", [typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_4__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_1__angular_common__["DatePipe"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_common__["DatePipe"]) === "function" && _e || Object])
], CommentDataComponent);

var _a, _b, _c, _d, _e;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/comment-data.component.js.map

/***/ }),

/***/ 651:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__ngx_translate_core__ = __webpack_require__(55);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_jquery_dist_jquery__ = __webpack_require__(58);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__services_login_service__ = __webpack_require__(69);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__classes_ticket__ = __webpack_require__(230);
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
     * ngOnDestroy - OnDestroy method of the component
     */
    TicketDataComponent.prototype.ngOnDestroy = function () {
        __WEBPACK_IMPORTED_MODULE_2_jquery_dist_jquery__('.ng-tool-tip-content').hide();
    };
    /**
     * ngOnInit - OnInit method of the component
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
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
    __metadata("design:type", typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_5__classes_ticket__["a" /* Ticket */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_5__classes_ticket__["a" /* Ticket */]) === "function" && _a || Object)
], TicketDataComponent.prototype, "ticket", void 0);
TicketDataComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
        selector: 'app-ticket-data',
        template: __webpack_require__(734),
        styles: [__webpack_require__(716)]
    }),
    __metadata("design:paramtypes", [typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__ngx_translate_core__["c" /* TranslateService */]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_4__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__services_login_service__["a" /* LoginService */]) === "function" && _d || Object])
], TicketDataComponent);

var _a, _b, _c, _d;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/ticket-data.component.js.map

/***/ }),

/***/ 652:
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
                                var minuteAgo = __WEBPACK_IMPORTED_MODULE_1_moment__(new Date()).subtract(1, 'minute');
                                if (d.isBefore(minuteAgo)) {
                                    std = d.format('HH:mm:ss');
                                }
                                else {
                                    std = 'just now';
                                }
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
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Pipe"])({
        name: 'dayTime'
    })
], DayTimePipe);

//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/day-time.pipe.js.map

/***/ }),

/***/ 69:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_http__ = __webpack_require__(152);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_toPromise__ = __webpack_require__(166);
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
     * @return {Promise<User>} Returns the response promise
     */
    LoginService.prototype.getUserData = function () {
        this.headers = new __WEBPACK_IMPORTED_MODULE_1__angular_http__["c" /* Headers */]();
        //this.headers.append('ZendeskAuth',email);
        this.headers.append('ZendeskAuth', this.connectionData.OauthToken);
        this.headers.append('Content-Type', 'application/json');
        return this.http.get(this.connectionData.zendeskUrl + 'users/me.json', { headers: this.headers })
            .toPromise()
            .then(function (response) { return response.json().user; });
    };
    /**
     * getUserData - Method to call to the API to make login
     *
     * @param {string}  email String with the email to make te login
     * @param {string}  password String with the password to make te login
     *
     * @return {Promise<string>} Returns the response promise
     */
    LoginService.prototype.getOauthToken = function (email, password) {
        this.headers = new __WEBPACK_IMPORTED_MODULE_1__angular_http__["c" /* Headers */]();
        this.headers.append('Content-Type', 'application/json');
        return this.http
            .post(this.connectionData.zendeskUrl + 'oauth/tokens', JSON.stringify({
            username: email,
            password: password
        }), { headers: this.headers })
            .toPromise()
            .then(function (res) { return res.json().access_token; });
    };
    return LoginService;
}());
LoginService = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])(),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_1__angular_http__["b" /* Http */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_http__["b" /* Http */]) === "function" && _a || Object])
], LoginService);

var _a;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/login.service.js.map

/***/ }),

/***/ 709:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(28)();
// imports


// module
exports.push([module.i, ".logout {\n  margin-top: -1.75rem;\n}\n\n.appTittle {\n  margin-left: 1.5rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 710:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(28)();
// imports


// module
exports.push([module.i, "span.glyphicon.glyphicon-paperclip{\n  background-color: transparent;\n}\n.btn.btn-sm.btn-default.btn-block.add-file-button{\n  margin-bottom:.5rem;\n  background-color: transparent;\n}\n.btn.btn-sm.btn-default.btn-block.add-file-button span.glyphicon.glyphicon-paperclip{\n  background-color: transparent;\n}\n.add-file-server-button{\n  width: 9rem;\n  height: 3.5rem;\n  white-space: normal;\n  font-size: smaller;\n  margin:.25rem;\n}\nspan.sr-only{\n  position: relative;\n  background-color: transparent;\n  color: #f0981b;\n}\n.panel-footer{\n  background-color: #E6E6E6;\n}\n.domain-block{\n  margin-top: 2rem;\n\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 711:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(28)();
// imports


// module
exports.push([module.i, ".new_comment{\n  resize: none;\n  outline: none;\n  width: 100%;\n  padding: 10px;\n  border: none;\n  height: 100%;\n  border-radius: 5px;\n  background-color: #ffffff;\n  margin-bottom: .5rem;\n  height: 7.5rem;\n}\n\n.comment_box{\n  background-color: #ffffff;\n  margin-bottom: .25rem;\n  padding: .25rem;\n}\n\npre code {\n    padding: 0;\n    font-size: .75rem;\n    margin-left: -1.5rem;\n}\n\n.attached-file {\n  color: #ffffff;\n  padding: .25rem;\n  border-radius: .25rem;\n  margin: .25rem;\n  font-style: oblique;\n  font-size: x-small;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 712:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(28)();
// imports


// module
exports.push([module.i, ".returnBack {\n  margin-top: -.5rem;\n}\n\nbutton.btn.btn-sm.pull-right.returnBack{\n  max-width: 2rem;\n  max-height: 1.75rem\n}\n\nspan.glyphicon.glyphicon-chevron-left{\n  background-color: transparent;\n}\n\nspan.glyphicon.glyphicon-chevron-left::before{\n    margin-left: -.15rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 713:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(28)();
// imports


// module
exports.push([module.i, ".addTicket {\n  margin-top: -.5rem;\n}\n\nbutton.btn.btn-sm.pull-right.addTicket{\n  max-width: 2rem;\n  max-height: 1.75rem\n}\n\nspan.glyphicon.glyphicon-chevron-up,\nspan.glyphicon.glyphicon-chevron-down,\nspan.glyphicon.glyphicon-plus{\n  background-color: transparent;\n}\n\nspan.glyphicon.glyphicon-plus::before{\n  margin-left: -.15rem;\n}\n\ntable.table-responsive.table-striped.table-md.table-inverse.table-sortable{\n  margin-top:3rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 714:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(28)();
// imports


// module
exports.push([module.i, "button.btn.btn-sm.btn-primary.btn-block{\n  margin-top:.75rem;\n}\n\n#inputEmail{\n  margin-top:.5rem;\n}\n\n.transparent{\n  background-color: transparent;\n  color:#002c3e;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 715:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(28)();
// imports


// module
exports.push([module.i, ".form-group.required .control-label:after {\n    color: #d00;\n    content: \"*\";\n    margin-left: .5rem;\n}\n\n.form-group .control-label,\n.form-group.required .control-label {\n  margin-bottom:.25rem;\n}\n\n.discardChanges {\n  margin-top: -.5rem;\n}\n\nbutton.btn.btn-sm.pull-right.discardChanges{\n  max-width: 2rem;\n  max-height: 1.75rem\n}\n\nspan.glyphicon.glyphicon-remove-circle{\n  background-color: transparent;\n}\n\nspan.glyphicon.glyphicon-remove-circle::before{\n    margin-left: -.15rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 716:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(28)();
// imports


// module
exports.push([module.i, ".detailContent {\n  border-left: thin solid #f0981b;\n  margin-left:1.5rem;\n  margin-top:1rem;\n}\n\n.detailData{\n  margin:.5rem;\n  padding: .25rem;\n  letter-spacing: .05rem;\n  display:-webkit-box;\n  display:-ms-flexbox;\n  display:flex;\n}\n\n.titleData{\n  margin-top:.25rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 718:
/***/ (function(module, exports, __webpack_require__) {

var map = {
	"./af": 375,
	"./af.js": 375,
	"./ar": 381,
	"./ar-dz": 376,
	"./ar-dz.js": 376,
	"./ar-ly": 377,
	"./ar-ly.js": 377,
	"./ar-ma": 378,
	"./ar-ma.js": 378,
	"./ar-sa": 379,
	"./ar-sa.js": 379,
	"./ar-tn": 380,
	"./ar-tn.js": 380,
	"./ar.js": 381,
	"./az": 382,
	"./az.js": 382,
	"./be": 383,
	"./be.js": 383,
	"./bg": 384,
	"./bg.js": 384,
	"./bn": 385,
	"./bn.js": 385,
	"./bo": 386,
	"./bo.js": 386,
	"./br": 387,
	"./br.js": 387,
	"./bs": 388,
	"./bs.js": 388,
	"./ca": 389,
	"./ca.js": 389,
	"./cs": 390,
	"./cs.js": 390,
	"./cv": 391,
	"./cv.js": 391,
	"./cy": 392,
	"./cy.js": 392,
	"./da": 393,
	"./da.js": 393,
	"./de": 395,
	"./de-at": 394,
	"./de-at.js": 394,
	"./de.js": 395,
	"./dv": 396,
	"./dv.js": 396,
	"./el": 397,
	"./el.js": 397,
	"./en-au": 398,
	"./en-au.js": 398,
	"./en-ca": 399,
	"./en-ca.js": 399,
	"./en-gb": 400,
	"./en-gb.js": 400,
	"./en-ie": 401,
	"./en-ie.js": 401,
	"./en-nz": 402,
	"./en-nz.js": 402,
	"./eo": 403,
	"./eo.js": 403,
	"./es": 405,
	"./es-do": 404,
	"./es-do.js": 404,
	"./es.js": 405,
	"./et": 406,
	"./et.js": 406,
	"./eu": 407,
	"./eu.js": 407,
	"./fa": 408,
	"./fa.js": 408,
	"./fi": 409,
	"./fi.js": 409,
	"./fo": 410,
	"./fo.js": 410,
	"./fr": 413,
	"./fr-ca": 411,
	"./fr-ca.js": 411,
	"./fr-ch": 412,
	"./fr-ch.js": 412,
	"./fr.js": 413,
	"./fy": 414,
	"./fy.js": 414,
	"./gd": 415,
	"./gd.js": 415,
	"./gl": 416,
	"./gl.js": 416,
	"./he": 417,
	"./he.js": 417,
	"./hi": 418,
	"./hi.js": 418,
	"./hr": 419,
	"./hr.js": 419,
	"./hu": 420,
	"./hu.js": 420,
	"./hy-am": 421,
	"./hy-am.js": 421,
	"./id": 422,
	"./id.js": 422,
	"./is": 423,
	"./is.js": 423,
	"./it": 424,
	"./it.js": 424,
	"./ja": 425,
	"./ja.js": 425,
	"./jv": 426,
	"./jv.js": 426,
	"./ka": 427,
	"./ka.js": 427,
	"./kk": 428,
	"./kk.js": 428,
	"./km": 429,
	"./km.js": 429,
	"./ko": 430,
	"./ko.js": 430,
	"./ky": 431,
	"./ky.js": 431,
	"./lb": 432,
	"./lb.js": 432,
	"./lo": 433,
	"./lo.js": 433,
	"./lt": 434,
	"./lt.js": 434,
	"./lv": 435,
	"./lv.js": 435,
	"./me": 436,
	"./me.js": 436,
	"./mi": 437,
	"./mi.js": 437,
	"./mk": 438,
	"./mk.js": 438,
	"./ml": 439,
	"./ml.js": 439,
	"./mr": 440,
	"./mr.js": 440,
	"./ms": 442,
	"./ms-my": 441,
	"./ms-my.js": 441,
	"./ms.js": 442,
	"./my": 443,
	"./my.js": 443,
	"./nb": 444,
	"./nb.js": 444,
	"./ne": 445,
	"./ne.js": 445,
	"./nl": 447,
	"./nl-be": 446,
	"./nl-be.js": 446,
	"./nl.js": 447,
	"./nn": 448,
	"./nn.js": 448,
	"./pa-in": 449,
	"./pa-in.js": 449,
	"./pl": 450,
	"./pl.js": 450,
	"./pt": 452,
	"./pt-br": 451,
	"./pt-br.js": 451,
	"./pt.js": 452,
	"./ro": 453,
	"./ro.js": 453,
	"./ru": 454,
	"./ru.js": 454,
	"./se": 455,
	"./se.js": 455,
	"./si": 456,
	"./si.js": 456,
	"./sk": 457,
	"./sk.js": 457,
	"./sl": 458,
	"./sl.js": 458,
	"./sq": 459,
	"./sq.js": 459,
	"./sr": 461,
	"./sr-cyrl": 460,
	"./sr-cyrl.js": 460,
	"./sr.js": 461,
	"./ss": 462,
	"./ss.js": 462,
	"./sv": 463,
	"./sv.js": 463,
	"./sw": 464,
	"./sw.js": 464,
	"./ta": 465,
	"./ta.js": 465,
	"./te": 466,
	"./te.js": 466,
	"./tet": 467,
	"./tet.js": 467,
	"./th": 468,
	"./th.js": 468,
	"./tl-ph": 469,
	"./tl-ph.js": 469,
	"./tlh": 470,
	"./tlh.js": 470,
	"./tr": 471,
	"./tr.js": 471,
	"./tzl": 472,
	"./tzl.js": 472,
	"./tzm": 474,
	"./tzm-latn": 473,
	"./tzm-latn.js": 473,
	"./tzm.js": 474,
	"./uk": 475,
	"./uk.js": 475,
	"./uz": 476,
	"./uz.js": 476,
	"./vi": 477,
	"./vi.js": 477,
	"./x-pseudo": 478,
	"./x-pseudo.js": 478,
	"./yo": 479,
	"./yo.js": 479,
	"./zh-cn": 480,
	"./zh-cn.js": 480,
	"./zh-hk": 481,
	"./zh-hk.js": 481,
	"./zh-tw": 482,
	"./zh-tw.js": 482
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
webpackContext.id = 718;


/***/ }),

/***/ 727:
/***/ (function(module, exports) {

module.exports = "\n\n<hr>\n\n<h2>\n  <p class=\"appTittle\">{{'Payara Support' | translate}}</p>\n  <button class=\"btn btn-default pull-right logout\" *ngIf=\"!isCurrentRoute('login')\" (click)=\"logout()\">\n    <span class=\"glyphicon glyphicon-off\" aria-hidden=\"true\">\n    </span>\n  </button>\n</h2>\n\n<hr>\n\n<router-outlet></router-outlet>\n"

/***/ }),

/***/ 728:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\">\n  <div class=\"panel-heading\">\n    <h1>{{'Select files to load' | translate}}</h1>\n  </div>\n  <div class=\"panel-body\">\n    <div class=\"row\">\n      <div class=\"col-xs-6 col-sm-4 col-lg-3\" *ngFor=\"let fileButton of fileButtons\">\n        <button\n              class=\"btn btn-sm btn-file add-file-server-button\"\n              [@state]='fileButton.loaded'\n              (click)=\"searchFile(fileButton)\"\n              [disabled]=\"fileButton.loaded!=='no'\">\n            <span class=\"glyphicon glyphicon-paperclip\" aria-hidden=\"true\"></span>\n            {{fileButton.title | translate}}\n        </button>\n      </div>\n      <div class=\"col-xs-12\">\n        <div *ngIf=\"loadingMessage\" class=\"progress\">\n          <div class=\"progress-bar progress-bar-striped active\" role=\"progressbar\" aria-valuenow=\"100\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 100%\">\n            <span class=\"sr-only\">{{loadingMessage | translate}}</span>\n          </div>\n        </div>\n      </div>\n      <div class=\"col-xs-12 domain-block\">\n        <div class=\"panel panel-default\">\n          <div class=\"panel-heading\">\n            <h2>{{'Edit' | translate}} Domain.xml</h2>\n          </div>\n          <div class=\"panel-body\">\n            <div class=\"row\">\n              <textarea id=\"{{elementId}}\"></textarea>\n            </div>\n          </div>\n          <div class=\"panel-footer\">\n            <div class=\"row\">\n              <div class=\"col-xs-8\">\n                <div *ngIf=\"loadingMessageXml\" class=\"progress\">\n                  <div class=\"progress-bar progress-bar-striped active\" role=\"progressbar\" aria-valuenow=\"100\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 100%\">\n                    <span class=\"sr-only\">{{loadingMessageXml | translate}}</span>\n                  </div>\n                </div>\n              </div>\n              <div class=\"col-xs-4\">\n                <button class=\"btn btn-default pull-right\" (click)=\"saveXml()\">{{'Save' | translate}}</button>\n                <button class=\"btn btn-primary pull-right\" (click)=\"discardXml()\">{{'Discard' | translate}}</button>\n              </div>\n            </div>\n          </div>\n        </div>\n      </div>\n    </div>\n  </div>\n</div>\n"

/***/ }),

/***/ 729:
/***/ (function(module, exports) {

module.exports = "<div class=\"commentContainer\" *ngFor=\"let comment of comments\">\n  <div class=\"row\">\n    <div class=\"col-sm-9\">\n      <pre class=\"comment_box\">\n        <code>{{comment.body}}</code>\n      </pre>\n    </div>\n    <div class=\"col-sm-3\">\n      <p>{{comment.created_at | dayTime}}</p>\n    </div>\n  </div>\n  <div class=\"row\" *ngIf=\"comment.attachments !== undefined && comment.attachments.length>0\">\n    <div class=\"col-xs-12 col-sm-6 col-md-4 col-lg-2\" *ngFor=\"let file of comment.attachments\">\n      <a href=\"{{file.content_url}}\">\n        <span class=\"\tglyphicon glyphicon-download-alt attached-file\" aria-hidden=\"true\">\n          {{file.file_name}}\n        </span>\n      </a>\n    </div>\n  </div>\n  <hr>\n</div>\n\n<div class=\"newCommentContainer\" *ngIf=\"ticket.status!=='closed' && ticket.status!=='solved'\">\n  <div class=\"row\">\n    <div class=\"col-md-12 col-sm-12 col-xs-12\">\n      <textarea class=\"new_comment\" [(ngModel)]=\"newCommentText\" (keyup)=\"keyUpEvent($event)\" tooltip content=\"{{'Insert comment text' | translate}}\"></textarea>\n    </div>\n    <div class=\"col-md-12 col-sm-12 col-xs-12\">\n      <app-add-file class=\"addFile\" (saved)=\"onSavedAttachment($event)\"></app-add-file>\n    </div>\n    <div class=\"col-md-12 col-sm-12 col-xs-12\">\n      <button class=\"btn btn-sm btn-default btn-block\" type=\"submit\" (click)=\"saveComment()\">\n        <span class=\"glyphicon glyphicon-ok\" aria-hidden=\"true\">\n        </span>\n      </button>\n    </div>\n  </div>\n</div>\n\n<div class=\"row\">\n  <div *ngIf=\"errorMessage\" class=\"alert alert-warning\" role=\"alert\">{{errorMessage}}</div>\n</div>\n"

/***/ }),

/***/ 730:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\" *ngIf=\"ticket\">\n  <div class=\"panel-heading\">\n    <h4>\n      {{'Request' | translate}} #{{ticket.id}} <strong>{{ticket.subject}}</strong>\n      <button class=\"btn btn-sm pull-right returnBack\" routerLink=\"/list\">\n        <span class=\"glyphicon glyphicon-chevron-left\" aria-hidden=\"true\">\n        </span>\n      </button>\n    </h4>\n  </div>\n  <div class=\"panel-body\">\n    <div class=\"row\">\n      <div class=\"col-sm-8\">\n        <app-comment-data [(ticket)]=\"ticket\" (saved)=\"onSavedComment(ticket,$event)\"></app-comment-data>\n      </div>\n      <div class=\"col-sm-4\">\n        <app-ticket-data [(ticket)]=\"ticket\"></app-ticket-data>\n      </div>\n  </div>\n</div>\n"

/***/ }),

/***/ 731:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\" *ngIf=\"tickets\">\n  <div class=\"panel-heading\">\n    <h4>\n      {{'My requests' | translate}}\n      <button class=\"btn btn-sm pull-right addTicket\" routerLink=\"/new\">\n        <span class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\">\n        </span>\n      </button>\n    </h4>\n  </div>\n  <div class=\"panel-body\">\n      <div class=\"col-sm-6\">\n        <input type=\"text\" class=\"form-control\" [(ngModel)]=\"query\" (keyup)=\"filter()\" placeholder=\"{{'Filter' | translate}}\" tooltip content=\"{{'Type to filter the tickets below' | translate}}\" />\n      </div>\n      <div class=\"col-sm-4\">\n        <div class=\"btn-group pull-right\" data-toggle=\"buttons\">\n          <label class=\"btn btn-primary\" [ngClass]=\"{active: userBool}\" tooltip content=\"{{'Select to filter tickets from user' | translate}}\">\n            <input type=\"radio\" autocomplete=\"off\" (click)=\"updateTickets(true)\"/>{{'User' | translate}}\n          </label>\n          <label class=\"btn btn-primary\" [ngClass]=\"{active: !userBool}\" tooltip content=\"{{'Select to filter tickets from company' | translate}}\">\n            <input type=\"radio\" autocomplete=\"off\" (click)=\"updateTickets(false)\"/>{{'Organization' | translate}}\n          </label>\n        </div>\n      </div>\n      <div class=\"col-sm-2 pull-right\">\n       <select class=\"form-control\" id=\"statusFilter\" [(ngModel)]=\"statusFilter\" (change)=\"filterStatus()\"  tooltip content=\"{{'Select to filter tickets by status' | translate}}\">\n         <option value=\"any\">{{'Any' | translate}}</option>\n         <option *ngFor=\"let statusOption of statusFields\" value=\"{{statusOption.value}}\">{{statusOption.name | translate}}</option>\n       </select>\n     </div>\n    <table class=\"table table-responsive table-striped table-md table-inverse table-sortable\">\n      <thead>\n        <tr>\n          <th (click)=\"changeSorting('id')\">Id\n            <span *ngIf=\"sort.column === 'id' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'id' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th (click)=\"changeSorting('subject')\">{{'Subject' | translate}}\n            <span *ngIf=\"sort.column === 'subject' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'subject' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th (click)=\"changeSorting('created_at')\">{{'Created' | translate}}\n            <span *ngIf=\"sort.column === 'created_at' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'created_at' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th (click)=\"changeSorting('updated_at')\">{{'Last activity' | translate}}\n            <span *ngIf=\"sort.column === 'updated_at' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'updated_at' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th (click)=\"changeSorting('status')\">{{'Status' | translate}}\n            <span *ngIf=\"sort.column === 'status' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'status' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n        </tr>\n      </thead>\n      <tbody>\n        <tr *ngFor=\"let ticket of tickets\" (click)=\"ticketClicked(ticket)\">\n          <th scope=\"row\">{{ticket.id}}</th>\n          <td>{{ticket.subject}}</td>\n          <td>{{ticket.created_at | dayTime}}</td>\n          <td>{{ticket.updated_at | dayTime}}</td>\n          <td style=\"text-align: center;\">\n              <span\n              [ngClass]=\"{\n                              'ticketOpen': ticket.status==='open',\n                              'ticketNew': ticket.status==='new',\n                              'ticketClosed': ticket.status==='closed',\n                              'ticketSolved': ticket.status==='solved',\n                              'ticketPending': ticket.status==='pending',\n                              'ticketHold': ticket.status==='hold'}\"\n              class=\"glyphicon glyphicon-flag\" aria-hidden=\"true\"> {{ticket.status | translate}}</span>\n          </td>\n        </tr>\n      </tbody>\n    </table>\n  </div>\n</div>\n\n<div *ngIf=\"!tickets && errorMessage\" class=\"alert alert-danger\" role=\"alert\">{{errorMessage}}</div>\n"

/***/ }),

/***/ 732:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\">\n  <div class=\"panel-heading\">\n    <div class=\"alert alert-info\" role=\"alert\">\n      <h4 class=\"transparent form-signin-heading\">{{'Please sign in' | translate}}</h4>\n      <p class=\"transparent\">{{'Insert Zendesk`s email and password to get OauthToken to communicate' | translate}}</p>\n    </div>\n  </div>\n  <div class=\"panel-body\">\n    <form class=\"form-signin\" id=\"login\">\n      <label for=\"inputEmail\" class=\"sr-only\">{{'Email address' | translate}}</label>\n      <input type=\"email\" id=\"inputEmail\" class=\"form-control\"\n              placeholder=\"{{'Email address' | translate}}\" required autofocus\n              [(ngModel)]=\"user.email\" name=\"email\" (keypress)=\"cleanError($event)\"\n               tooltip content=\"{{'Insert email address to login into Zendesk' | translate}}\">\n     <label for=\"inputPassword\" class=\"sr-only\">{{'Password' | translate}}</label>\n     <input type=\"password\" id=\"inputPassword\" class=\"form-control\"\n             placeholder=\"{{'Password' | translate}}\" required autofocus\n             [(ngModel)]=\"user.password\" name=\"password\" (keypress)=\"cleanError($event)\"\n              tooltip content=\"{{'Insert password to login into Zendesk, it not will be stored' | translate}}\">\n      <button class=\"btn btn-sm btn-primary btn-block\" type=\"submit\" [disabled]=\"!user.email && !user.password\" (click)=\"loginToZendesk(user)\">{{'Sign in' | translate}}</button>\n    </form>\n  </div>\n</div>\n\n<button class=\"btn btn-sm btn-default pull-right\" (click)=\"shopSupport()\">\n  {{'Unsupported? Hire support now!' | translate}}\n  <span class=\"glyphicon glyphicon-shopping-cart\" aria-hidden=\"true\">\n  </span>\n</button>\n\n<div *ngIf=\"errorMessage\" class=\"alert alert-danger\" role=\"alert\">{{errorMessage}}</div>\n"

/***/ }),

/***/ 733:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\">\n  <div *ngIf=\"errorMessage\" class=\"alert alert-danger alert-dismissible\" role=\"alert1\">\n    {{errorMessage}}\n  </div>\n  <div *ngIf=\"successMessage\" class=\"alert alert-success alert-dismissible\" role=\"alert2\">\n    {{successMessage}}\n  </div>\n  <div class=\"panel-heading\">\n    <h4>\n      {{'Submit a request' | translate}}\n      <button class=\"btn btn-sm pull-right discardChanges\" (click)=\"discardChanges()\">\n        <span class=\"glyphicon glyphicon-remove-circle\" aria-hidden=\"true\">\n        </span>\n      </button>\n    </h4>\n  </div>\n  <div class=\"panel-body\">\n    <form class=\"form-ticket form-vertical\" [formGroup]=\"ticketForm\">\n      <div class=\"form-group required\" *ngIf=\"genericFields[0]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[0].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <input class=\"form-control\" id=\"subject\" formControlName=\"subject\" required type=\"text\"/>\n           <small *ngIf=\"!ticketForm.controls.subject.valid &&\n                         (ticketForm.controls.subject.dirty ||\n                         ticketForm.controls.subject.touched)\"\n                        class=\"badge text-warning\">\n             {{genericFields[0].title_in_portal | translate}} {{'is required' | translate}}\n           </small>\n         </div>\n      </div>\n      <div class=\"form-group required\" *ngIf=\"genericFields[1]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[1].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <textarea class=\"form-control\" rows=\"5\" id=\"description\" formControlName=\"description\"></textarea>\n           <small *ngIf=\"!ticketForm.controls.description.valid &&\n                         (ticketForm.controls.description.dirty ||\n                         ticketForm.controls.description.touched)\"\n                        class=\"badge text-warning\">\n             {{genericFields[0].title_in_portal | translate}} {{'is required' | translate}}\n           </small>\n         </div>\n      </div>\n      <div class=\"form-group\" *ngIf=\"genericFields[3]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[3].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <select class=\"form-control\" id=\"type\" formControlName=\"type\">\n             <option *ngFor=\"let typeOption of genericFields[3].system_field_options\" value=\"{{typeOption.value}}\">{{typeOption.name | translate}}</option>\n           </select>\n        </div>\n      </div>\n      <div class=\"form-group required\" *ngIf=\"genericFields[4]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[4].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <select class=\"form-control\" id=\"environment\" formControlName=\"environment\" required>\n              <option *ngFor=\"let environmentOption of genericFields[4].custom_field_options\" value=\"{{environmentOption.value}}\">{{environmentOption.name | translate}}</option>\n           </select>\n        </div>\n      </div>\n      <div class=\"form-group required\" *ngIf=\"genericFields[5]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[5].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <select class=\"form-control\" id=\"priority\" formControlName=\"priority\" required>\n              <option *ngFor=\"let priorityOption of genericFields[5].system_field_options\" value=\"{{priorityOption.value}}\">{{priorityOption.name | translate}}</option>\n           </select>\n        </div>\n      </div>\n      <div class=\"form-group required\" *ngIf=\"genericFields[15]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[15].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <input class=\"form-control\" id=\"version\" formControlName=\"version\" required type=\"text\"/>\n           <small *ngIf=\"!ticketForm.controls.version.valid &&\n                         (ticketForm.controls.version.dirty ||\n                         ticketForm.controls.version.touched)\"\n                        class=\"badge text-warning\">\n             {{genericFields[15].title_in_portal | translate}} {{'is required' | translate}}\n           </small>\n         </div>\n      </div>\n\n      <div class=\"form-group\">\n         <label class=\"col-md-2 control-label\">{{'Attachments' | translate}}</label>\n         <div class=\"col-md-10\">\n           <app-add-file (saved)=\"onSavedAttachment($event)\"></app-add-file>\n         </div>\n      </div>\n\n      <button class=\"btn btn-sm btn-primary btn-block\" type=\"submit\" [disabled]=\"!ticketForm.valid\" (click)=\"checkData(ticketForm)\">{{'Submit' | translate}}</button>\n    </form>\n  </div>\n</div>\n"

/***/ }),

/***/ 734:
/***/ (function(module, exports) {

module.exports = "  <div class=\"col-md-11 detailContent\">\n    <h5>\n      <strong>{{ticket.submitter_name}}</strong> {{'submitted this request' | translate}}\n    </h5>\n    <hr>\n    <p class=\"detailData\">\n      <strong>{{'Status' | translate}}</strong>\n      &nbsp;\n      <span\n      [ngClass]=\"{\n                      'ticketOpen': ticket.status==='open',\n                      'ticketNew': ticket.status==='new',\n                      'ticketClosed': ticket.status==='closed',\n                      'ticketSolved': ticket.status==='solved',\n                      'ticketPending': ticket.status==='pending',\n                      'ticketHold': ticket.status==='hold'}\"\n      class=\"glyphicon glyphicon-flag\" aria-hidden=\"true\"> {{ticket.status | translate}}</span>\n    </p>\n    <p class=\"detailData\">\n      <strong>{{'Type' | translate}}</strong>\n      &nbsp;\n      {{ticket.type | translate}}\n    </p>\n    <p class=\"detailData\">\n      <strong>{{'Priority' | translate}}</strong>\n      &nbsp;\n      {{ticket.priority | translate}}\n    </p>\n    <div *ngFor=\"let field of ticket.custom_fields\">\n      <div class=\"detailData\">\n        <strong>{{field.title_in_portal | translate}}</strong>\n        &nbsp;\n        <p>{{getValue(field) | translate}}</p>\n      </div>\n    </div>\n  </div>\n"

/***/ })

},[1002]);
//# sourceMappingURL=main.bundle.js.map
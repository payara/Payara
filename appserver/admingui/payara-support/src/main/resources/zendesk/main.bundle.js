webpackJsonp([1,5],{

/***/ 107:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__ = __webpack_require__(94);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__environments_environment__ = __webpack_require__(53);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_tinymce_tinymce__ = __webpack_require__(651);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_tinymce_tinymce___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_tinymce_tinymce__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_tinymce_themes_modern_theme__ = __webpack_require__(650);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_tinymce_themes_modern_theme___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_4_tinymce_themes_modern_theme__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_tinymce_plugins_paste_plugin__ = __webpack_require__(648);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_tinymce_plugins_paste_plugin___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_5_tinymce_plugins_paste_plugin__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_tinymce_plugins_searchreplace_plugin__ = __webpack_require__(649);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_tinymce_plugins_searchreplace_plugin___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_6_tinymce_plugins_searchreplace_plugin__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__ngx_translate_core__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8_jquery_dist_jquery__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_8_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__services_zendesk_service__ = __webpack_require__(24);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10__services_payara_service__ = __webpack_require__(72);
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
        this.environment = __WEBPACK_IMPORTED_MODULE_2__environments_environment__["a" /* environment */];
        this.tinymce = __WEBPACK_IMPORTED_MODULE_3_tinymce_tinymce___default.a;
        this.groups = [];
        this.staticFileButtons = [
            {
                title: 'Monitoring Data',
                url: 'get-monitoring-configuration.json',
                group: 'general',
                uploaded: false,
                type: 'json'
            },
            {
                title: 'Domain Log',
                url: 'view-log',
                group: 'general',
                uploaded: false,
                type: 'log'
            },
            {
                title: 'Health Checks',
                url: 'configs/config/server-config/health-check-service-configuration/list-historic-healthchecks.json',
                group: 'general',
                uploaded: false,
                type: 'health'
            },
            {
                title: 'Request Traces',
                url: 'configs/config/server-config/request-tracing-service-configuration/list-historic-requesttraces',
                group: 'general',
                uploaded: false,
                type: 'traces'
            },
        ];
        this.fileButtons = [];
        this.filesLoaded = false;
        this.filesSaved = false;
        this.saved = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.removed = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.elementId = 'domainEditor';
        this.isVisibleEditor = true;
        this.xmlLoaded = false;
        this.xmlLoading = 'LOADING';
        this.xmlColor = 'warning';
    }
    /**
     * ngAfterViewInit - AfterViewInit method of the component
     */
    AddFileComponent.prototype.ngAfterViewInit = function () {
        this.discardXml();
    };
    /**
     * ngOnDestroy - OnDestroy method of the component
     */
    AddFileComponent.prototype.ngOnDestroy = function () {
        __WEBPACK_IMPORTED_MODULE_8_jquery_dist_jquery__('.ng-tool-tip-content').hide();
        if (this.sub)
            this.sub.unsubscribe();
    };
    AddFileComponent.prototype.filter = function (buttons, group) {
        return buttons.filter(function (data) { return data.group === group; });
    };
    /**
     * ngOnInit - OnInit method of the component
     */
    AddFileComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.groups.push('general');
        this.fileButtons = Object.assign([], this.staticFileButtons);
        this.payaraService.getServerInstances()
            .then(function (responseData) {
            if (responseData !== undefined && responseData !== null) {
                _this.filesLoaded = true;
                var responseArray_1 = Object.keys(responseData);
                var _loop_1 = function (prop) {
                    if (responseArray_1[prop] === 'server') {
                        _this.fileButtons.push({
                            title: _this.translate.instant('JVM Report') + ': ' + responseArray_1[prop],
                            url: 'servers/server/' + responseArray_1[prop] + '/generate-jvm-report.json?type=summary',
                            group: 'server',
                            uploaded: false,
                            type: 'json'
                        });
                        _this.fileButtons.push({
                            title: _this.translate.instant('Thread Dump') + ': ' + responseArray_1[prop],
                            url: 'servers/server/' + responseArray_1[prop] + '/generate-jvm-report.json?type=thread',
                            group: 'server',
                            uploaded: false,
                            type: 'json'
                        });
                        _this.fileButtons.push({
                            title: 'Log: ' + responseArray_1[prop],
                            url: 'logs_' + responseArray_1[prop] + '.zip?contentSourceId=LogFiles&target=' + responseArray_1[prop] + '&restUrl=' + window.location.protocol + '//' + window.location.hostname + ':' + window["globalPort"] + '/management/domain',
                            group: 'server',
                            file: 'logs_' + responseArray_1[prop] + '.zip',
                            uploaded: false,
                            type: 'zip'
                        });
                    }
                    else {
                        _this.payaraService.getInstanceStatus(responseArray_1[prop])
                            .then(function (responseData) {
                            if (responseArray_1[prop] === 'server' || (responseData !== undefined && responseData !== null && responseData === 'RUNNING')) {
                                _this.groups.push(responseArray_1[prop]);
                                _this.fileButtons.push({
                                    title: _this.translate.instant('JVM Report') + ': ' + responseArray_1[prop],
                                    url: 'servers/server/' + responseArray_1[prop] + '/generate-jvm-report.json?type=summary',
                                    group: responseArray_1[prop],
                                    uploaded: false,
                                    type: 'json'
                                });
                                _this.fileButtons.push({
                                    title: _this.translate.instant('Thread Dump') + ': ' + responseArray_1[prop],
                                    url: 'servers/server/' + responseArray_1[prop] + '/generate-jvm-report.json?type=thread',
                                    group: responseArray_1[prop],
                                    uploaded: false,
                                    type: 'json'
                                });
                                _this.fileButtons.push({
                                    title: 'Log: ' + responseArray_1[prop],
                                    url: 'Log: ' + responseArray_1[prop] + '.zip?contentSourceId=LogFiles&target=' + responseArray_1[prop] + '&restUrl=' + window.location.protocol + '//' + window.location.hostname + ':' + window["globalPort"] + '/management/domain',
                                    group: responseArray_1[prop],
                                    file: 'Log: ' + responseArray_1[prop] + '.zip',
                                    uploaded: false,
                                    type: 'zip'
                                });
                            }
                        }, function (error) {
                            _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                                _this.errorMessage = res;
                            });
                        });
                    }
                };
                for (var prop in responseArray_1) {
                    _loop_1(prop);
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
        this.errorMessage = "";
        this.loadingMessage = "";
        this.successMessage = "";
        if (fileButton.uploaded) {
            this.removed.emit(fileButton.title);
            fileButton.uploaded = false;
        }
        else {
            this.loadingMessage = "Loading file ..." + fileButton.title;
            if (fileButton.url && fileButton.url !== '') {
                switch (fileButton.type) {
                    case 'log':
                        this.getFile(fileButton.url, false, function (response) {
                            if (response !== false) {
                                _this.addFile(response['_body'], fileButton.title + '.log', 'application/octet-stream', function (response) {
                                    if (response) {
                                        _this.saved.emit(response);
                                        fileButton.uploaded = true;
                                        _this.filesSaved = true;
                                        _this.loadingMessage = null;
                                        _this.successMessage = "";
                                        _this.translate.get('File added successfully!').subscribe(function (res) {
                                            _this.successMessage = res;
                                        });
                                        var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1000, 50);
                                        _this.sub = timer.subscribe(function (t) {
                                            _this.successMessage = "";
                                            _this.sub.unsubscribe();
                                        });
                                    }
                                    else {
                                        fileButton.uploaded = false;
                                        _this.loadingMessage = null;
                                        _this.successMessage = "";
                                    }
                                });
                            }
                            else {
                                _this.translate.get('Empty data1').subscribe(function (res) {
                                    _this.errorMessage = res;
                                    var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1000, 50);
                                    _this.sub = timer.subscribe(function (t) {
                                        _this.errorMessage = "";
                                        _this.sub.unsubscribe();
                                    });
                                    fileButton.uploaded = false;
                                    _this.loadingMessage = null;
                                    _this.successMessage = "";
                                });
                            }
                        });
                        break;
                    case 'traces':
                        this.payaraService.postFile(fileButton.url).then(function (responseData) {
                            if (responseData !== false && responseData.message !== "" && responseData.exit_code !== 'FAIULRE' && responseData.message.indexOf('is not enabled') < 0) {
                                _this.addFile(responseData.message, fileButton.title + '.json', 'application/octet-stream', function (response) {
                                    if (response) {
                                        _this.saved.emit(response);
                                        fileButton.uploaded = true;
                                        _this.filesSaved = true;
                                        _this.loadingMessage = null;
                                        _this.successMessage = "";
                                        _this.translate.get('File added successfully!').subscribe(function (res) {
                                            _this.successMessage = res;
                                        });
                                        var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1000, 50);
                                        _this.sub = timer.subscribe(function (t) {
                                            _this.successMessage = "";
                                            _this.sub.unsubscribe();
                                        });
                                    }
                                    else {
                                        fileButton.uploaded = false;
                                        _this.loadingMessage = null;
                                        _this.successMessage = "";
                                    }
                                });
                            }
                            else {
                                _this.translate.get('Empty data1').subscribe(function (res) {
                                    _this.errorMessage = res;
                                    var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1000, 50);
                                    _this.sub = timer.subscribe(function (t) {
                                        _this.errorMessage = "";
                                        _this.sub.unsubscribe();
                                    });
                                    fileButton.uploaded = false;
                                    _this.loadingMessage = null;
                                    _this.successMessage = "";
                                });
                            }
                        }, function (error) {
                            _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                                _this.errorMessage = res;
                                var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1000, 50);
                                _this.sub = timer.subscribe(function (t) {
                                    _this.errorMessage = "";
                                    _this.sub.unsubscribe();
                                });
                                fileButton.uploaded = false;
                                _this.loadingMessage = null;
                                _this.successMessage = "";
                            });
                        });
                        break;
                    case 'health':
                        this.getFile(fileButton.url, false, function (response) {
                            if (response !== false && response.extraProperties !== undefined && response.extraProperties.historicmessages.length > 0) {
                                _this.addFile(JSON.stringify(response.json().extraProperties.historicmessages), fileButton.title + '.json', 'application/octet-stream', function (response) {
                                    if (response) {
                                        _this.saved.emit(response);
                                        fileButton.uploaded = true;
                                        _this.filesSaved = true;
                                        _this.loadingMessage = null;
                                        _this.successMessage = "";
                                        _this.translate.get('File added successfully!').subscribe(function (res) {
                                            _this.successMessage = res;
                                        });
                                        var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1000, 50);
                                        _this.sub = timer.subscribe(function (t) {
                                            _this.successMessage = "";
                                            _this.sub.unsubscribe();
                                        });
                                    }
                                    else {
                                        fileButton.uploaded = false;
                                        _this.loadingMessage = null;
                                        _this.successMessage = "";
                                    }
                                });
                            }
                            else {
                                fileButton.uploaded = false;
                                _this.loadingMessage = null;
                                _this.successMessage = "";
                            }
                        });
                        break;
                    case 'json':
                        this.getFile(fileButton.url, false, function (response) {
                            if (response !== false && response.json().message !== undefined && response.json().message !== "") {
                                _this.addFile(response.json().message, fileButton.title + '.json', 'application/octet-stream', function (response) {
                                    if (response) {
                                        _this.saved.emit(response);
                                        fileButton.uploaded = true;
                                        _this.filesSaved = true;
                                        _this.loadingMessage = null;
                                        _this.successMessage = "";
                                        _this.translate.get('File added successfully!').subscribe(function (res) {
                                            _this.successMessage = res;
                                        });
                                        var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1000, 50);
                                        _this.sub = timer.subscribe(function (t) {
                                            _this.successMessage = "";
                                            _this.sub.unsubscribe();
                                        });
                                    }
                                    else {
                                        fileButton.uploaded = false;
                                        _this.loadingMessage = null;
                                        _this.successMessage = "";
                                    }
                                });
                            }
                        });
                        break;
                    case 'zip':
                        this.getFile(fileButton.url, true, function (response2) {
                            if (response2 !== false) {
                                _this.addFile(new File([response2], fileButton.file, { type: 'application/zip' }), fileButton.file, 'application/zip', function (result) {
                                    if (result) {
                                        _this.saved.emit(result);
                                        fileButton.uploaded = true;
                                        _this.filesSaved = true;
                                        _this.loadingMessage = null;
                                        _this.successMessage = "";
                                        _this.translate.get('File added successfully!').subscribe(function (res) {
                                            _this.successMessage = res;
                                        });
                                        var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1000, 50);
                                        _this.sub = timer.subscribe(function (t) {
                                            _this.successMessage = "";
                                            _this.sub.unsubscribe();
                                        });
                                    }
                                    else {
                                        fileButton.uploaded = false;
                                        _this.loadingMessage = null;
                                        _this.successMessage = "";
                                    }
                                });
                            }
                        });
                        break;
                }
            }
        }
    };
    /**
    * cleanFiles - Method to clean the add file buttons and enable it
    */
    AddFileComponent.prototype.cleanFiles = function () {
        this.filesSaved = false;
        this.filesLoaded = false;
        this.xmlLoaded = false;
        this.isVisibleEditor = false;
        this.xmlColor = 'default';
        this.xmlLoading = 'NO';
        for (var _i = 0, _a = this.fileButtons; _i < _a.length; _i++) {
            var button = _a[_i];
            button.uploaded = false;
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
        if (!(content instanceof String)) {
            this.zendeskService.addNewFile(content, name, type)
                .then(function (responseData) {
                if (responseData !== undefined && responseData !== null) {
                    back(JSON.parse(responseData).upload);
                }
            }, function (error) {
                _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                    _this.errorMessage = res;
                    var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1000, 50);
                    _this.sub = timer.subscribe(function (t) {
                        _this.errorMessage = "";
                        _this.sub.unsubscribe();
                    });
                    back(false);
                });
            });
        }
        else {
            if (content.indexOf('offline') > 0) {
                this.zendeskService.addNewFile(content, name, type)
                    .then(function (responseData) {
                    if (responseData !== undefined && responseData !== null) {
                        back(JSON.parse(responseData).upload);
                    }
                }, function (error) {
                    _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                        _this.errorMessage = res;
                        var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1000, 50);
                        _this.sub = timer.subscribe(function (t) {
                            _this.errorMessage = "";
                            _this.sub.unsubscribe();
                        });
                        back(false);
                    });
                });
            }
            else {
                this.translate.get('Instance seems to be offline').subscribe(function (res) {
                    _this.errorMessage = res;
                    var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1000, 50);
                    _this.sub = timer.subscribe(function (t) {
                        _this.errorMessage = "";
                        _this.sub.unsubscribe();
                    });
                    back(false);
                });
            }
        }
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
                        var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1000, 50);
                        _this.sub = timer.subscribe(function (t) {
                            _this.errorMessage = "";
                            _this.sub.unsubscribe();
                        });
                        back(false);
                    });
                }
                else {
                    back(data);
                }
            });
        }
        else {
            this.payaraService.getFile(url)
                .then(function (responseData) {
                if (responseData !== undefined && responseData !== null) {
                    back(responseData);
                }
                else {
                    _this.translate.get('Empty data2').subscribe(function (res) {
                        _this.errorMessage = res;
                        var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1000, 50);
                        _this.sub = timer.subscribe(function (t) {
                            _this.errorMessage = "";
                            _this.sub.unsubscribe();
                        });
                        back(false);
                    });
                }
            }, function (error) {
                _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                    _this.errorMessage = res;
                    var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1000, 50);
                    _this.sub = timer.subscribe(function (t) {
                        _this.errorMessage = "";
                        _this.sub.unsubscribe();
                    });
                    back(false);
                });
            });
        }
    };
    /**
     * showEditor - Function to show/hide xml editor
     */
    AddFileComponent.prototype.showEditor = function () {
        if (this.xmlLoading === 'LOADING') {
            this.isVisibleEditor = false;
            this.xmlColor = 'default';
            this.xmlLoading = 'NO';
        }
        else if (this.xmlLoading === 'YES') {
            this.removed.emit('Domain.xml');
            this.isVisibleEditor = false;
            this.xmlColor = 'default';
            this.xmlLoading = 'NO';
        }
        else if (this.xmlLoading === 'NO') {
            this.isVisibleEditor = true;
            this.xmlColor = 'warning';
            this.xmlLoading = 'LOADING';
        }
    };
    /**
     * discardXml - Function to discard changes made to xml file shown
     */
    AddFileComponent.prototype.discardXml = function () {
        this.loadFileData();
        this.xmlLoaded = false;
        this.showEditor();
    };
    /**
     * loadFileData - Function to discard changes made to xml file shown
     */
    AddFileComponent.prototype.loadFileData = function () {
        var _this = this;
        this.getFile('configs/config/server-config/zendesk-support-configuration/get-domain-xml.json', false, function (response) {
            if (response !== undefined && response !== null) {
                _this.tinymce.remove('#' + _this.elementId);
                _this.editor = null;
                var initObject = {
                    relative_urls: false,
                    remove_script_host: false,
                    document_base_url: null,
                    skin_url: '../assets/skins/lightgray',
                    selector: '#' + _this.elementId,
                    plugins: ['paste', 'searchreplace'],
                    elementpath: false,
                    height: 300,
                    menubar: false,
                    toolbar: false,
                    statusbar: false,
                    forced_root_block: false,
                    setup: function (editor) { _this.editor = editor; },
                };
                if (_this.environment.production) {
                    initObject.relative_urls = false;
                    initObject.remove_script_host = false;
                    initObject.document_base_url = window.location.protocol + '//' + window.location.hostname + ':' + window["globalPort"] + '/resource/payara_support/zendesk/';
                    initObject.skin_url = './assets/skins/lightgray';
                }
                _this.tinymce.init(initObject);
                _this.tinymce.activeEditor.setContent(response.json().message.replace(/>/g, '&gt;').replace(/</g, '<br>&lt;').replace('<br>', ''));
            }
            /*else{
              this.translate.get('Empty data').subscribe((res: string) => {
                  this.errorMessage = res;
                  this.isVisibleEditor = false;
                  this.xmlColor = 'default';
                  this.xmlLoading = 'NO';
              });
            }*/
        });
    };
    /**
     * saveXml - Function to save changes made to xml file shown
     */
    AddFileComponent.prototype.saveXml = function () {
        var _this = this;
        this.loadingMessageXml = "Loading Domain.xml file ...";
        this.addFile(this.editor.getContent().replace(/&gt;/g, '>').replace(/&lt;/g, '<').replace(/<br \/>/g, '\n'), 'Domain.xml', 'application/binary', function (response) {
            _this.saved.emit(response);
            _this.filesSaved = true;
            _this.isVisibleEditor = false;
            _this.xmlColor = 'success';
            _this.xmlLoading = 'YES';
            _this.tinymce.activeEditor.setMode('readonly');
            _this.loadingMessageXml = null;
            _this.successMessage = "";
            _this.translate.get('File added successfully!').subscribe(function (res) {
                _this.successMessage = res;
            });
            var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1000, 50);
            _this.sub = timer.subscribe(function (t) {
                _this.successMessage = "";
                _this.sub.unsubscribe();
            });
        });
    };
    AddFileComponent.prototype.otherFile = function (event) {
        var _this = this;
        var fileList = event.target.files;
        if (fileList.length > 0 && fileList[0].size < 20971520) {
            var file = fileList[0];
            var formData = new FormData();
            formData.append('upload', file, file.name);
            this.addFile(formData, file.name, 'application/binary', function (response) {
                _this.saved.emit(response);
                _this.successMessage = "";
                _this.translate.get('File added successfully!').subscribe(function (res) {
                    _this.successMessage = res;
                });
                var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1000, 50);
                _this.sub = timer.subscribe(function (t) {
                    _this.successMessage = "";
                    _this.sub.unsubscribe();
                });
            });
        }
        else {
            this.translate.get('Error! File not exist or size bigger than 20 MB').subscribe(function (res) {
                _this.errorMessage = res;
                var timer = __WEBPACK_IMPORTED_MODULE_1_rxjs_Rx__["Observable"].timer(1500, 50);
                _this.sub = timer.subscribe(function (t) {
                    _this.errorMessage = "";
                    _this.sub.unsubscribe();
                });
            });
        }
    };
    return AddFileComponent;
}());
__decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
    __metadata("design:type", String)
], AddFileComponent.prototype, "title", void 0);
__decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
    __metadata("design:type", Object)
], AddFileComponent.prototype, "saved", void 0);
__decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
    __metadata("design:type", Object)
], AddFileComponent.prototype, "removed", void 0);
AddFileComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
        selector: 'app-add-file',
        template: __webpack_require__(386),
        styles: [__webpack_require__(367)],
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_7__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_7__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_9__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_9__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_10__services_payara_service__["a" /* PayaraService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_10__services_payara_service__["a" /* PayaraService */]) === "function" && _c || Object])
], AddFileComponent);

var _a, _b, _c;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/add-file.component.js.map

/***/ }),

/***/ 108:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(20);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__(19);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_rxjs_add_operator_switchMap__ = __webpack_require__(246);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_rxjs_add_operator_switchMap___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_4_rxjs_add_operator_switchMap__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__ticket_data_ticket_data_component__ = __webpack_require__(112);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__services_zendesk_service__ = __webpack_require__(24);
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
        this.ticketDataComponent.addFiles(ticket.comment.attachments);
    };
    return DetailedTicketComponent;
}());
__decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["ViewChild"])(__WEBPACK_IMPORTED_MODULE_5__ticket_data_ticket_data_component__["a" /* TicketDataComponent */]),
    __metadata("design:type", typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_5__ticket_data_ticket_data_component__["a" /* TicketDataComponent */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_5__ticket_data_ticket_data_component__["a" /* TicketDataComponent */]) === "function" && _a || Object)
], DetailedTicketComponent.prototype, "ticketDataComponent", void 0);
DetailedTicketComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
        selector: 'app-detailed-ticket',
        template: __webpack_require__(388),
        styles: [__webpack_require__(369)]
    }),
    __metadata("design:paramtypes", [typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_6__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_6__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_2__angular_common__["DatePipe"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__angular_common__["DatePipe"]) === "function" && _d || Object])
], DetailedTicketComponent);

var _a, _b, _c, _d;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/detailed-ticket.component.js.map

/***/ }),

/***/ 109:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(20);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__services_zendesk_service__ = __webpack_require__(24);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__services_login_service__ = __webpack_require__(32);
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
        this.supportType = 'basic';
    }
    /**
     * ngOnDestroy - OnDestroy method of the component
     */
    ListTicketsComponent.prototype.ngOnDestroy = function () {
        __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__('.ng-tool-tip-content').hide();
    };
    ListTicketsComponent.prototype.hideMessage = function () {
        this.showMessage = false;
        localStorage.setItem('showMessage', JSON.stringify(false));
    };
    /**
     * ngOnInit - OnInit method of the component
     */
    ListTicketsComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.showMessage = localStorage.getItem('showMessage') !== 'null' ? false : true;
        this.loginService.initiating = true;
        this.sort = {
            column: 'id',
            descending: true
        };
        this.query = '';
        this.statusFilter = 'any';
        if (localStorage.getItem('currentUser') !== undefined && localStorage.getItem('currentUser') !== null) {
            this.user = JSON.parse(localStorage.getItem('currentUser'));
        }
        else {
            if (this.loginService.user !== undefined) {
                this.user = this.loginService.user;
            }
        }
        if (this.user !== undefined && this.user.token !== undefined && this.user.token !== '') {
            var type = JSON.stringify(this.user.tags);
            if (type !== undefined) {
                if (type.indexOf('professional') >= 0) {
                    this.supportType = 'professional';
                }
                else if (type.indexOf('enterprise') >= 0) {
                    this.supportType = 'enterprise';
                }
                else if (type.indexOf('developer') >= 0) {
                    this.supportType = 'developer';
                }
            }
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
            }, function (error) {
                _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                    _this.errorMessage = res;
                });
            });
            this.updateTickets(this.zendeskService.organization !== undefined ? !this.zendeskService.organization : true);
        }
        else {
            this.loginService.initiating = false;
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
        template: __webpack_require__(389),
        styles: [__webpack_require__(370)]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_4__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_5__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_5__services_login_service__["a" /* LoginService */]) === "function" && _e || Object])
], ListTicketsComponent);

var _a, _b, _c, _d, _e;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/list-tickets.component.js.map

/***/ }),

/***/ 110:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(20);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_crypto_js_crypto_js__ = __webpack_require__(128);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_crypto_js_crypto_js___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_4_crypto_js_crypto_js__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__ = __webpack_require__(24);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__services_login_service__ = __webpack_require__(32);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__services_payara_service__ = __webpack_require__(72);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__classes_user__ = __webpack_require__(71);
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
        var regExpEmail = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        if (user !== undefined && user !== null && user.email !== undefined && user.email !== '' && regExpEmail.test(user.email) && user.password !== undefined && user.password !== '') {
            this.loginService.getOauthToken(user.email, user.password)
                .then(function (responseData) {
                if (responseData !== undefined && responseData !== null && responseData !== '') {
                    var encryptedData_1 = __WEBPACK_IMPORTED_MODULE_4_crypto_js_crypto_js__["AES"].encrypt(user.email + '|' + responseData, 'payara').toString();
                    _this.loginService.connectionData.OauthToken = responseData;
                    _this.loginService.getUserData()
                        .then(function (responseData) {
                        if (responseData !== undefined && responseData !== null && responseData.id !== null) {
                            _this.user = new __WEBPACK_IMPORTED_MODULE_8__classes_user__["a" /* User */]();
                            _this.user = responseData;
                            _this.user.token = _this.loginService.connectionData.OauthToken;
                            _this.loginService.user = _this.user;
                            localStorage.setItem('currentUser', JSON.stringify(_this.user));
                            _this.zendeskService.OAuthToken = _this.loginService.connectionData.OauthToken;
                            _this.payaraService.setStoredEmail(encryptedData_1)
                                .then(function (responseData) {
                                if (responseData !== undefined && responseData !== null && responseData.exit_code === "SUCCESS") {
                                    localStorage.setItem('showMessage', null);
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
        template: __webpack_require__(390),
        styles: [__webpack_require__(371)]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_6__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_6__services_login_service__["a" /* LoginService */]) === "function" && _e || Object, typeof (_f = typeof __WEBPACK_IMPORTED_MODULE_7__services_payara_service__["a" /* PayaraService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_7__services_payara_service__["a" /* PayaraService */]) === "function" && _f || Object])
], LoginComponent);

var _a, _b, _c, _d, _e, _f;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/login.component.js.map

/***/ }),

/***/ 111:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(20);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__(19);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_forms__ = __webpack_require__(51);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_jquery_dist_jquery__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_5_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_rxjs_Rx__ = __webpack_require__(94);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_rxjs_Rx___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_6_rxjs_Rx__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__ = __webpack_require__(24);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__services_login_service__ = __webpack_require__(32);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__classes_ticket__ = __webpack_require__(70);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10__classes_user__ = __webpack_require__(71);
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
        if (this.sub)
            this.sub.unsubscribe();
    };
    /**
     * initTicket - Method to initiate the new ticket data
     */
    NewTicketComponent.prototype.initTicket = function () {
        this.ticketForm = this.fb.group({
            subject: ['', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["Validators"].required]],
            description: ['', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["Validators"].required]],
            type: ['problem', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["Validators"].required]],
            environment: ['prod', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["Validators"].required]],
            priority: ['normal', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["Validators"].required]],
            version: ['', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["Validators"].required]]
        });
    };
    /**
     * discardChanges - Method to discard the new ticket data and return to the list of tickets
     */
    NewTicketComponent.prototype.discardChanges = function () {
        this.router.navigate(['/list']);
    };
    /**
     * onRemovedAttachment - Method to remove files from a files array
     *
     * @param {string}  removedAttachment String with name of the file to remove
     */
    NewTicketComponent.prototype.onRemovedAttachment = function (removedAttachment) {
        if (this.newAttachments.length > 0) {
            this.newAttachments = this.newAttachments.filter(function (data) { return data.attachment.file_name.indexOf(removedAttachment) === -1; });
        }
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
                    _this.successMessage = "";
                    _this.translate.get('Request added successfully!').subscribe(function (res) {
                        _this.successMessage = res;
                    });
                    var timer = __WEBPACK_IMPORTED_MODULE_6_rxjs_Rx__["Observable"].timer(1000, 50);
                    _this.sub = timer.subscribe(function (t) {
                        _this.successMessage = "";
                        _this.router.navigate(['/list']);
                    });
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
        template: __webpack_require__(391),
        styles: [__webpack_require__(372)]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_3__angular_forms__["FormBuilder"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_3__angular_forms__["FormBuilder"]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_8__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_8__services_login_service__["a" /* LoginService */]) === "function" && _e || Object, typeof (_f = typeof __WEBPACK_IMPORTED_MODULE_2__angular_common__["DatePipe"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__angular_common__["DatePipe"]) === "function" && _f || Object])
], NewTicketComponent);

var _a, _b, _c, _d, _e, _f;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/new-ticket.component.js.map

/***/ }),

/***/ 112:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__ngx_translate_core__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_jquery_dist_jquery__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__ = __webpack_require__(24);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__services_login_service__ = __webpack_require__(32);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__classes_ticket__ = __webpack_require__(70);
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
        this.files = [];
        this.zendeskService.getTicketComments(this.ticket.id)
            .then(function (responseData) {
            if (responseData !== undefined && responseData !== null) {
                responseData.forEach(function (comment) {
                    if (comment.attachments) {
                        comment.attachments.forEach(function (file) {
                            _this.files.push(file);
                        });
                    }
                });
            }
        }, function (error) {
            _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                _this.errorMessage = res;
            });
        });
        this.zendeskService.getUserIdentity(this.ticket.requester_id)
            .then(function (responseData) {
            if (responseData !== undefined && responseData !== null) {
                _this.ticket.submitter_name = responseData.name;
            }
        }, function (error) {
            _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                _this.errorMessage = res;
            });
        });
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
    TicketDataComponent.prototype.addFiles = function (files) {
        var _this = this;
        if (files) {
            files.forEach(function (file) {
                _this.files.push(file);
            });
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
        template: __webpack_require__(392),
        styles: [__webpack_require__(373)]
    }),
    __metadata("design:paramtypes", [typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__ngx_translate_core__["c" /* TranslateService */]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_4__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__services_login_service__["a" /* LoginService */]) === "function" && _d || Object])
], TicketDataComponent);

var _a, _b, _c, _d;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/ticket-data.component.js.map

/***/ }),

/***/ 113:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(20);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__services_login_service__ = __webpack_require__(32);
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
            this.loginService.user = JSON.parse(localStorage.getItem('currentUser'));
            return true;
        }
        else {
            // not logged in so redirect to login page with the return url
            this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
            return false;
        }
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

/***/ 24:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_http__ = __webpack_require__(52);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_jquery__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_add_operator_toPromise__ = __webpack_require__(61);
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
                "content-type": contentType + ';charset=UTF-8'
            },
            "data": input,
            "cache": false,
            "contentType": false,
            "processData": false,
            "mimeType": "multipart/form-data"
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

/***/ 283:
/***/ (function(module, exports) {

function webpackEmptyContext(req) {
	throw new Error("Cannot find module '" + req + "'.");
}
webpackEmptyContext.keys = function() { return []; };
webpackEmptyContext.resolve = webpackEmptyContext;
module.exports = webpackEmptyContext;
webpackEmptyContext.id = 283;


/***/ }),

/***/ 284:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser_dynamic__ = __webpack_require__(298);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__environments_environment__ = __webpack_require__(53);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__app_app_module__ = __webpack_require__(304);




if (__WEBPACK_IMPORTED_MODULE_2__environments_environment__["a" /* environment */].production) {
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_1__angular_core__["enableProdMode"])();
}
__webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_platform_browser_dynamic__["a" /* platformBrowserDynamic */])().bootstrapModule(__WEBPACK_IMPORTED_MODULE_3__app_app_module__["a" /* AppModule */]);
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/main.js.map

/***/ }),

/***/ 303:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(20);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_moment__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_moment___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_moment__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__environments_environment__ = __webpack_require__(53);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_crypto_js_crypto_js__ = __webpack_require__(128);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_crypto_js_crypto_js___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_5_crypto_js_crypto_js__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__services_login_service__ = __webpack_require__(32);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__ = __webpack_require__(24);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__services_payara_service__ = __webpack_require__(72);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__classes_user__ = __webpack_require__(71);
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










//import * as pnp from "sp-pnp-js";
//import * as spauth from 'node-sp-auth';
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
            shopURL: this.environment.payara.shopUrl,
            supportGuideURL: this.environment.supportGuides,
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
        this.loginService.initiating = true;
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
                                localStorage.setItem('showMessage', null);
                                _this.router.navigate(['/list']);
                            }
                            else {
                                _this.user = new __WEBPACK_IMPORTED_MODULE_9__classes_user__["a" /* User */]();
                                _this.zendeskService.OAuthToken = '';
                                _this.loginService.user = _this.user;
                            }
                        }, function (error) {
                            _this.user = new __WEBPACK_IMPORTED_MODULE_9__classes_user__["a" /* User */]();
                            _this.zendeskService.OAuthToken = '';
                            _this.loginService.user = _this.user;
                        });
                    }
                }
                else {
                    _this.loginService.initiating = false;
                    _this.router.navigate(['/login']);
                }
            }, function (error) {
                _this.user = new __WEBPACK_IMPORTED_MODULE_9__classes_user__["a" /* User */]();
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
        this.loginService.initiating = false;
        this.user = new __WEBPACK_IMPORTED_MODULE_9__classes_user__["a" /* User */]();
        this.loginService.user = this.user;
        localStorage.setItem('currentUser', JSON.stringify(this.user));
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
        template: __webpack_require__(385),
        styles: [__webpack_require__(366)],
        providers: [__WEBPACK_IMPORTED_MODULE_6__services_login_service__["a" /* LoginService */], __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__["a" /* ZendeskService */], __WEBPACK_IMPORTED_MODULE_8__services_payara_service__["a" /* PayaraService */]]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_6__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_6__services_login_service__["a" /* LoginService */]) === "function" && _e || Object, typeof (_f = typeof __WEBPACK_IMPORTED_MODULE_8__services_payara_service__["a" /* PayaraService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_8__services_payara_service__["a" /* PayaraService */]) === "function" && _f || Object])
], AppComponent);

var _a, _b, _c, _d, _e, _f;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/app.component.js.map

/***/ }),

/***/ 304:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser__ = __webpack_require__(31);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_forms__ = __webpack_require__(51);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_http__ = __webpack_require__(52);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__angular_router__ = __webpack_require__(20);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__angular_common__ = __webpack_require__(19);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__ngx_translate_core__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__ngx_translate_http_loader__ = __webpack_require__(309);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8_ngx_modal__ = __webpack_require__(378);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8_ngx_modal___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_8_ngx_modal__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9_angular2_tooltip__ = __webpack_require__(300);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10_jw_bootstrap_switch_ng2__ = __webpack_require__(376);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10_jw_bootstrap_switch_ng2___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_10_jw_bootstrap_switch_ng2__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_11__angular_platform_browser_animations__ = __webpack_require__(299);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_12__environments_environment__ = __webpack_require__(53);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_13__app_component__ = __webpack_require__(303);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_14__app_routing__ = __webpack_require__(305);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_15__components_login_login_component__ = __webpack_require__(110);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_16__components_list_tickets_list_tickets_component__ = __webpack_require__(109);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_17__components_new_ticket_new_ticket_component__ = __webpack_require__(111);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_18__components_detailed_ticket_detailed_ticket_component__ = __webpack_require__(108);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_19__components_add_file_add_file_component__ = __webpack_require__(107);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_20__components_ticket_data_ticket_data_component__ = __webpack_require__(112);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_21__components_comment_data_comment_data_component__ = __webpack_require__(307);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_22__services_login_service__ = __webpack_require__(32);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_23__guards_auth_guard__ = __webpack_require__(113);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_24__pipes_day_time_pipe__ = __webpack_require__(308);
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
if (__WEBPACK_IMPORTED_MODULE_12__environments_environment__["a" /* environment */].production) {
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
            __WEBPACK_IMPORTED_MODULE_13__app_component__["a" /* AppComponent */],
            __WEBPACK_IMPORTED_MODULE_15__components_login_login_component__["a" /* LoginComponent */],
            __WEBPACK_IMPORTED_MODULE_16__components_list_tickets_list_tickets_component__["a" /* ListTicketsComponent */],
            __WEBPACK_IMPORTED_MODULE_17__components_new_ticket_new_ticket_component__["a" /* NewTicketComponent */],
            __WEBPACK_IMPORTED_MODULE_18__components_detailed_ticket_detailed_ticket_component__["a" /* DetailedTicketComponent */],
            __WEBPACK_IMPORTED_MODULE_19__components_add_file_add_file_component__["a" /* AddFileComponent */],
            __WEBPACK_IMPORTED_MODULE_20__components_ticket_data_ticket_data_component__["a" /* TicketDataComponent */],
            __WEBPACK_IMPORTED_MODULE_21__components_comment_data_comment_data_component__["a" /* CommentDataComponent */],
            __WEBPACK_IMPORTED_MODULE_24__pipes_day_time_pipe__["a" /* DayTimePipe */]
        ],
        imports: [
            __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser__["a" /* BrowserModule */],
            __WEBPACK_IMPORTED_MODULE_11__angular_platform_browser_animations__["a" /* BrowserAnimationsModule */],
            __WEBPACK_IMPORTED_MODULE_10_jw_bootstrap_switch_ng2__["JWBootstrapSwitchModule"],
            __WEBPACK_IMPORTED_MODULE_2__angular_forms__["FormsModule"],
            __WEBPACK_IMPORTED_MODULE_2__angular_forms__["ReactiveFormsModule"],
            __WEBPACK_IMPORTED_MODULE_3__angular_http__["a" /* HttpModule */],
            __WEBPACK_IMPORTED_MODULE_8_ngx_modal__["ModalModule"],
            __WEBPACK_IMPORTED_MODULE_9_angular2_tooltip__["a" /* ToolTipModule */],
            __WEBPACK_IMPORTED_MODULE_14__app_routing__["a" /* routing */],
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
            __WEBPACK_IMPORTED_MODULE_24__pipes_day_time_pipe__["a" /* DayTimePipe */],
            __WEBPACK_IMPORTED_MODULE_6__ngx_translate_core__["a" /* TranslateModule */]
        ],
        providers: [
            __WEBPACK_IMPORTED_MODULE_5__angular_common__["DatePipe"],
            __WEBPACK_IMPORTED_MODULE_24__pipes_day_time_pipe__["a" /* DayTimePipe */],
            __WEBPACK_IMPORTED_MODULE_23__guards_auth_guard__["a" /* AuthGuard */],
            __WEBPACK_IMPORTED_MODULE_22__services_login_service__["a" /* LoginService */]
        ],
        bootstrap: [__WEBPACK_IMPORTED_MODULE_13__app_component__["a" /* AppComponent */]]
    })
], AppModule);

//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/app.module.js.map

/***/ }),

/***/ 305:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_router__ = __webpack_require__(20);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__components_login_login_component__ = __webpack_require__(110);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__components_list_tickets_list_tickets_component__ = __webpack_require__(109);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__components_new_ticket_new_ticket_component__ = __webpack_require__(111);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__components_detailed_ticket_detailed_ticket_component__ = __webpack_require__(108);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__guards_auth_guard__ = __webpack_require__(113);
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

/***/ 306:
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

/***/ 307:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common__ = __webpack_require__(19);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_Rx__ = __webpack_require__(94);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_Rx___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_rxjs_Rx__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_jquery_dist_jquery__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_4_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__ = __webpack_require__(24);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__add_file_add_file_component__ = __webpack_require__(107);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__classes_ticket__ = __webpack_require__(70);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__classes_comment__ = __webpack_require__(306);
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
        __WEBPACK_IMPORTED_MODULE_4_jquery_dist_jquery__('.ng-tool-tip-content').hide();
        if (this.sub)
            this.sub.unsubscribe();
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
                responseData.sort(function (a, b) {
                    if (a['created_at'] > b['created_at'])
                        return -1;
                    else if (a['created_at'] < b['created_at'])
                        return 1;
                    else
                        return 0;
                });
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
        this.successMessage = '';
    };
    /**
     * onRemovedAttachment - Method to remove files from a files array
     *
     * @param {string}  removedAttachment String with name of the file to remove
     */
    CommentDataComponent.prototype.onRemovedAttachment = function (removedAttachment) {
        var _this = this;
        if (this.newAttachments.length > 0) {
            this.newAttachments = this.newAttachments.filter(function (data) { return data.attachment.file_name.indexOf(removedAttachment) === -1; });
        }
        if (this.newCommentText) {
            var comments = this.newCommentText.split(' -------------- ')[0];
            var addedFiles = this.newCommentText.split(' -------------- ')[1].split('\n');
            this.newCommentText = comments.trim();
            if (addedFiles.length > 2) {
                this.newCommentText += '\n -------------- \n';
                addedFiles.forEach(function (line) {
                    if ('*' + line + '*' !== '**' && line.indexOf(removedAttachment) === -1) {
                        _this.newCommentText += line + '\n';
                    }
                });
            }
            if (this.newCommentText.indexOf('- File:') === -1) {
                this.newCommentText = comments;
            }
        }
    };
    /**
     * onSavedAttachment - Method to add new files to a files array
     *
     * @param {Attachment}  newAttachment Object with a new file to attach
     */
    CommentDataComponent.prototype.onSavedAttachment = function (newAttachment) {
        if (newAttachment.attachment !== null) {
            this.newAttachments.push(newAttachment);
            if (this.newAttachments.length === 1) {
                this.newCommentText = this.newCommentText !== undefined ? this.newCommentText : "";
                if ('*' + this.newCommentText + '*' !== '**') {
                    this.newCommentText = this.newCommentText.trim();
                }
                this.newCommentText += '\n -------------- \n';
            }
            else if (this.newAttachments.length > 1) {
                var comments = this.newCommentText.split(' -------------- ')[0].trim();
                var files = this.newCommentText.split(' -------------- ')[1];
                this.newCommentText = comments;
                this.newCommentText += '\n -------------- ';
                this.newCommentText += files;
            }
            this.newCommentText += '- File: ' + newAttachment['attachment'].file_name + ' added!\n';
        }
        else {
            this.addFilecomponent.cleanFiles();
            this.newAttachments = [];
            this.newCommentText = this.newCommentText.split(' -------------- ')[0].trim();
        }
    };
    /**
     * saveComment - Method to save new comments
     */
    CommentDataComponent.prototype.saveComment = function () {
        var _this = this;
        if (this.newCommentText !== undefined && this.newCommentText !== null && this.newCommentText !== '') {
            var newComment_1 = new __WEBPACK_IMPORTED_MODULE_8__classes_comment__["a" /* Comment */]();
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
            this.comments.sort(function (a, b) {
                if (a['created_at'] > b['created_at'])
                    return -1;
                else if (a['created_at'] < b['created_at'])
                    return 1;
                else
                    return 0;
            });
            this.saved.emit(newComment_1);
            this.addFilecomponent.cleanFiles();
            this.newCommentText = '';
            this.newAttachments = [];
            this.successMessage = "";
            this.translate.get('Comment added successfully!').subscribe(function (res) {
                _this.successMessage = res;
            });
            var timer = __WEBPACK_IMPORTED_MODULE_3_rxjs_Rx__["Observable"].timer(1000, 50);
            this.sub = timer.subscribe(function (t) {
                _this.successMessage = "";
                _this.sub.unsubscribe();
            });
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
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["ViewChild"])(__WEBPACK_IMPORTED_MODULE_6__add_file_add_file_component__["a" /* AddFileComponent */]),
    __metadata("design:type", typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_6__add_file_add_file_component__["a" /* AddFileComponent */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_6__add_file_add_file_component__["a" /* AddFileComponent */]) === "function" && _a || Object)
], CommentDataComponent.prototype, "addFilecomponent", void 0);
__decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
    __metadata("design:type", Object)
], CommentDataComponent.prototype, "saved", void 0);
__decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Input"])(),
    __metadata("design:type", typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_7__classes_ticket__["a" /* Ticket */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_7__classes_ticket__["a" /* Ticket */]) === "function" && _b || Object)
], CommentDataComponent.prototype, "ticket", void 0);
__decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Output"])(),
    __metadata("design:type", String)
], CommentDataComponent.prototype, "newCommentText", void 0);
CommentDataComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
        selector: 'app-comment-data',
        template: __webpack_require__(387),
        styles: [__webpack_require__(368)]
    }),
    __metadata("design:paramtypes", [typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_1__angular_common__["DatePipe"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_common__["DatePipe"]) === "function" && _e || Object])
], CommentDataComponent);

var _a, _b, _c, _d, _e;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/comment-data.component.js.map

/***/ }),

/***/ 308:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_moment__ = __webpack_require__(1);
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

/***/ 32:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_http__ = __webpack_require__(52);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_toPromise__ = __webpack_require__(61);
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
        this.initiating = false;
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

/***/ 366:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(15)();
// imports


// module
exports.push([module.i, ".logout {\n  margin-top: -1.75rem;\n}\n\n.appTittle {\n  margin-left: 1.5rem;\n}\n\n.username{\n  font-size: small;\n  font-style: oblique;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 367:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(15)();
// imports


// module
exports.push([module.i, "span.glyphicon.glyphicon-paperclip{\n  background-color: transparent;\n}\n.btn.btn-sm.btn-default.btn-block.add-file-button{\n  margin-bottom:.5rem;\n  background-color: transparent;\n}\n.btn.btn-sm.btn-default.btn-block.add-file-button span.glyphicon.glyphicon-paperclip{\n  background-color: transparent;\n}\n.add-file-server-button{\n  width: 7rem;\n  height: 2rem;\n  white-space: normal;\n  font-size: smaller;\n  margin:.25rem;\n  float: left!important;\n}\nspan.sr-only{\n  position: relative;\n  background-color: transparent;\n  color: #f0981b;\n}\n.panel-footer{\n  background-color: #E6E6E6;\n}\n.domain-block{\n  margin-top: 2rem;\n\n}\n.objectVisible{\n  display:block;;\n}\n.objectNoVisible{\n  display: none;\n}\n.groupSeparation{\n  border:none;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 368:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(15)();
// imports


// module
exports.push([module.i, ".new_comment{\n  resize: none;\n  outline: none;\n  width: 100%;\n  padding: 10px;\n  border: none;\n  height: 100%;\n  border-radius: 5px;\n  background-color: #ffffff;\n  margin-bottom: .5rem;\n  height: 7.5rem;\n}\n\n.comment_box{\n  background-color: #ffffff;\n  margin-bottom: .25rem;\n  padding: .25rem;\n}\n\npre code {\n    padding: 0;\n    font-size: .75rem;\n    margin-left: -1.5rem;\n}\n\n.attached-file {\n  color: #ffffff;\n  padding: .25rem;\n  border-radius: .25rem;\n  margin: .25rem;\n  font-style: oblique;\n  font-size: small;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 369:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(15)();
// imports


// module
exports.push([module.i, ".returnBack {\n  margin-top: -.5rem;\n}\n\nbutton.btn.btn-sm.pull-right.returnBack{\n  /*max-width: 2rem;*/\n  max-height: 1.75rem\n}\n\nspan.glyphicon.glyphicon-chevron-left{\n  background-color: transparent;\n}\n\nspan.glyphicon.glyphicon-chevron-left::before{\n    margin-left: -.15rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 370:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(15)();
// imports


// module
exports.push([module.i, ".addTicket {\n  margin-top: -.5rem;\n}\n\nbutton.btn.btn-sm.pull-right.addTicket{\n  /*max-width: 2rem;*/\n  max-height: 1.75rem\n}\n\nspan.glyphicon.glyphicon-chevron-up,\nspan.glyphicon.glyphicon-chevron-down,\nspan.glyphicon.glyphicon-plus{\n  background-color: transparent;\n  margin-left: .15rem;\n}\n\nspan.glyphicon.glyphicon-plus::before{\n  margin-left: -.15rem;\n}\n\n\ntable.table-responsive.table-striped.table-md.table-inverse.table-sortable{\n  margin-top:3rem;\n}\n\n.selectable{\n    cursor: pointer;\n}\n\n.transparent{\n  background-color: transparent;\n}\n\n.close{\n  opacity:.45;\n}\n.closeMessage{\n  color: red;\n  font-size: x-large;\n}\n\n.supportGuide{\n  text-decoration:none;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 371:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(15)();
// imports


// module
exports.push([module.i, "button.btn.btn-sm.btn-primary.btn-block{\n  margin-top:.75rem;\n}\n\n#inputEmail{\n  margin-top:.5rem;\n}\n\n.transparent{\n  background-color: transparent;\n  color:#03354a;\n}\n\n.form-signin-heading{\n  color:#011720;\n  font-weight: 600;\n}\n\n.info{\n  color:#0674a1;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 372:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(15)();
// imports


// module
exports.push([module.i, ".form-group.required .control-label:after {\n    color: #d00;\n    content: \"*\";\n    margin-left: .5rem;\n}\n\n.form-group .control-label,\n.form-group.required .control-label {\n  margin-bottom:.25rem;\n}\n\n.discardChanges {\n  margin-top: -.5rem;\n}\n\nbutton.btn.btn-sm.pull-right.discardChanges,\nbutton.btn.btn-sm.pull-right.discardChanges{\n  /*max-width: 2rem;*/\n  max-height: 1.75rem;;\n}\n\nspan.glyphicon.glyphicon-remove-circle,\nspan.glyphicon.glyphicon-ok-circle{\n  background-color: transparent;\n  margin-left: .15rem\n}\n\nspan.glyphicon.glyphicon-remove-circle::before,\nspan.glyphicon.glyphicon-ok-circle::before{\n    margin-left: -.15rem;\n}\n.attached-file-list {\n  color: #ffffff;\n  padding: .25rem;\n  border-radius: .25rem;\n  margin: .25rem;\n  margin-left: .25rem;\n  font-style: oblique;\n  font-size: small;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 373:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(15)();
// imports


// module
exports.push([module.i, ".detailContent {\n  border-left: thin solid #f0981b;\n  margin-left:1.5rem;\n  margin-top:1rem;\n}\n\n.detailData{\n  margin:.5rem;\n  padding: .25rem;\n  letter-spacing: .05rem;\n  display:-webkit-box;\n  display:-ms-flexbox;\n  display:flex;\n}\n\n.titleData{\n  margin-top:.25rem;\n}\n\n.fileList{\n  margin:.5rem;\n  padding: .25rem;\n  letter-spacing: .05rem;\n  display:block;\n}\n\n.attached-file-list {\n  color: #ffffff;\n  padding: .25rem;\n  border-radius: .25rem;\n  margin: .25rem;\n  margin-left: .25rem;\n  font-style: oblique;\n  font-size: small;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 377:
/***/ (function(module, exports, __webpack_require__) {

var map = {
	"./af": 130,
	"./af.js": 130,
	"./ar": 136,
	"./ar-dz": 131,
	"./ar-dz.js": 131,
	"./ar-ly": 132,
	"./ar-ly.js": 132,
	"./ar-ma": 133,
	"./ar-ma.js": 133,
	"./ar-sa": 134,
	"./ar-sa.js": 134,
	"./ar-tn": 135,
	"./ar-tn.js": 135,
	"./ar.js": 136,
	"./az": 137,
	"./az.js": 137,
	"./be": 138,
	"./be.js": 138,
	"./bg": 139,
	"./bg.js": 139,
	"./bn": 140,
	"./bn.js": 140,
	"./bo": 141,
	"./bo.js": 141,
	"./br": 142,
	"./br.js": 142,
	"./bs": 143,
	"./bs.js": 143,
	"./ca": 144,
	"./ca.js": 144,
	"./cs": 145,
	"./cs.js": 145,
	"./cv": 146,
	"./cv.js": 146,
	"./cy": 147,
	"./cy.js": 147,
	"./da": 148,
	"./da.js": 148,
	"./de": 150,
	"./de-at": 149,
	"./de-at.js": 149,
	"./de.js": 150,
	"./dv": 151,
	"./dv.js": 151,
	"./el": 152,
	"./el.js": 152,
	"./en-au": 153,
	"./en-au.js": 153,
	"./en-ca": 154,
	"./en-ca.js": 154,
	"./en-gb": 155,
	"./en-gb.js": 155,
	"./en-ie": 156,
	"./en-ie.js": 156,
	"./en-nz": 157,
	"./en-nz.js": 157,
	"./eo": 158,
	"./eo.js": 158,
	"./es": 160,
	"./es-do": 159,
	"./es-do.js": 159,
	"./es.js": 160,
	"./et": 161,
	"./et.js": 161,
	"./eu": 162,
	"./eu.js": 162,
	"./fa": 163,
	"./fa.js": 163,
	"./fi": 164,
	"./fi.js": 164,
	"./fo": 165,
	"./fo.js": 165,
	"./fr": 168,
	"./fr-ca": 166,
	"./fr-ca.js": 166,
	"./fr-ch": 167,
	"./fr-ch.js": 167,
	"./fr.js": 168,
	"./fy": 169,
	"./fy.js": 169,
	"./gd": 170,
	"./gd.js": 170,
	"./gl": 171,
	"./gl.js": 171,
	"./he": 172,
	"./he.js": 172,
	"./hi": 173,
	"./hi.js": 173,
	"./hr": 174,
	"./hr.js": 174,
	"./hu": 175,
	"./hu.js": 175,
	"./hy-am": 176,
	"./hy-am.js": 176,
	"./id": 177,
	"./id.js": 177,
	"./is": 178,
	"./is.js": 178,
	"./it": 179,
	"./it.js": 179,
	"./ja": 180,
	"./ja.js": 180,
	"./jv": 181,
	"./jv.js": 181,
	"./ka": 182,
	"./ka.js": 182,
	"./kk": 183,
	"./kk.js": 183,
	"./km": 184,
	"./km.js": 184,
	"./ko": 185,
	"./ko.js": 185,
	"./ky": 186,
	"./ky.js": 186,
	"./lb": 187,
	"./lb.js": 187,
	"./lo": 188,
	"./lo.js": 188,
	"./lt": 189,
	"./lt.js": 189,
	"./lv": 190,
	"./lv.js": 190,
	"./me": 191,
	"./me.js": 191,
	"./mi": 192,
	"./mi.js": 192,
	"./mk": 193,
	"./mk.js": 193,
	"./ml": 194,
	"./ml.js": 194,
	"./mr": 195,
	"./mr.js": 195,
	"./ms": 197,
	"./ms-my": 196,
	"./ms-my.js": 196,
	"./ms.js": 197,
	"./my": 198,
	"./my.js": 198,
	"./nb": 199,
	"./nb.js": 199,
	"./ne": 200,
	"./ne.js": 200,
	"./nl": 202,
	"./nl-be": 201,
	"./nl-be.js": 201,
	"./nl.js": 202,
	"./nn": 203,
	"./nn.js": 203,
	"./pa-in": 204,
	"./pa-in.js": 204,
	"./pl": 205,
	"./pl.js": 205,
	"./pt": 207,
	"./pt-br": 206,
	"./pt-br.js": 206,
	"./pt.js": 207,
	"./ro": 208,
	"./ro.js": 208,
	"./ru": 209,
	"./ru.js": 209,
	"./se": 210,
	"./se.js": 210,
	"./si": 211,
	"./si.js": 211,
	"./sk": 212,
	"./sk.js": 212,
	"./sl": 213,
	"./sl.js": 213,
	"./sq": 214,
	"./sq.js": 214,
	"./sr": 216,
	"./sr-cyrl": 215,
	"./sr-cyrl.js": 215,
	"./sr.js": 216,
	"./ss": 217,
	"./ss.js": 217,
	"./sv": 218,
	"./sv.js": 218,
	"./sw": 219,
	"./sw.js": 219,
	"./ta": 220,
	"./ta.js": 220,
	"./te": 221,
	"./te.js": 221,
	"./tet": 222,
	"./tet.js": 222,
	"./th": 223,
	"./th.js": 223,
	"./tl-ph": 224,
	"./tl-ph.js": 224,
	"./tlh": 225,
	"./tlh.js": 225,
	"./tr": 226,
	"./tr.js": 226,
	"./tzl": 227,
	"./tzl.js": 227,
	"./tzm": 229,
	"./tzm-latn": 228,
	"./tzm-latn.js": 228,
	"./tzm.js": 229,
	"./uk": 230,
	"./uk.js": 230,
	"./uz": 231,
	"./uz.js": 231,
	"./vi": 232,
	"./vi.js": 232,
	"./x-pseudo": 233,
	"./x-pseudo.js": 233,
	"./yo": 234,
	"./yo.js": 234,
	"./zh-cn": 235,
	"./zh-cn.js": 235,
	"./zh-hk": 236,
	"./zh-hk.js": 236,
	"./zh-tw": 237,
	"./zh-tw.js": 237
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
webpackContext.id = 377;


/***/ }),

/***/ 385:
/***/ (function(module, exports) {

module.exports = "\n\n<hr>\n\n<h2>\n  <p class=\"appTittle\">{{'Payara Support' | translate}}\n    <span *ngIf=\"loginService.initiating && loginService.user\"\n          class=\"username\"> : {{loginService.user.name}} [{{loginService.user.email}}]\n    </span>\n  </p>\n  <button class=\"btn btn-default pull-right logout\" *ngIf=\"!isCurrentRoute('login')\" (click)=\"logout()\"\n          tooltip content=\"{{'Logout from Zendesk' | translate}}\">\n    {{'Logout' | translate}}\n    <span class=\"glyphicon glyphicon-off\" aria-hidden=\"true\">\n    </span>\n  </button>\n</h2>\n\n<hr>\n\n<router-outlet></router-outlet>\n"

/***/ }),

/***/ 386:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\">\n  <div class=\"panel-heading\">\n    <h1>{{title | translate}}</h1>\n  </div>\n  <div class=\"panel-body\">\n    <div class=\"row\">\n      <div class=\"col-xs-12\" *ngFor=\"let group of groups\">\n        <div class=\"col-xs-12 col-sm-6 col-lg-3\" *ngFor=\"let fileButton of filter(fileButtons,group)\">\n          <bSwitch\n                    [switch-label-text]=\"fileButton.title\"\n                    [(ngModel)]=\"fileButton.uploaded\"\n                    [switch-handle-width]=\"40\"\n                    [switch-label-width]=\"140\"\n                    [switch-animate]=\"true\"\n                    [switch-inverse]=\"true\"\n                    [switch-off-text]=\"'NO'\"\n                    [switch-on-text]=\"'YES'\"\n                    [switch-on-color]=\"'success'\"\n                    [switch-off-color]=\"'default'\"\n                    [switch-size]=\"'small'\"\n                    (onChangeState)=\"searchFile(fileButton)\">\n            </bSwitch>\n      </div>\n      <div class=\"col-xs-12\">\n        <hr class=\"groupSeparation\">\n      </div>\n    </div>\n    <div class=\"col-xs-12\">\n      <div class=\"col-xs-12 col-sm-6 col-lg-3\">\n          <bSwitch\n                    [switch-label-text]=\"'Domain.xml'\"\n                    [(ngModel)]=\"xmlLoaded\"\n                    [switch-handle-width]=\"80\"\n                    [switch-label-width]=\"140\"\n                    [switch-animate]=\"true\"\n                    [switch-inverse]=\"true\"\n                    [switch-off-text]=\"'NO'\"\n                    [switch-on-text]=\"xmlLoading\"\n                    [switch-on-color]=\"xmlColor\"\n                    [switch-off-color]=\"'default'\"\n                    [switch-size]=\"'small'\"\n                    (onChangeState)=\"showEditor()\">\n            </bSwitch>\n        </div>\n        <div class=\"col-xs-12 col-sm-6 col-lg-3\">\n          <label class=\"btn btn-sm btn-file add-file-server-button\">\n              <span class=\"glyphicon glyphicon-paperclip\" aria-hidden=\"true\"></span>\n              {{'Other File' | translate}}\n              <input type=\"file\" class=\"hidden\" (change)=\"otherFile($event)\">\n          </label>\n        </div>\n      </div>\n      <div class=\"col-xs-12\">\n        <div *ngIf=\"loadingMessage\" class=\"progress\">\n          <div class=\"progress-bar progress-bar-striped active\" role=\"progressbar\" aria-valuenow=\"100\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 100%\">\n            <span class=\"sr-only\">{{loadingMessage | translate}}</span>\n          </div>\n        </div>\n        <div *ngIf=\"errorMessage\" class=\"alert alert-danger alert-dismissible\" role=\"alert1\">\n          {{errorMessage}}\n        </div>\n        <div *ngIf=\"successMessage\" class=\"alert alert-success alert-dismissible\" role=\"alert2\">\n          {{successMessage}}\n        </div>\n      </div>\n      <div class=\"col-xs-12 domain-block\" [ngClass]=\"{\n                                              'objectVisible': isVisibleEditor,\n                                              'objectNoVisible': !isVisibleEditor\n                                            }\">\n        <div class=\"panel panel-default\">\n          <div class=\"panel-heading\">\n            <h2>{{'Edit' | translate}} Domain.xml</h2>\n          </div>\n          <div class=\"panel-body\">\n            <div class=\"row\">\n              <textarea id=\"{{elementId}}\"></textarea>\n            </div>\n          </div>\n          <div class=\"panel-footer\">\n            <div class=\"row\">\n              <div class=\"col-xs-8\">\n                <div *ngIf=\"loadingMessageXml\" class=\"progress\">\n                  <div class=\"progress-bar progress-bar-striped active\" role=\"progressbar\" aria-valuenow=\"100\" aria-valuemin=\"0\" aria-valuemax=\"100\" style=\"width: 100%\">\n                    <span class=\"sr-only\">{{loadingMessageXml | translate}}</span>\n                  </div>\n                </div>\n              </div>\n              <div class=\"col-xs-4\">\n                <button class=\"btn btn-default pull-right\" (click)=\"saveXml()\">{{'Save' | translate}}</button>\n                <button class=\"btn btn-primary pull-right\" (click)=\"discardXml()\">{{'Discard' | translate}}</button>\n              </div>\n            </div>\n          </div>\n        </div>\n      </div>\n    </div>\n  </div>\n</div>\n"

/***/ }),

/***/ 387:
/***/ (function(module, exports) {

module.exports = "<div class=\"row\">\n  <div *ngIf=\"errorMessage\" class=\"alert alert-danger alert-dismissible\" role=\"alert1\">\n    {{errorMessage}}\n  </div>\n  <div *ngIf=\"successMessage\" class=\"alert alert-success alert-dismissible\" role=\"alert2\">\n    {{successMessage}}\n  </div>\n</div>\n\n<div class=\"newCommentContainer\" *ngIf=\"ticket.status!=='closed' && ticket.status!=='solved'\">\n  <div class=\"row\">\n    <div class=\"col-md-12 col-sm-12 col-xs-12\">\n      <textarea class=\"new_comment\" [(ngModel)]=\"newCommentText\" (keyup)=\"keyUpEvent($event)\" tooltip content=\"{{'Insert comment text' | translate}}\"></textarea>\n    </div>\n    <div class=\"col-md-12 col-sm-12 col-xs-12\">\n      <button class=\"btn btn-sm btn-default btn-block\" type=\"submit\" (click)=\"saveComment()\">\n        {{'Submit' | translate}}\n        <span class=\"glyphicon glyphicon-ok\" aria-hidden=\"true\">\n        </span>\n      </button>\n    </div>\n    <div class=\"col-md-12 col-sm-12 col-xs-12\">\n      <app-add-file class=\"addFile\"\n                    (saved)=\"onSavedAttachment($event)\"\n                    (removed)=\"onRemovedAttachment($event)\"\n                    [title]=\"'Attach files to new comment of the request'\">\n      </app-add-file>\n    </div>\n  </div>\n</div>\n\n<div class=\"commentContainer\" *ngFor=\"let comment of comments\">\n  <div class=\"row\">\n    <div class=\"col-sm-9\">\n      <pre class=\"comment_box\">\n        <code>{{comment.body}}</code>\n      </pre>\n    </div>\n    <div class=\"col-sm-3\">\n      <p>{{comment.created_at | dayTime}}</p>\n    </div>\n  </div>\n  <div class=\"row\" *ngIf=\"comment.attachments !== undefined && comment.attachments.length>0\">\n    <div class=\"col-xs-12 col-sm-6 col-md-4 col-lg-2\" *ngFor=\"let file of comment.attachments\">\n      <a href=\"{{file.content_url}}\">\n        <span class=\"\tglyphicon glyphicon-download-alt attached-file\" aria-hidden=\"true\">\n          {{file.file_name}}\n        </span>\n      </a>\n    </div>\n  </div>\n  <hr>\n</div>\n"

/***/ }),

/***/ 388:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\" *ngIf=\"ticket\">\n  <div class=\"panel-heading\">\n    <h4>\n      {{'Request' | translate}} #{{ticket.id}} <strong>{{ticket.subject}}</strong>\n      <button class=\"btn btn-sm pull-right returnBack\" routerLink=\"/list\"\n              tooltip content=\"{{'Return to requests list' | translate}}\">\n        <span class=\"glyphicon glyphicon-chevron-left\" aria-hidden=\"true\">\n        </span>\n        {{'Back' | translate}}        \n      </button>\n    </h4>\n  </div>\n  <div class=\"panel-body\">\n    <div class=\"row\">\n      <div class=\"col-sm-8\">\n        <app-comment-data [(ticket)]=\"ticket\" (saved)=\"onSavedComment(ticket,$event)\"></app-comment-data>\n      </div>\n      <div class=\"col-sm-4\">\n        <app-ticket-data [(ticket)]=\"ticket\"></app-ticket-data>\n      </div>\n  </div>\n</div>\n"

/***/ }),

/***/ 389:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\" *ngIf=\"tickets\">\n  <div class=\"panel-heading\">\n    <div class=\"alert alert-info alert-dismissible\" role=\"alert\" *ngIf=\"showMessage\">\n      <button type=\"button\" class=\"close\" data-dismiss=\"alert\" aria-label=\"Close\" (click)=\"hideMessage()\">\n        <span aria-hidden=\"true\" class=\"transparent closeMessage\">&times;</span>\n      </button>\n      <p class=\"transparent\">{{'Welcome to Zendesk support for Payara Server' | translate}}!</p>\n      <br/>\n      <p class=\"transparent info\">{{'usefulInfo' | translate}}</p>\n      <br/>\n      <a class=\"btn btn-sm btn-default btn-block supportGuide\" target=\"_blank\" href=\"{{loginService.connectionData.supportGuideURL}}{{supportType}}\">{{'Support Guide' | translate}}</a>\n    </div>\n    <h4>\n      {{'My requests' | translate}}\n      <button class=\"btn btn-sm pull-right addTicket\" routerLink=\"/new\"\n              tooltip content=\"{{'Create New Request' | translate}}\">\n        {{'New Request' | translate}}\n        <span class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\">\n        </span>\n      </button>\n    </h4>\n  </div>\n  <div class=\"panel-body\">\n      <div class=\"col-sm-6\">\n        <input type=\"text\" class=\"form-control\" [(ngModel)]=\"query\" (keyup)=\"filter()\" placeholder=\"{{'Filter' | translate}}\" tooltip content=\"{{'Type to filter the tickets below' | translate}}\" />\n      </div>\n      <div class=\"col-sm-4\">\n        <div class=\"btn-group pull-right\" data-toggle=\"buttons\">\n          <label class=\"btn btn-primary\" [ngClass]=\"{active: userBool}\" tooltip content=\"{{'Select to filter tickets from user' | translate}}\">\n            <input type=\"radio\" autocomplete=\"off\" (click)=\"updateTickets(true)\"/>{{'User' | translate}}\n          </label>\n          <label class=\"btn btn-primary\" [ngClass]=\"{active: !userBool}\" tooltip content=\"{{'Select to filter tickets from company' | translate}}\">\n            <input type=\"radio\" autocomplete=\"off\" (click)=\"updateTickets(false)\"/>{{'Organization' | translate}}\n          </label>\n        </div>\n      </div>\n      <div class=\"col-sm-2 pull-right\">\n       <select class=\"form-control\" id=\"statusFilter\" [(ngModel)]=\"statusFilter\" (change)=\"filterStatus()\"  tooltip content=\"{{'Select to filter tickets by status' | translate}}\">\n         <option value=\"any\">{{'Any' | translate}}</option>\n         <option *ngFor=\"let statusOption of statusFields\" value=\"{{statusOption.value}}\">{{statusOption.name | translate}}</option>\n       </select>\n     </div>\n    <table class=\"table table-responsive table-striped table-md table-inverse table-sortable\">\n      <thead>\n        <tr>\n          <th (click)=\"changeSorting('id')\">Id\n            <span *ngIf=\"sort.column === 'id' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'id' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th (click)=\"changeSorting('subject')\">{{'Subject' | translate}}\n            <span *ngIf=\"sort.column === 'subject' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'subject' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th (click)=\"changeSorting('created_at')\">{{'Created' | translate}}\n            <span *ngIf=\"sort.column === 'created_at' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'created_at' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th (click)=\"changeSorting('updated_at')\">{{'Last activity' | translate}}\n            <span *ngIf=\"sort.column === 'updated_at' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'updated_at' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th (click)=\"changeSorting('status')\">{{'Status' | translate}}\n            <span *ngIf=\"sort.column === 'status' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'status' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n        </tr>\n      </thead>\n      <tbody>\n        <tr *ngFor=\"let ticket of tickets\" (click)=\"ticketClicked(ticket)\" class=\"selectable\"\n            tooltip content=\"{{'Go to request details...' | translate}}\">\n          <th scope=\"row\">{{ticket.id}}</th>\n          <td>{{ticket.subject}}</td>\n          <td>{{ticket.created_at | dayTime}}</td>\n          <td>{{ticket.updated_at | dayTime}}</td>\n          <td style=\"text-align: center;\">\n              <span\n              [ngClass]=\"{\n                              'ticketOpen': ticket.status==='open',\n                              'ticketNew': ticket.status==='new',\n                              'ticketClosed': ticket.status==='closed',\n                              'ticketSolved': ticket.status==='solved',\n                              'ticketPending': ticket.status==='pending',\n                              'ticketHold': ticket.status==='hold'}\"\n              class=\"glyphicon glyphicon-flag\" aria-hidden=\"true\"> {{ticket.status | translate}}</span>\n          </td>\n        </tr>\n      </tbody>\n    </table>\n  </div>\n</div>\n\n<div *ngIf=\"!tickets && errorMessage\" class=\"alert alert-danger\" role=\"alert\">{{errorMessage}}</div>\n"

/***/ }),

/***/ 390:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\" *ngIf=\"!loginService.initiating\">\n  <div class=\"panel-heading\">\n    <div class=\"alert alert-info\" role=\"alert\">\n      <h3 class=\"transparent form-signin-heading\">{{'Please sign in' | translate}}</h3>\n      <br/>\n      <p class=\"transparent\">{{'Insert Zendesk`s email and password to get OauthToken to communicate' | translate}}</p>\n      <br/>\n      <p class=\"transparent info\">{{'loginInfo' | translate}}</p>\n    </div>\n  </div>\n  <div class=\"panel-body\">\n    <div class=\"row\">\n      <form class=\"form-signin\" id=\"login\">\n        <label for=\"inputEmail\" class=\"sr-only\">{{'Email address' | translate}}</label>\n        <input type=\"email\" id=\"inputEmail\" class=\"form-control\"\n                placeholder=\"{{'Email address' | translate}}\" required autofocus\n                [(ngModel)]=\"user.email\" name=\"email\" (keypress)=\"cleanError($event)\"\n                 tooltip content=\"{{'Insert email address to login into Zendesk' | translate}}\">\n       <label for=\"inputPassword\" class=\"sr-only\">{{'Password' | translate}}</label>\n       <input type=\"password\" id=\"inputPassword\" class=\"form-control\"\n               placeholder=\"{{'Password' | translate}}\" required autofocus\n               [(ngModel)]=\"user.password\" name=\"password\" (keypress)=\"cleanError($event)\"\n                tooltip content=\"{{'Insert password to login into Zendesk, it not will be stored' | translate}}\">\n        <button class=\"btn btn-sm btn-primary btn-block\" type=\"submit\" [disabled]=\"!user.email && !user.password\" (click)=\"loginToZendesk(user)\"\n                tooltip content=\"{{'Login to Zendesk' | translate}}\">\n                {{'Sign in' | translate}}\n        </button>\n      </form>\n    </div>\n    <div class=\"row\">\n      <div *ngIf=\"errorMessage\" class=\"alert alert-danger\" role=\"alert\">{{errorMessage}}</div>\n    </div>\n  </div>\n</div>\n\n<button class=\"btn btn-sm btn-default pull-right\" (click)=\"shopSupport()\"\n        tooltip content=\"{{'Go to hire support!' | translate}}\">\n  {{'Unsupported? Hire support now!' | translate}}\n  <span class=\"glyphicon glyphicon-shopping-cart\" aria-hidden=\"true\">\n  </span>\n</button>\n"

/***/ }),

/***/ 391:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\">\n  <div class=\"panel-heading\">\n    <h4>\n      {{'Submit a request' | translate}}\n      <button class=\"btn btn-sm pull-right discardChanges\" (click)=\"discardChanges()\"\n              tooltip content=\"{{'Discard Request Data' | translate}}\">\n        {{'Discard' | translate}}\n        <span class=\"glyphicon glyphicon-remove-circle\" aria-hidden=\"true\">\n        </span>\n      </button>\n      <button class=\"btn btn-sm pull-right discardChanges\" type=\"submit\"\n              [disabled]=\"!ticketForm.valid\" (click)=\"checkData(ticketForm)\"\n              tooltip content=\"{{'Submit Request Data' | translate}}\">\n        {{'Submit' | translate}}\n        <span class=\"glyphicon glyphicon-ok-circle\" aria-hidden=\"true\">\n        </span>\n      </button>\n    </h4>\n  </div>\n  <div class=\"panel-body\">\n    <div *ngIf=\"errorMessage\" class=\"alert alert-danger alert-dismissible\" role=\"alert1\">\n      {{errorMessage}}\n    </div>\n    <div *ngIf=\"successMessage\" class=\"alert alert-success alert-dismissible\" role=\"alert2\">\n      {{successMessage}}\n    </div>\n    <form class=\"form-ticket form-vertical\" [formGroup]=\"ticketForm\">\n      <div class=\"form-group required\" *ngIf=\"genericFields[0]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[0].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <input class=\"form-control\" id=\"subject\" formControlName=\"subject\" required type=\"text\"\n                  tooltip content=\"{{'Request subject' | translate}}\"/>\n           <small *ngIf=\"!ticketForm.controls.subject.valid &&\n                         (ticketForm.controls.subject.dirty ||\n                         ticketForm.controls.subject.touched)\"\n                        class=\"badge text-warning\">\n             {{genericFields[0].title_in_portal | translate}} {{'is required' | translate}}\n           </small>\n         </div>\n      </div>\n      <div class=\"form-group required\" *ngIf=\"genericFields[1]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[1].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <textarea class=\"form-control\" rows=\"5\" id=\"description\" formControlName=\"description\"\n                     tooltip content=\"{{'Request description, the more specific, the easier it will be to help you' | translate}}\"></textarea>\n           <small *ngIf=\"!ticketForm.controls.description.valid &&\n                         (ticketForm.controls.description.dirty ||\n                         ticketForm.controls.description.touched)\"\n                        class=\"badge text-warning\">\n             {{genericFields[0].title_in_portal | translate}} {{'is required' | translate}}\n           </small>\n         </div>\n      </div>\n      <div class=\"form-group\" *ngIf=\"genericFields[3]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[3].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <select class=\"form-control\" id=\"type\" formControlName=\"type\"\n                   tooltip content=\"{{'Request type' | translate}}\">\n             <option *ngFor=\"let typeOption of genericFields[3].system_field_options\" value=\"{{typeOption.value}}\">{{typeOption.name | translate}}</option>\n           </select>\n        </div>\n      </div>\n      <div class=\"form-group required\" *ngIf=\"genericFields[4]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[4].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <select class=\"form-control\" id=\"environment\" formControlName=\"environment\" required\n                   tooltip content=\"{{'Request environment where issue appears' | translate}}\">\n              <option *ngFor=\"let environmentOption of genericFields[4].custom_field_options\" value=\"{{environmentOption.value}}\">{{environmentOption.name | translate}}</option>\n           </select>\n        </div>\n      </div>\n      <div class=\"form-group required\" *ngIf=\"genericFields[5]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[5].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <select class=\"form-control\" id=\"priority\" formControlName=\"priority\" required\n               tooltip content=\"{{'Request priority' | translate}}\">\n              <option *ngFor=\"let priorityOption of genericFields[5].system_field_options\" value=\"{{priorityOption.value}}\">{{priorityOption.name | translate}}</option>\n           </select>\n        </div>\n      </div>\n      <div class=\"form-group required\" *ngIf=\"genericFields[15]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[15].title_in_portal | translate}}</label>\n         <div class=\"col-md-4\">\n           <input class=\"form-control\" id=\"version\" formControlName=\"version\" required type=\"text\"\n                  tooltip content=\"{{'Request version of Payara' | translate}}\"/>\n           <small *ngIf=\"!ticketForm.controls.version.valid &&\n                         (ticketForm.controls.version.dirty ||\n                         ticketForm.controls.version.touched)\"\n                        class=\"badge text-warning\">\n             {{genericFields[15].title_in_portal | translate}} {{'is required' | translate}}\n           </small>\n         </div>\n      </div>\n      <div class=\"form-group\">\n         <div [ngClass]=\"{\n                         'col-xs-8': newAttachments.length > 0,\n                         'col-xs-12': newAttachments.length === 0\n                       }\">\n           <app-add-file (saved)=\"onSavedAttachment($event)\"\n                         (removed)=\"onRemovedAttachment($event)\"\n                         [title]=\"'Attach files to new request'\">\n            </app-add-file>\n         </div>\n         <div class=\"col-xs-4\" *ngIf=\"newAttachments.length > 0\">\n           <strong>{{'Files list' | translate}}</strong>\n           &nbsp;\n           <div *ngFor=\"let file of newAttachments\">\n             <a href=\"{{file.attachment.content_url}}\">\n               <span class=\"glyphicon glyphicon-download-alt attached-file-list\" aria-hidden=\"true\">\n                 {{file.attachment.file_name}}\n               </span>\n             </a>\n           </div>\n         </div>\n      </div>\n<!--\n      <button class=\"btn btn-sm btn-primary btn-block\" type=\"submit\" [disabled]=\"!ticketForm.valid\" (click)=\"checkData(ticketForm)\">{{'Submit' | translate}}</button>\n-->\n    </form>\n  </div>\n</div>\n"

/***/ }),

/***/ 392:
/***/ (function(module, exports) {

module.exports = "  <div class=\"col-md-11 detailContent\">\n    <h5>\n      <strong>{{ticket.submitter_name}}</strong> {{'submitted this request' | translate}}\n    </h5>\n    <hr>\n    <p class=\"detailData\">\n      <strong>{{'Status' | translate}}</strong>\n      &nbsp;\n      <span\n      [ngClass]=\"{\n                      'ticketOpen': ticket.status==='open',\n                      'ticketNew': ticket.status==='new',\n                      'ticketClosed': ticket.status==='closed',\n                      'ticketSolved': ticket.status==='solved',\n                      'ticketPending': ticket.status==='pending',\n                      'ticketHold': ticket.status==='hold'}\"\n      class=\"glyphicon glyphicon-flag\" aria-hidden=\"true\"> {{ticket.status | translate}}</span>\n    </p>\n    <p class=\"detailData\">\n      <strong>{{'Type' | translate}}</strong>\n      &nbsp;\n      {{ticket.type | translate}}\n    </p>\n    <p class=\"detailData\">\n      <strong>{{'Priority' | translate}}</strong>\n      &nbsp;\n      {{ticket.priority | translate}}\n    </p>\n    <div *ngFor=\"let field of ticket.custom_fields\">\n      <div class=\"detailData\">\n        <strong>{{field.title_in_portal | translate}}</strong>\n        &nbsp;\n        <p>{{getValue(field) | translate}}</p>\n      </div>\n    </div>\n    <div class=\"fileList\">\n      <strong>{{'Files list' | translate}}</strong>\n      &nbsp;\n      <div *ngFor=\"let file of files\">\n        <a href=\"{{file.content_url}}\">\n          <span class=\"\tglyphicon glyphicon-download-alt attached-file-list\" aria-hidden=\"true\">\n            {{file.file_name}}\n          </span>\n        </a>\n      </div>\n    </div>\n  </div>\n"

/***/ }),

/***/ 53:
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
    },
    supportGuides: 'https://api.payara.fish/api/payaraCustomer/supportGuide/'
};
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/environment.js.map

/***/ }),

/***/ 657:
/***/ (function(module, exports, __webpack_require__) {

module.exports = __webpack_require__(284);


/***/ }),

/***/ 70:
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

/***/ 71:
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

/***/ 72:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_http__ = __webpack_require__(52);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_toPromise__ = __webpack_require__(61);
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
     * postFile - Method to call to the API to get Payara server files by post
     *
     * @param {string}  url String with the url to call
     *
     * @return {Promise<string>} Returns the response promise
     */
    PayaraService.prototype.postFile = function (url) {
        this.headers = new __WEBPACK_IMPORTED_MODULE_1__angular_http__["c" /* Headers */]();
        this.headers.append('accept', 'application/json');
        this.headers.append('X-Requested-By', 'payara');
        this.headers.append('Content-Type', 'application/json');
        return this.http.post(this.connectionData.payaraURL + url, JSON.stringify({
            "first": "",
            "target": "server-config",
            "__remove_empty_entries__": "true"
        }), { headers: this.headers })
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
    /**
     * getInstanceStatus - Method to call to the API to get Payara server instance status
     *
     * @param {string}  id String with the id of the instance
     *
     * @return {Promise<any>} Returns the response promise
     */
    PayaraService.prototype.getInstanceStatus = function (id) {
        this.headers = new __WEBPACK_IMPORTED_MODULE_1__angular_http__["c" /* Headers */]();
        this.headers.append('Content-Type', 'application/json');
        return this.http.get(this.connectionData.payaraURL + 'list-instances.json?id=' + id, this.headers)
            .toPromise()
            .then(function (response) { return response.json().extraProperties.instanceList[0].status; });
    };
    return PayaraService;
}());
PayaraService = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])(),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_1__angular_http__["b" /* Http */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_http__["b" /* Http */]) === "function" && _a || Object])
], PayaraService);

var _a;
//# sourceMappingURL=/home/daniel/Work/Zendesk_Integration/src/payara.service.js.map

/***/ })

},[657]);
//# sourceMappingURL=main.bundle.js.map
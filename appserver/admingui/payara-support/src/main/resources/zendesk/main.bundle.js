webpackJsonp([1,5],{

/***/ 128:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common__ = __webpack_require__(18);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_Rx__ = __webpack_require__(105);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_Rx___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_rxjs_Rx__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__environments_environment__ = __webpack_require__(60);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_tinymce_tinymce__ = __webpack_require__(788);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_tinymce_tinymce___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_4_tinymce_tinymce__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_tinymce_themes_modern_theme__ = __webpack_require__(787);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_tinymce_themes_modern_theme___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_5_tinymce_themes_modern_theme__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_tinymce_plugins_paste_plugin__ = __webpack_require__(785);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_tinymce_plugins_paste_plugin___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_6_tinymce_plugins_paste_plugin__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7_tinymce_plugins_searchreplace_plugin__ = __webpack_require__(786);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7_tinymce_plugins_searchreplace_plugin___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_7_tinymce_plugins_searchreplace_plugin__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__ngx_translate_core__ = __webpack_require__(26);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9_jquery_dist_jquery__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_9_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10__services_zendesk_service__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_11__services_payara_service__ = __webpack_require__(59);
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
    function AddFileComponent(translate, zendeskService, payaraService, datePipe) {
        this.translate = translate;
        this.zendeskService = zendeskService;
        this.payaraService = payaraService;
        this.datePipe = datePipe;
        /**
         * Properties and objects of the component
         */
        this.environment = __WEBPACK_IMPORTED_MODULE_3__environments_environment__["a" /* environment */];
        this.tinymce = __WEBPACK_IMPORTED_MODULE_4_tinymce_tinymce___default.a;
        this.groups = [];
        this.staticFileButtons = [
            /*
            {
              title:this.translate.instant('Monitoring Data'),
              url:'get-monitoring-configuration.json',
              group:'general',
              uploaded:false,
              type:'json'
            },
            */
            {
                title: this.translate.instant('Domain Log'),
                url: 'view-log',
                group: 'general',
                uploaded: false,
                type: 'log'
            },
            {
                title: this.translate.instant('Health Checks'),
                url: 'configs/config/server-config/health-check-service-configuration/list-historic-healthchecks.json',
                group: 'reports',
                uploaded: false,
                type: 'health'
            },
            {
                title: this.translate.instant('Request Traces'),
                url: 'configs/config/server-config/request-tracing-service-configuration/list-historic-requesttraces.json',
                group: 'reports',
                uploaded: false,
                type: 'traces'
            }
        ];
        this.fileButtons = [];
        this.filesLoaded = false;
        this.filesSaved = false;
        this.saved = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.removed = new __WEBPACK_IMPORTED_MODULE_0__angular_core__["EventEmitter"]();
        this.elementId = 'domainEditor';
        this.isVisibleEditor = true;
        this.xmlLoaded = false;
        this.xmlColor = 'warning';
        this.otherfiles = [];
    }
    /**
     * ngAfterViewInit - AfterViewInit method of the component
     */
    AddFileComponent.prototype.ngAfterViewInit = function () { };
    /**
     * ngOnDestroy - OnDestroy method of the component
     */
    AddFileComponent.prototype.ngOnDestroy = function () {
        __WEBPACK_IMPORTED_MODULE_9_jquery_dist_jquery__('span.ng-tooltip').hide();
        if (this.sub)
            this.sub.unsubscribe();
    };
    /**
     * filter - Method to filter the list buttons to show on the screen
     *
     * @param {any[]}   buttons Array of buttons to filter
     * @param {string}  group   String to use to filter buttons
     */
    AddFileComponent.prototype.filter = function (buttons, group) {
        return buttons.filter(function (data) { return data.group === group; });
    };
    /**
     * ngOnInit - OnInit method of the component
     */
    AddFileComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.groups.push('general');
        this.groups.push('reports');
        this.fileButtons = Object.assign([], this.staticFileButtons);
        this.payaraService.getServerInstances().then(function (responseData) {
            if (responseData !== undefined && responseData !== null) {
                _this.filesLoaded = true;
                var responseArray_1 = Object.keys(responseData);
                var _loop_1 = function (prop) {
                    if (responseArray_1[prop] === 'server') {
                        _this.groups.push('DAS');
                        _this.fileButtons.push({
                            title: _this.translate.instant('JVM Report') +
                                ': ' +
                                responseArray_1[prop],
                            url: 'servers/server/' +
                                responseArray_1[prop] +
                                '/generate-jvm-report.json?type=summary',
                            group: 'DAS',
                            uploaded: false,
                            type: 'json'
                        });
                        _this.fileButtons.push({
                            title: _this.translate.instant('Thread Dump') +
                                ': ' +
                                responseArray_1[prop],
                            url: 'servers/server/' +
                                responseArray_1[prop] +
                                '/generate-jvm-report.json?type=thread',
                            group: 'DAS',
                            uploaded: false,
                            type: 'json'
                        });
                        _this.fileButtons.push({
                            title: 'Log: ' + responseArray_1[prop],
                            url: 'logs_' +
                                responseArray_1[prop] +
                                '.zip?contentSourceId=LogFiles&target=' +
                                responseArray_1[prop] +
                                '&restUrl=' +
                                window.location.protocol +
                                '//' +
                                window.location.hostname +
                                ':' +
                                window['globalPort'] +
                                '/management/domain',
                            group: 'DAS',
                            file: 'logs_' + responseArray_1[prop] + '.zip',
                            uploaded: false,
                            type: 'zip'
                        });
                        _this.fileButtons.push({
                            title: _this.translate.instant('Monitoring Data') +
                                ': ' +
                                responseArray_1[prop],
                            url: 'get.json?pattern=' + responseArray_1[prop] + '*&monitor=true',
                            group: 'DAS',
                            uploaded: false,
                            type: 'json'
                        });
                    }
                    else {
                        _this.payaraService.getInstanceStatus(responseArray_1[prop]).then(function (responseData) {
                            _this.groups.push('Instance: ' + responseArray_1[prop]);
                            if (responseArray_1[prop] === 'server' ||
                                (responseData !== undefined &&
                                    responseData !== null &&
                                    responseData === 'RUNNING')) {
                                _this.fileButtons.push({
                                    title: _this.translate.instant('JVM Report') +
                                        ': ' +
                                        responseArray_1[prop],
                                    url: 'servers/server/' +
                                        responseArray_1[prop] +
                                        '/generate-jvm-report.json?type=summary',
                                    group: 'Instance: ' + responseArray_1[prop],
                                    uploaded: false,
                                    type: 'json'
                                });
                                _this.fileButtons.push({
                                    title: _this.translate.instant('Thread Dump') +
                                        ': ' +
                                        responseArray_1[prop],
                                    url: 'servers/server/' +
                                        responseArray_1[prop] +
                                        '/generate-jvm-report.json?type=thread',
                                    group: 'Instance: ' + responseArray_1[prop],
                                    uploaded: false,
                                    type: 'json'
                                });
                                _this.fileButtons.push({
                                    title: 'Log: ' + responseArray_1[prop],
                                    url: 'Log: ' +
                                        responseArray_1[prop] +
                                        '.zip?contentSourceId=LogFiles&target=' +
                                        responseArray_1[prop] +
                                        '&restUrl=' +
                                        window.location.protocol +
                                        '//' +
                                        window.location.hostname +
                                        ':' +
                                        window['globalPort'] +
                                        '/management/domain',
                                    group: 'Instance: ' + responseArray_1[prop],
                                    file: 'Log: ' + responseArray_1[prop] + '.zip',
                                    uploaded: false,
                                    type: 'zip'
                                });
                                _this.fileButtons.push({
                                    title: _this.translate.instant('Monitoring Data') +
                                        ': ' +
                                        responseArray_1[prop],
                                    url: 'get.json?pattern=' +
                                        responseArray_1[prop] +
                                        '*&monitor=true',
                                    group: 'Instance: ' + responseArray_1[prop],
                                    uploaded: false,
                                    type: 'json'
                                });
                            }
                        }, function (error) {
                            _this.showErrorMessage('Error! Service Unavailable');
                        });
                    }
                };
                for (var prop in responseArray_1) {
                    _loop_1(prop);
                }
            }
        }, function (error) {
            _this.showErrorMessage('Error! Service Unavailable');
        });
        this.translate.get('LOADING').subscribe(function (res) {
            _this.xmlLoading = res;
            _this.discardXml();
        });
    };
    /**
     * showErrorMessage - Method to show an error message
     *
     * @param {string}   message Message to show
     */
    AddFileComponent.prototype.showErrorMessage = function (message) {
        var _this = this;
        this.translate.get(message).subscribe(function (res) {
            _this.errorMessage = res;
            var timer = __WEBPACK_IMPORTED_MODULE_2_rxjs_Rx__["Observable"].timer(1000, 50);
            _this.sub = timer.subscribe(function (t) {
                _this.errorMessage = '';
                _this.sub.unsubscribe();
            });
            _this.loadingMessage = null;
            _this.successMessage = '';
        });
    };
    /**
     * fileUploaded - Method to show a message when a file is uploaded successfully
     *
     * @param {any}   file Object with the file attached
     */
    AddFileComponent.prototype.fileUploaded = function (file) {
        var _this = this;
        this.saved.emit(file);
        this.filesSaved = true;
        this.loadingMessage = null;
        this.successMessage = '';
        this.translate.get('File added successfully!').subscribe(function (res) {
            _this.successMessage = res;
        });
        var timer = __WEBPACK_IMPORTED_MODULE_2_rxjs_Rx__["Observable"].timer(1000, 50);
        this.sub = timer.subscribe(function (t) {
            _this.successMessage = '';
            _this.sub.unsubscribe();
        });
    };
    /**
     * searchFile - Event to get the data for the file when the button is pressed
     *
     * @param {any}  fileButton Object with the data of the fileButton
     */
    AddFileComponent.prototype.searchFile = function (fileButton) {
        var _this = this;
        this.errorMessage = '';
        this.loadingMessage = '';
        this.successMessage = '';
        if (fileButton.uploaded) {
            this.removed.emit(fileButton.title);
            fileButton.uploaded = false;
        }
        else {
            this.loadingMessage =
                this.translate.instant('Loading file ...') + fileButton.title;
            if (fileButton.url && fileButton.url !== '') {
                switch (fileButton.type) {
                    case 'log':
                        this.getFile(fileButton.url, false, function (response) {
                            if (response !== false) {
                                _this.addFile(response['_body'], fileButton.title + '.log', 'application/octet-stream', function (response) {
                                    if (response) {
                                        _this.fileUploaded(response);
                                        fileButton.uploaded = true;
                                    }
                                    else {
                                        fileButton.uploaded = false;
                                        _this.loadingMessage = null;
                                        _this.successMessage = '';
                                    }
                                });
                            }
                            else {
                                fileButton.uploaded = false;
                                _this.showErrorMessage('Error! Service Unavailable');
                            }
                        });
                        break;
                    case 'traces':
                        this.payaraService.postFile(fileButton.url).then(function (responseData) {
                            if (responseData !== false &&
                                responseData.message !== '' &&
                                responseData.exit_code !== 'FAIULRE' &&
                                responseData.message.indexOf('is not enabled') < 0) {
                                _this.addFile(responseData.message, fileButton.title + '.txt', 'application/octet-stream', function (response) {
                                    if (response) {
                                        _this.fileUploaded(response);
                                        fileButton.uploaded = true;
                                    }
                                    else {
                                        fileButton.uploaded = false;
                                        _this.loadingMessage = null;
                                        _this.successMessage = '';
                                    }
                                });
                            }
                            else {
                                fileButton.uploaded = false;
                                _this.showErrorMessage('Empty data');
                            }
                        }, function (error) {
                            fileButton.uploaded = false;
                            _this.showErrorMessage('Error! Service Unavailable');
                        });
                        break;
                    case 'health':
                        this.getFile(fileButton.url, false, function (response) {
                            var healthData = JSON.parse(response['_body']);
                            if (healthData !== false &&
                                healthData.extraProperties !== undefined &&
                                healthData.extraProperties.historicmessages.length > 0) {
                                _this.addFile(JSON.stringify(healthData.extraProperties.historicmessages), fileButton.title + '.txt', 'application/octet-stream', function (response) {
                                    if (response) {
                                        _this.fileUploaded(response);
                                        fileButton.uploaded = true;
                                    }
                                    else {
                                        fileButton.uploaded = false;
                                        _this.loadingMessage = null;
                                        _this.successMessage = '';
                                    }
                                });
                            }
                            else {
                                fileButton.uploaded = false;
                                _this.showErrorMessage('Empty data');
                            }
                        });
                        break;
                    case 'json':
                        this.getFile(fileButton.url, false, function (response) {
                            if (response !== false &&
                                response.json().message !== undefined &&
                                (response.json().message !== '' ||
                                    response.json().exit_code === 'SUCCESS')) {
                                _this.addFile(fileButton.url.indexOf('get.json?') < 0
                                    ? response.json().message
                                    : JSON.stringify('' + response.json().children), fileButton.title + '.txt', 'application/octet-stream', function (response) {
                                    if (response) {
                                        _this.fileUploaded(response);
                                        fileButton.uploaded = true;
                                    }
                                    else {
                                        fileButton.uploaded = false;
                                        _this.loadingMessage = null;
                                        _this.successMessage = '';
                                    }
                                });
                            }
                            else {
                                fileButton.uploaded = false;
                                _this.showErrorMessage('Empty data');
                            }
                        });
                        break;
                    case 'zip':
                        this.getFile(fileButton.url, true, function (response2) {
                            if (response2 !== false) {
                                _this.addFile(new File([response2], fileButton.file, {
                                    type: 'application/zip'
                                }), fileButton.file, 'application/zip', function (result) {
                                    if (result) {
                                        _this.fileUploaded(result);
                                        fileButton.uploaded = true;
                                    }
                                    else {
                                        fileButton.uploaded = false;
                                        _this.loadingMessage = null;
                                        _this.successMessage = '';
                                    }
                                });
                            }
                            else {
                                fileButton.uploaded = false;
                                _this.showErrorMessage('Empty data');
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
        this.otherfiles = [];
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
        var fileDate = this.datePipe.transform(new Date(), 'yyyy-MM-ddTHH:mm:ss');
        if (!(content instanceof String)) {
            this.zendeskService.addNewFile(content, fileDate + '_' + name, type).then(function (responseData) {
                if (responseData !== undefined && responseData !== null) {
                    back(JSON.parse(responseData).upload);
                }
            }, function (error) {
                _this.showErrorMessage('Error! Service Unavailable');
                back(false);
            });
        }
        else {
            if (content.indexOf('offline') < 0) {
                this.zendeskService.addNewFile(content, name, type).then(function (responseData) {
                    if (responseData !== undefined && responseData !== null) {
                        back(JSON.parse(responseData).upload);
                    }
                }, function (error) {
                    _this.showErrorMessage('Error! Service Unavailable');
                    back(false);
                });
            }
            else {
                this.showErrorMessage('Instance seems to be offline');
                back(false);
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
                    _this.showErrorMessage('Error! Service Unavailable');
                    back(false);
                }
                else {
                    back(data);
                }
            });
        }
        else {
            this.payaraService.getFile(url).then(function (responseData) {
                if (responseData !== undefined && responseData !== null) {
                    back(responseData);
                }
                else {
                    _this.showErrorMessage('Empty data');
                    back(false);
                }
            }, function (error) {
                _this.showErrorMessage('Error! Service Unavailable');
                back(false);
            });
        }
    };
    /**
     * showEditor - Function to show/hide xml editor
     */
    AddFileComponent.prototype.showEditor = function () {
        var loadingText = this.translate.instant('LOADING');
        if (this.xmlLoading === loadingText) {
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
            this.xmlLoading = loadingText;
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
                    setup: function (editor) {
                        _this.editor = editor;
                    }
                };
                if (_this.environment.production) {
                    initObject.relative_urls = false;
                    initObject.remove_script_host = false;
                    initObject.document_base_url =
                        window.location.protocol +
                            '//' +
                            window.location.hostname +
                            ':' +
                            window['globalPort'] +
                            '/resource/payara_support/zendesk/';
                    initObject.skin_url = './assets/skins/lightgray';
                }
                _this.tinymce.init(initObject);
                _this.tinymce.activeEditor.setContent(response
                    .json()
                    .message.replace(/>/g, '&gt;')
                    .replace(/</g, '<br>&lt;')
                    .replace('<br>', ''));
            }
        });
    };
    /**
     * saveXml - Function to save changes made to xml file shown
     */
    AddFileComponent.prototype.saveXml = function () {
        var _this = this;
        this.loadingMessageXml =
            this.translate.instant('Loading file ...') + ' Domain.xml';
        this.addFile(this.editor
            .getContent()
            .replace(/&gt;/g, '>')
            .replace(/&lt;/g, '<')
            .replace(/<br \/>/g, '\n'), 'Domain.xml', 'application/binary', function (response) {
            _this.fileUploaded(response);
            _this.isVisibleEditor = false;
            _this.xmlColor = 'success';
            _this.xmlLoading = 'YES';
            _this.loadingMessageXml = null;
            _this.tinymce.activeEditor.setMode('readonly');
        });
    };
    /**
     * otherFile - Method to attach one or more files selected from the file system
     *
     * @param {any}   event Object with the event data of the files selected
     */
    AddFileComponent.prototype.otherFile = function (event) {
        var _this = this;
        var fileList = event.target.files;
        if (fileList.length > 0) {
            for (var i = 0; i < fileList.length; i++) {
                var file = fileList[i];
                if (file.size < 20971520) {
                    var formData = new FormData();
                    formData.append('upload', file, file.name);
                    this.otherfiles.push(file);
                    this.addFile(formData, file.name, 'application/binary', function (response) {
                        _this.fileUploaded(response);
                    });
                }
                else {
                    this.showErrorMessage('Error! File ' + file.name + ' size bigger than 20 MB');
                }
            }
        }
        else {
            this.showErrorMessage('Error! File not exist');
        }
    };
    /**
     * removeFile - Method to remove a file to attach
     *
     * @param {any}   file File object with the file to remove
     */
    AddFileComponent.prototype.removeFile = function (file) {
        this.otherfiles = this.otherfiles.filter(function (data) { return data.name !== file.name; });
        this.removed.emit(file.name);
        __WEBPACK_IMPORTED_MODULE_9_jquery_dist_jquery__('span.ng-tooltip').hide();
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
        template: __webpack_require__(509),
        styles: [__webpack_require__(475)]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_8__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_8__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_10__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_10__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_11__services_payara_service__["a" /* PayaraService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_11__services_payara_service__["a" /* PayaraService */]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_1__angular_common__["DatePipe"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_common__["DatePipe"]) === "function" && _d || Object])
], AddFileComponent);

var _a, _b, _c, _d;
//# sourceMappingURL=add-file.component.js.map

/***/ }),

/***/ 129:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(21);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__(18);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_rxjs_add_operator_switchMap__ = __webpack_require__(277);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_rxjs_add_operator_switchMap___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_4_rxjs_add_operator_switchMap__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__ticket_data_ticket_data_component__ = __webpack_require__(133);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__services_zendesk_service__ = __webpack_require__(25);
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
        __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__('span.ng-tooltip').hide();
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
        template: __webpack_require__(511),
        styles: [__webpack_require__(477)]
    }),
    __metadata("design:paramtypes", [typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_6__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_6__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_2__angular_common__["DatePipe"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__angular_common__["DatePipe"]) === "function" && _d || Object])
], DetailedTicketComponent);

var _a, _b, _c, _d;
//# sourceMappingURL=detailed-ticket.component.js.map

/***/ }),

/***/ 130:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(21);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__ = __webpack_require__(26);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__services_zendesk_service__ = __webpack_require__(25);
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
    }
    /**
     * ngOnDestroy - OnDestroy method of the component
     */
    ListTicketsComponent.prototype.ngOnDestroy = function () {
        __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__('span.ng-tooltip').hide();
    };
    /**
     * hideMessage - Hide the advice message on the main window
     */
    ListTicketsComponent.prototype.hideMessage = function () {
        __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__('span.ng-tooltip').hide();
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
                    this.loginService.connectionData.supportType = 'professional';
                }
                else if (type.indexOf('enterprise') >= 0) {
                    this.loginService.connectionData.supportType = 'enterprise';
                }
                else if (type.indexOf('developer') >= 0) {
                    this.loginService.connectionData.supportType = 'developer';
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
                    _this.statusFields.push({ name: 'Closed', value: 'closed' });
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
        var _this = this;
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
                this.tickets.forEach(function (ticket) {
                    _this.zendeskService.getUserIdentity('' + ticket.requester_id)
                        .then(function (responseData) {
                        if (responseData !== undefined && responseData !== null) {
                            ticket.submitter_name = responseData.name;
                        }
                    }, function (error) {
                        _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                            _this.errorMessage = res;
                        });
                    });
                });
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
        template: __webpack_require__(512),
        styles: [__webpack_require__(478)]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_4__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_5__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_5__services_login_service__["a" /* LoginService */]) === "function" && _e || Object])
], ListTicketsComponent);

var _a, _b, _c, _d, _e;
//# sourceMappingURL=list-tickets.component.js.map

/***/ }),

/***/ 131:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(21);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__ = __webpack_require__(26);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_crypto_js_crypto_js__ = __webpack_require__(151);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_crypto_js_crypto_js___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_4_crypto_js_crypto_js__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__services_login_service__ = __webpack_require__(32);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__services_payara_service__ = __webpack_require__(59);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__classes_user__ = __webpack_require__(83);
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
        __WEBPACK_IMPORTED_MODULE_3_jquery_dist_jquery__('span.ng-tooltip').hide();
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
        template: __webpack_require__(513),
        styles: [__webpack_require__(479)]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_6__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_6__services_login_service__["a" /* LoginService */]) === "function" && _e || Object, typeof (_f = typeof __WEBPACK_IMPORTED_MODULE_7__services_payara_service__["a" /* PayaraService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_7__services_payara_service__["a" /* PayaraService */]) === "function" && _f || Object])
], LoginComponent);

var _a, _b, _c, _d, _e, _f;
//# sourceMappingURL=login.component.js.map

/***/ }),

/***/ 132:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(21);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_common__ = __webpack_require__(18);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_forms__ = __webpack_require__(58);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__ = __webpack_require__(26);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_jquery_dist_jquery__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_5_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_rxjs_Rx__ = __webpack_require__(105);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6_rxjs_Rx___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_6_rxjs_Rx__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__services_payara_service__ = __webpack_require__(59);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__services_login_service__ = __webpack_require__(32);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10__classes_ticket__ = __webpack_require__(82);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_11__classes_user__ = __webpack_require__(83);
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
    function NewTicketComponent(translate, fb, router, zendeskService, payaraService, loginService, datePipe) {
        this.translate = translate;
        this.fb = fb;
        this.router = router;
        this.zendeskService = zendeskService;
        this.payaraService = payaraService;
        this.loginService = loginService;
        this.datePipe = datePipe;
        this.user = new __WEBPACK_IMPORTED_MODULE_11__classes_user__["a" /* User */]();
        this.newTicket = new __WEBPACK_IMPORTED_MODULE_10__classes_ticket__["a" /* Ticket */]();
        this.payaraVersion = "";
    }
    /**
     * ngOnInit - OnInit method of the component
     */
    NewTicketComponent.prototype.ngOnInit = function () {
        var _this = this;
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
        this.payaraService.getPayaraVersion()
            .then(function (responseData) {
            if (responseData !== undefined && responseData !== null) {
                _this.payaraVersion = responseData;
            }
            _this.initTicket();
        }, function (error) {
            _this.translate.get('Error! Service Unavailable').subscribe(function (res) {
                _this.errorMessage = res;
            });
        });
    };
    /**
     * ngOnDestroy - OnDestroy method of the component
     */
    NewTicketComponent.prototype.ngOnDestroy = function () {
        __WEBPACK_IMPORTED_MODULE_5_jquery_dist_jquery__('span.ng-tooltip').hide();
        this.initTicket();
        this.errorMessage = "";
        if (this.sub)
            this.sub.unsubscribe();
    };
    /**
     * initTicket - Method to initiate the new ticket data
     */
    NewTicketComponent.prototype.initTicket = function () {
        this.newAttachments = [];
        this.ticketForm = this.fb.group({
            subject: ['', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["Validators"].required]],
            description: ['', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["Validators"].required]],
            type: ['problem', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["Validators"].required]],
            environment: ['prod', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["Validators"].required]],
            priority: ['normal', [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["Validators"].required]],
            version: [this.payaraVersion, [__WEBPACK_IMPORTED_MODULE_3__angular_forms__["Validators"].required]]
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
            ticketData.custom_fields = [];
            if (ticketData.environment) {
                ticketData.custom_fields.push({ id: this.genericFields[4].id, value: ticketData.environment });
            }
            if (ticketData.version) {
                ticketData.custom_fields.push({ id: this.genericFields[15].id, value: ticketData.version });
            }
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
        template: __webpack_require__(514),
        styles: [__webpack_require__(480)]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_3__angular_forms__["FormBuilder"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_3__angular_forms__["FormBuilder"]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_8__services_payara_service__["a" /* PayaraService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_8__services_payara_service__["a" /* PayaraService */]) === "function" && _e || Object, typeof (_f = typeof __WEBPACK_IMPORTED_MODULE_9__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_9__services_login_service__["a" /* LoginService */]) === "function" && _f || Object, typeof (_g = typeof __WEBPACK_IMPORTED_MODULE_2__angular_common__["DatePipe"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__angular_common__["DatePipe"]) === "function" && _g || Object])
], NewTicketComponent);

var _a, _b, _c, _d, _e, _f, _g;
//# sourceMappingURL=new-ticket.component.js.map

/***/ }),

/***/ 133:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__ngx_translate_core__ = __webpack_require__(26);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_jquery_dist_jquery__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__services_login_service__ = __webpack_require__(32);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__classes_ticket__ = __webpack_require__(82);
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
        __WEBPACK_IMPORTED_MODULE_2_jquery_dist_jquery__('span.ng-tooltip').hide();
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
    /**
     * addFiles - Method that receives an event with an array of files to add to this component
     *
     * @param {Attachment[]}  files Array of objects to add
     */
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
        template: __webpack_require__(515),
        styles: [__webpack_require__(481)]
    }),
    __metadata("design:paramtypes", [typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__ngx_translate_core__["c" /* TranslateService */]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_3__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_4__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__services_login_service__["a" /* LoginService */]) === "function" && _d || Object])
], TicketDataComponent);

var _a, _b, _c, _d;
//# sourceMappingURL=ticket-data.component.js.map

/***/ }),

/***/ 134:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(21);
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
//# sourceMappingURL=auth.guard.js.map

/***/ }),

/***/ 25:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_http__ = __webpack_require__(31);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_jquery__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_add_operator_toPromise__ = __webpack_require__(70);
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
//# sourceMappingURL=zendesk.service.js.map

/***/ }),

/***/ 32:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_http__ = __webpack_require__(31);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_toPromise__ = __webpack_require__(70);
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
//# sourceMappingURL=login.service.js.map

/***/ }),

/***/ 388:
/***/ (function(module, exports) {

function webpackEmptyContext(req) {
	throw new Error("Cannot find module '" + req + "'.");
}
webpackEmptyContext.keys = function() { return []; };
webpackEmptyContext.resolve = webpackEmptyContext;
module.exports = webpackEmptyContext;
webpackEmptyContext.id = 388;


/***/ }),

/***/ 389:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser_dynamic__ = __webpack_require__(403);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__environments_environment__ = __webpack_require__(60);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__app_app_module__ = __webpack_require__(407);




if (__WEBPACK_IMPORTED_MODULE_2__environments_environment__["a" /* environment */].production) {
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_1__angular_core__["enableProdMode"])();
}
__webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_platform_browser_dynamic__["a" /* platformBrowserDynamic */])().bootstrapModule(__WEBPACK_IMPORTED_MODULE_3__app_app_module__["a" /* AppModule */]);
//# sourceMappingURL=main.js.map

/***/ }),

/***/ 406:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_router__ = __webpack_require__(21);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_moment__ = __webpack_require__(1);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_moment___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_2_moment__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__environments_environment__ = __webpack_require__(60);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__ = __webpack_require__(26);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_crypto_js_crypto_js__ = __webpack_require__(151);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5_crypto_js_crypto_js___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_5_crypto_js_crypto_js__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__services_login_service__ = __webpack_require__(32);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__services_payara_service__ = __webpack_require__(59);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9__classes_user__ = __webpack_require__(83);
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
            shopURL: this.environment.payara.shopUrl,
            supportGuideURL: this.environment.supportGuides,
            supportType: 'basic'
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
        var globalPort = window['globalPort'] !== undefined ? window['globalPort'] : '5000';
        if (globalPort !== undefined && globalPort !== null) {
            this.payaraService.connectionData = {
                payaraURL: window.location.protocol + '//' + window.location.hostname + ':' + globalPort + this.environment.payara.baseUrl,
                filesURL: window.location.protocol + '//' + window.location.hostname + ':' + globalPort + '/download/'
            };
            var specialAdminIndicator = window['specialAdminIndicator'];
            if (specialAdminIndicator != null) {
                this.payaraService.setSpecialAdminIndicator(specialAdminIndicator);
            }
            this.payaraService.getStoredEmail()
                .then(function (responseData) {
                if (responseData !== undefined && responseData !== null) {
                    var decryptedData = __WEBPACK_IMPORTED_MODULE_5_crypto_js_crypto_js__["AES"].decrypt(responseData, 'payara').toString(__WEBPACK_IMPORTED_MODULE_5_crypto_js_crypto_js__["enc"].Utf8);
                    var email = decryptedData.split('|')[0];
                    var regExpEmail = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
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
                                localStorage.setItem('showMessage', JSON.stringify(false));
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
        this.payaraService.setStoredEmail(null);
    };
    return AppComponent;
}());
AppComponent = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Component"])({
        selector: 'app-root',
        template: __webpack_require__(508),
        styles: [__webpack_require__(474)],
        providers: [__WEBPACK_IMPORTED_MODULE_6__services_login_service__["a" /* LoginService */], __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__["a" /* ZendeskService */], __WEBPACK_IMPORTED_MODULE_8__services_payara_service__["a" /* PayaraService */]]
    }),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_4__ngx_translate_core__["c" /* TranslateService */]) === "function" && _a || Object, typeof (_b = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["Router"]) === "function" && _b || Object, typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_router__["ActivatedRoute"]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_7__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_6__services_login_service__["a" /* LoginService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_6__services_login_service__["a" /* LoginService */]) === "function" && _e || Object, typeof (_f = typeof __WEBPACK_IMPORTED_MODULE_8__services_payara_service__["a" /* PayaraService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_8__services_payara_service__["a" /* PayaraService */]) === "function" && _f || Object])
], AppComponent);

var _a, _b, _c, _d, _e, _f;
//# sourceMappingURL=app.component.js.map

/***/ }),

/***/ 407:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser__ = __webpack_require__(37);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__angular_forms__ = __webpack_require__(58);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__angular_http__ = __webpack_require__(31);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__angular_router__ = __webpack_require__(21);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__angular_common__ = __webpack_require__(18);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__ngx_translate_core__ = __webpack_require__(26);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__ngx_translate_http_loader__ = __webpack_require__(412);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8_ngx_modal__ = __webpack_require__(487);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8_ngx_modal___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_8_ngx_modal__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_9_ng2_tooltip_directive_components__ = __webpack_require__(486);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10_jw_bootstrap_switch_ng2__ = __webpack_require__(483);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_10_jw_bootstrap_switch_ng2___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_10_jw_bootstrap_switch_ng2__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_11__angular_platform_browser_animations__ = __webpack_require__(404);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_12_angular2_markdown__ = __webpack_require__(414);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_13__environments_environment__ = __webpack_require__(60);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_14__app_component__ = __webpack_require__(406);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_15__app_routing__ = __webpack_require__(408);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_16__components_login_login_component__ = __webpack_require__(131);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_17__components_list_tickets_list_tickets_component__ = __webpack_require__(130);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_18__components_new_ticket_new_ticket_component__ = __webpack_require__(132);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_19__components_detailed_ticket_detailed_ticket_component__ = __webpack_require__(129);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_20__components_add_file_add_file_component__ = __webpack_require__(128);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_21__components_ticket_data_ticket_data_component__ = __webpack_require__(133);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_22__components_comment_data_comment_data_component__ = __webpack_require__(410);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_23__services_login_service__ = __webpack_require__(32);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_24__guards_auth_guard__ = __webpack_require__(134);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_25__pipes_day_time_pipe__ = __webpack_require__(411);
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
if (__WEBPACK_IMPORTED_MODULE_13__environments_environment__["a" /* environment */].production) {
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
            __WEBPACK_IMPORTED_MODULE_14__app_component__["a" /* AppComponent */],
            __WEBPACK_IMPORTED_MODULE_16__components_login_login_component__["a" /* LoginComponent */],
            __WEBPACK_IMPORTED_MODULE_17__components_list_tickets_list_tickets_component__["a" /* ListTicketsComponent */],
            __WEBPACK_IMPORTED_MODULE_18__components_new_ticket_new_ticket_component__["a" /* NewTicketComponent */],
            __WEBPACK_IMPORTED_MODULE_19__components_detailed_ticket_detailed_ticket_component__["a" /* DetailedTicketComponent */],
            __WEBPACK_IMPORTED_MODULE_20__components_add_file_add_file_component__["a" /* AddFileComponent */],
            __WEBPACK_IMPORTED_MODULE_21__components_ticket_data_ticket_data_component__["a" /* TicketDataComponent */],
            __WEBPACK_IMPORTED_MODULE_22__components_comment_data_comment_data_component__["a" /* CommentDataComponent */],
            __WEBPACK_IMPORTED_MODULE_25__pipes_day_time_pipe__["a" /* DayTimePipe */],
            __WEBPACK_IMPORTED_MODULE_9_ng2_tooltip_directive_components__["a" /* TooltipDirective */]
        ],
        schemas: [__WEBPACK_IMPORTED_MODULE_1__angular_core__["CUSTOM_ELEMENTS_SCHEMA"]],
        imports: [
            __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser__["a" /* BrowserModule */],
            __WEBPACK_IMPORTED_MODULE_11__angular_platform_browser_animations__["a" /* BrowserAnimationsModule */],
            __WEBPACK_IMPORTED_MODULE_10_jw_bootstrap_switch_ng2__["JWBootstrapSwitchModule"],
            __WEBPACK_IMPORTED_MODULE_2__angular_forms__["FormsModule"],
            __WEBPACK_IMPORTED_MODULE_2__angular_forms__["ReactiveFormsModule"],
            __WEBPACK_IMPORTED_MODULE_3__angular_http__["a" /* HttpModule */],
            __WEBPACK_IMPORTED_MODULE_8_ngx_modal__["ModalModule"],
            __WEBPACK_IMPORTED_MODULE_15__app_routing__["a" /* routing */],
            __WEBPACK_IMPORTED_MODULE_12_angular2_markdown__["a" /* MarkdownModule */].forRoot(),
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
            __WEBPACK_IMPORTED_MODULE_25__pipes_day_time_pipe__["a" /* DayTimePipe */],
            __WEBPACK_IMPORTED_MODULE_6__ngx_translate_core__["a" /* TranslateModule */]
        ],
        providers: [
            __WEBPACK_IMPORTED_MODULE_5__angular_common__["DatePipe"],
            __WEBPACK_IMPORTED_MODULE_25__pipes_day_time_pipe__["a" /* DayTimePipe */],
            __WEBPACK_IMPORTED_MODULE_24__guards_auth_guard__["a" /* AuthGuard */],
            __WEBPACK_IMPORTED_MODULE_23__services_login_service__["a" /* LoginService */]
        ],
        bootstrap: [__WEBPACK_IMPORTED_MODULE_14__app_component__["a" /* AppComponent */]]
    })
], AppModule);

//# sourceMappingURL=app.module.js.map

/***/ }),

/***/ 408:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_router__ = __webpack_require__(21);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__components_login_login_component__ = __webpack_require__(131);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__components_list_tickets_list_tickets_component__ = __webpack_require__(130);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__components_new_ticket_new_ticket_component__ = __webpack_require__(132);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__components_detailed_ticket_detailed_ticket_component__ = __webpack_require__(129);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__guards_auth_guard__ = __webpack_require__(134);
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
    { path: 'list', component: __WEBPACK_IMPORTED_MODULE_2__components_list_tickets_list_tickets_component__["a" /* ListTicketsComponent */], canActivate: [__WEBPACK_IMPORTED_MODULE_5__guards_auth_guard__["a" /* AuthGuard */]] },
    { path: 'detail/:id', component: __WEBPACK_IMPORTED_MODULE_4__components_detailed_ticket_detailed_ticket_component__["a" /* DetailedTicketComponent */], canActivate: [__WEBPACK_IMPORTED_MODULE_5__guards_auth_guard__["a" /* AuthGuard */]] },
    { path: 'new', component: __WEBPACK_IMPORTED_MODULE_3__components_new_ticket_new_ticket_component__["a" /* NewTicketComponent */], canActivate: [__WEBPACK_IMPORTED_MODULE_5__guards_auth_guard__["a" /* AuthGuard */]] },
    { path: 'login', component: __WEBPACK_IMPORTED_MODULE_1__components_login_login_component__["a" /* LoginComponent */] },
    // otherwise redirect to list of tickets
    { path: '**', redirectTo: '' }
];
var routing = __WEBPACK_IMPORTED_MODULE_0__angular_router__["RouterModule"].forRoot(appRoutes);
//# sourceMappingURL=app.routing.js.map

/***/ }),

/***/ 409:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return Comment; });
var Comment = (function () {
    function Comment() {
    }
    return Comment;
}());

//# sourceMappingURL=comment.js.map

/***/ }),

/***/ 410:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_common__ = __webpack_require__(18);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__ = __webpack_require__(26);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_Rx__ = __webpack_require__(105);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3_rxjs_Rx___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_3_rxjs_Rx__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_jquery_dist_jquery__ = __webpack_require__(28);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4_jquery_dist_jquery___default = __webpack_require__.n(__WEBPACK_IMPORTED_MODULE_4_jquery_dist_jquery__);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__ = __webpack_require__(25);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__add_file_add_file_component__ = __webpack_require__(128);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_7__classes_ticket__ = __webpack_require__(82);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_8__classes_comment__ = __webpack_require__(409);
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
        this.showFiles = true;
    }
    /**
     * ngOnDestroy - OnDestroy method of the component
     */
    CommentDataComponent.prototype.ngOnDestroy = function () {
        __WEBPACK_IMPORTED_MODULE_4_jquery_dist_jquery__('span.ng-tooltip').hide();
        if (this.sub)
            this.sub.unsubscribe();
    };
    /**
     * ngOnInit - OnInit method of the component
     */
    CommentDataComponent.prototype.ngOnInit = function () {
        var _this = this;
        this.translate.get('File').subscribe(function (res) {
            _this.fileText = res;
        });
        this.newAttachments = [];
        this.getComments();
    };
    /**
     * getComments - Method to recover the comments of a ticket from the API
     */
    CommentDataComponent.prototype.getComments = function () {
        var _this = this;
        this.zendeskService.getTicketComments(this.ticket.id).then(function (responseData) {
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
                _this.comments.forEach(function (comment) {
                    _this.zendeskService.getUserIdentity('' + comment.author_id).then(function (responseData) {
                        if (responseData !== undefined && responseData !== null) {
                            comment.author_name = responseData.name;
                        }
                    }, function (error) {
                        _this.translate
                            .get('Error! Service Unavailable')
                            .subscribe(function (res) {
                            _this.errorMessage = res;
                        });
                    });
                });
            }
        }, function (error) {
            _this.translate
                .get('Error! Service Unavailable')
                .subscribe(function (res) {
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
        if (this.newAttachments.length > 0) {
            this.newAttachments = this.newAttachments.filter(function (data) { return data.attachment.file_name.indexOf(removedAttachment) === -1; });
        }
        /*
        if(this.newCommentText){
          let comments = this.newCommentText.split(' -------------- ')[0];
          let addedFiles = this.newCommentText.split(' -------------- ')[1].split('\n');
          this.newCommentText = comments.trim();
          if(addedFiles.length > 2){
            this.newCommentText += '\n -------------- \n';
            addedFiles.forEach((line) => {
              if('*'+line+'*'!=='**' && line.indexOf(removedAttachment) === -1){
                this.newCommentText += line+'\n';
              }
            });
          }
          if(this.newCommentText.indexOf('- '+this.fileText+':') === -1){
            this.newCommentText = comments;
          }
        }
        */
    };
    /**
     * onSavedAttachment - Method to add new files to a files array
     *
     * @param {Attachment}  newAttachment Object with a new file to attach
     */
    CommentDataComponent.prototype.onSavedAttachment = function (newAttachment) {
        if (newAttachment.attachment !== null) {
            this.newAttachments.push(newAttachment);
            /*
            if(this.newAttachments.length === 1){
              this.newCommentText = this.newCommentText!==undefined?this.newCommentText:"";
              if('*'+this.newCommentText+'*'!=='**'){
                this.newCommentText = this.newCommentText.trim()
              }
              this.newCommentText += '\n -------------- \n';
            } else if(this.newAttachments.length > 1){
              let comments = this.newCommentText.split(' -------------- ')[0].trim();
              let files = this.newCommentText.split(' -------------- ')[1];
              this.newCommentText = comments;
              this.newCommentText += '\n -------------- ';
              this.newCommentText += files;
            }
            this.newCommentText += '- '+this.fileText+': ' + newAttachment['attachment'].file_name + ' added!\n';
            */
        }
        else {
            this.addFilecomponent.cleanFiles();
            this.newAttachments = [];
            //this.newCommentText = this.newCommentText.split(' -------------- ')[0].trim();
        }
    };
    /**
     * saveComment - Method to save new comments
     */
    CommentDataComponent.prototype.saveComment = function () {
        var _this = this;
        if (this.newCommentText !== undefined &&
            this.newCommentText !== null &&
            this.newCommentText !== '') {
            var newComment_1 = new __WEBPACK_IMPORTED_MODULE_8__classes_comment__["a" /* Comment */]();
            newComment_1.body = this.newCommentText;
            newComment_1.created_at = this.datePipe.transform(new Date(), 'yyyy-MM-ddTHH:mm:ss');
            if (this.newAttachments !== undefined && this.newAttachments.length > 0) {
                newComment_1.uploads = [];
                newComment_1.attachments = [];
                this.newAttachments.forEach(function (file) {
                    newComment_1.uploads.push(file.token);
                    newComment_1.attachments.push(file.attachment);
                });
            }
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
            this.successMessage = '';
            this.translate
                .get('Comment added successfully!')
                .subscribe(function (res) {
                _this.successMessage = res;
            });
            var timer = __WEBPACK_IMPORTED_MODULE_3_rxjs_Rx__["Observable"].timer(1000, 50);
            this.sub = timer.subscribe(function (t) {
                _this.successMessage = '';
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
        template: __webpack_require__(510),
        styles: [__webpack_require__(476)]
    }),
    __metadata("design:paramtypes", [typeof (_c = typeof __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_2__ngx_translate_core__["c" /* TranslateService */]) === "function" && _c || Object, typeof (_d = typeof __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__["a" /* ZendeskService */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_5__services_zendesk_service__["a" /* ZendeskService */]) === "function" && _d || Object, typeof (_e = typeof __WEBPACK_IMPORTED_MODULE_1__angular_common__["DatePipe"] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_common__["DatePipe"]) === "function" && _e || Object])
], CommentDataComponent);

var _a, _b, _c, _d, _e;
//# sourceMappingURL=comment-data.component.js.map

/***/ }),

/***/ 411:
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
                    std = d.format('D MMMM HH:mm');
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
                                std = d.format('HH:mm:ss');
                                /*
                                let minuteAgo = moment(new Date()).subtract(1,'minute');
                                if (d.isBefore(minuteAgo)) {
                                    std = d.format('HH:mm:ss');
                                }else{
                                    std = 'just now';
                                }
                                */
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

//# sourceMappingURL=day-time.pipe.js.map

/***/ }),

/***/ 474:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(15)();
// imports


// module
exports.push([module.i, ".logout {\n  margin-top: -3.75rem;\n}\n\n.app_header{\n  padding-bottom: 1.5rem;\n}\n\n.appTittle {\n  margin-left: 1.8rem;\n  font-weight: bold;\n  color: #0a303d;\n  text-align: left;\n}\n\n.username{\n  font-size: small;\n  font-style: oblique;\n  display: block;\n}\n\nbutton.btn.btn-default{\n  background: #18353d;\n  color: #FFF;\n  vertical-align: middle;\n  padding: 7px 15px 7px 15px;\n  border-radius: 3px;\n  font-size: 1.0em;\n  border-top-color:#83858C;\n\tborder-right-color:#62656B;\n\tborder-bottom-color:#44464C;\n\tborder-left-color:#62656B;\n}\nbutton.btn.btn-default:hover{\n  background:#f89d1f;\n\tborder-top-color:#6D8197;\n\tborder-right-color:#475D75;\n\tborder-bottom-color:#273E5A;\n\tborder-left-color:#475D75;\n}\n\n.supportGuide{\n  vertical-align: middle;\n  padding: 7px 15px 7px 15px;\n  border-radius: 3px;\n  font-size: 1.0em;\n  margin-top: -3.75rem;\n  text-decoration:none;\n  background: #E2E7EA;\n  color: #333333;\n}\n.supportGuide:hover{\n  text-decoration:none;\n  background: #4581B3;\n  color: #fff;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 475:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(15)();
// imports


// module
exports.push([module.i, "span.glyphicon.glyphicon-paperclip {\n  background-color: transparent;\n}\n.btn.btn-sm.btn-default.btn-block.add-file-button {\n  margin-bottom: 0.5rem;\n  background-color: transparent;\n}\n.btn.btn-sm.btn-default.btn-block.add-file-button\n  span.glyphicon.glyphicon-paperclip {\n  background-color: transparent;\n}\n.add-file-server-button {\n  width: 19rem;\n  height: 2.75rem;\n  white-space: normal;\n  font-size: smaller;\n}\nspan.sr-only {\n  position: relative;\n  background-color: transparent;\n  color: #f0981b;\n}\n.panel-footer {\n  background-color: #fff;\n  border-top: none;\n}\n.domain-block {\n  margin-top: 2rem;\n}\n.objectVisible {\n  display: block;\n}\n.objectNoVisible {\n  display: none;\n}\n.groupSeparation {\n  border: none;\n}\n\n.attached-file {\n  color: #f0981b;\n  background-color: #325764;\n  padding: 0.5rem;\n  border-radius: 0.25rem;\n  margin: 0.25rem;\n  width: 19rem;\n  height: 2.75rem;\n}\n.attached-file span > span {\n  color: #ffffff;\n  background-color: #325764;\n  font-style: oblique;\n  font-size: small;\n  font-family: Arial, Helvetica, sans-serif;\n}\n\n.groupTitle {\n  margin-bottom: 1rem;\n  text-decoration: underline;\n  text-transform: uppercase;\n  font-weight: bolder;\n  font-family: Arial, Helvetica, sans-serif;\n}\n\n.instance-offline {\n  width: 19rem;\n  height: 2.75rem;\n  padding: 0.5rem;\n  border-radius: 0.25rem;\n  margin: 0.25rem;\n  margin: 0 auto;\n  color: #fff;\n  font-family: Arial, Helvetica, sans-serif;\n  font-weight: bold;\n}\n\n.group-border {\n  width: 50%;\n  min-height: 15rem;\n  height: 100%;\n  min-width: 21.75rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 476:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(15)();
// imports


// module
exports.push([module.i, ".new_comment {\n  resize: none;\n  outline: none;\n  width: 100%;\n  padding: 10px;\n  height: 100%;\n  border-radius: 5px;\n  background-color: #ffffff;\n  margin-bottom: 0.5rem;\n  height: 9rem;\n}\n\n.comment_box {\n  border: 0.15rem solid #cfcfcf;\n  box-shadow: 0.5rem 0.5rem #cfcfcf;\n  margin-bottom: 1rem;\n  padding: 0.25rem;\n  text-align: left;\n}\n\npre code {\n  padding: 0;\n  font-size: 1.25rem;\n  margin-left: -1.5rem;\n}\n\n.attached-file {\n  color: #f0981b;\n  background-color: #325764;\n  padding: 0.5rem;\n  border-radius: 0.25rem;\n  margin: 0.25rem;\n}\n.attached-file span {\n  color: #ffffff;\n  background-color: #325764;\n  font-style: oblique;\n  font-size: small;\n  font-family: Arial, Helvetica, sans-serif;\n}\n.authorName {\n  white-space: nowrap;\n}\n.objectVisible {\n  display: block;\n}\n.objectNoVisible {\n  display: none;\n}\n\n/* The switch - the box around the slider */\n.switch-files {\n  text-align: right;\n  margin-bottom: 2rem;\n}\n.switch-label {\n  position: relative;\n  top: -1rem;\n}\n.switch {\n  position: relative;\n  display: inline-block;\n  top: 0.5rem;\n  width: 6rem;\n  height: 3rem;\n}\n\n/* Hide default HTML checkbox */\n.switch input {\n  display: none;\n}\n\n/* The slider */\n.slider {\n  position: absolute;\n  cursor: pointer;\n  top: 0;\n  left: 0;\n  right: 0;\n  bottom: 0;\n  background-color: #ccc;\n  transition: 0.3s;\n}\n\n.slider:before {\n  position: absolute;\n  content: '';\n  height: 2.25rem;\n  width: 2.25rem;\n  left: 0.5rem;\n  bottom: 0.4rem;\n  background-color: #325764;\n  transition: 0.3s;\n}\n\ninput:checked + .slider {\n  background-color: #ccc;\n}\n\ninput:focus + .slider {\n  box-shadow: 0 0 0.15rem #ccc;\n}\n\ninput:checked + .slider:before {\n  transform: translateX(2.25rem);\n}\n\n/* Rounded sliders */\n.slider.round {\n  border-radius: 3rem;\n}\n\n.slider.round:before {\n  border-radius: 3rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 477:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(15)();
// imports


// module
exports.push([module.i, ".returnBack {\n  margin-top: 0.75rem;\n}\n\nspan.glyphicon.glyphicon-chevron-left{\n  background-color: transparent;\n}\n\nspan.glyphicon.glyphicon-chevron-left::before{\n    margin-left: -.15rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 478:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(15)();
// imports


// module
exports.push([module.i, ".addTicket {\n  margin-top: 0.75rem;\n  font-size: .95rem;\n  margin-right: 1.5rem;\n}\n\n.btn.btn-primary,\n.form-control{\n    font-size: 1.05rem;\n    height: auto;\n}\n.btn.btn-primary{\n  display: block;\n  padding: 8px 13px 7px;\n  text-align: center;\n  background-position: top right;\n  background-repeat: no-repeat;\n  border-color: #80929B;\n  background: #FCFFFF;\n  color:#000;\n}\n\n.btn.btn-primary.active{\n  background: #f89d1f;\n}\n\nspan.glyphicon.glyphicon-chevron-up,\nspan.glyphicon.glyphicon-chevron-down,\nspan.glyphicon.glyphicon-plus{\n  background-color: transparent;\n  margin-left: .15rem;\n}\n\nspan.glyphicon.glyphicon-plus::before{\n  margin-left: -.15rem;\n}\n\n\ntable.table-responsive.table-striped.table-sm.table-inverse.table-sortable{\n  margin-top: 5rem;\n}\n\n.selectable{\n    cursor: pointer;\n    text-align: left;\n    font-size: 1.15rem\n}\n\n.transparent{\n  background-color: transparent;\n}\n\n.alert-info{\n  background-color: #325764;\n  border-color: #325764;\n}\n\n.info{\n    color: #fff;\n    text-align: left;\n}\n\n.close{\n  opacity:1;\n}\n.closeMessage{\n  color: #f89d1f;\n  font-size: x-large;\n}\n.panel.panel-default{\n  margin-top: 1rem;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 479:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(15)();
// imports


// module
exports.push([module.i, "button.btn.btn-sm.btn-primary.btn-block{\n  margin-top:.75rem;\n  text-decoration:none;\n  background: #E2E7EA;\n  color: #333333;\n}\nbutton.btn.btn-sm.btn-primary.btn-block:hover{\n  text-decoration:none;\n  background: #4581B3;\n  color: #f89d1f;\n}\n\n#inputEmail{\n  margin-top:.5rem;\n}\n\n.transparent{\n  background-color: transparent;\n}\n\n.form-signin-heading{\n  color:#fff;\n  font-weight: 600;\n}\n\n.alert-info{\n  background-color: #325764;\n  border-color: #325764;\n}\n\n.info{\n    color: #fff;\n    text-align: left;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 480:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(15)();
// imports


// module
exports.push([module.i, ".form-group.required .control-label:after {\n    color: #d00;\n    content: \"*\";\n    margin-left: .5rem;\n}\n\n.form-group .control-label,\n.form-group.required .control-label {\n  margin-bottom:.25rem;\n  text-align: right;\n}\n\n.form-control{\n  margin-bottom: 1rem;  \n}\n\n.discardChanges {\n  margin-top: -.5rem;\n}\n\nspan.glyphicon.glyphicon-remove-circle,\nspan.glyphicon.glyphicon-ok-circle{\n  background-color: transparent;\n  margin-left: .15rem\n}\n\nspan.glyphicon.glyphicon-remove-circle::before,\nspan.glyphicon.glyphicon-ok-circle::before{\n    margin-left: -.15rem;\n}\n.attached-file-list {\n  color:#f0981b;\n  background-color: #325764;\n  padding: .25rem;\n  border-radius: .25rem;\n  margin: .25rem;\n  margin-left: .25rem;\n}\n.attached-file-list span{\n  color: #ffffff;\n  background-color: #325764;\n  font-style: oblique;\n  font-size: small;\n  font-family: Arial, Helvetica, sans-serif;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 481:
/***/ (function(module, exports, __webpack_require__) {

exports = module.exports = __webpack_require__(15)();
// imports


// module
exports.push([module.i, "@media (min-width: 768px){\n  .detailContent {\n    border-left: thin solid #f0981b;\n    margin-left:1.5rem;\n    margin-top:1rem;\n  }\n}\n@media (max-width: 767px){\n  .detailContent {\n    border-left:none;\n  }\n}\n\n.detailData{\n  margin:.5rem;\n  padding: .25rem;\n  letter-spacing: 0.1rem;\n  text-align: left;\n}\n\n.detailData .titleDetailData{\n   float:left;\n}\n\n.detailData .contentDetailData{\n   float:right;\n   display:-ms-flexbox;\n   display:flex;\n}\n\n.titleData{\n  margin-top:.25rem;\n}\n\n.fileList{\n  margin:.5rem;\n  padding: .25rem;\n  letter-spacing: .05rem;\n  display:block;\n  text-align: left;\n}\n\n.attached-file-list {\n  color:#f0981b;\n  background-color: #325764;\n  padding: .5rem;\n  border-radius: .25rem;\n  margin: .25rem;\n  margin-left: .25rem;\n\n}\n.attached-file-list span{\n  color: #ffffff;\n  background-color: #325764;\n  font-style: oblique;\n  font-size: small;\n  font-family: Arial, Helvetica, sans-serif;\n}\n.attached-list{\n  float:right;\n}\n", ""]);

// exports


/*** EXPORTS FROM exports-loader ***/
module.exports = module.exports.toString();

/***/ }),

/***/ 485:
/***/ (function(module, exports, __webpack_require__) {

var map = {
	"./af": 153,
	"./af.js": 153,
	"./ar": 160,
	"./ar-dz": 154,
	"./ar-dz.js": 154,
	"./ar-kw": 155,
	"./ar-kw.js": 155,
	"./ar-ly": 156,
	"./ar-ly.js": 156,
	"./ar-ma": 157,
	"./ar-ma.js": 157,
	"./ar-sa": 158,
	"./ar-sa.js": 158,
	"./ar-tn": 159,
	"./ar-tn.js": 159,
	"./ar.js": 160,
	"./az": 161,
	"./az.js": 161,
	"./be": 162,
	"./be.js": 162,
	"./bg": 163,
	"./bg.js": 163,
	"./bn": 164,
	"./bn.js": 164,
	"./bo": 165,
	"./bo.js": 165,
	"./br": 166,
	"./br.js": 166,
	"./bs": 167,
	"./bs.js": 167,
	"./ca": 168,
	"./ca.js": 168,
	"./cs": 169,
	"./cs.js": 169,
	"./cv": 170,
	"./cv.js": 170,
	"./cy": 171,
	"./cy.js": 171,
	"./da": 172,
	"./da.js": 172,
	"./de": 175,
	"./de-at": 173,
	"./de-at.js": 173,
	"./de-ch": 174,
	"./de-ch.js": 174,
	"./de.js": 175,
	"./dv": 176,
	"./dv.js": 176,
	"./el": 177,
	"./el.js": 177,
	"./en-au": 178,
	"./en-au.js": 178,
	"./en-ca": 179,
	"./en-ca.js": 179,
	"./en-gb": 180,
	"./en-gb.js": 180,
	"./en-ie": 181,
	"./en-ie.js": 181,
	"./en-nz": 182,
	"./en-nz.js": 182,
	"./eo": 183,
	"./eo.js": 183,
	"./es": 185,
	"./es-do": 184,
	"./es-do.js": 184,
	"./es.js": 185,
	"./et": 186,
	"./et.js": 186,
	"./eu": 187,
	"./eu.js": 187,
	"./fa": 188,
	"./fa.js": 188,
	"./fi": 189,
	"./fi.js": 189,
	"./fo": 190,
	"./fo.js": 190,
	"./fr": 193,
	"./fr-ca": 191,
	"./fr-ca.js": 191,
	"./fr-ch": 192,
	"./fr-ch.js": 192,
	"./fr.js": 193,
	"./fy": 194,
	"./fy.js": 194,
	"./gd": 195,
	"./gd.js": 195,
	"./gl": 196,
	"./gl.js": 196,
	"./gom-latn": 197,
	"./gom-latn.js": 197,
	"./he": 198,
	"./he.js": 198,
	"./hi": 199,
	"./hi.js": 199,
	"./hr": 200,
	"./hr.js": 200,
	"./hu": 201,
	"./hu.js": 201,
	"./hy-am": 202,
	"./hy-am.js": 202,
	"./id": 203,
	"./id.js": 203,
	"./is": 204,
	"./is.js": 204,
	"./it": 205,
	"./it.js": 205,
	"./ja": 206,
	"./ja.js": 206,
	"./jv": 207,
	"./jv.js": 207,
	"./ka": 208,
	"./ka.js": 208,
	"./kk": 209,
	"./kk.js": 209,
	"./km": 210,
	"./km.js": 210,
	"./kn": 211,
	"./kn.js": 211,
	"./ko": 212,
	"./ko.js": 212,
	"./ky": 213,
	"./ky.js": 213,
	"./lb": 214,
	"./lb.js": 214,
	"./lo": 215,
	"./lo.js": 215,
	"./lt": 216,
	"./lt.js": 216,
	"./lv": 217,
	"./lv.js": 217,
	"./me": 218,
	"./me.js": 218,
	"./mi": 219,
	"./mi.js": 219,
	"./mk": 220,
	"./mk.js": 220,
	"./ml": 221,
	"./ml.js": 221,
	"./mr": 222,
	"./mr.js": 222,
	"./ms": 224,
	"./ms-my": 223,
	"./ms-my.js": 223,
	"./ms.js": 224,
	"./my": 225,
	"./my.js": 225,
	"./nb": 226,
	"./nb.js": 226,
	"./ne": 227,
	"./ne.js": 227,
	"./nl": 229,
	"./nl-be": 228,
	"./nl-be.js": 228,
	"./nl.js": 229,
	"./nn": 230,
	"./nn.js": 230,
	"./pa-in": 231,
	"./pa-in.js": 231,
	"./pl": 232,
	"./pl.js": 232,
	"./pt": 234,
	"./pt-br": 233,
	"./pt-br.js": 233,
	"./pt.js": 234,
	"./ro": 235,
	"./ro.js": 235,
	"./ru": 236,
	"./ru.js": 236,
	"./sd": 237,
	"./sd.js": 237,
	"./se": 238,
	"./se.js": 238,
	"./si": 239,
	"./si.js": 239,
	"./sk": 240,
	"./sk.js": 240,
	"./sl": 241,
	"./sl.js": 241,
	"./sq": 242,
	"./sq.js": 242,
	"./sr": 244,
	"./sr-cyrl": 243,
	"./sr-cyrl.js": 243,
	"./sr.js": 244,
	"./ss": 245,
	"./ss.js": 245,
	"./sv": 246,
	"./sv.js": 246,
	"./sw": 247,
	"./sw.js": 247,
	"./ta": 248,
	"./ta.js": 248,
	"./te": 249,
	"./te.js": 249,
	"./tet": 250,
	"./tet.js": 250,
	"./th": 251,
	"./th.js": 251,
	"./tl-ph": 252,
	"./tl-ph.js": 252,
	"./tlh": 253,
	"./tlh.js": 253,
	"./tr": 254,
	"./tr.js": 254,
	"./tzl": 255,
	"./tzl.js": 255,
	"./tzm": 257,
	"./tzm-latn": 256,
	"./tzm-latn.js": 256,
	"./tzm.js": 257,
	"./uk": 258,
	"./uk.js": 258,
	"./ur": 259,
	"./ur.js": 259,
	"./uz": 261,
	"./uz-latn": 260,
	"./uz-latn.js": 260,
	"./uz.js": 261,
	"./vi": 262,
	"./vi.js": 262,
	"./x-pseudo": 263,
	"./x-pseudo.js": 263,
	"./yo": 264,
	"./yo.js": 264,
	"./zh-cn": 265,
	"./zh-cn.js": 265,
	"./zh-hk": 266,
	"./zh-hk.js": 266,
	"./zh-tw": 267,
	"./zh-tw.js": 267
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
webpackContext.id = 485;


/***/ }),

/***/ 508:
/***/ (function(module, exports) {

module.exports = "\n<div class=\"app_header\">\n  <h1 class=\"appTittle\">{{'Payara Support' | translate}}\n    <span *ngIf=\"loginService.initiating && loginService.user\"\n          class=\"username\">{{loginService.user.name}} [{{loginService.user.email}}]\n    </span>\n  </h1>\n  <div class=\"pull-right\">\n  <a class=\"btn btn-sm btn-default supportGuide\" target=\"_blank\" *ngIf=\"!isCurrentRoute('login')\"\n      href=\"{{loginService.connectionData.supportGuideURL}}{{loginService.connectionData.supportType}}\"\n      placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Download suppport guide PDF' | translate}}\">\n      {{'Support Guide' | translate}}\n  </a>\n  <button class=\"btn btn-sm btn-default logout\" *ngIf=\"!isCurrentRoute('login')\" (click)=\"logout()\"\n          placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Logout from Zendesk' | translate}}\">\n    {{'Logout' | translate}}\n    <span class=\"glyphicon glyphicon-off\" aria-hidden=\"true\">\n    </span>\n  </button>\n</div>\n</div>\n\n\n<router-outlet></router-outlet>\n"

/***/ }),

/***/ 509:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\">\n  <div class=\"panel-heading\">\n    <h4>{{title | translate}}</h4>\n  </div>\n  <div class=\"panel-body\">\n    <div class=\"row\">\n      <div class=\"col-xs-6 group-border\">\n        <div class=\"col-xs-12\">\n          <hr class=\"groupSeparation\">\n          <h5 class=\"groupTitle\">Domain.xml</h5>\n        </div>\n        <div class=\"col-xs-12\" placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Edit or deattach' | translate}} Domain.xml\">\n          <bSwitch [switch-label-text]=\"'Domain.xml'\" [(ngModel)]=\"xmlLoaded\" [switch-handle-width]=\"80\" [switch-label-width]=\"100\"\n            [switch-animate]=\"true\" [switch-inverse]=\"true\" [switch-off-text]=\"'NO'\" [switch-on-text]=\"xmlLoading\" [switch-on-color]=\"xmlColor\"\n            [switch-off-color]=\"'default'\" [switch-size]=\"'small'\" (onChangeState)=\"showEditor()\">\n          </bSwitch>\n        </div>\n        <div class=\"col-xs-12\">\n          <hr class=\"groupSeparation\">\n        </div>\n      </div>\n\n      <div class=\"col-xs-12 domain-block group-border\" [ngClass]=\"{\n                                              'objectVisible': isVisibleEditor,\n                                              'objectNoVisible': !isVisibleEditor\n                                            }\">\n        <div class=\"panel panel-default\">\n          <div class=\"panel-heading\">\n            <h5>{{'Edit' | translate}} Domain.xml</h5>\n          </div>\n          <div class=\"panel-body\">\n            <div class=\"row\" placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Remove or change sensitive data from' | translate}} Domain.xml\">\n              <textarea id=\"{{elementId}}\"></textarea>\n            </div>\n          </div>\n          <div class=\"panel-footer\">\n            <div class=\"row\">\n              <div class=\"col-xs-8\">\n                <div *ngIf=\"loadingMessageXml\" class=\"progress\">\n                  <div class=\"progress-bar progress-bar-striped active\" role=\"progressbar\" aria-valuenow=\"100\" aria-valuemin=\"0\" aria-valuemax=\"100\"\n                    style=\"width: 100%\">\n                    <span class=\"sr-only\">{{loadingMessageXml | translate}}</span>\n                  </div>\n                </div>\n              </div>\n              <div class=\"col-xs-4\">\n                <button class=\"btn btn-default pull-right\" (click)=\"saveXml()\" placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Attach to request' | translate}} Domain.xml\">{{'Save' | translate}}</button>\n                <button class=\"btn btn-primary pull-right\" (click)=\"discardXml()\" placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Discard changes on' | translate}} Domain.xml\">{{'Discard' | translate}}</button>\n              </div>\n            </div>\n          </div>\n        </div>\n      </div>\n      <div class=\"col-xs-6 group-border\" *ngFor=\"let group of groups\">\n        <div class=\"col-xs-12\">\n          <hr class=\"groupSeparation\">\n          <h5 class=\"groupTitle\">{{group}}</h5>\n        </div>\n        <div class=\"col-xs-12\" *ngIf=\"filter(fileButtons,group).length === 0\">\n          <div class=\"alert alert-info instance-offline\" role=\"alert0\">\n            {{'Instance offline' | translate}}\n          </div>\n        </div>\n        <div class=\"col-xs-12\" *ngFor=\"let fileButton of filter(fileButtons,group)\" placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Attach or deattach to request' | translate}} {{fileButton.title}}\">\n          <bSwitch [switch-label-text]=\"fileButton.title\" [(ngModel)]=\"fileButton.uploaded\" [switch-handle-width]=\"50\" [switch-label-width]=\"155\"\n            [switch-animate]=\"true\" [switch-inverse]=\"true\" [switch-off-text]=\"'NO'\" [switch-on-text]=\"'OK'\" [switch-on-color]=\"'success'\"\n            [switch-off-color]=\"'default'\" [switch-size]=\"'small'\" (onChangeState)=\"searchFile(fileButton)\">\n          </bSwitch>\n        </div>\n        <div class=\"col-xs-12\">\n          <hr class=\"groupSeparation\">\n        </div>\n      </div>\n      <div class=\"col-xs-12\">\n        <hr class=\"groupSeparation\">\n      </div>\n      <div class=\"col-xs-6 group-border\">\n        <div class=\"col-xs-12\">\n          <hr class=\"groupSeparation\">\n          <h5 class=\"groupTitle\">{{'File Upload' | translate}}</h5>\n        </div>\n        <div class=\"col-xs-12\">\n          <label class=\"btn btn-sm btn-file add-file-server-button\" placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Attach to request' | translate}} {{'a file selected from local file system' | translate}}\">\n            <span class=\"glyphicon glyphicon-paperclip\" aria-hidden=\"true\"></span>\n            {{'Browse' | translate}}\n            <input type=\"file\" multiple class=\"hidden\" (change)=\"otherFile($event)\">\n          </label>\n        </div>\n        <div class=\"col-xs-12\">\n          <hr class=\"groupSeparation\">\n        </div>\n      </div>\n      <div class=\"col-xs-6 group-border\" *ngIf=\"otherfiles.length > 0\">\n        <div class=\"col-xs-12\">\n          <hr class=\"groupSeparation\">\n          <h5 class=\"groupTitle\">{{'Files to be added' | translate}}</h5>\n        </div>\n        <div class=\"col-xs-12\" *ngFor=\"let file of otherfiles\">\n          <label class=\"btn btn-sm btn-file attached-file\" (click)=\"removeFile(file)\" placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Remove file to upload' | translate}}\">\n            <span class=\"glyphicon glyphicon-trash\" aria-hidden=\"true\">\n              <span>{{file.name}}</span>\n            </span>\n          </label>\n        </div>\n        <div class=\"col-xs-12\">\n          <hr class=\"groupSeparation\">\n        </div>\n      </div>\n    </div>\n    <div class=\"row\">\n      <div class=\"col-xs-12\">\n        <hr class=\"groupSeparation\">\n        <div *ngIf=\"loadingMessage\" class=\"progress\">\n          <div class=\"progress-bar progress-bar-striped active\" role=\"progressbar\" aria-valuenow=\"100\" aria-valuemin=\"0\" aria-valuemax=\"100\"\n            style=\"width: 100%\">\n            <span class=\"sr-only\">{{loadingMessage | translate}}</span>\n          </div>\n        </div>\n        <div *ngIf=\"errorMessage\" class=\"alert alert-danger alert-dismissible\" role=\"alert1\">\n          {{errorMessage}}\n        </div>\n        <div *ngIf=\"successMessage\" class=\"alert alert-success alert-dismissible\" role=\"alert2\">\n          {{successMessage}}\n        </div>\n      </div>\n    </div>\n  </div>\n</div>\n"

/***/ }),

/***/ 510:
/***/ (function(module, exports) {

module.exports = "<div class=\"row\">\n  <div *ngIf=\"errorMessage\" class=\"alert alert-danger alert-dismissible\" role=\"alert1\">\n    {{errorMessage}}\n  </div>\n  <div *ngIf=\"successMessage\" class=\"alert alert-success alert-dismissible\" role=\"alert2\">\n    {{successMessage}}\n  </div>\n</div>\n\n<div class=\"newCommentContainer\" *ngIf=\"ticket.status!=='closed' && ticket.status!=='solved'\">\n  <div class=\"row\">\n    <div class=\"col-md-12 col-sm-12 col-xs-12\">\n      <textarea class=\"new_comment\" [(ngModel)]=\"newCommentText\" (keyup)=\"keyUpEvent($event)\" placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Insert comment text' | translate}}\"></textarea>\n    </div>\n    <div class=\"col-md-12 col-sm-12 col-xs-12\">\n      <button class=\"btn btn-sm btn-default btn-block\" type=\"submit\" (click)=\"saveComment()\" placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Send comment with attachements' | translate}}\">\n        {{'Submit' | translate}}\n        <span class=\"glyphicon glyphicon-ok\" aria-hidden=\"true\">\n        </span>\n      </button>\n    </div>\n    <div class=\"col-md-12 col-sm-12 col-xs-12 switch-files\">\n      <span class=\"switch-label\">Show Files</span>\n      <label class=\"switch\">\n        <input type=\"checkbox\" [(ngModel)]=\"showFiles\">\n        <span class=\"slider round\"></span>\n      </label>\n    </div>\n    <div class=\"col-md-12 col-sm-12 col-xs-12\" [ngClass]=\"{\n      'objectVisible': showFiles,\n      'objectNoVisible': !showFiles\n    }\">\n      <app-add-file class=\"addFile\" (saved)=\"onSavedAttachment($event)\" (removed)=\"onRemovedAttachment($event)\" [title]=\"'Attach files to new comment of the request'\">\n      </app-add-file>\n    </div>\n  </div>\n</div>\n\n<div class=\"commentContainer\" *ngFor=\"let comment of comments\">\n  <div class=\"row\">\n    <div class=\"col-sm-9\">\n      <pre class=\"comment_box\">\n        <markdown>{{comment.body}}</markdown>\n      </pre>\n    </div>\n    <div class=\"col-sm-3\">\n      <p class=\"authorName\">{{comment.author_name}}</p>\n      <p placement=\"bottom\" delay=\"1500\" tooltip=\"{{comment.created_at}}\">{{comment.created_at | dayTime}}</p>\n    </div>\n  </div>\n  <div class=\"row\" *ngIf=\"comment.attachments !== undefined && comment.attachments.length>0\">\n    <div class=\"col-xs-12 col-sm-6 col-md-4 col-lg-2\" *ngFor=\"let file of comment.attachments\">\n      <a href=\"{{file.content_url}}\">\n        <span class=\"\tglyphicon glyphicon-download-alt attached-file\" aria-hidden=\"true\" placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Download file' | translate}} {{file.file_name}}\">\n          <span>{{file.file_name}}</span>\n        </span>\n      </a>\n    </div>\n  </div>\n  <hr>\n</div>\n"

/***/ }),

/***/ 511:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\" *ngIf=\"ticket\">\n  <div class=\"panel-heading\">\n    <h3>\n      {{'Request' | translate}} #{{ticket.id}} <strong>{{ticket.subject}}</strong>\n      <button class=\"btn btn-sm pull-right returnBack\" routerLink=\"/list\"\n              placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Return to requests list' | translate}}\">\n        <span class=\"glyphicon glyphicon-chevron-left\" aria-hidden=\"true\">\n        </span>\n        {{'Back' | translate}}\n      </button>\n    </h3>\n  </div>\n  <div class=\"panel-body\">\n    <div class=\"row\">\n      <div class=\"col-sm-7\">\n        <app-comment-data [(ticket)]=\"ticket\" (saved)=\"onSavedComment(ticket,$event)\"></app-comment-data>\n      </div>\n      <div class=\"col-sm-5\">\n        <app-ticket-data [(ticket)]=\"ticket\"></app-ticket-data>\n      </div>\n  </div>\n</div>\n"

/***/ }),

/***/ 512:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\" *ngIf=\"tickets\">\n  <div class=\"panel-heading\">\n    <div class=\"alert alert-info alert-dismissible\" role=\"alert\" *ngIf=\"showMessage\">\n      <button type=\"button\" class=\"close\" data-dismiss=\"alert\" aria-label=\"Close\" (click)=\"hideMessage()\"\n              placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Close this advice' | translate}}\">\n        <span aria-hidden=\"true\" class=\"transparent closeMessage\">&times;</span>\n      </button>\n      <p class=\"transparent info\">{{'Welcome to Zendesk support for Payara Server' | translate}}!</p>\n      <br/>\n      <p class=\"transparent info\">{{'usefulInfo' | translate}}</p>\n      <br/>\n    </div>\n    <h3>\n      {{'My requests' | translate}}\n      <button class=\"btn btn-sm pull-right addTicket\" routerLink=\"/new\"\n              placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Create New Request' | translate}}\">\n        {{'New Request' | translate}}\n        <span class=\"glyphicon glyphicon-plus\" aria-hidden=\"true\">\n        </span>\n      </button>\n    </h3>\n  </div>\n  <div class=\"panel-body\">\n      <div class=\"col-sm-6\">\n        <input type=\"text\" class=\"form-control\" [(ngModel)]=\"query\" (keyup)=\"filter()\" placeholder=\"{{'Filter' | translate}}\" placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Type to filter the tickets below' | translate}}\" />\n      </div>\n      <div class=\"col-sm-4\">\n        <div class=\"btn-group pull-right\" data-toggle=\"buttons\">\n          <label class=\"btn btn-primary\" [ngClass]=\"{active: userBool}\" placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Select to filter tickets from user' | translate}}\">\n            <input type=\"radio\" autocomplete=\"off\" (click)=\"updateTickets(true)\"/>{{'User' | translate}}\n          </label>\n          <label class=\"btn btn-primary\" [ngClass]=\"{active: !userBool}\" placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Select to filter tickets from organization' | translate}}\">\n            <input type=\"radio\" autocomplete=\"off\" (click)=\"updateTickets(false)\"/>{{'Organization' | translate}}\n          </label>\n        </div>\n      </div>\n      <div class=\"col-sm-2 pull-right\">\n       <select class=\"form-control\" id=\"statusFilter\" [(ngModel)]=\"statusFilter\" (change)=\"filterStatus()\"  placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Select to filter tickets by status' | translate}}\">\n         <option value=\"any\">{{'Any' | translate}}</option>\n         <option *ngFor=\"let statusOption of statusFields\" value=\"{{statusOption.value}}\">{{statusOption.name | translate}}</option>\n       </select>\n     </div>\n    <table class=\"table table-responsive table-striped table-sm table-inverse table-sortable\">\n      <thead>\n        <tr>\n          <th (click)=\"changeSorting('id')\"\n              placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Sort by Id number' | translate}}\">Id\n            <span *ngIf=\"sort.column === 'id' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'id' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th (click)=\"changeSorting('subject')\"\n              placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Sort by request subject' | translate}}\">{{'Subject' | translate}}\n            <span *ngIf=\"sort.column === 'subject' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'subject' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th *ngIf=\"!userBool\" (click)=\"changeSorting('submitter_name')\"\n              placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Sort by request user' | translate}}\">{{'Request user' | translate}}\n            <span *ngIf=\"sort.column === 'submitter_name' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'submitter_name' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th (click)=\"changeSorting('created_at')\"\n              placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Sort by creation date' | translate}}\">{{'Created' | translate}}\n            <span *ngIf=\"sort.column === 'created_at' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'created_at' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th (click)=\"changeSorting('updated_at')\"\n              placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Sort by last modification date' | translate}}\">{{'Last activity' | translate}}\n            <span *ngIf=\"sort.column === 'updated_at' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'updated_at' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n          <th (click)=\"changeSorting('status')\"\n              placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Sort by request status' | translate}}\">{{'Status' | translate}}\n            <span *ngIf=\"sort.column === 'status' && !sort.descending\" class=\"glyphicon glyphicon-chevron-up\" aria-hidden=\"true\"></span>\n            <span *ngIf=\"sort.column === 'status' && sort.descending\" class=\"glyphicon glyphicon-chevron-down\" aria-hidden=\"true\"></span>\n          </th>\n        </tr>\n      </thead>\n      <tbody>\n        <tr *ngFor=\"let ticket of tickets\" (click)=\"ticketClicked(ticket)\" class=\"selectable\"\n            placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Go to request details...' | translate}}\">\n          <th scope=\"row\">{{ticket.id}}</th>\n          <td>{{ticket.subject}}</td>\n          <td *ngIf=\"!userBool\">{{ticket.submitter_name}}</td>\n          <td>{{ticket.created_at | dayTime}}</td>\n          <td>{{ticket.updated_at | dayTime}}</td>\n          <td style=\"text-align: center;\">\n              <span\n              [ngClass]=\"{\n                              'ticketOpen': ticket.status==='open',\n                              'ticketNew': ticket.status==='new',\n                              'ticketClosed': ticket.status==='closed',\n                              'ticketSolved': ticket.status==='solved',\n                              'ticketPending': ticket.status==='pending',\n                              'ticketHold': ticket.status==='hold'}\"\n              class=\"glyphicon glyphicon-flag\" aria-hidden=\"true\">\n                <span>{{ticket.status | translate}}</span>\n              </span>\n          </td>\n        </tr>\n      </tbody>\n    </table>\n  </div>\n</div>\n\n<div *ngIf=\"!tickets && errorMessage\" class=\"alert alert-danger\" role=\"alert\">{{errorMessage}}</div>\n"

/***/ }),

/***/ 513:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\" *ngIf=\"!loginService.initiating\">\n  <div class=\"panel-heading\">\n    <div class=\"alert alert-info\" role=\"alert\">\n      <h3 class=\"transparent form-signin-heading\">{{'Please sign in' | translate}}</h3>\n      <br/>\n      <p class=\"transparent info\">{{'Insert Zendesk`s email and password to get OauthToken to communicate' | translate}}</p>\n      <br/>\n      <p class=\"transparent info\">{{'loginInfo' | translate}}</p>\n    </div>\n  </div>\n  <div class=\"panel-body\">\n    <div class=\"row\">\n      <form class=\"form-signin\" id=\"login\">\n        <label for=\"inputEmail\" class=\"sr-only\">{{'Email address' | translate}}</label>\n        <input type=\"email\" id=\"inputEmail\" class=\"form-control\"\n                placeholder=\"{{'Email address' | translate}}\" required autofocus\n                [(ngModel)]=\"user.email\" name=\"email\" (keypress)=\"cleanError($event)\"\n                 placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Insert email address to login into Zendesk' | translate}}\">\n       <label for=\"inputPassword\" class=\"sr-only\">{{'Password' | translate}}</label>\n       <input type=\"password\" id=\"inputPassword\" class=\"form-control\"\n               placeholder=\"{{'Password' | translate}}\" required autofocus\n               [(ngModel)]=\"user.password\" name=\"password\" (keypress)=\"cleanError($event)\"\n                placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Insert password to login into Zendesk, it not will be stored' | translate}}\">\n        <button class=\"btn btn-sm btn-primary btn-block\" type=\"submit\" [disabled]=\"!user.email && !user.password\" (click)=\"loginToZendesk(user)\"\n                placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Login to Zendesk' | translate}}\">\n                {{'Sign in' | translate}}\n        </button>\n      </form>\n    </div>\n    <div class=\"row\">\n      <div *ngIf=\"errorMessage\" class=\"alert alert-danger\" role=\"alert\">{{errorMessage}}</div>\n    </div>\n  </div>\n</div>\n\n<button class=\"btn btn-sm btn-default pull-right\" (click)=\"shopSupport()\"\n        placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Go to hire support!' | translate}}\">\n  {{'Unsupported? Hire support now!' | translate}}\n  <span class=\"glyphicon glyphicon-shopping-cart\" aria-hidden=\"true\">\n  </span>\n</button>\n"

/***/ }),

/***/ 514:
/***/ (function(module, exports) {

module.exports = "<div class=\"panel panel-default\" *ngIf=\"ticketForm\">\n  <div class=\"panel-heading\">\n    <h3>\n      {{'Submit a request' | translate}}\n      <button class=\"btn btn-sm pull-right discardChanges\" (click)=\"discardChanges()\"\n              placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Discard Request Data' | translate}}\">\n        {{'Discard' | translate}}\n        <span class=\"glyphicon glyphicon-remove-circle\" aria-hidden=\"true\">\n        </span>\n      </button>\n      <button class=\"btn btn-sm pull-right discardChanges\" type=\"submit\"\n              [disabled]=\"!ticketForm.valid\" (click)=\"checkData(ticketForm)\"\n              placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Submit Request Data' | translate}}\">\n        {{'Submit' | translate}}\n        <span class=\"glyphicon glyphicon-ok-circle\" aria-hidden=\"true\">\n        </span>\n      </button>\n    </h3>\n  </div>\n  <div class=\"panel-body\">\n    <div *ngIf=\"errorMessage\" class=\"alert alert-danger alert-dismissible\" role=\"alert1\">\n      {{errorMessage}}\n    </div>\n    <div *ngIf=\"successMessage\" class=\"alert alert-success alert-dismissible\" role=\"alert2\">\n      {{successMessage}}\n    </div>\n    <form class=\"form-ticket form-vertical\" [formGroup]=\"ticketForm\">\n      <div class=\"form-group row\">\n        <label class=\"col-md-2 control-label\"></label>\n        <div class=\"col-md-5\">\n        <small *ngIf=\"!ticketForm.controls.subject.valid &&\n                      (ticketForm.controls.subject.dirty ||\n                      ticketForm.controls.subject.touched)\"\n                     class=\"badge text-warning\">\n          {{genericFields[0].title_in_portal | translate}} {{'is required' | translate}}\n        </small>\n      </div>\n      </div>\n      <div class=\"form-group required\" *ngIf=\"genericFields[0]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[0].title_in_portal | translate}}</label>\n         <div class=\"col-md-5\">\n           <input class=\"form-control\" id=\"subject\" formControlName=\"subject\" required type=\"text\"\n                  placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Request subject' | translate}}\"/>\n         </div>\n      </div>\n      <div class=\"form-group\" *ngIf=\"genericFields[3]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[3].title_in_portal | translate}}</label>\n         <div class=\"col-md-3\">\n           <select class=\"form-control\" id=\"type\" formControlName=\"type\"\n                   placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Request type' | translate}}\">\n             <option *ngFor=\"let typeOption of genericFields[3].system_field_options\" value=\"{{typeOption.value}}\">{{typeOption.name | translate}}</option>\n           </select>\n        </div>\n      </div>\n      <div class=\"form-group required\" *ngIf=\"genericFields[1]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[1].title_in_portal | translate}}</label>\n         <div class=\"col-md-5\">\n           <textarea class=\"form-control\" rows=\"5\" id=\"description\" formControlName=\"description\"\n                     placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Request description, the more specific, the easier it will be to help you' | translate}}\"></textarea>\n           <small *ngIf=\"!ticketForm.controls.description.valid &&\n                         (ticketForm.controls.description.dirty ||\n                         ticketForm.controls.description.touched)\"\n                        class=\"badge text-warning\">\n             {{genericFields[0].title_in_portal | translate}} {{'is required' | translate}}\n           </small>\n         </div>\n      </div>\n      <div class=\"form-group required\" *ngIf=\"genericFields[4]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[4].title_in_portal | translate}}</label>\n         <div class=\"col-md-3\">\n           <select class=\"form-control\" id=\"environment\" formControlName=\"environment\" required\n                   placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Request environment where issue appears' | translate}}\">\n              <option *ngFor=\"let environmentOption of genericFields[4].custom_field_options\" value=\"{{environmentOption.value}}\">{{environmentOption.name | translate}}</option>\n           </select>\n        </div>\n      </div>\n      <div class=\"form-group required\" *ngIf=\"genericFields[5]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[5].title_in_portal | translate}}</label>\n         <div class=\"col-md-3\">\n           <select class=\"form-control\" id=\"priority\" formControlName=\"priority\" required\n               placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Request priority' | translate}}\">\n              <option *ngFor=\"let priorityOption of genericFields[5].system_field_options\" value=\"{{priorityOption.value}}\">{{priorityOption.name | translate}}</option>\n           </select>\n        </div>\n      </div>\n      <div class=\"form-group required\" *ngIf=\"genericFields[15]\">\n         <label class=\"col-md-2 control-label\">{{genericFields[15].title_in_portal | translate}}</label>\n         <div class=\"col-md-3\">\n           <input class=\"form-control\" id=\"version\" formControlName=\"version\" required type=\"text\"\n                  placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Request version of Payara' | translate}}\"/>\n           <small *ngIf=\"!ticketForm.controls.version.valid &&\n                         (ticketForm.controls.version.dirty ||\n                         ticketForm.controls.version.touched)\"\n                        class=\"badge text-warning\">\n             {{genericFields[15].title_in_portal | translate}} {{'is required' | translate}}\n           </small>\n         </div>\n      </div>\n\n      <div class=\"form-group\">\n         <div [ngClass]=\"{\n                         'col-xs-8': newAttachments.length > 0,\n                         'col-xs-12': newAttachments.length === 0\n                       }\">\n           <app-add-file (saved)=\"onSavedAttachment($event)\"\n                         (removed)=\"onRemovedAttachment($event)\"\n                         [title]=\"'Attach files to new request'\">\n            </app-add-file>\n         </div>\n         <div class=\"col-xs-4\" *ngIf=\"newAttachments.length > 0\">\n           <strong>{{'Files list' | translate}}</strong>\n           &nbsp;\n           <div *ngFor=\"let file of newAttachments\">\n             <a href=\"{{file.attachment.content_url}}\">\n               <span class=\"glyphicon glyphicon-download-alt attached-file-list\" aria-hidden=\"true\">\n                 <span>{{file.attachment.file_name}}</span>\n               </span>\n             </a>\n           </div>\n         </div>\n      </div>\n    </form>\n  </div>\n</div>\n"

/***/ }),

/***/ 515:
/***/ (function(module, exports) {

module.exports = "  <div class=\"col-md-11 detailContent\">\n    <h5>\n      <strong>{{ticket.submitter_name}}</strong> {{'submitted this request' | translate}}\n    </h5>\n    <hr>\n    <p class=\"detailData\">\n      <span class=\"titleDetailData\">\n        <strong>{{'Status' | translate}}</strong>\n      </span>\n      <span class=\"contentDetailData\">\n        <span [ngClass]=\"{\n                        'ticketOpen': ticket.status==='open',\n                        'ticketNew': ticket.status==='new',\n                        'ticketClosed': ticket.status==='closed',\n                        'ticketSolved': ticket.status==='solved',\n                        'ticketPending': ticket.status==='pending',\n                        'ticketHold': ticket.status==='hold'}\"\n        class=\"glyphicon glyphicon-flag\" aria-hidden=\"true\">\n          <span>{{ticket.status | translate}}</span>\n        </span>\n      </span>\n    </p>\n    <br/>\n    <p class=\"detailData\">\n      <span class=\"titleDetailData\">\n        <strong>{{'Type' | translate}}</strong>\n      </span>\n      <span class=\"contentDetailData\">\n        {{ticket.type | translate | uppercase}}\n      </span>\n    </p>\n    <br/>\n    <p class=\"detailData\">\n      <span class=\"titleDetailData\">\n        <strong>{{'Priority' | translate}}</strong>\n      </span>\n      <span class=\"contentDetailData\">\n        {{ticket.priority | translate | uppercase}}\n      </span>\n    </p>\n    <br/>\n    <div *ngFor=\"let field of ticket.custom_fields\">\n      <div class=\"detailData\" *ngIf=\"field.title_in_portal!=='Defect ID'\">\n        <span class=\"titleDetailData\">\n          <strong>{{field.title_in_portal | translate}}</strong>\n        </span>\n        <span class=\"contentDetailData\">\n          <p>{{getValue(field) | translate}}</p>\n        </span>\n        <br/>\n      </div>\n    </div>\n    <div class=\"fileList\">\n      <strong>{{'Files list' | translate}}</strong>\n      <br/>\n      <div *ngFor=\"let file of files\" class=\"attached-list\">\n        <a href=\"{{file.content_url}}\">\n          <span class=\"\tglyphicon glyphicon-download-alt attached-file-list\" aria-hidden=\"true\"\n                placement=\"bottom\" delay=\"1500\" tooltip=\"{{'Download file' | translate}} {{file.file_name}}\">\n            <span>{{file.file_name}}</span>\n          </span>\n        </a>\n      </div>\n      <br/>\n    </div>\n  </div>\n"

/***/ }),

/***/ 59:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(3);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_http__ = __webpack_require__(31);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_rxjs_add_operator_toPromise__ = __webpack_require__(70);
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
        this.commonHeaders = new __WEBPACK_IMPORTED_MODULE_1__angular_http__["c" /* Headers */]();
        this.commonHeaders.set('Content-Type', 'application/json');
    }
    /**
     * setSpecialAdminIndicator - Method to configure the special admin indicator for the
     * service to use for secure REST requests.
     *
     * @param {string} specialAdminIndicator The string to use in the headers.
     */
    PayaraService.prototype.setSpecialAdminIndicator = function (specialAdminIndicator) {
        this.commonHeaders.set('X-GlassFish-admin', specialAdminIndicator);
    };
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
            return this.http.get(this.connectionData.payaraURL + 'configs/config/server-config/zendesk-support-configuration/get-zendesk-support-configuration.json', { headers: this.commonHeaders })
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
        var headers = new __WEBPACK_IMPORTED_MODULE_1__angular_http__["c" /* Headers */](this.commonHeaders);
        headers.append('Accept', 'application/json');
        headers.append('X-Requested-By', 'payara');
        return this.http.post(this.connectionData.payaraURL + 'configs/config/server-config/zendesk-support-configuration/set-zendesk-support-configuration', JSON.stringify({ emailAddress: email }), { headers: headers })
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
        var headers = new __WEBPACK_IMPORTED_MODULE_1__angular_http__["c" /* Headers */](this.commonHeaders);
        headers.append('Accept', 'application/json');
        headers.append('X-Requested-By', 'payara');
        return this.http.post(this.connectionData.payaraURL + url, JSON.stringify({
            "first": "",
            "target": "server-config",
            "__remove_empty_entries__": "true"
        }), { headers: headers })
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
        return this.http.get(this.connectionData.payaraURL + url, { headers: this.commonHeaders })
            .toPromise()
            .then(function (response) { return response; });
    };
    /**
     * getServerInstances - Method to call to the API to get Payara server instances
     *
     * @return {Promise<any>} Returns the response promise
     */
    PayaraService.prototype.getServerInstances = function () {
        return this.http.get(this.connectionData.payaraURL + 'servers/server.json', { headers: this.commonHeaders })
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
        return this.http.get(this.connectionData.payaraURL + 'list-instances.json?id=' + id, { headers: this.commonHeaders })
            .toPromise()
            .then(function (response) { return response.json().extraProperties.instanceList[0].status; });
    };
    /**
     * getPayaraVersion - Method to call to the API to get Payara server version
     *
     * @return {Promise<string>} Returns the response promise
     */
    PayaraService.prototype.getPayaraVersion = function () {
        return this.http.get(this.connectionData.payaraURL + 'version.json', { headers: this.commonHeaders })
            .toPromise()
            .then(function (response) { return response.json().extraProperties.version; });
    };
    return PayaraService;
}());
PayaraService = __decorate([
    __webpack_require__.i(__WEBPACK_IMPORTED_MODULE_0__angular_core__["Injectable"])(),
    __metadata("design:paramtypes", [typeof (_a = typeof __WEBPACK_IMPORTED_MODULE_1__angular_http__["b" /* Http */] !== "undefined" && __WEBPACK_IMPORTED_MODULE_1__angular_http__["b" /* Http */]) === "function" && _a || Object])
], PayaraService);

var _a;
//# sourceMappingURL=payara.service.js.map

/***/ }),

/***/ 60:
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
        shopUrl: 'https://www.payara.fish/choose_support'
    },
    supportGuides: 'https://api.payara.fish/api/payaraCustomer/supportGuide/'
};
//# sourceMappingURL=environment.js.map

/***/ }),

/***/ 794:
/***/ (function(module, exports, __webpack_require__) {

module.exports = __webpack_require__(389);


/***/ }),

/***/ 82:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return Ticket; });
var Ticket = (function () {
    function Ticket() {
    }
    return Ticket;
}());

//# sourceMappingURL=ticket.js.map

/***/ }),

/***/ 83:
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

//# sourceMappingURL=user.js.map

/***/ })

},[794]);
//# sourceMappingURL=main.bundle.js.map
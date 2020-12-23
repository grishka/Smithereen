var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var LayerManager = /** @class */ (function () {
    function LayerManager() {
        var _this = this;
        this.stack = [];
        this.escapeKeyListener = function (ev) {
            if (ev.keyCode == 27) {
                _this.maybeDismissTopLayer();
            }
        };
        this.scrim = ce("div", { id: "layerScrim", onclick: function () {
                _this.maybeDismissTopLayer();
            } });
        this.scrim.hide();
        document.body.appendChild(this.scrim);
        var container = ce("div", { id: "layerContainer" });
        container.hide();
        this.layerContainer = container;
        document.body.appendChild(container);
    }
    LayerManager.getInstance = function () {
        if (!LayerManager.instance) {
            LayerManager.instance = new LayerManager();
        }
        return LayerManager.instance;
    };
    LayerManager.prototype.show = function (layer) {
        var layerContent = layer.getContent();
        this.layerContainer.appendChild(layerContent);
        if (this.stack.length == 0) {
            this.scrim.showAnimated();
            this.layerContainer.show();
            layerContent.showAnimated(layer.getCustomAppearAnimation());
            document.body.addEventListener("keydown", this.escapeKeyListener);
            document.body.style.overflowY = "hidden";
        }
        else {
            var prevLayer = this.stack[this.stack.length - 1];
            prevLayer.getContent().hide();
            prevLayer.onHidden();
        }
        this.stack.push(layer);
        layer.onShown();
    };
    LayerManager.prototype.dismiss = function (layer) {
        var _this = this;
        var i = this.stack.indexOf(layer);
        if (i == -1)
            return;
        var layerContent = layer.getContent();
        if (isVisible(layerContent)) {
            layer.onHidden();
        }
        if (i == this.stack.length - 1) {
            this.stack.pop();
            if (this.stack.length) {
                var newLayer = this.stack[this.stack.length - 1];
                newLayer.getContent().show();
                newLayer.onShown();
            }
        }
        else {
            this.stack.splice(i, 1);
        }
        if (this.stack.length == 0) {
            document.body.removeEventListener("keydown", this.escapeKeyListener);
            var anim = layer.getCustomDismissAnimation();
            var duration = 200;
            if (anim) {
                duration = anim.options.duration;
                layerContent.hideAnimated(anim, function () {
                    _this.layerContainer.removeChild(layerContent);
                    _this.layerContainer.hide();
                    document.body.style.overflowY = "";
                });
            }
            else {
                this.layerContainer.removeChild(layerContent);
                this.layerContainer.hide();
                document.body.style.overflowY = "";
            }
            this.scrim.hideAnimated({ keyframes: [{ opacity: 1 }, { opacity: 0 }], options: { duration: duration, easing: "ease" } });
        }
        else {
            this.layerContainer.removeChild(layerContent);
        }
    };
    LayerManager.prototype.maybeDismissTopLayer = function () {
        var topLayer = this.stack[this.stack.length - 1];
        if (topLayer.allowDismiss())
            this.dismiss(topLayer);
    };
    return LayerManager;
}());
var BaseLayer = /** @class */ (function () {
    function BaseLayer() {
    }
    BaseLayer.prototype.show = function () {
        if (!this.content) {
            this.content = ce("div", { className: "layerContent" });
            var contentView = this.onCreateContentView();
            this.content.appendChild(contentView);
        }
        LayerManager.getInstance().show(this);
    };
    BaseLayer.prototype.dismiss = function () {
        LayerManager.getInstance().dismiss(this);
    };
    BaseLayer.prototype.getContent = function () {
        return this.content;
    };
    BaseLayer.prototype.allowDismiss = function () {
        return true;
    };
    BaseLayer.prototype.onShown = function () { };
    BaseLayer.prototype.onHidden = function () { };
    BaseLayer.prototype.getCustomDismissAnimation = function () {
        return { keyframes: [{ opacity: 1 }, { opacity: 0 }], options: { duration: 200, easing: "ease" } };
    };
    BaseLayer.prototype.getCustomAppearAnimation = function () {
        return { keyframes: [{ opacity: 0 }, { opacity: 1 }], options: { duration: 200, easing: "ease" } };
    };
    return BaseLayer;
}());
var Box = /** @class */ (function (_super) {
    __extends(Box, _super);
    function Box(title, buttonTitles, onButtonClick) {
        if (buttonTitles === void 0) { buttonTitles = []; }
        if (onButtonClick === void 0) { onButtonClick = null; }
        var _this = _super.call(this) || this;
        _this.closeable = true;
        _this.noPrimaryButton = false;
        _this.title = title;
        _this.buttonTitles = buttonTitles;
        _this.onButtonClick = onButtonClick;
        var contentWrap = ce("div", { className: "boxContent" });
        _this.contentWrap = contentWrap;
        return _this;
    }
    Box.prototype.onCreateContentView = function () {
        var _this = this;
        var content = ce("div", { className: "boxLayer" }, [
            this.titleBar = ce("div", { className: "boxTitleBar", innerText: this.title }),
            this.contentWrap,
            this.buttonBar = ce("div", { className: "boxButtonBar" })
        ]);
        if (!this.title)
            this.titleBar.hide();
        if (this.closeable) {
            this.closeButton = ce("span", { className: "close", title: lang("close"), onclick: function () { return _this.dismiss(); } });
            this.titleBar.appendChild(this.closeButton);
        }
        this.updateButtonBar();
        this.boxLayer = content;
        return content;
    };
    Box.prototype.setContent = function (content) {
        if (this.contentWrap.hasChildNodes) {
            for (var i = 0; i < this.contentWrap.children.length; i++)
                this.contentWrap.firstChild.remove();
        }
        this.contentWrap.appendChild(content);
    };
    Box.prototype.getButton = function (index) {
        return this.buttonBar.children[index];
    };
    Box.prototype.setButtons = function (buttonTitles, onButtonClick) {
        this.buttonTitles = buttonTitles;
        this.onButtonClick = onButtonClick;
        this.updateButtonBar();
    };
    Box.prototype.setCloseable = function (closeable) {
        this.closeable = closeable;
    };
    Box.prototype.allowDismiss = function () {
        return this.closeable;
    };
    Box.prototype.getCustomDismissAnimation = function () {
        if (mobile) {
            var height = this.boxLayer.offsetHeight + 32;
            return {
                keyframes: [{ transform: "translateY(0)" }, { transform: "translateY(100%)" }],
                options: { duration: 300, easing: "cubic-bezier(0.32, 0, 0.67, 0)" }
            };
        }
        return {
            keyframes: [{ opacity: 1, transform: "scale(1)" }, { opacity: 0, transform: "scale(0.95)" }],
            options: { duration: 200, easing: "ease" }
        };
    };
    Box.prototype.getCustomAppearAnimation = function () {
        if (mobile) {
            var height = this.boxLayer.offsetHeight + 32;
            console.log("height " + height);
            return {
                keyframes: [{ transform: "translateY(" + height + "px)" }, { transform: "translateY(0)" }],
                options: { duration: 600, easing: "cubic-bezier(0.22, 1, 0.36, 1)" }
            };
        }
        return {
            keyframes: [{ opacity: 0, transform: "scale(0.9)" }, { opacity: 1, transform: "scale(1)" }],
            options: { duration: 300, easing: "ease" }
        };
    };
    Box.prototype.updateButtonBar = function () {
        if (this.buttonTitles.length) {
            this.buttonBar.show();
            while (this.buttonBar.firstChild)
                this.buttonBar.lastChild.remove();
            for (var i = 0; i < this.buttonTitles.length; i++) {
                var btn = ce("input", { type: "button", value: this.buttonTitles[i] });
                if (i > 0 || this.noPrimaryButton) {
                    btn.className = "secondary";
                }
                if (this.onButtonClick) {
                    btn.onclick = this.onButtonClick.bind(this, i);
                }
                else {
                    btn.onclick = this.dismiss.bind(this);
                }
                this.buttonBar.appendChild(btn);
            }
        }
        else {
            this.buttonBar.hide();
        }
    };
    return Box;
}(BaseLayer));
var BoxWithoutContentPadding = /** @class */ (function (_super) {
    __extends(BoxWithoutContentPadding, _super);
    function BoxWithoutContentPadding(title, buttonTitles, onButtonClick) {
        if (buttonTitles === void 0) { buttonTitles = []; }
        if (onButtonClick === void 0) { onButtonClick = null; }
        var _this = _super.call(this, title, buttonTitles, onButtonClick) || this;
        _this.contentWrap.style.padding = "0";
        return _this;
    }
    return BoxWithoutContentPadding;
}(Box));
var ConfirmBox = /** @class */ (function (_super) {
    __extends(ConfirmBox, _super);
    function ConfirmBox(title, msg, onConfirmed) {
        var _this = _super.call(this, title, [lang("yes"), lang("no")], function (idx) {
            if (idx == 0) {
                onConfirmed();
            }
            else {
                this.dismiss();
            }
        }) || this;
        var content = ce("div", { innerHTML: msg });
        _this.setContent(content);
        return _this;
    }
    return ConfirmBox;
}(Box));
var MessageBox = /** @class */ (function (_super) {
    __extends(MessageBox, _super);
    function MessageBox(title, msg, btn) {
        var _this = _super.call(this, title, [btn]) || this;
        var content = ce("div", { innerHTML: msg });
        _this.setContent(content);
        return _this;
    }
    return MessageBox;
}(Box));
var FormBox = /** @class */ (function (_super) {
    __extends(FormBox, _super);
    function FormBox(title, c, btn, act) {
        var _this = _super.call(this, title, [btn, lang("cancel")], function (idx) {
            if (idx == 0) {
                var btn = this.getButton(0);
                btn.setAttribute("disabled", "");
                this.getButton(1).setAttribute("disabled", "");
                btn.classList.add("loading");
                setGlobalLoading(true);
                ajaxSubmitForm(this.form, this.dismiss.bind(this));
            }
            else {
                this.dismiss();
            }
        }) || this;
        var content = ce("div", {}, [
            _this.form = ce("form", { innerHTML: c, action: act })
        ]);
        _this.setContent(content);
        return _this;
    }
    return FormBox;
}(Box));
var FileUploadBox = /** @class */ (function (_super) {
    __extends(FileUploadBox, _super);
    function FileUploadBox(title, message) {
        if (message === void 0) { message = null; }
        var _this = _super.call(this, title, [lang("cancel")], function (idx) {
            this.dismiss();
        }) || this;
        _this.acceptMultiple = false;
        if (!message)
            message = lang(mobile ? "choose_file_mobile" : "drag_or_choose_file");
        var content = ce("div", { className: "fileUploadBoxContent", innerHTML: "<div class=\"inner\">" + message + "<br/>\n\t\t\t\t<form>\n\t\t\t\t\t<input type=\"file\" id=\"fileUploadBoxInput\" accept=\"image/*\"/>\n\t\t\t\t\t<label for=\"fileUploadBoxInput\" class=\"button\">" + lang("choose_file") + "</label>\n\t\t\t\t</form>\n\t\t\t</div>"
        });
        if (!mobile) {
            content.innerHTML += "<div class=\"dropOverlay\">" + lang("drop_files_here") + "</div>";
            _this.dragOverlay = content.qs(".dropOverlay");
            _this.dragOverlay.addEventListener("dragenter", function (ev) {
                this.dragOverlay.classList.add("over");
            }.bind(_this), false);
            _this.dragOverlay.addEventListener("dragleave", function (ev) {
                this.dragOverlay.classList.remove("over");
            }.bind(_this), false);
            content.addEventListener("drop", function (ev) {
                ev.preventDefault();
                this.dragOverlay.classList.remove("over");
                this.handleFiles(ev.dataTransfer.files);
            }.bind(_this), false);
        }
        _this.setContent(content);
        _this.fileField = content.qs("input[type=file]");
        _this.fileField.addEventListener("change", function (ev) {
            _this.handleFiles(_this.fileField.files);
            _this.fileField.form.reset();
        });
        return _this;
    }
    FileUploadBox.prototype.handleFiles = function (files) {
        for (var i = 0; i < files.length; i++) {
            var f = files[i];
            if (f.type.indexOf("image/") == 0) {
                this.handleFile(f);
                if (!this.acceptMultiple)
                    return;
            }
        }
    };
    return FileUploadBox;
}(Box));
var ProfilePictureBox = /** @class */ (function (_super) {
    __extends(ProfilePictureBox, _super);
    function ProfilePictureBox() {
        var _this = _super.call(this, lang("update_profile_picture")) || this;
        _this.file = null;
        _this.areaSelector = null;
        if (mobile)
            _this.noPrimaryButton = true;
        return _this;
    }
    ProfilePictureBox.prototype.handleFile = function (file) {
        var _this = this;
        this.file = file;
        var objURL = URL.createObjectURL(file);
        var img = ce("img");
        img.onload = function () {
            var ratio = img.naturalWidth / img.naturalHeight;
            if (ratio > 2.5) {
                new MessageBox(lang("error"), lang("picture_too_wide"), lang("ok")).show();
                return;
            }
            else if (ratio < 0.25) {
                new MessageBox(lang("error"), lang("picture_too_narrow"), lang("ok")).show();
                return;
            }
            var content = ce("div");
            content.innerText = lang("profile_pic_select_square_version");
            content.align = "center";
            var imgWrap = ce("div");
            imgWrap.className = "profilePictureBoxImgWrap";
            imgWrap.appendChild(img);
            content.appendChild(ce("br"));
            content.appendChild(imgWrap);
            _this.setContent(content);
            if (mobile)
                _this.noPrimaryButton = false;
            _this.setButtons([lang("save"), lang("cancel")], function (idx) {
                if (idx == 1) {
                    _this.dismiss();
                    return;
                }
                var area = _this.areaSelector.getSelectedArea();
                var contW = imgWrap.clientWidth;
                var contH = imgWrap.clientHeight;
                var x1 = area.x / contW;
                var y1 = area.y / contH;
                var x2 = (area.x + area.w) / contW;
                var y2 = (area.y + area.h) / contH;
                _this.areaSelector.setEnabled(false);
                _this.upload(x1, y1, x2, y2);
            });
            _this.areaSelector = new ImageAreaSelector(imgWrap, true);
            var w = imgWrap.clientWidth;
            var h = imgWrap.clientHeight;
            if (w > h) {
                _this.areaSelector.setSelectedArea(Math.round(w / 2 - h / 2), 0, h, h);
            }
            else {
                _this.areaSelector.setSelectedArea(0, 0, w, w);
            }
        };
        img.onerror = function () {
            new MessageBox(lang("error"), lang("error_loading_picture"), lang("ok")).show();
        };
        img.src = objURL;
    };
    ProfilePictureBox.prototype.upload = function (x1, y1, x2, y2) {
        var _this = this;
        var btn = this.getButton(0);
        btn.setAttribute("disabled", "");
        this.getButton(1).setAttribute("disabled", "");
        btn.classList.add("loading");
        setGlobalLoading(true);
        ajaxUpload("/settings/updateProfilePicture?x1=" + x1 + "&y1=" + y1 + "&x2=" + x2 + "&y2=" + y2, "pic", this.file, function (resp) {
            _this.dismiss();
            setGlobalLoading(false);
            return false;
        });
    };
    return ProfilePictureBox;
}(FileUploadBox));
var MobileOptionsBox = /** @class */ (function (_super) {
    __extends(MobileOptionsBox, _super);
    function MobileOptionsBox(options) {
        var _this = _super.call(this, null, [lang("cancel")]) || this;
        _this.noPrimaryButton = true;
        var list;
        var content = ce("div", {}, [list = ce("ul", { className: "actionList" })]);
        _this.contentWrap.classList.add("optionsBoxContent");
        options.forEach(function (opt) {
            var attrs = { innerText: opt.label };
            if (opt.type == "link") {
                attrs.href = opt.href;
                if (opt.target) {
                    attrs.target = opt.target;
                    attrs.rel = "noopener";
                }
            }
            attrs.onclick = function () {
                if (opt.type == "confirm") {
                    ajaxConfirm(opt.title, opt.msg, opt.url);
                }
                _this.dismiss();
            };
            list.appendChild(ce("li", {}, [
                ce("a", attrs)
            ]));
        });
        _this.setContent(content);
        return _this;
    }
    return MobileOptionsBox;
}(Box));
var submittingForm = null;
function ge(id) {
    return document.getElementById(id);
}
function ce(tag, attrs, children) {
    if (attrs === void 0) { attrs = {}; }
    if (children === void 0) { children = []; }
    var el = document.createElement(tag);
    for (var attrName in attrs) {
        el[attrName] = attrs[attrName];
    }
    for (var _i = 0, children_1 = children; _i < children_1.length; _i++) {
        var child = children_1[_i];
        el.appendChild(child);
    }
    return el;
}
;
String.prototype.format = function () {
    var args = [];
    for (var _i = 0; _i < arguments.length; _i++) {
        args[_i] = arguments[_i];
    }
    var currentIndex = 0;
    return this.replace(/%(?:(\d+)\$)?([ds%])/gm, function (match, g1, g2) {
        if (g2 == "%")
            return "%";
        var index = g1 ? (parseInt(g1) - 1) : currentIndex;
        currentIndex++;
        switch (g2) {
            case "d":
                return Number(args[index]);
            case "s":
                return args[index].toString().escapeHTML();
        }
    });
};
String.prototype.escapeHTML = function () {
    var el = document.createElement("span");
    el.innerText = this;
    return el.innerHTML;
};
Array.prototype.remove = function (item) {
    var index = this.indexOf(item);
    if (index == -1)
        return;
    this.splice(index, 1);
};
HTMLElement.prototype.qs = function (sel) {
    return this.querySelector(sel);
};
HTMLElement.prototype.hide = function () {
    this.style.display = "none";
};
HTMLElement.prototype.hideAnimated = function (animName, onEnd) {
    var _this = this;
    if (animName === void 0) { animName = { keyframes: [{ opacity: 1 }, { opacity: 0 }], options: { duration: 200, easing: "ease" } }; }
    if (onEnd === void 0) { onEnd = null; }
    if (this.currentVisibilityAnimation)
        this.currentVisibilityAnimation.cancel();
    this.currentVisibilityAnimation = this.anim(animName.keyframes, animName.options, function () {
        _this.hide();
        _this.currentVisibilityAnimation = null;
        if (onEnd)
            onEnd();
    });
};
HTMLElement.prototype.show = function () {
    this.style.display = "";
};
HTMLElement.prototype.showAnimated = function (animName, onEnd) {
    var _this = this;
    if (animName === void 0) { animName = { keyframes: [{ opacity: 0 }, { opacity: 1 }], options: { duration: 200, easing: "ease" } }; }
    if (onEnd === void 0) { onEnd = null; }
    if (this.currentVisibilityAnimation)
        this.currentVisibilityAnimation.cancel();
    this.show();
    this.currentVisibilityAnimation = this.anim(animName.keyframes, animName.options, function () {
        _this.currentVisibilityAnimation = null;
        if (onEnd)
            onEnd();
    });
};
var compatAnimStyle;
function cssRuleForCamelCase(s) {
    return s.replace(/([A-Z])/g, "-$1");
}
function removeCssRuleByName(sheet, name) {
    for (var i = 0; i < sheet.rules.length; i++) {
        if (sheet.rules[i].name == name) {
            sheet.removeRule(i);
            return;
        }
    }
}
HTMLElement.prototype.anim = function (keyframes, options, onFinish) {
    var _this = this;
    if (this.animate !== undefined) {
        var a = this.animate(keyframes, options);
        if (onFinish)
            a.onfinish = onFinish;
        return a;
    }
    else if (this.style.animationName !== undefined || this.style.webkitAnimationName !== undefined) {
        if (!compatAnimStyle) {
            compatAnimStyle = ce("style");
            document.body.appendChild(compatAnimStyle);
        }
        var ruleName = "";
        for (var i = 0; i < 40; i++) {
            ruleName += String.fromCharCode(0x61 + Math.floor(Math.random() * 26));
        }
        var rule = (this.style.animationName === undefined ? "@-webkit-" : "@") + "keyframes " + ruleName + "{";
        rule += "0%{";
        var _keyframes = keyframes;
        for (var k in _keyframes[0]) {
            rule += cssRuleForCamelCase(k) + ": " + _keyframes[0][k] + ";";
        }
        rule += "} 100%{";
        for (var k in _keyframes[1]) {
            rule += cssRuleForCamelCase(k) + ": " + _keyframes[1][k] + ";";
        }
        rule += "}}";
        var sheet = compatAnimStyle.sheet;
        sheet.insertRule(rule, sheet.rules.length);
        var duration = (options instanceof Number) ? options : options.duration;
        var easing = (options instanceof Number) ? "" : (options.easing);
        if (this.style.animation !== undefined) {
            this.style.animation = ruleName + " " + (duration / 1000) + "s " + easing;
            var fn = function () {
                _this.style.animation = "";
                removeCssRuleByName(sheet, ruleName);
                if (onFinish)
                    onFinish();
                _this.removeEventListener("animationend", fn);
            };
            this.addEventListener("animationend", fn);
        }
        else {
            this.style.webkitAnimation = ruleName + " " + (duration / 1000) + "s " + easing;
            var fn = function () {
                _this.style.webkitAnimation = "";
                removeCssRuleByName(sheet, ruleName);
                if (onFinish)
                    onFinish();
                _this.removeEventListener("webkitanimationend", fn);
            };
            this.addEventListener("webkitanimationend", fn);
        }
        return { cancel: function () {
                if (this.style.animation !== undefined)
                    this.style.animation = "";
                else
                    this.style.webkitAnimation = "";
            } };
    }
    if (onFinish)
        onFinish();
    return null;
};
function ajaxPost(uri, params, onDone, onError) {
    var xhr = new XMLHttpRequest();
    xhr.open("POST", uri);
    xhr.onload = function () {
        onDone(xhr.response);
    };
    xhr.onerror = function (ev) {
        console.log(ev);
        onError();
    };
    xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    var formData = [];
    for (var key in params) {
        formData.push(key + "=" + encodeURIComponent(params[key]));
    }
    formData.push("_ajax=1");
    xhr.responseType = "json";
    xhr.send(formData.join("&"));
}
function ajaxGet(uri, onDone, onError) {
    var xhr = new XMLHttpRequest();
    if (uri.indexOf("?") != -1)
        uri += "&_ajax=1";
    else
        uri += "?_ajax=1";
    xhr.open("GET", uri);
    xhr.onload = function () {
        onDone(xhr.response);
    };
    xhr.onerror = function (ev) {
        console.log(ev);
        onError();
    };
    xhr.responseType = "json";
    xhr.send();
}
function ajaxUpload(uri, fieldName, file, onDone, onError, onProgress) {
    if (onDone === void 0) { onDone = null; }
    if (onError === void 0) { onError = null; }
    if (onProgress === void 0) { onProgress = null; }
    var formData = new FormData();
    formData.append(fieldName, file);
    var xhr = new XMLHttpRequest();
    if (uri.indexOf("?") != -1)
        uri += "&";
    else
        uri += "?";
    uri += "_ajax=1&csrf=" + userConfig.csrf;
    xhr.open("POST", uri);
    xhr.onload = function () {
        var resp = xhr.response;
        if (onDone) {
            if (onDone(resp))
                return;
        }
        if (resp instanceof Array) {
            for (var i = 0; i < resp.length; i++) {
                applyServerCommand(resp[i]);
            }
        }
    }.bind(this);
    xhr.onerror = function (ev) {
        console.log(ev);
        if (onError)
            onError();
    };
    xhr.upload.onprogress = function (ev) {
        // pbarInner.style.transform="scaleX("+(ev.loaded/ev.total)+")";
        if (onProgress)
            onProgress(ev.loaded / ev.total);
    };
    xhr.responseType = "json";
    xhr.send(formData);
}
function isVisible(el) {
    return el.style.display != "none";
}
function lang(key) {
    if (typeof key === "string")
        return (langKeys[key] ? langKeys[key] : key);
    var _key = key[0];
    if (!langKeys[_key])
        return key.toString().escapeHTML();
    return langKeys[_key].format.apply(key.slice(1));
}
function setGlobalLoading(loading) {
    document.body.style.cursor = loading ? "progress" : "";
}
function ajaxConfirm(titleKey, msgKey, url, params) {
    if (params === void 0) { params = {}; }
    var box;
    box = new ConfirmBox(lang(titleKey), lang(msgKey), function () {
        var btn = box.getButton(0);
        btn.setAttribute("disabled", "");
        box.getButton(1).setAttribute("disabled", "");
        btn.classList.add("loading");
        setGlobalLoading(true);
        params.csrf = userConfig.csrf;
        ajaxPost(url, params, function (resp) {
            setGlobalLoading(false);
            box.dismiss();
            if (resp instanceof Array) {
                for (var i = 0; i < resp.length; i++) {
                    applyServerCommand(resp[i]);
                }
            }
        }, function () {
            setGlobalLoading(false);
            box.dismiss();
            new MessageBox(lang("error"), lang("network_error"), lang("ok")).show();
        });
    });
    box.show();
    return false;
}
function ajaxSubmitForm(form, onDone) {
    if (onDone === void 0) { onDone = null; }
    if (submittingForm)
        return false;
    submittingForm = form;
    var submitBtn = form.querySelector("input[type=submit]");
    if (submitBtn)
        submitBtn.classList.add("loading");
    setGlobalLoading(true);
    var data = {};
    var elems = form.elements;
    for (var i = 0; i < elems.length; i++) {
        var el = elems[i];
        if (!el.name)
            continue;
        if (el.type != "radio" || (el.type == "radio" && el.checked))
            data[el.name] = el.value;
    }
    data.csrf = userConfig.csrf;
    ajaxPost(form.action, data, function (resp) {
        submittingForm = null;
        if (submitBtn)
            submitBtn.classList.remove("loading");
        setGlobalLoading(false);
        if (resp instanceof Array) {
            for (var i = 0; i < resp.length; i++) {
                applyServerCommand(resp[i]);
            }
        }
        if (onDone)
            onDone();
    }, function () {
        submittingForm = null;
        if (submitBtn)
            submitBtn.classList.remove("loading");
        setGlobalLoading(false);
        new MessageBox(lang("error"), lang("network_error"), lang("ok")).show();
        if (onDone)
            onDone();
    });
    return false;
}
function ajaxFollowLink(link) {
    if (link.getAttribute("data-ajax")) {
        ajaxGetAndApplyActions(link.href);
        return true;
    }
    return false;
}
function ajaxGetAndApplyActions(url) {
    setGlobalLoading(true);
    ajaxGet(url, function (resp) {
        setGlobalLoading(false);
        if (resp instanceof Array) {
            for (var i = 0; i < resp.length; i++) {
                applyServerCommand(resp[i]);
            }
        }
    }, function () {
        setGlobalLoading(false);
        new MessageBox(lang("error"), lang("network_error"), lang("ok")).show();
    });
}
function applyServerCommand(cmd) {
    switch (cmd.a) {
        case "remove":
            {
                var ids = cmd.ids;
                for (var i = 0; i < ids.length; i++) {
                    var el = document.getElementById(ids[i]);
                    if (el) {
                        el.parentNode.removeChild(el);
                    }
                }
            }
            break;
        case "setContent":
            {
                var id = cmd.id;
                var content = cmd.c;
                var el = document.getElementById(id);
                if (el) {
                    el.innerHTML = content;
                }
            }
            break;
        case "setAttr":
            {
                var id = cmd.id;
                var value = cmd.v;
                var name = cmd.n;
                var el = document.getElementById(id);
                if (el) {
                    el.setAttribute(name, value);
                }
            }
            break;
        case "msgBox":
            new MessageBox(cmd.t, cmd.m, cmd.b).show();
            break;
        case "formBox":
            new FormBox(cmd.t, cmd.m, cmd.b, cmd.fa).show();
            break;
        case "box":
            {
                var box = new BoxWithoutContentPadding(cmd.t);
                var cont = ce("div");
                if (cmd.i) {
                    cont.id = cmd.i;
                }
                cont.innerHTML = cmd.c;
                box.setContent(cont);
                box.show();
                if (cmd.w) {
                    box.getContent().querySelector(".boxLayer").style.width = cmd.w + "px";
                }
            }
            break;
        case "show":
            {
                var ids = cmd.ids;
                for (var i = 0; i < ids.length; i++) {
                    var el = document.getElementById(ids[i]);
                    if (el) {
                        el.show();
                    }
                }
            }
            break;
        case "hide":
            {
                var ids = cmd.ids;
                for (var i = 0; i < ids.length; i++) {
                    var el = document.getElementById(ids[i]);
                    if (el) {
                        el.hide();
                    }
                }
            }
            break;
        case "insert":
            {
                var el = document.getElementById(cmd.id);
                if (!el)
                    return;
                var mode = { "bb": "beforeBegin", "ab": "afterBegin", "be": "beforeEnd", "ae": "afterEnd" }[cmd.m];
                el.insertAdjacentHTML(mode, cmd.c);
            }
            break;
        case "setValue":
            {
                var el = document.getElementById(cmd.id);
                if (!el)
                    return;
                el.value = cmd.v;
            }
            break;
        case "addClass":
            {
                var el = document.getElementById(cmd.id);
                if (!el)
                    return;
                el.classList.add(cmd.cl);
            }
            break;
        case "remClass":
            {
                var el = document.getElementById(cmd.id);
                if (!el)
                    return;
                el.classList.remove(cmd.cl);
            }
            break;
        case "refresh":
            location.reload();
            break;
    }
}
function showPostReplyForm(id) {
    var form = document.getElementById("wallPostForm_reply");
    var replies = document.getElementById("postReplies" + id);
    replies.insertAdjacentElement("afterbegin", form);
    postForms["wallPostForm_reply"].setupForReplyTo(id);
    return false;
}
function highlightComment(id) {
    var existing = document.querySelectorAll(".highlight");
    for (var i = 0; i < existing.length; i++)
        existing[i].classList.remove("highlight");
    ge("post" + id).classList.add("highlight");
    window.location.hash = "#comment" + id;
    return false;
}
function likeOnClick(btn) {
    if (btn.hasAttribute("in_progress"))
        return false;
    var objType = btn.getAttribute("data-obj-type");
    var objID = btn.getAttribute("data-obj-id");
    var liked = btn.classList.contains("liked");
    var counter = ge("likeCounter" + objType.substring(0, 1).toUpperCase() + objType.substring(1) + objID);
    var count = parseInt(counter.innerText);
    var ownAva = document.querySelector(".likeAvatars .currentUserLikeAva");
    if (!liked) {
        counter.innerText = (count + 1).toString();
        btn.classList.add("liked");
        if (count == 0)
            counter.show();
        if (btn.popover) {
            if (!btn.popover.isShown())
                btn.popover.show();
            var title = btn.popover.getTitle();
            btn.popover.setTitle(btn.customData.altPopoverTitle);
            btn.customData.altPopoverTitle = title;
        }
        if (ownAva)
            ownAva.show();
    }
    else {
        counter.innerText = (count - 1).toString();
        btn.classList.remove("liked");
        if (count == 1) {
            counter.hide();
            if (btn.popover) {
                btn.popover.hide();
            }
        }
        if (btn.popover) {
            var title = btn.popover.getTitle();
            btn.popover.setTitle(btn.customData.altPopoverTitle);
            btn.customData.altPopoverTitle = title;
        }
        if (ownAva)
            ownAva.hide();
    }
    btn.setAttribute("in_progress", "");
    ajaxGet(btn.href, function (resp) {
        btn.removeAttribute("in_progress");
        if (resp instanceof Array) {
            for (var i = 0; i < resp.length; i++) {
                applyServerCommand(resp[i]);
            }
        }
    }, function () {
        btn.removeAttribute("in_progress");
        new MessageBox(lang("error"), lang("network_error"), lang("ok")).show();
        if (liked) {
            counter.innerText = (count + 1).toString();
            btn.classList.add("liked");
            if (count == 0)
                counter.show();
        }
        else {
            counter.innerText = (count - 1).toString();
            btn.classList.remove("liked");
            if (count == 1)
                counter.hide();
        }
    });
    return false;
}
function likeOnMouseChange(wrap, entered) {
    var btn = wrap.querySelector(".like");
    var objID = btn.getAttribute("data-obj-id");
    var objType = btn.getAttribute("data-obj-type");
    var ev = event;
    var popover = btn.popover;
    if (entered) {
        if (!btn.customData)
            btn.customData = {};
        btn.customData.popoverTimeout = setTimeout(function () {
            delete btn.customData.popoverTimeout;
            ajaxGet(btn.getAttribute("data-popover-url"), function (resp) {
                if (!popover) {
                    popover = new Popover(wrap.querySelector(".popoverPlace"));
                    popover.setOnClick(function () {
                        popover.hide();
                        ajaxGetAndApplyActions(resp.fullURL);
                    });
                    btn.popover = popover;
                }
                popover.setTitle(resp.title);
                popover.setContent(resp.content);
                btn.customData.altPopoverTitle = resp.altTitle;
                if (resp.show)
                    popover.show(ev.offsetX, ev.offsetY);
                for (var i = 0; i < resp.actions.length; i++) {
                    applyServerCommand(resp.actions[i]);
                }
            }, function () {
                if (popover)
                    popover.show(ev.offsetX, ev.offsetY);
            });
        }, 500);
    }
    else {
        if (btn.customData.popoverTimeout) {
            clearTimeout(btn.customData.popoverTimeout);
            delete btn.customData.popoverTimeout;
        }
        else if (popover) {
            popover.hide();
        }
    }
}
function showOptions(el) {
    new MobileOptionsBox(JSON.parse(el.getAttribute("data-options"))).show();
    return false;
}
var ImageAreaSelector = /** @class */ (function () {
    function ImageAreaSelector(parentEl, square) {
        if (square === void 0) { square = false; }
        this.curTarget = null;
        this.enabled = true;
        this.parentEl = parentEl;
        this.container = ce("div");
        this.container.className = "imageAreaSelector";
        this.parentEl.appendChild(this.container);
        this.scrimTop = this.makeDiv("scrim");
        this.scrimTop.style.top = "0";
        this.container.appendChild(this.scrimTop);
        this.scrimBottom = this.makeDiv("scrim");
        this.scrimBottom.style.bottom = "0";
        this.container.appendChild(this.scrimBottom);
        this.scrimLeft = this.makeDiv("scrim");
        this.scrimLeft.style.left = this.scrimLeft.style.top = this.scrimLeft.style.bottom = "0";
        this.container.appendChild(this.scrimLeft);
        this.scrimRight = this.makeDiv("scrim");
        this.scrimRight.style.right = this.scrimRight.style.top = this.scrimRight.style.bottom = "0";
        this.container.appendChild(this.scrimRight);
        this.selected = ce("div");
        this.selected.className = "selected";
        this.container.appendChild(this.selected);
        this.container.addEventListener("mousedown", this.onMouseDown.bind(this), false);
        this.container.addEventListener("touchstart", this.onTouchDown.bind(this), false);
        this.container.addEventListener("dragstart", function (ev) { ev.preventDefault(); }, false);
        var markerCont = ce("div");
        markerCont.className = "markers";
        this.selected.appendChild(markerCont);
        markerCont.appendChild(this.markerTL = this.makeDiv("marker tl"));
        markerCont.appendChild(this.markerTR = this.makeDiv("marker tr"));
        markerCont.appendChild(this.markerBL = this.makeDiv("marker bl"));
        markerCont.appendChild(this.markerBR = this.makeDiv("marker br"));
        if (!square) {
            markerCont.appendChild(this.edgeTop = this.makeDiv("edge top"));
            markerCont.appendChild(this.edgeBottom = this.makeDiv("edge bottom"));
            markerCont.appendChild(this.edgeLeft = this.makeDiv("edge left"));
            markerCont.appendChild(this.edgeRight = this.makeDiv("edge right"));
        }
        this.square = square;
    }
    ImageAreaSelector.prototype.makeDiv = function (cls) {
        var el = ce("div");
        el.className = cls;
        return el;
    };
    ImageAreaSelector.prototype.setSelectedArea = function (x, y, w, h) {
        this.curX = x;
        this.curY = y;
        this.curW = w;
        this.curH = h;
        this.updateStyles();
    };
    ImageAreaSelector.prototype.getSelectedArea = function () {
        return { x: this.curX, y: this.curY, w: this.curW, h: this.curH };
    };
    ImageAreaSelector.prototype.setEnabled = function (enabled) {
        this.enabled = enabled;
    };
    ImageAreaSelector.prototype.updateStyles = function () {
        var contW = Math.round(this.container.clientWidth);
        var contH = Math.round(this.container.clientHeight);
        // Round to avoid rendering artifacts
        var x = Math.round(this.curX);
        var y = Math.round(this.curY);
        var w = Math.round(this.curW);
        var h = Math.round(this.curH);
        this.selected.style.left = x + "px";
        this.selected.style.top = y + "px";
        this.selected.style.width = w + "px";
        this.selected.style.height = h + "px";
        this.scrimTop.style.left = x + "px";
        this.scrimTop.style.width = w + "px";
        this.scrimTop.style.height = y + "px";
        this.scrimBottom.style.left = x + "px";
        this.scrimBottom.style.width = w + "px";
        this.scrimBottom.style.height = (contH - y - h + 1) + "px";
        this.scrimLeft.style.width = x + "px";
        this.scrimRight.style.width = (contW - x - w) + "px";
    };
    ImageAreaSelector.prototype.onTouchDown = function (ev) {
        ev.preventDefault();
        if (this.trackedTouchID)
            return;
        var touch = ev.touches[0];
        this.trackedTouchID = touch.identifier;
        this.onPointerDown(Math.round(touch.clientX), Math.round(touch.clientY), touch.target);
        window.addEventListener("touchend", this.touchUpListener = this.onTouchUp.bind(this), false);
        window.addEventListener("touchcancel", this.touchUpListener, false);
        window.addEventListener("touchmove", this.touchMoveListener = this.onTouchMove.bind(this), false);
    };
    ImageAreaSelector.prototype.onTouchMove = function (ev) {
        // ev.preventDefault();
        for (var i = 0; i < ev.touches.length; i++) {
            var touch = ev.touches[i];
            if (touch.identifier == this.trackedTouchID) {
                this.onPointerMove(Math.round(touch.clientX), Math.round(touch.clientY));
                break;
            }
        }
    };
    ImageAreaSelector.prototype.onTouchUp = function (ev) {
        ev.preventDefault();
        for (var i = 0; i < ev.changedTouches.length; i++) {
            var touch = ev.changedTouches[i];
            if (touch.identifier == this.trackedTouchID) {
                this.onPointerUp();
                this.trackedTouchID = null;
                window.removeEventListener("touchend", this.touchUpListener);
                window.removeEventListener("touchcancel", this.touchUpListener);
                window.removeEventListener("touchmove", this.touchMoveListener);
                break;
            }
        }
    };
    ImageAreaSelector.prototype.onMouseDown = function (ev) {
        this.onPointerDown(ev.clientX, ev.clientY, ev.target);
        window.addEventListener("mouseup", this.mouseUpListener = this.onMouseUp.bind(this), false);
        window.addEventListener("mousemove", this.mouseMoveListener = this.onMouseMove.bind(this), false);
    };
    ImageAreaSelector.prototype.onMouseUp = function (ev) {
        this.onPointerUp();
        window.removeEventListener("mouseup", this.mouseUpListener);
        window.removeEventListener("mousemove", this.mouseMoveListener);
    };
    ImageAreaSelector.prototype.onMouseMove = function (ev) {
        this.onPointerMove(ev.clientX, ev.clientY);
    };
    ImageAreaSelector.prototype.onPointerDown = function (x, y, target) {
        if (!this.enabled)
            return;
        this.curTarget = target;
        this.downX = x;
        this.downY = y;
        this.downSelectedX = this.curX;
        this.downSelectedY = this.curY;
        this.downSelectedW = this.curW;
        this.downSelectedH = this.curH;
    };
    ImageAreaSelector.prototype.onPointerUp = function () {
        this.curTarget = null;
    };
    ImageAreaSelector.prototype.onPointerMove = function (x, y) {
        if (!this.curTarget)
            return;
        var dX = x - this.downX;
        var dY = y - this.downY;
        var contW = this.container.clientWidth;
        var contH = this.container.clientHeight;
        if (this.curTarget == this.selected) {
            this.curX = Math.max(0, Math.min(this.downSelectedX + dX, contW - this.curW));
            this.curY = Math.max(0, Math.min(this.downSelectedY + dY, contH - this.curH));
            this.updateStyles();
        }
        else if (this.curTarget == this.edgeRight) {
            this.curW = Math.max(30, Math.min(this.downSelectedW + dX, contW - this.curX));
            this.updateStyles();
        }
        else if (this.curTarget == this.edgeBottom) {
            this.curH = Math.max(30, Math.min(this.downSelectedH + dY, contH - this.curY));
            this.updateStyles();
        }
        else if (this.curTarget == this.markerBR) {
            this.curW = Math.max(30, Math.min(this.downSelectedW + dX, contW - this.curX));
            this.curH = Math.max(30, Math.min(this.downSelectedH + dY, contH - this.curY));
            if (this.square) {
                this.curW = this.curH = Math.min(this.curH, this.curW);
            }
            this.updateStyles();
        }
        else if (this.curTarget == this.edgeTop) {
            var prevH = this.curH;
            this.curH = Math.max(30, Math.min(this.downSelectedH - dY, this.curY + this.curH));
            this.curY += prevH - this.curH;
            this.updateStyles();
        }
        else if (this.curTarget == this.edgeLeft) {
            var prevW = this.curW;
            this.curW = Math.max(30, Math.min(this.downSelectedW - dX, this.curX + this.curW));
            this.curX += prevW - this.curW;
            this.updateStyles();
        }
        else if (this.curTarget == this.markerTL) {
            var prevW = this.curW;
            this.curW = Math.max(30, Math.min(this.downSelectedW - dX, this.curX + this.curW));
            var prevH = this.curH;
            this.curH = Math.max(30, Math.min(this.downSelectedH - dY, this.curY + this.curH));
            if (this.square) {
                this.curW = this.curH = Math.min(this.curH, this.curW);
            }
            this.curX += prevW - this.curW;
            this.curY += prevH - this.curH;
            this.updateStyles();
        }
        else if (this.curTarget == this.markerTR) {
            this.curW = Math.max(30, Math.min(this.downSelectedW + dX, contW - this.curX));
            var prevH = this.curH;
            this.curH = Math.max(30, Math.min(this.downSelectedH - dY, this.curY + this.curH));
            if (this.square) {
                this.curW = this.curH = Math.min(this.curH, this.curW);
            }
            this.curY += prevH - this.curH;
            this.updateStyles();
        }
        else if (this.curTarget == this.markerBL) {
            this.curH = Math.max(30, Math.min(this.downSelectedH + dY, contH - this.curY));
            var prevW = this.curW;
            this.curW = Math.max(30, Math.min(this.downSelectedW - dX, this.curX + this.curW));
            if (this.square) {
                this.curW = this.curH = Math.min(this.curH, this.curW);
            }
            this.curX += prevW - this.curW;
            this.updateStyles();
        }
    };
    return ImageAreaSelector;
}());
var PostForm = /** @class */ (function () {
    function PostForm(el) {
        this.attachmentIDs = [];
        this.currentReplyName = "";
        this.id = el.getAttribute("data-unique-id");
        this.root = el;
        this.input = ge("postFormText_" + this.id);
        this.form = el.getElementsByTagName("form")[0];
        this.dragOverlay = el.querySelector(".dropOverlay");
        this.attachContainer = ge("postFormAttachments_" + this.id);
        this.fileField = ge("uploadField_" + this.id);
        this.attachField = el.querySelector("input[name=attachments]");
        this.replyToField = ge("postFormReplyTo_" + this.id);
        this.form.addEventListener("submit", this.onFormSubmit.bind(this), false);
        this.input.addEventListener("keydown", this.onInputKeyDown.bind(this), false);
        this.input.addEventListener("paste", this.onInputPaste.bind(this), false);
        if (this.input.hasAttribute("data-reply-name")) {
            this.currentReplyName = this.input.getAttribute("data-reply-name");
        }
        if (this.dragOverlay) {
            this.dragOverlay.addEventListener("dragenter", function (ev) {
                this.dragOverlay.classList.add("over");
            }.bind(this), false);
            this.dragOverlay.addEventListener("dragleave", function (ev) {
                this.dragOverlay.classList.remove("over");
            }.bind(this), false);
            this.root.addEventListener("drop", this.onDrop.bind(this), false);
        }
        this.fileField.addEventListener("change", function (ev) {
            this.handleFiles(this.fileField.files);
            this.fileField.form.reset();
        }.bind(this), false);
        if (this.attachContainer.children.length) {
            for (var i = 0; i < this.attachContainer.children.length; i++) {
                var attach = this.attachContainer.children[i];
                var aid = attach.getAttribute("data-id");
                this.attachmentIDs.push(aid);
                attach.querySelector(".deleteBtn").onclick = function (ev) {
                    ev.preventDefault();
                    this.deleteAttachment(aid);
                }.bind(this);
            }
        }
        window.addEventListener("beforeunload", function (ev) {
            if ((this.input.value.length > 0 && this.input.value != this.currentReplyName) || this.attachmentIDs.length > 0) {
                var msg = lang("confirm_discard_post_draft");
                (ev || window.event).returnValue = msg;
                return msg;
            }
        }.bind(this));
    }
    PostForm.prototype.onFormSubmit = function (ev) {
        ev.preventDefault();
        this.send();
    };
    PostForm.prototype.onInputKeyDown = function (ev) {
        if (ev.keyCode == 13 && (isApple ? ev.metaKey : ev.ctrlKey)) {
            this.send();
        }
    };
    PostForm.prototype.onInputPaste = function (ev) {
        if (ev.clipboardData.files.length) {
            ev.preventDefault();
            this.handleFiles(ev.clipboardData.files);
        }
    };
    PostForm.prototype.onDrop = function (ev) {
        ev.preventDefault();
        this.dragOverlay.classList.remove("over");
        this.handleFiles(ev.dataTransfer.files);
    };
    PostForm.prototype.handleFiles = function (files) {
        for (var i = 0; i < files.length; i++) {
            var f = files[i];
            if (f.type.indexOf("image/") == 0) {
                this.uploadFile(f);
            }
        }
    };
    PostForm.prototype.uploadFile = function (f) {
        var objURL = URL.createObjectURL(f);
        var cont = ce("div");
        cont.className = "attachment uploading";
        var img = ce("img");
        img.src = objURL;
        cont.appendChild(img);
        var scrim = ce("div");
        scrim.className = "scrim";
        cont.appendChild(scrim);
        var pbar = ce("div");
        pbar.className = "progressBarFrame";
        var pbarInner = ce("div");
        pbarInner.className = "progressBar";
        pbar.appendChild(pbarInner);
        cont.appendChild(pbar);
        var del = ce("a");
        del.className = "deleteBtn";
        del.title = lang("delete");
        del.href = "javascript:void(0)";
        cont.appendChild(del);
        pbarInner.style.transform = "scaleX(0)";
        this.attachContainer.appendChild(cont);
        var formData = new FormData();
        formData.append("file", f);
        var xhr = new XMLHttpRequest();
        xhr.open("POST", "/system/upload/postPhoto?_ajax=1&csrf=" + userConfig.csrf);
        xhr.onload = function () {
            cont.classList.remove("uploading");
            var resp = xhr.response;
            del.href = "/system/deleteDraftAttachment?id=" + resp.id + "&csrf=" + userConfig.csrf;
            img.outerHTML = '<picture><source srcset="' + resp.thumbs.webp + '" type="image/webp"/><source srcset="' + resp.thumbs.jpeg + '" type="image/jpeg"/><img src="' + resp.thumbs.jpeg + '"/></picture>';
            del.onclick = function (ev) {
                ev.preventDefault();
                this.deleteAttachment(resp.id);
            }.bind(this);
            cont.id = "attachment_" + resp.id;
            this.attachmentIDs.push(resp.id);
            this.attachField.value = this.attachmentIDs.join(",");
        }.bind(this);
        xhr.onerror = function (ev) {
            console.log(ev);
        };
        xhr.upload.onprogress = function (ev) {
            pbarInner.style.transform = "scaleX(" + (ev.loaded / ev.total) + ")";
        };
        xhr.responseType = "json";
        xhr.send(formData);
        del.onclick = function () {
            xhr.abort();
            cont.parentNode.removeChild(cont);
        };
    };
    PostForm.prototype.deleteAttachment = function (id) {
        var el = ge("attachment_" + id);
        el.parentNode.removeChild(el);
        ajaxGet("/system/deleteDraftAttachment?id=" + id + "&csrf=" + userConfig.csrf, function () { }, function () { });
        this.attachmentIDs.remove(id);
        this.attachField.value = this.attachmentIDs.join(",");
    };
    PostForm.prototype.send = function () {
        if (this.input.value.length == 0 && this.attachmentIDs.length == 0)
            return;
        ajaxSubmitForm(this.form, function () {
            this.attachmentIDs = [];
            this.attachField.value = "";
        }.bind(this));
    };
    PostForm.prototype.setupForReplyTo = function (id) {
        this.replyToField.value = id + "";
        var name = document.getElementById("post" + id).getAttribute("data-reply-name");
        if (name) {
            if (this.input.value.length == 0 || (this.input.value == this.currentReplyName)) {
                this.input.value = name + ", ";
            }
            this.currentReplyName = name + ", ";
        }
        this.input.focus();
    };
    return PostForm;
}());
///<reference path="./PostForm.ts"/>
// Use Cmd instead of Ctrl on Apple devices.
var isApple = navigator.platform.indexOf("Mac") == 0 || navigator.platform == "iPhone" || navigator.platform == "iPad" || navigator.platform == "iPod touch";
var postForms = {};
var timeZone;
if (window["Intl"]) {
    timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
}
else {
    var offset = new Date().getTimezoneOffset();
    timeZone = "GMT" + (offset > 0 ? "+" : "") + Math.floor(offset / 60) + (offset % 60 != 0 ? (":" + (offset % 60)) : "");
}
if (!userConfig || !userConfig["timeZone"] || timeZone != userConfig.timeZone) {
    ajaxPost("/settings/setTimezone", { tz: timeZone }, function (resp) { }, function () { });
}
document.body.addEventListener("click", function (ev) {
    if (ev.target.tagName == "A") {
        if (ajaxFollowLink(ev.target)) {
            ev.preventDefault();
        }
    }
}, false);
document.querySelectorAll(".wallPostForm").forEach(function (el) {
    postForms[el.id] = new PostForm(el);
});
var dragTimeout = -1;
var dragEventCount = 0;
document.body.addEventListener("dragenter", function (ev) {
    if (ev.dataTransfer.types.indexOf("Files") != -1)
        document.body.classList.add("fileIsBeingDragged");
    ev.preventDefault();
    dragEventCount++;
    if (dragTimeout != -1) {
        clearTimeout(dragTimeout);
        dragTimeout = -1;
    }
}, false);
document.body.addEventListener("dragover", function (ev) {
    ev.preventDefault();
}, false);
document.body.addEventListener("dragleave", function (ev) {
    dragEventCount--;
    if (dragEventCount == 0 && dragTimeout == -1) {
        dragTimeout = setTimeout(function () {
            dragTimeout = -1;
            document.body.classList.remove("fileIsBeingDragged");
            dragEventCount = 0;
        }, 100);
    }
}, false);
document.body.addEventListener("drop", function (ev) {
    if (dragTimeout != -1)
        clearTimeout(dragTimeout);
    dragTimeout = -1;
    dragEventCount = 0;
    document.body.classList.remove("fileIsBeingDragged");
}, false);
///<reference path="./Main.ts"/>
var Popover = /** @class */ (function () {
    function Popover(wrap) {
        this.shown = false;
        this.root = wrap.querySelector(".popover");
        if (!this.root) {
            this.root = ce("div", { className: "popover aboveAnchor" }, [
                this.header = ce("div", { className: "popoverHeader" }),
                this.content = ce("div", { className: "popoverContent" }),
                this.arrow = ce("div", { className: "popoverArrow" })
            ]);
            this.root.hide();
            wrap.appendChild(this.root);
        }
    }
    Popover.prototype.show = function (x, y) {
        if (x === void 0) { x = -1; }
        if (y === void 0) { y = -1; }
        this.shown = true;
        this.root.show();
        var anchor = this.root.parentElement;
        var anchorRect = anchor.getBoundingClientRect();
        this.root.classList.remove("belowAnchor", "aboveAnchor");
        if (this.root.offsetHeight > anchorRect.top) {
            this.root.classList.add("belowAnchor");
            this.root.style.top = "";
        }
        else {
            this.root.classList.add("aboveAnchor");
            this.root.style.top = "-" + (this.root.offsetHeight) + "px";
        }
    };
    Popover.prototype.hide = function () {
        this.shown = false;
        this.root.hideAnimated();
    };
    Popover.prototype.setTitle = function (title) {
        this.header.innerHTML = title;
    };
    Popover.prototype.setContent = function (content) {
        this.content.innerHTML = content;
    };
    Popover.prototype.getTitle = function () {
        return this.header.innerHTML;
    };
    Popover.prototype.isShown = function () {
        return this.shown;
    };
    Popover.prototype.setOnClick = function (onClick) {
        this.header.style.cursor = "pointer";
        this.header.onclick = onClick;
    };
    return Popover;
}());
//# sourceMappingURL=common.js.map
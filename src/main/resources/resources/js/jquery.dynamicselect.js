(function( $ ){
    var dialogbox;
    var contextPath;
    
    var methods = {

        init: function(options) {
            return this.each(function() {
                var element = $(this);
                contextPath = options.contextPath;
                $(this).data('fieldName', options.fieldName);
                $(this).data('appId', options.appId);
                $(this).data('appVersion', options.appVersion);
                $(this).data('formDefId', options.formDefId);
                $(this).data('label', options.buttonLabel);
                $(this).data('title', options.title);
                $(this).data('primaryKey', options.primaryKey);
                $(this).data('idField', options.idField);
                $(this).data('displayField', options.displayField);
                $(this).data('type', (options.multiple == "true")? "" : "single");
                $(this).data('requestParams', options.requestParams);
                $(this).data('width', options.width);
                $(this).data('height', options.height);
                $(this).data('nonce', options.nonce);
                $(this).data('json', options.json);
                $(this).data('buttonPrimary', options.buttonPrimary);
                $(this).data('buttonInlineStyle', options.buttonInlineStyle);

                // check for settings
                var readOnly = false;
                if (options && options.readOnly) {
                    element.attr("readonly", "true");
                    readOnly = true;
                }

                //create new button
                var button = "";
                if (!readOnly) {
                    if($(this).data('buttonPrimary') == 'true'){
                        button += '<button class="col-sm-auto btn btn-sm btn-primary mb-auto selector_button"';
                    } else {
                        button += '<button class="col-sm-auto btn btn-sm mb-auto selector_button"';
                    }

                    if($(this).data('buttonInlineStyle') != ''){
                        button += 'style ="' + $(this).data('buttonInlineStyle') + '"';
                    }

                    button += '>' +options.buttonLabel+'</button>';
                    $(this).closest(".dynamicselect_addbutton select").after(button);
                }
                
                if (!readOnly) {
                    var button = $(this).closest(".dynamicselect_addbutton select").next(".selector_button");
                    $(button).click(function() {
                        methods.add.apply(element);
                        //focus on 1st foucusable element when popup opened
                        $(".boxy-wrapper .close").focus();
                        return false;
                    });
                }

                // refresh
                methods.refresh.apply(element);
            });
        },
        
        getFrameId: function(id) {
            return "popupSelectFrame_" + id;
        },
        
        initPopupDialog: function(args){
            
            var frameId = methods.getFrameId($(this).attr("id"));

            var width = $(this).data('width');
            var height = $(this).data('height');
            JPopup.create(frameId, args.title, width, height);
        },
        
        popupForm: function(id) {
            var frameId = methods.getFrameId($(this).attr("id"));
            
            var params = "";


            var width = $(this).data('width');
            var height = $(this).data('height');
            var url = contextPath+'/web/app/'+$(this).data('appId')+'/'+$(this).data('appVersion')+'/form/embed?_submitButtonLabel=Submit' + UI.userviewThemeParams();

            var vars = {
                _callback : id+"___"+$(this).data('formDefId'),
                _setting : "{}",
                _nonce : $(this).data('nonce'),
                _json : $(this).data('json')
            };

            JPopup.show(frameId, url, vars, "", width, height);
        },
        
        add: function() {
            return this.each(function() {
                var methodName = $(this).attr('id');
                methods.popupForm.call(this, methodName);
            });
        },
        
        submitForm: function (args){
            var frameId = methods.getFrameId($(this).attr("id"));
            $.ajax({
                type: "POST",
                data: args,
                dataType : "json",
                url: contextPath + '/web/json/app/' +$(this).data('appId')+'/'+$(this).data('appVersion')+ '/plugin/org.joget.marketplace.DynamicOptionsSelectbox/service',
                success: function(response) {
                    if (response.result === "rowAdded") {
                        JPopup.hide(frameId);
                    } else if (response.result === "failed") {
                        JPopup.hide(frameId);
                        var button = $(this).closest(".dynamicselect_addbutton select").next(".selector_button");
                        button.after("<div style='color: red;' class='col-sm-auto error-message'>Error adding record.</div>");
                    }
                }
            });
        },
        
        addOption: function(args) {
            return this.each(function() {
                var frameId = methods.getFrameId($(this).attr("id"));
                
                var element = $(this);
                var primaryKey = $(element).data("primaryKey");
                var idField = $(element).data("idField");
                var displayField = $(element).data("displayField");
                var obj = eval("[" + args.result + "]");
                    
                var value = args.id;
                if (idField != "") {
                    value = obj[0][idField];
                }
                //check option exist, if not, add it
                if ($(element).find("option[value=\""+value+"\"]").length == 0) {
                    var label = obj[0][displayField];
                    var option = $("<option></option>");
                    option.attr("value", value);
                    option.html(label);
                    $(element).append(option);
                }
                
                // select option
                var val = $(element).val();
                if (typeof val == "string") {
                    var previous = val;
                    val = new Array();
                    
                    if (element.attr("multiple")) {
                        val.push(previous);
                    }
                } else if (!val) {
                    val = new Array();
                }
                val.push(value);
                $(element).val(val);
                
                
                methods.refresh.apply(element);
                $("[name=" + $(element).data("fieldName") + "]").trigger("change");
                
                // JPopup.hide(frameId);
            });
        },
        
        refresh: function() {
            return this.each(function() {
                var element = $(this);
                var elementId = element.attr("id");
                var readOnly = element.attr("readonly");

                var selectorItems = element.closest(".dynamicselect_addbutton").next(".selector_element").find(".selector_items");

                // show selected items
                element.children("option:selected").each(function(index, el) {
                    // create new item
                    var value = $(el).val();
                    var label = $(el).attr("html");
                    if (!label || label == '') {
                        label = $(el).text();
                    }

                    // create new item
                    var span = '<span class="selector_item">';
                    if (!readOnly) {
                        span += '<a class="selector_remove" href="#">x</a>';
                    }
                    if (readOnly) {
                        span += '<input type="hidden" class="selector_id" id="' + elementId + '" name="' + $(element).data("fieldName") + '" />';
                    }
                    span += '<span class="selector_label">' + label + '</span></span>';
                    var s = $(span);
                    if (readOnly) {
                        $(s).find("input").attr("value", value);
                    } else {
                        $(s).find(".selector_label").attr("option-value", value);
                    }
                    
                    // append item
                    selectorItems.append(s);
                });
            });
        }

    };

    $.fn.dynamicselect = function( method ) {

        if ( methods[method] ) {
            return methods[method].apply( this, Array.prototype.slice.call( arguments, 1 ));
        } else if ( typeof method === 'object' || ! method ) {
            return methods.init.apply( this, arguments );
        } else {
            $.error( 'Method ' +  method + ' does not exist on jQuery.dynamicselect' );
        }

    };

})( jQuery );


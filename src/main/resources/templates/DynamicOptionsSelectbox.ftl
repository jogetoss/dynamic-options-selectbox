<div class="form-cell" ${elementMetaData!}>

    <#if (element.properties.readonly! == 'true' && element.properties.readonlyLabel! == 'true') >
        <label field-tooltip="${elementParamName!}" class="label">${element.properties.label} <span class="form-cell-validator">${decoration}</span><#if error??> <span class="form-error-message">${error}</span></#if></label>
        <div class="form-cell-value">
            <#list options as option>
                <#if values?? && values?seq_contains(option.value!)>
                    <label class="readonly_label">
                        <span>${option.label!?html}</span>
                        <input id="${elementParamName!}" name="${elementParamName!}" type="hidden" value="${option.value!?html}" />
                    </label>
                </#if>
            </#list>
        </div>
        <div style="clear:both;"></div>
    <#else>
        <#if !(request.getAttribute("org.joget.marketplace.DynamicOptionsSelectbox_EDITABLE")??) >
            <script type="text/javascript" src="${request.contextPath}/plugin/org.joget.marketplace.DynamicOptionsSelectbox/js/jquery.dynamicselect.js"></script>
        </#if>
            <label class="label">${element.properties.label!} <span class="form-cell-validator">${decoration}</span><#if error??> <span class="form-error-message">${error}</span></#if></label>
            <div class="container-fluid dynamicselect_addbutton">
             <div class="row">
                <select id="dynamicselect_${element.properties.elementUniqueKey!}" name="${elementParamName!}" <#if element.properties.multiple! == 'true'>multiple="multiple" data-role="none" data-native-menu="true"</#if> class="col ${elementParamName!} <#if error??>form-error-cell</#if>"<#if element.properties.readonly! == 'true'> disabled </#if>>
                    <#list options as option>
                        <option value="${option.value!?html}" html="${option.label!?html}" grouping="${option.grouping!?html}" <#if values?? && values?seq_contains(option.value!)>selected</#if>>${option.label!?html}</option>
                    </#list>
                </select>
            </div>
            </div>
        <script type="text/javascript">
            $(document).ready(function() {
                $("#dynamicselect_${element.properties.elementUniqueKey!}[name='${elementParamName!}']").dynamicselect({
                    contextPath: "${request.contextPath}",
                    title: "${elementParamName!}",
                    fieldName: "${elementParamName!}",
                    readOnly: "${element.properties.readonly!}",
                    primaryKey: "${element.properties.primaryKey!}",
                    idField: "${element.properties.idField!?html}",
                    displayField: "${element.properties.displayField!?html}",
                    buttonLabel: "${element.properties.buttonLabel!?html}",
                    formDefId: "${element.properties.formDefId!}",
                    appId : "${appId!}",
                    appVersion : "${appVersion!}",
                    nonce: "${nonceForm!}",
                    json: "${json!}",
                    height: "${element.properties.height!}",
                    width: "${element.properties.width!}",
                    multiple: "${element.properties.multiple!}",
                    buttonPrimary: "${element.properties.buttonPrimary!}",
                    buttonInlineStyle: "${element.properties.buttonInlineStyle!}",
                    <#if requestParams??>, requestParams:${requestParams}</#if>
                });
                $("#dynamicselect_${element.properties.elementUniqueKey!}[name='${elementParamName!}']").dynamicselect("initPopupDialog", {contextPath: "${request.contextPath}"});
            });
            function dynamicselect_${element.properties.elementUniqueKey!}___${element.properties.formDefId!}(args){
                $("#dynamicselect_${element.properties.elementUniqueKey!}[name='${elementParamName!}']").dynamicselect("submitForm", args);
                $("#dynamicselect_${element.properties.elementUniqueKey!}[name='${elementParamName!}']").dynamicselect("addOption", args);
            }
        </script>
    </#if>
</div>

package org.joget.marketplace;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.joget.apps.app.dao.DatalistDefinitionDao;
import org.joget.apps.app.model.DatalistDefinition;
import org.joget.apps.datalist.model.DataListBinder;
import org.joget.apps.datalist.model.DataListCollection;
import org.joget.apps.datalist.service.DataListService;
import org.joget.apps.form.lib.SelectBox;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBuilderPalette;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormReferenceDataRetriever;
import org.joget.plugin.base.PluginWebSupport;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormBinder;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.FileManager;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.ResourceBundleUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.commons.util.StringUtil;
import org.joget.commons.util.UuidGenerator;
import org.json.JSONArray;
import org.json.JSONObject;

public class DynamicOptionsSelectbox extends SelectBox implements FormBuilderPaletteElement, FormReferenceDataRetriever, PluginWebSupport {
    public static String MESSAGE_PATH = "message/DynamicOptionsSelectbox";
    private String idField;
    
    @Override
    public String getName() {
        return AppPluginUtil.getMessage("org.joget.marketplace.dynamicoptionsselectbox.name", getClassName(), MESSAGE_PATH);

    }

    @Override
    public String getVersion() {
        return "8.0.2";
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage("org.joget.marketplace.dynamicoptionsselectbox.desc", getClassName(), MESSAGE_PATH);
    }
    
    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getFormBuilderTemplate() {
        return "<label class='label'>" + getLabel() + "</label><button onclick='return false;'>" + ResourceBundleUtil.getMessage("form.dynamicoptionsselectbox.selectlabel") + "</button>";
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage("org.joget.marketplace.dynamicoptionsselectbox.label", getClassName(), MESSAGE_PATH);
    }
    
    @Override
    public String getPropertyOptions() {
        String json = AppUtil.readPluginResource(getClass().getName(), "/properties/DynamicOptionsSelectbox.json", null, true, MESSAGE_PATH);
        return json;
    }
    
    @Override
    public String getFormBuilderCategory() {
        return FormBuilderPalette.CATEGORY_CUSTOM;
    }

    @Override
    public int getFormBuilderPosition() {
        return 350;
    }
    
    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        String template = "DynamicOptionsSelectbox.ftl";

        // set value
        String[] valueArray = FormUtil.getElementPropertyValues(this, formData);
        List<String> values = Arrays.asList(valueArray);
        dataModel.put("values", values);

        // set options
        Collection<Map> optionMap = getOptionMap(valueArray, formData);
        dataModel.put("options", optionMap);

        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        dataModel.put("appId", appDef.getAppId());
        dataModel.put("appVersion", appDef.getVersion().toString());

        Object requestParamsProperty = getProperty("requestParams");
        if (requestParamsProperty != null && requestParamsProperty instanceof Object[]) {
            String requestJson = "[";
            for (Object param : ((Object[]) requestParamsProperty)) {
                Map paramMap = ((Map)param);

                if (requestJson.length() > 1) {
                    requestJson += ",";
                }
                requestJson += "{";
                requestJson += "param:'" + paramMap.get("param") + "',";
                requestJson += "field:'" + paramMap.get("field") + "',";
                requestJson += "defaultValue:'" + paramMap.get("defaultValue") + "'";
                requestJson += "}";
            }
            requestJson += "]";

            if (requestJson.length() > 2) {
                dataModel.put("requestParams", requestJson);
            }
        }
        
        Form form = getForm(getPropertyString("formDefId"));
   
        if (form != null) {
            String elJson = StringEscapeUtils.escapeHtml(getSelectedFormJson(form));
            dataModel.put("json", elJson);
            String nonceForm = SecurityUtil.generateNonce(new String[]{"EmbedForm", appDef.getAppId(), appDef.getVersion().toString(), elJson}, 1);
            dataModel.put("nonceForm", nonceForm);

            String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
            return html;
        }
        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
        
    }

    public FormRowSet loadFormRows(String[] primaryKeyValues, FormData formData) {
        FormRowSet rowSet = new FormRowSet();
        DataListCollection list = getDataListCollection(primaryKeyValues, formData);
        if (list != null && !list.isEmpty()) {
            Iterator i = list.iterator();
            while (i.hasNext()) {
                Map item = (Map) i.next();
                FormRow row = new FormRow();
                row.putAll(item);
                rowSet.add(row);
            }
        }
        return rowSet;
    }

     @Override
    public FormData formatDataForValidation(FormData formData) {
        String[] paramValues = FormUtil.getRequestParameterValues(this, formData);
        String paramName = FormUtil.getElementParameterName(this);
        
        if ((paramValues == null || paramValues.length == 0) && FormUtil.isFormSubmitted(this, formData)) {
            formData.addRequestParameterValues(paramName, new String[]{""});
        } else if (paramValues != null && FormUtil.isFormSubmitted(this, formData)) {
            //check & remove invalid data from values
            Collection<String> newValues = new ArrayList<String>();
            Set<String> allValues = new HashSet<String>();
            Collection<Map> optionMap = getOptionMap(paramValues, formData);
            for (Map option : optionMap) {
                if (option.containsKey(FormUtil.PROPERTY_VALUE)) {
                    allValues.add(option.get(FormUtil.PROPERTY_VALUE).toString());
                }
            }
            for (String pv : paramValues) {
                if (allValues.contains(pv)) {
                    newValues.add(pv);
                }
            }
            
            if (newValues.isEmpty()) {
                newValues.add("");
            }
            
            formData.addRequestParameterValues(paramName, newValues.toArray(new String[0]));
        }
        return formData;
    }
    
  public Collection<Map> getOptionMap(String[] valueArray, FormData formData){
        Collection<Map> options = new ArrayList<Map>();
        // add empty option
        Map optionEmpty = new HashMap();
        optionEmpty.put("", "");
        optionEmpty.put("", "");
        options.add(optionEmpty);
        DataListCollection rows = getDataListCollection(valueArray, formData);
        if (rows != null && !rows.isEmpty()) {
            String displayField = getPropertyString("displayField");
            if (idField != null && displayField != null) {
                Iterator i = rows.iterator();
                while (i.hasNext()) {
                    Object r = i.next();
                    Map option = new HashMap();
                    option.put(FormUtil.PROPERTY_VALUE, DataListService.evaluateColumnValueFromRow(r, idField));
                    option.put(FormUtil.PROPERTY_LABEL, DataListService.evaluateColumnValueFromRow(r, displayField));

                    options.add(option);
                }
            }
        }
        return options;
    }

    protected DataListCollection getDataListCollection(String[] valueArray, FormData formData) {
        DataListCollection rows = null;
  
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        DatalistDefinitionDao datalistDefinitionDao = (DatalistDefinitionDao) AppUtil.getApplicationContext().getBean("datalistDefinitionDao");
        DatalistDefinition datalistDefinition = datalistDefinitionDao.loadById(getPropertyString("listId"), appDef);
        if (datalistDefinition != null && datalistDefinition.getJson() != null) {
        String json = datalistDefinition.getJson();

            //populate request params
            Object requestParamsProperty = getProperty("requestParams");
            if (requestParamsProperty != null && requestParamsProperty instanceof Object[]) {
                Form form = FormUtil.findRootForm(this);

                for (Object param : ((Object[]) requestParamsProperty)) {
                    Map paramMap = ((Map)param);
                    String parameter = (String) paramMap.get("param");
                    String fieldId = (String) paramMap.get("field");
                    String defaultValue = (String) paramMap.get("defaultValue");
                    String[] paramValues = null;
                    String paramValue = "";

                    if (fieldId != null && !fieldId.isEmpty()) {
                        Element field = FormUtil.findElement(fieldId, form, formData);
                        paramValues = FormUtil.getElementPropertyValues(field, formData);
                    }

                    if (!(paramValues != null && paramValues.length > 0)) {
                        paramValues = new String[]{defaultValue};
                    }

                    paramValue = FormUtil.generateElementPropertyValues(paramValues);
                    json = json.replaceAll(StringUtil.escapeRegex("#requestParam."+parameter+"#"), StringUtil.escapeRegex(paramValue));
                }
            }

            DataListService dataListService = (DataListService) AppUtil.getApplicationContext().getBean("dataListService");
            DataList dataList = dataListService.fromJson(json);

            DataListBinder binder = dataList.getBinder();
            idField = getPropertyString("idField");
            if (idField == null || idField.isEmpty()) {
                idField = binder.getPrimaryKeyColumnName();
            }
            String displayField = getPropertyString("displayField");
            rows = binder.getData(dataList, binder.getProperties(), null, displayField, false, null, null); 
        }
        return rows;
    }
 
    public static Form getForm(String formDefId) {
        Form form = null;
        if (formDefId != null && !formDefId.isEmpty()) {
            AppDefinition appDef = AppUtil.getCurrentAppDefinition();
            if (appDef != null) {
                FormDefinitionDao formDefinitionDao = (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");
                FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
                FormDefinition formDef = formDefinitionDao.loadById(formDefId, appDef);

                if (formDef != null) {
                    String json = formDef.getJson();
                    form = (Form) formService.createElementFromJson(json);
                }
            }
        }
        return form;
    }

     protected String getSelectedFormJson(Form form) {
        if (form != null) {
            FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
            String json = formService.generateElementJson(form);
            
            //replace the binder in json for popup form
            try {
                JSONObject temp = new JSONObject(json);
                JSONObject jsonProps = temp.getJSONObject(FormUtil.PROPERTY_PROPERTIES);
                
                JSONObject jsonLoadBinder = new JSONObject();
                jsonLoadBinder.put(FormUtil.PROPERTY_CLASS_NAME, "org.joget.plugin.enterprise.JsonFormBinder");
                jsonLoadBinder.put(FormUtil.PROPERTY_PROPERTIES, new JSONObject());
                jsonProps.put(FormBinder.FORM_LOAD_BINDER, jsonLoadBinder);
                jsonProps.put(FormBinder.FORM_STORE_BINDER, jsonLoadBinder);
                
                json = temp.toString();
            } catch (Exception e) {
                //ignore
            }
            return SecurityUtil.encrypt(json);
        }
        setProperty(FormUtil.PROPERTY_READONLY, "true");
        
        return "";
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String result = request.getParameter("result");
        String pluginName = request.getParameter("pluginName");
 
        if (pluginName.equalsIgnoreCase(getClassName())) {
            String formDefId = "";
            JsonObject jsonObject = JsonParser.parseString(result).getAsJsonObject();
            String recordId = jsonObject.get("id").getAsString();
            if (jsonObject.has("_tempRequestParamsMap") && !jsonObject.get("_tempRequestParamsMap").isJsonNull()) {
                JsonObject tempParams = jsonObject.getAsJsonObject("_tempRequestParamsMap");
                
                if (tempParams.has("_callback") && !tempParams.get("_callback").isJsonNull()) {
                    String[] parts = tempParams.get("_callback").getAsString().split("___");
        
                    if (parts.length > 1) {
                        formDefId = parts[1];
               
                    } 
                } 
            }
     
            if (formDefId != null && !formDefId.isEmpty()) {

                AppDefinition appDef = AppUtil.getCurrentAppDefinition();
                AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");
                FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
                Form form = getForm(formDefId);

                // submit
                String formDataRecordId = appService.getOriginProcessId(recordId);
                FormData formData = getFormData(result, formDataRecordId, null, form);
                JSONObject jsonResult = new JSONObject();
                if (formData != null) {
                    try {
                        formService.recursiveExecuteFormStoreBinders(form, form, formData);
                        jsonResult.put("result", "rowAdded");
                    } catch (Exception e) {
                        LogUtil.error(getClassName(), e, "");
                        jsonResult.put("result", "failed");
                    }
                } else {
                    jsonResult.put("result", "failed");
                }
                String jsonResults = jsonResult.toString();
                response.getWriter().write(jsonResults);
            }
        }
    }

    protected FormData getFormData(String json, String recordId, String processId, Form form) {
        try {
            FormData formData = new FormData();
            formData.setPrimaryKeyValue(recordId);
            formData.setProcessId(processId);

            FormRowSet rows = new FormRowSet();
            FormRow row = new FormRow();
            rows.add(row);

            JSONObject jsonObject = new JSONObject(json);
            for(Iterator iterator = jsonObject.keys(); iterator.hasNext();) {
                String key = (String) iterator.next();
                if (FormUtil.PROPERTY_TEMP_REQUEST_PARAMS.equals(key)) {
                    JSONObject tempRequestParamMap = jsonObject.getJSONObject(FormUtil.PROPERTY_TEMP_REQUEST_PARAMS);
                    JSONArray tempRequestParams = tempRequestParamMap.names();
                    if (tempRequestParams != null && tempRequestParams.length() > 0) {
                        for (int l = 0; l < tempRequestParams.length(); l++) {                        
                            List<String> rpValues = new ArrayList<String>();
                            String rpKey = tempRequestParams.getString(l);
                            JSONArray tempValues = tempRequestParamMap.getJSONArray(rpKey);
                            if (tempValues != null && tempValues.length() > 0) {
                                for (int m = 0; m < tempValues.length(); m++) {
                                    rpValues.add(tempValues.getString(m));
                                }
                            }
                            formData.addRequestParameterValues(rpKey, rpValues.toArray(new String[]{}));
                        }
                    }
                } else if (FormUtil.PROPERTY_TEMP_FILE_PATH.equals(key)) {
                    JSONObject tempFileMap = jsonObject.getJSONObject(FormUtil.PROPERTY_TEMP_FILE_PATH);
                    JSONArray tempFiles = tempFileMap.names();
                    if (tempFiles != null && tempFiles.length() > 0) {
                        for (int l = 0; l < tempFiles.length(); l++) {                        
                            List<String> rpValues = new ArrayList<String>();
                            String rpKey = tempFiles.getString(l);
                            JSONArray tempValues = tempFileMap.getJSONArray(rpKey);
                            if (tempValues != null && tempValues.length() > 0) {
                                for (int m = 0; m < tempValues.length(); m++) {
                                    String path = tempValues.getString(m);
                                    File file = FileManager.getFileByPath(path);
                                    if (file != null & file.exists()) {
                                        String newPath = UuidGenerator.getInstance().getUuid() + File.separator + file.getName();
                                        FileUtils.copyFile(file, new File(FileManager.getBaseDirectory(), newPath));
                                        rpValues.add(newPath);
                                    }
                                }
                            }
                            row.putTempFilePath(rpKey, rpValues.toArray(new String[]{}));
                        }
                    }
                } else {
                    String value = jsonObject.getString(key);
                    row.setProperty(key, value);
                }
            }
            row.setId(recordId);
            formData.setStoreBinderData(form.getStoreBinder(), rows);
            
            return formData;
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, recordId);
            return null;
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.jdt.templates;

import org.eclipse.che.ide.ext.java.jdt.templates.api.Template;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:evidolob@exoplatform.com">Evgen Vidolob</a>
 * @version $Id: 5:48:13 PM 34360 2009-07-22 23:58:59Z evgen $
 */
public class TemplateStore {

//    protected interface Templates extends ClientBundle {
//        @Source("templates.js")
//        TextResource templatesText();
//
//        @Source("codeTemplates.js")
//        TextResource codeTemplatesText();
//    }

    private static Template[] templates;
    private static Template[] codeTemplates;

    /**
     *
     */
    public TemplateStore() {
        if (templates == null) {
            parseTemplates();
        }
    }

    /** @return  */
    public Template[] getTemplates() {
        return templates;
    }

    /**
     *
     */
    private void parseTemplates() {
//        Templates templatesText = GWT.create(Templates.class);
//        JSONObject jsonObject = JSONParser.parseLenient(templatesText.templatesText().getText()).isObject();


        JSONObject jsonObject = JSONParser.parseLenient(TemplatesJs.codeTemplate()).isObject();
        JSONArray templatesJson = jsonObject.get("codeTemplates").isArray();
        if (templatesJson != null) {
            templates = new Template[templatesJson.size()];
            fillTemplates(templatesJson, templates);
        } else {
            templates = new Template[0];
        }

        jsonObject = JSONParser.parseLenient(TemplatesJs.templates()).isObject();
        templatesJson = jsonObject.get("templates").isArray();
        if (templatesJson != null) {
            codeTemplates = new Template[templatesJson.size()];
            fillTemplates(templatesJson, codeTemplates);
        } else {
            codeTemplates = new Template[0];
        }
    }

    /** @param templatesJson */
    private void fillTemplates(JSONArray templatesJson, Template[] templates) {
        String name, description, contextTypeId, pattern, id = null;
        boolean isAutoInsertable = false;
//        JsoArray<JsonObject> array = templatesJson.getJavaScriptObject().cast();
        for (int i = 0; i < templatesJson.size(); i++) {
            JSONObject tem = templatesJson.get(i).isObject();
            name = tem.get("name").isString().stringValue();
            description = tem.get("description").isString().stringValue();
            contextTypeId = tem.get("context").isString().stringValue();
            pattern = tem.get("text").isString().stringValue();
            isAutoInsertable = tem.get("autoinsert").isBoolean().booleanValue();
            id = tem.get("id").isString().stringValue();
            templates[i] = new Template(id, name, description, contextTypeId, pattern, isAutoInsertable);
        }
    }

    /**
     * @param contextTypeId
     * @return
     */
    public Template[] getTemplates(String contextTypeId) {
        List<Template> temList = new ArrayList<Template>();
        for (Template t : templates) {
            if (t.getContextTypeId().equals(contextTypeId))
                temList.add(t);
        }

        return temList.toArray(new Template[temList.size()]);
    }

    /**
     * @param id
     * @return
     */
    public Template findTemplateById(String id) {
        for (Template t : codeTemplates) {
            if (t.getId().equals(id))
                return t;
        }
        return null;
    }


}

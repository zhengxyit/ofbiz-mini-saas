package org.ofbiz.service;

import com.alibaba.fastjson.JSONObject;
import org.ofbiz.base.config.ResourceLoader;
import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/5/9.
 */
public class ApiServiceReader {

    private static List<JSONObject> apiList = new ArrayList();

    public static List<JSONObject> getApiList() throws Exception {
        if (apiList.size() == 0) {
            apiList.addAll(readModelService("sso"));
            apiList.addAll(readModelService("hr"));
            apiList.addAll(readModelService("crm"));
            apiList.addAll(readModelService("eam"));
        }

        return apiList;
    }

    public static String readModelService(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<JSONObject> modelServices = getApiList();

        request.setAttribute("success", true);
        request.setAttribute("list", modelServices);
        return "success";
    }

    public static List<JSONObject> readModelService(String module) throws Exception {
        // 不用这个过时的方法，因为没有国际化要用local的
        String ofbizHome = System.getProperty("ofbiz.home");
        Document document = ResourceLoader.getXmlDocument(ofbizHome + "/applications/" + module + "/servicedef/services.xml");

        List<JSONObject> modelServices = new ArrayList();

        Element docElement = document.getDocumentElement();
        if (docElement == null) {
            return null;
        }

        docElement.normalize();

        Node curChild = docElement.getFirstChild();
        if (curChild != null) {
            do {
                if (curChild.getNodeType() == Node.ELEMENT_NODE && "service".equals(curChild.getNodeName())) {
                    Element curServiceElement = (Element) curChild;
                    JSONObject service = createModelService(curServiceElement);
                    if (service == null) {
                        continue;
                    }
                    service.put("name", "/" + module + "/j/" + service.getString("name"));
                    modelServices.add(service);
                }
            } while ((curChild = curChild.getNextSibling()) != null);
        }
        return modelServices;
    }

    private static JSONObject createModelService(Element serviceElement) {
        JSONObject service = new JSONObject();

        service.put("name", UtilXml.checkEmpty(serviceElement.getAttribute("name")).intern());
        service.put("engineName", UtilXml.checkEmpty(serviceElement.getAttribute("engine")).intern());
        service.put("location", UtilXml.checkEmpty(serviceElement.getAttribute("location")).intern());
        service.put("auth", "true".equalsIgnoreCase(serviceElement.getAttribute("auth")));
        service.put("baseModule", serviceElement.getAttribute("base-module"));
        service.put("retModel", serviceElement.getAttribute("ret-model"));
        service.put("description", getCDATADef(serviceElement, "description"));

        String release = UtilXml.checkEmpty(serviceElement.getAttribute("release")).intern();
        if ("Y".equals(release)) {
            createAttrDefs(serviceElement, service); // 加载属性
            return service;
        }
        return null;
    }

    private static String getCDATADef(Element baseElement, String tagName) {
        String value = "";
        NodeList nl = baseElement.getElementsByTagName(tagName);

        if (nl.getLength() > 0) {
            Node n = nl.item(0);
            NodeList childNodes = n.getChildNodes();
            if (childNodes.getLength() > 0) {
                Node cdata = childNodes.item(0);
                value = UtilXml.checkEmpty(cdata.getNodeValue());
            }
        }
        return value;
    }

    private static void createAttrDefs(Element baseElement, JSONObject service) {
        List<JSONObject> attributes = new ArrayList();

        for (Element attribute : UtilXml.childElementList(baseElement, "attribute")) {
            JSONObject param = new JSONObject();

            String type = UtilXml.checkEmpty(attribute.getAttribute("type")).intern();
            param.put("name", UtilXml.checkEmpty(attribute.getAttribute("name")).intern());
            param.put("description", getCDATADef(attribute, "description"));
            param.put("type", type);
            param.put("mode", UtilXml.checkEmpty(attribute.getAttribute("mode")).intern());
            param.put("optional", "true".equalsIgnoreCase(attribute.getAttribute("optional")));

            if ("json".equals(type) || "List".equals(type)) {
                createAttrDefs(attribute, param);
            }

            attributes.add(param);
        }
        service.put("attributes", attributes);
    }

}

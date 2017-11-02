/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.service.engine;

import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceDispatcher;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic Service SOAP Interface
 */
public final class HttpJsonEngine extends GenericAsyncEngine {

    public static final String module = HttpJsonEngine.class.getName();

    public HttpJsonEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * @see GenericEngine#runSyncIgnore(String, ModelService, Map)
     */
    @Override
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        runSync(localName, modelService, context);
    }

    /**
     * @see GenericEngine#runSync(String, ModelService, Map)
     */
    @Override
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        Map<String, Object> result = serviceInvoker(modelService, context);

        if (result == null)
            throw new GenericServiceException("Service did not return expected result");

        result.put("data","ffffffffffff");
        return result;
    }

    // Invoke the remote SOAP service
    private Map<String, Object> serviceInvoker(ModelService modelService, Map<String, Object> context) throws GenericServiceException {
//        Delegator delegator = dispatcher.getDelegator();
//        if (modelService.location == null || modelService.invoke == null)
//            throw new GenericServiceException("Cannot locate service to invoke");
//
//        ServiceClient client = null;
//        QName serviceName = null;
//
//        try {
//            client = new ServiceClient();
//            Options options = new Options();
//            EndpointReference endPoint = new EndpointReference(this.getLocation(modelService));
//            options.setTo(endPoint);
//            client.setOptions(options);
//        } catch (AxisFault e) {
//            throw new GenericServiceException("RPC service error", e);
//        }
//
//        List<ModelParam> inModelParamList = modelService.getInModelParamList();
//
//        if (Debug.infoOn()) Debug.logInfo("[SOAPClientEngine.invoke] : Parameter length - " + inModelParamList.size(), module);
//
//        if (UtilValidate.isNotEmpty(modelService.nameSpace)) {
//            serviceName = new QName(modelService.nameSpace, modelService.invoke);
//        } else {
//            serviceName = new QName(modelService.invoke);
//        }
//
//        int i = 0;
//
//        Map<String, Object> parameterMap = new HashMap<String, Object>();
//        for (ModelParam p: inModelParamList) {
//            if (Debug.infoOn()) Debug.logInfo("[SOAPClientEngine.invoke} : Parameter: " + p.name + " (" + p.mode + ") - " + i, module);
//
//            // exclude params that ModelServiceReader insert into (internal params)
//            if (!p.internal) {
//                parameterMap.put(p.name, context.get(p.name));
//            }
//            i++;
//        }
//
//
//        Map<String, Object> results = null;
//        try {
//            OMFactory factory = OMAbstractFactory.getOMFactory();
//            OMElement payload = factory.createOMElement(serviceName);
//            payload.addChild(parameterSer.getFirstElement());
//            OMElement respOMElement = client.sendReceive(payload);
//            client.cleanupTransport();
//            results = UtilGenerics.cast(SoapSerializer.deserialize(respOMElement.toString(), delegator));
//        } catch (Exception e) {
//            Debug.logError(e, module);
//        }
        //return results;

        return new HashMap<String,Object>();
    }
}

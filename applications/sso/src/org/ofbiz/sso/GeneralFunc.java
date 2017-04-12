package org.ofbiz.sso;

import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ted Zheng on 2016/5/21.
 */
public class GeneralFunc {

    public static void sendNotifyByWorkflow(String msg, String tenantId, String orderCode, String workflowId) {
        Delegator delegator = DelegatorFactory.getDelegator("default#" + tenantId);

        // 查EamWorkflowView视图
        try {
            List<GenericValue> empList = delegator.findList("EamWorkflowView", EntityCondition.makeCondition("workflowId", EntityOperator.EQUALS, workflowId), null, null, null, false);
            List<String> recevers = new ArrayList<String>();
            for (GenericValue o : empList) {
                if (o.getString("mobile") != null && !"".equals(o.getString("mobile"))) {
                    recevers.add(o.getString("mobile"));
                }
            }
            // 根据手机查推送的ID
            List<GenericValue> apiUsers = delegator.findList("ApiLoginUser", EntityCondition.makeCondition("username1", EntityOperator.IN, recevers), null, null, null, false);
            List<String> registerIds = new ArrayList<String>();
            for (GenericValue o : apiUsers) {
                if (o.getString("registrationId") != null && !"".equals(o.getString("registrationId"))) {
                    registerIds.add(o.getString("registrationId"));
                }
            }
            if (registerIds.size() != 0) {
                PushServer.sendNotifyMessage(msg, orderCode, registerIds);
            }
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }
    }

    public static void sendNotifyByMobiles(String msg, String tenantId, String orderCode, List<String> mobiles) {
        Delegator delegator = DelegatorFactory.getDelegator("default#" + tenantId);

        // 查EamWorkflowView视图
        try {
            // 根据手机查推送的ID
            List<GenericValue> apiUsers = delegator.findList("ApiLoginUser", EntityCondition.makeCondition("username1", EntityOperator.IN, mobiles), null, null, null, false);
            List<String> registerIds = new ArrayList<String>();
            for (GenericValue o : apiUsers) {
                if (o.getString("registrationId") != null && !"".equals(o.getString("registrationId"))) {
                    registerIds.add(o.getString("registrationId"));
                }
            }
            if (registerIds.size() != 0) {
                PushServer.sendNotifyMessage(msg, orderCode, registerIds);
            }
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }
    }
}

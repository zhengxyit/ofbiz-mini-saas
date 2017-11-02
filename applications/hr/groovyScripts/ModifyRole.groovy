import com.alibaba.fastjson.JSONArray
import org.ofbiz.base.util.UtilMisc
import org.ofbiz.entity.GenericValue
import org.ofbiz.entity.condition.EntityCondition
import org.ofbiz.entity.condition.EntityOperator

roleId = parameters.roleId;
roleName = parameters.roleName;
authList = parameters.authList;

fields = new HashMap<String, Object>();
fields.roleId = roleId;
role = delegator.findOne("HrRole", fields, false);
if (role == null) {
    return returnFAIL("未找到可更改的数据");
}
if (roleName != null) {
    role.roleName = roleName;
}

role.store();

if (authList != null) {
    // 删除所有
    oldList = delegator.findList("HrRoleAuth", EntityCondition.makeCondition("roleId", EntityOperator.EQUALS, role.roleId), null, null, null, false);
    for (GenericValue gv : oldList) {
        gv.remove();
    }

    auths = JSONArray.parseArray(authList);
    for (String auth : auths) {
        roleAuth = delegator.makeValue("HrRoleAuth");
        roleAuth.roleAuthId = delegator.getNextSeqId("HrRoleAuth").toString();
        roleAuth.roleId = role.roleId;
        roleAuth.serviceUrl = auth;
        roleAuth.create();
    }
}

return returnSUCCESS();
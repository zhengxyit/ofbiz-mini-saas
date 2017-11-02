import com.alibaba.fastjson.JSONArray

roleName = parameters.roleName;
authList = parameters.authList;

role = delegator.makeValue("HrRole");
role.roleId = delegator.getNextSeqId("HrRole").toString();
role.roleName = roleName;

role.create();

auths = JSONArray.parseArray(authList);
for (String auth : auths) {
    roleAuth = delegator.makeValue("HrRoleAuth");
    roleAuth.roleAuthId = delegator.getNextSeqId("HrRoleAuth").toString();
    roleAuth.roleId = role.roleId;
    roleAuth.serviceUrl = auth;
    roleAuth.create();
}

return returnSUCCESS();
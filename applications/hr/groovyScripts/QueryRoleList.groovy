import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import org.ofbiz.base.util.UtilMisc
import org.ofbiz.entity.GenericValue
import org.ofbiz.entity.condition.EntityCondition
import org.ofbiz.entity.condition.EntityOperator
import org.ofbiz.entity.util.EntityListIterator

query = parameters.query;
isAll = parameters.isAll;
useCache = parameters.refresh;

if (useCache == null) {
    useCache = true;
}
if (useCache == "Y") {
    useCache = false;
}
expr = null;
if(query != null){
    expr = EntityCondition.makeCondition("roleName", EntityOperator.LIKE, "%" + query + "%");
}

fields = UtilMisc.toSet("roleId", "roleName");

roleList = null;
// 分页
if (isAll == null || isAll != "Y") {
    int start = 1;
    int rowSize = 20;
    if (parameters.start != null) {
        start = Integer.valueOf(parameters.start);
    }
    if (parameters.rowSize != null) {
        rowSize = Integer.valueOf(parameters.rowSize);
    }

    EntityListIterator listIt = delegator.find("HrRole", expr, null, fields, UtilMisc.toList("roleName"), null);
    roleList = listIt.getPartialList(start, rowSize);
    listIt.close();
} else {
    roleList = delegator.findList("HrRole", expr, fields, UtilMisc.toList("roleName"), null, useCache);
}

result = new HashMap<String, Object>();
result.list = [];
// 查询所有角色的权限
for (GenericValue gv : roleList) {
    JSONObject obj = gv.toSorted();
    authList = delegator.findList("HrRoleAuth", EntityCondition.makeCondition("roleId", EntityOperator.EQUALS, gv.get("roleId")), UtilMisc.toSet("serviceUrl"), null, null, useCache);
    List<String> stringList = new ArrayList();
    if (authList != null) {
        for (GenericValue auth : authList) {
            stringList.add(auth.get("serviceUrl").toString());
        }
    }
    obj.authList = stringList;
    result.list.add(obj);
}

result.count = delegator.findCountByCondition("HrRole", expr, null, null);

return returnSUCCESS(result);
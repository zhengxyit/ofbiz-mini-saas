import com.alibaba.fastjson.JSONArray
import org.ofbiz.entity.GenericValue
import org.ofbiz.entity.condition.EntityCondition
import org.ofbiz.entity.condition.EntityOperator

/**
 * 创建部门
 * 两种情况
 * 1 不支持工厂
 * 2 支持工厂
 * @author Ted Zheng 2017-4-27
 */

groupId = parameters.groupId;
superId = parameters.superId;
groupName = parameters.groupName;

fields = new HashMap<String, Object>();
fields.groupId = groupId;
group = delegator.findOne("HrGroup", fields, false);
if (group == null) {
    return returnFAIL("未找到可更改的数据");
}
if (groupName != null) {
    group.groupName = groupName;
}
if (superId != null) {
    group.pGroupId = superId;
}

if (parameters.roles != null && !"".equals(parameters.roles)) {
    // 先删掉
    roleList = delegator.findList("HrGroupRole", EntityCondition.makeCondition("groupId", EntityOperator.EQUALS, group.groupId), null, null, null, false);
    for (GenericValue gv : roleList) {
        gv.remove();
    }
    roles = JSONArray.parseArray(parameters.roles);
    for (Object roleId : roles) {
        groupRole = delegator.makeValue("HrGroupRole");
        groupRole.groupRoleId = delegator.getNextSeqId("HrGroupRole");
        groupRole.roleId = roleId;
        groupRole.groupId = group.groupId;
        groupRole.create();
    }
}

if (parameters.employees != null && !"".equals(parameters.employees)) {
    // 先删掉
    employeeList = delegator.findList("HrGroupEmployee", EntityCondition.makeCondition("groupId", EntityOperator.EQUALS, group.groupId), null, null, null, false);
    for (GenericValue gv : employeeList) {
        gv.remove();
    }
    employees = JSONArray.parseArray(parameters.employees);
    for (Object employeeId : employees) {
        groupEmployee = delegator.makeValue("HrGroupEmployee");
        groupEmployee.groupEmployeeId = delegator.getNextSeqId("HrGroupEmployee");
        groupEmployee.groupId = group.groupId;
        groupEmployee.employeeId = employeeId;
        groupEmployee.create();
    }
}

group.store();

return returnSUCCESS();
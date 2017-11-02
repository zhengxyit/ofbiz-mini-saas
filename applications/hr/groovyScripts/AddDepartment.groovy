import com.alibaba.fastjson.JSONArray
import org.ofbiz.base.util.UtilMisc
import org.ofbiz.base.util.UtilValidate
import org.ofbiz.base.util.StringUtil

/**
 * 创建部门
 * 两种情况
 * 1 不支持工厂
 * 2 支持工厂
 * @author Ted Zheng 2017-4-27
 */

superId = parameters.superId;
groupName = parameters.groupName;

group = delegator.makeValue("HrGroup");
group.groupId = StringUtil.radix26(Integer.valueOf(delegator.getNextSeqId("HrGroup").toString()) - 99999999) + ""; // 将序列车成字母
if (!UtilValidate.isEmpty(superId)) {
    superGroup = delegator.findOne("HrGroup", UtilMisc.toMap("groupId", superId), true);
    group.groupType = "D";
    group.pGroupId = superId;
    group.groupGrade = superGroup.groupGrade + 1;
    group.groupPath = superGroup.groupPath + group.groupId + ",";
} else {
    group.groupType = "D";
    group.groupGrade = 1;
    group.groupPath = "," + group.groupId + ",";
}

group.groupName = groupName;

if (parameters.roles != null && !"".equals(parameters.roles)) {
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
    employees = JSONArray.parseArray(parameters.employees);
    for (Object employeeId : employees) {
        groupEmployee = delegator.makeValue("HrGroupEmployee");
        groupEmployee.groupEmployeeId = delegator.getNextSeqId("HrGroupEmployee");
        groupEmployee.groupId = group.groupId;
        groupEmployee.employeeId = employeeId;
        groupEmployee.create();
    }
}

group.create();

return returnSUCCESS();
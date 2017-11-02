import org.ofbiz.base.util.UtilMisc
import org.ofbiz.entity.condition.EntityCondition
import org.ofbiz.entity.condition.EntityOperator
import org.ofbiz.entity.util.EntityListIterator;

def queryGroup() {
    query = parameters.query;
    isAll = parameters.isAll;
    useCache = parameters.refresh;

    if (useCache == null) {
        useCache = true;
    }
    if (useCache == "Y") {
        useCache = false;
    }
    exprList = [];
    if (query != null && !"".equals(query)) {
        exprList.add(EntityCondition.makeCondition("groupName", EntityOperator.LIKE, "%" + query + "%"));
    }

    topCond = EntityCondition.makeCondition(exprList, EntityOperator.AND);
    fields = UtilMisc.toSet("groupId", "groupName", "pGroupId", "groupGrade", "groupPath");

    dataList = null;
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

        EntityListIterator listIt = delegator.find("HrGroup", topCond, null, fields, null, null);
        dataList = listIt.getPartialList(start, rowSize);
        listIt.close();
    } else {
        dataList = delegator.findList("HrGroup", topCond, fields, null, null, useCache);
    }

    result = new HashMap<String, Object>();
    result.list = dataList;
    result.count = delegator.findCountByCondition("HrGroup", topCond, null, null);

    return returnSUCCESS(result);
}

def groupInfo() {
    groupId = parameters.groupId;
    obj = new HashMap<String, Object>();

    group = delegator.findOne("HrGroup", UtilMisc.toMap("groupId", groupId), true);

    // 查角色
    roleList = group.getRelatedMulti("HrGroupRole", "HrRole", UtilMisc.toList("roleName"));
    // 查部门下员工
    employeeList = group.getRelatedMulti("HrGroupEmployee", "HrEmployee", UtilMisc.toList("employeeName"));
    // 组织单位是该单位的
    equipmentList = delegator.findList("EamEquipment", EntityCondition.makeCondition("maintainInfo", EntityOperator.LIKE, "%\"departmentId\":\"" + groupId + "\"%"), null, null, null, true);

    obj.roles = roleList.toSorted();
    obj.employees = employeeList.toSorted();
    obj.equipments = equipmentList.toSorted();

    result = new HashMap<String, Object>();
    result.data = obj;

    return returnSUCCESS(result);
}

def removeEmployee() {
    groupId = parameters.groupId;
    employeeId = parameters.employeeId;

    exprList = [];
    exprList.add(EntityCondition.makeCondition("groupId", EntityOperator.EQUALS, groupId));
    exprList.add(EntityCondition.makeCondition("employeeId", EntityOperator.EQUALS, employeeId));
    topCond = EntityCondition.makeCondition(exprList, EntityOperator.AND);

    ges = delegator.findList("HrGroupEmployee", topCond, null, null, null, false);
    if (ges != null && ges.size()>0) {
        ges.get(0).remove();
    }
    return returnSUCCESS();
}
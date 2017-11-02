import org.ofbiz.entity.condition.EntityExpr
import org.ofbiz.entity.condition.EntityOperator
import org.ofbiz.entity.field.JsonValue
import org.ofbiz.entity.util.EntityFindOptions

employeeId = parameters.employeeId
employeeName = parameters.employeeName
groupId = parameters.groupId
mobile = parameters.mobile
email = parameters.email
isApp = parameters.isApp
isPc = parameters.isPc
name = parameters.name
age = parameters.age

if (employeeId == null || employeeId == "") {
    // TODO 需要改成国际化
    return returnFAIL("未找到员工信息");
}

fields = new HashMap<String, Object>();
fields.employeeId = employeeId;
employee = delegator.findOne("HrEmployee", fields, false);

if (employee == null || employee == "") {
    // TODO 需要改成国际化
    return returnFAIL("未找到员工信息");
}

if (employeeName != null && !"".equals(employeeName)) {
    employee.employeeName = employeeName;
}
if (groupId != null && !"".equals(groupId)) {
    employee.groupId = groupId;
}
if (mobile != null && !"".equals(mobile)) {
    EntityFindOptions findOptions = new EntityFindOptions();
    findOptions.setMaxRows(1);
    userList = delegator.findList("Account", new EntityExpr("username1", EntityOperator.EQUALS, employee.mobile), null, null, findOptions, false);
    if (userList != null && userList.size() != 0) {
        user = userList.get(0);
        user.username1 = mobile;
        if (user.accountStatus == 0 && ("Y".equals(isApp) || "Y".equals(isPc))) {
            user.accountStatus = 2; // 第一次要求改密码
        }
        if ((user.accountStatus == 1 || user.accountStatus == 2) && !"Y".equals(isApp) && !"Y".equals(isPc)) {
            user.accountStatus = 0;
        }
        user.store();
    }

    employee.mobile = mobile;
}
if (email != null && !"".equals(email)) {
    employee.email = email;
}
if (isApp != null && !"".equals(isApp)) {
    employee.isApp = isApp;
} else {
    employee.isApp = "";
}
if (isPc != null && !"".equals(isPc)) {
    employee.isPc = isPc;
} else {
    employee.isPc = "";
}

employee.custom = JsonValue.create(employee.getModelEntity().getField("custom"), null)
employee.custom.name = name
employee.custom.age = age

employee.store();

return returnSUCCESS();
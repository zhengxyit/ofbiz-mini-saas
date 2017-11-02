import org.ofbiz.entity.condition.EntityCondition
import org.ofbiz.entity.condition.EntityOperator
import org.ofbiz.sso.LoginService

employeeName = parameters.employeeName;
groupId = parameters.groupId;
mobile = parameters.mobile;
email = parameters.email;
isApp = parameters.isApp;
isPc = parameters.isPc;

// 还缺少用户ID关联

if (employeeName == null || employeeName == "") {
    // TODO 需要改成国际化
    return returnFAIL("员工姓名不能为空");
}

// 手机号不能重复
list = delegator.findList("Account", EntityCondition.makeCondition("username1", EntityOperator.EQUALS, mobile), null, null, null, false);
if (list != null && list.size() != 0) {
    return returnFAIL("该手机号已存在");
}

employee = delegator.makeValue("HrEmployee");
employeeId = delegator.getNextSeqId("HrEmployee").toString();
employee.employeeId = employeeId;
employee.employeeName = employeeName;
if (mobile != null && !"".equals(mobile)) {
    employee.mobile = mobile;
}
if (email != null && !"".equals(email)) {
    employee.email = email;
}
if (groupId != null && !"".equals(groupId)) {
    employee.groupId = groupId;
}
if (isApp != null && !"".equals(isApp)) {
    employee.isApp = isApp;
}
if (isPc != null && !"".equals(isPc)) {
    employee.isPc = isPc;
}
employee.create();

status = 0;
if ("Y".equals(isApp) || "Y".equals(isPc)) {
    status = 2; // 创建后要求改密码
}
// 需创建一个Login帐号
try {
    LoginService.register(employee.mobile, "123456", status);
} catch (Exception e) {
    delegator.rollback(); // 调Java不成功需要自己调
    return returnFAIL(e.message);
}
Map result = new HashMap<String, Object>();
result.employeeId = employeeId;

return returnSUCCESS(result);
import org.ofbiz.entity.condition.EntityCondition
import org.ofbiz.entity.condition.EntityOperator
import org.ofbiz.entity.util.EntityFindOptions
import org.ofbiz.sso.LoginService

employeeId = parameters.employeeId;

fields = new HashMap<String, Object>();
fields.employeeId = employeeId;
employee = delegator.findOne("HrEmployee", fields, false); // 必须是False不能是缓存
if (employee == null) {
    return returnFAIL("未找到可更改的数据");
}
EntityFindOptions findOptions = new EntityFindOptions();
findOptions.setMaxRows(1);
// 1 部门里使用
gList = delegator.findList("HrGroupEmployee", EntityCondition.makeCondition("employeeId", EntityOperator.EQUALS, employeeId), null, null, findOptions, true);
if (gList != null && gList.size() != 0) {
    return returnFAIL("员工已分配到部门，无法删除");
}

// 同时删除登录
try {
    LoginService.removeUser(employee.get("mobile"));
} catch (Exception e) {
    delegator.rollback(); // 调Java不成功需要自己调
    return returnFAIL(e.message);
}

employee.remove();

return returnSUCCESS();
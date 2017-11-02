import org.ofbiz.entity.GenericValue
import org.ofbiz.entity.condition.EntityCondition
import org.ofbiz.entity.condition.EntityOperator
import org.ofbiz.entity.util.EntityFindOptions

// 仅仅是修改帐户的登录标识
employeeId = parameters.employeeId;

fields = new HashMap<String, Object>();
fields.employeeId = employeeId;
employee = delegator.findOne("HrEmployee", fields, false)

if (employee != null) {
    // 应该只有一条
    EntityFindOptions findOptions = new EntityFindOptions();
    findOptions.setMaxRows(1);

    List<GenericValue> userList = delegator.findList("Account", EntityCondition.makeCondition("username1", EntityOperator.EQUALS, employee.mobile), null, null, findOptions, false);
    // 找登录LoginUser
    if (userList != null && userList.size() == 1) {
        account = userList.get(0);
        account.accountStatus = 2;
        account.store();

        // 还需要删除所有的Token
        tokens = delegator.findList("LoginToken", EntityCondition.makeCondition("ticket", EntityOperator.LIKE, "%#" + account.accountId + "#%"), null, null, null, false);
        for (GenericValue gv : tokens) {
            gv.remove();
        }
    }
}

return returnSUCCESS();
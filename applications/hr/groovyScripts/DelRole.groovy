import org.ofbiz.entity.GenericValue
import org.ofbiz.entity.condition.EntityCondition
import org.ofbiz.entity.condition.EntityOperator
import org.ofbiz.entity.util.EntityFindOptions

roleId = parameters.roleId;

fields = new HashMap<String, Object>();
fields.roleId = roleId;
role = delegator.findOne("HrRole", fields, false);

if (role == null) {
    // TODO 需要改成国际化
    return returnFAIL("未找到可更改的数据");
}

// 1 查部门关系是否已关联
EntityFindOptions findOptions = new EntityFindOptions();
findOptions.setMaxRows(1);
rList = delegator.findList("HrGroupRole", EntityCondition.makeCondition("roleId", EntityOperator.EQUALS, roleId), null, null, findOptions, true);
if (rList != null && rList.size() != 0) {
    return returnFAIL("部门中已使用该角色，无法删除");
}

// 删除所有
oldList = delegator.findList("HrRoleAuth", EntityCondition.makeCondition("roleId", EntityOperator.EQUALS, role.roleId), null, null, null, false);
for (GenericValue gv : oldList) {
    gv.remove();
}

role.remove();

return returnSUCCESS();
import org.ofbiz.entity.GenericValue
import org.ofbiz.entity.condition.EntityCondition
import org.ofbiz.entity.condition.EntityOperator
import org.ofbiz.entity.util.EntityFindOptions

/**
 * 创建部门
 * 两种情况
 * 1 不支持工厂
 * 2 支持工厂
 * @author Ted Zheng 2017-4-27
 */

groupId = parameters.groupId;

fields = new HashMap<String, Object>();
fields.groupId = groupId;
group = delegator.findOne("HrGroup", fields, false); // 必须是False不能是缓存
if (group == null) {
    return returnFAIL("未找到可更改的数据");
}

roleList = delegator.findList("HrGroupRole", EntityCondition.makeCondition("groupId", EntityOperator.EQUALS, group.groupId), null, null, null, false);
for (GenericValue gv : roleList) {
    gv.remove();
}
employeeList = delegator.findList("HrGroupEmployee", EntityCondition.makeCondition("groupId", EntityOperator.EQUALS, group.groupId), null, null, null, false);
for (GenericValue gv : employeeList) {
    gv.remove();
}

group.remove();

return returnSUCCESS();
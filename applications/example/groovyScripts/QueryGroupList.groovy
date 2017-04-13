
/**
 *  查询组信息（对应部门）
 */

List companyList = delegator.findList("Group", null, null, null, null, true);

Map result = new HashMap<String, Object>();
result.list = companyList;

return returnSUCCESS(result);
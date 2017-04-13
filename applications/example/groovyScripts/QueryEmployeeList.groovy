/**
 *  查询员工信息
 */

List companyList = delegator.findList("Employee", null, null, null, null, true);

Map result = new HashMap<String,Object>();
result.list = companyList;

return returnSUCCESS(result);
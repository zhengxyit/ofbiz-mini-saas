import org.ofbiz.entity.util.EntityQuery

query = parameters.query
isAll = parameters.isAll
useCache = parameters.refresh

if (useCache == null) {
    useCache = true
}
if (useCache == "Y") {
    useCache = false
}

companyList = EntityQuery.use(delegator).from("HrEmployee").cache().queryList()

result = new HashMap<String, Object>()
result.list = companyList
result.count = 1l

return returnSUCCESS(result)
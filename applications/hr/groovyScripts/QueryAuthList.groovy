result = new HashMap<String, Object>()
result.list = []

Map<String, Object> auth = null

auth = new HashMap<String, Object>()
auth.name = "管理员"
auth.code = "ADMIN"
result.list.add(auth)

auth = new HashMap<String, Object>()
auth.name = "基础信息维护"
auth.code = "BASE"
result.list.add(auth)

auth = new HashMap<String, Object>()
auth.name = "系统管理维护"
auth.code = "SYS"
result.list.add(auth)

auth = new HashMap<String, Object>()
auth.name = "业务管理维护"
auth.code = "BUSY"
result.list.add(auth)

return returnSUCCESS(result)
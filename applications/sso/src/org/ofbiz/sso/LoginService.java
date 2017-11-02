package org.ofbiz.sso;

import com.alibaba.fastjson.JSONObject;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.Base64;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.TicketUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.*;

/**
 * 现在只适合后台登录
 * Created by Ted Zheng on 2016/4/8.
 */
public class LoginService {

    public static final String module = LoginService.class.getName();

    public static final String resourceError = "SsoErrorUiLabels";

    public static String basicLogin(HttpServletRequest request, HttpServletResponse response) {
        // 先使用默认的查用户的租户信息
        String registrationID = request.getParameter("registrationID"); // 极光推送使用
        String platform = request.getParameter("platform"); // 平台 P:PC Web A:App X:平台管理员，自己人
        String openId = request.getParameter("openId");
        if (platform == null) {
            platform = "A"; // 默认来源是App
        }

        String auth = request.getHeader("Authorization");

        if (auth != null && auth.length() > 6) {
            auth = auth.substring(6, auth.length());

            String decodedAuth = Base64.base64Decode(auth);

            String[] strs = decodedAuth.split(":");
            if (strs == null || strs.length != 2) {
                return authErrorMsg(request, response, "1000", "用户名或密码错误!(52)");
            }

            String username = strs[0];
            String password = strs[1];
            try {
                Delegator delegator = DelegatorFactory.getDelegator("default");

                // 验证用户名密码,两种登录都支持
                List usernameList = UtilMisc.toList(new EntityExpr("username1", EntityOperator.EQUALS, username), new EntityExpr("username2", EntityOperator.EQUALS, username));
                EntityConditionList userConditions = new EntityConditionList(usernameList, EntityOperator.OR);

                GenericValue loginUser = EntityQuery.use(delegator).from("Account").where(userConditions).queryFirst();
                // 找登录LoginUser
                if (loginUser == null) {
                    return authErrorMsg(request, response, "1000", "用户名或密码错误!(67)");
                }
                if (loginUser.get("accountStatus") == null || loginUser.getInteger("accountStatus") == 0) {
                    return authErrorMsg(request, response, "1000", "该用户被禁止登录，请联系企业管理员!");
                }
                if (loginUser.get("accountStatus") == null || loginUser.getInteger("accountStatus") == 2) {
                    return authErrorMsg(request, response, "1001", "需要更改密码才能登录!");
                }
                // 判断状态后再判断密码
                if (!Base64.base64Encode(password).equals(loginUser.getString("currentPassword"))) {
                    return authErrorMsg(request, response, "1000", "用户名或密码错误!(77)");
                }

                String[] tenants = TicketUtil.asArrayByString(loginUser.getString("tenants"));
                String tenant = tenants[0];
                System.out.println(tenant);
                // TODO 先当一个处理
                delegator.setTenantId(tenant);

                Map<String, Object> userLoginToken = new HashMap<String, Object>();

                if (loginUser != null) {
                    String mobile = loginUser.get("username1").toString();
                    String permissionData = "BUSY"; // 默认
                    JSONObject personal = new JSONObject();

                    // 从员工信息中读
                    GenericValue employee = EntityQuery.use(delegator).from("HrEmployee").where("mobile", mobile).queryFirst();
                    if (employee == null) {
                        return authErrorMsg(request, response, "1000", "用户名或密码错误!(95)");
                    }

                    if ("A".equals(platform) && !"Y".equals(employee.get("isApp"))) {
                        return authErrorMsg(request, response, "1000", "该用户被禁止在移动端登录，请联系企业管理员!");
                    }
                    if ("P".equals(platform) && !"Y".equals(employee.get("isPc"))) {
                        return authErrorMsg(request, response, "1000", "该用户被禁止在管理后台登录，请联系企业管理员!");
                    }

                    personal.put("accountId", loginUser.get("accountId"));
                    personal.put("platform", platform);
                    personal.put("employeeId", employee.get("employeeId"));
                    personal.put("name", employee.get("employeeName"));
                    personal.put("mobile", employee.get("mobile"));
                    personal.put("email", employee.get("email"));
                    personal.put("code", employee.get("code"));
                    personal.put("headPic", employee.get("headPic"));
                    personal.put("employeeStatus", employee.get("employeeStatus"));
                    personal.put("openId", openId);
                    personal.put("tenant", tenant);

                    if (registrationID != null && !"".equals(registrationID)) {
                        // 极光推送注册ID更新
                        loginUser.put("registrationId", registrationID);
                        loginUser.store();
                    }
                    if (openId != null && !"".equals(openId)) {
                        employee.put("openId", openId);
                        employee.store();
                    }

                    // 设备企业的缓存
                    GenericValue enterprise = EntityQuery.use(delegator).from("Enterprise").where("domainName", tenant).queryFirst();
                    if (enterprise == null) {
                        return authErrorMsg(request, response, "1000", "未查询到该企业信息!");
                    }

                    if ("P".equals(platform)) {
                        // 如果是管理员不用判断
                        if (personal.getString("mobile").equals(enterprise.getString("manager"))) {
                            permissionData = "ADMIN";
                        } else {
                            // 先查自己的角色，再查权限
                            List<GenericValue> groupList = delegator.findList("HrGroupEmployee", EntityCondition.makeCondition("employeeId", EntityOperator.EQUALS, employee.get("employeeId")), null, null, null, true);
                            List groups = new ArrayList<String>();
                            for (int i = 0; i < groupList.size(); i++) {
                                groups.add(groupList.get(i).get("groupId"));
                            }

                            List<GenericValue> groupRoleList = delegator.findList("HrGroupRole", EntityCondition.makeCondition("groupId", EntityOperator.IN, groups), null, null, null, true);
                            if (groupRoleList.size() > 0) {
                                permissionData = ""; // 如果有权限组，使用定义的
                            }
                            for (int j = 0; j < groupRoleList.size(); j++) {
                                if (j != 0) {
                                    permissionData += ",";
                                }
                                GenericValue gr = groupRoleList.get(j);
                                List<GenericValue> authList = gr.getRelatedMulti("HrRole", "HrRoleAuth");
                                for (int i = 0; i < authList.size(); i++) {
                                    String serviceUrl = authList.get(i).get("serviceUrl").toString();
                                    if (permissionData.contains(serviceUrl)) {
                                        // 如果已有就不添加了
                                        continue;
                                    }
                                    if (i != 0) {
                                        permissionData += ",";
                                    }
                                    if ("ADMIN".equals(serviceUrl)) {
                                        permissionData = "ADMIN";
                                        break;
                                    }
                                    permissionData += serviceUrl;
                                }
                            }

                            userLoginToken.put("permissionData", permissionData);
                        }
                    }

                    // ticket
                    String ticket = null;
                    if (tenant != null) {
                        ticket = TicketUtil.makeTicket() + ":" + personal.get("employeeId") + ":" + platform + ":" + tenant;
                    } else {
                        ticket = TicketUtil.makeTicket() + ":" + personal.get("employeeId") + ":" + platform;
                    }

                    userLoginToken.put("ticket", ticket);
                    userLoginToken.put("expirationTime", UtilDateTime.getDayEnd(new Timestamp(Calendar.getInstance().getTimeInMillis()), 7l));

                    JSONObject result = new JSONObject();
                    result.put("personal", personal);
                    result.put("permissionData", permissionData);

                    // 将当前的用户信息放在缓存中 未来会用Redis代替
                    userLoginToken.put("cacheData", result.toString());

                    try {
                        // 先踢掉所有用户
                        List<GenericValue> tokens = delegator.findList("LoginToken", EntityCondition.makeCondition("ticket", EntityOperator.LIKE, "%:" + personal.get("employeeId") + ":" + platform + "%"), null, null, null, false);
                        for (GenericValue t : tokens) {
                            t.remove();
                        }

                        delegator.create("LoginToken", userLoginToken);
                        request.setAttribute("success", true);
                        request.setAttribute("ticket", ticket);
                        request.setAttribute("personal", result.get("personal"));
                        request.setAttribute("permissionData", result.get("permissionData"));
                        return "success";
                    } catch (GenericEntityException e) {
                        throw e;
                    }
                }
            } catch (GenericEntityException e) {
                return authErrorMsg(request, response, "1000", e.getMessage());
            }
        }

        return authErrorMsg(request, response, "1000", "用户名或密码错误!(216)");
    }

    /**
     * 注册帐号
     *
     * @param username
     * @return
     */
    public static String register(String username, String password, int status) throws GenericEntityException {
        Delegator delegator = DelegatorFactory.getDelegator("default");

        EntityFindOptions findOptions = new EntityFindOptions();
        findOptions.setMaxRows(1);
        // 插入登录表
        List uList = UtilMisc.toList(new EntityExpr("username1", EntityOperator.EQUALS, username), new EntityExpr("username2", EntityOperator.EQUALS, username));
        EntityConditionList userConditions = new EntityConditionList(uList, EntityOperator.OR);

        List<GenericValue> userList = delegator.findList("Account", userConditions, null, null, findOptions, false);
        if (userList != null && userList.size() > 0) {
            return "该用户已存在";
        }

        GenericValue account = delegator.makeValue("Account");
        account.put("accountId", delegator.getNextSeqId("Account").toString());
        if (ValiUtil.isEmail(username)) {
            account.put("username2", username);
        } else {
            // 不是手机号就是邮箱
            account.put("username1", username);
        }

        account.put("accountStatus", status);
        account.put("accountType", "A"); // 这里只能建分域帐户
        account.put("currentPassword", Base64.base64Encode(password));
        account.create();

        return null;
    }

    public static String changePassword(HttpServletRequest request, HttpServletResponse response) {
        String username = request.getParameter("username");
        String vcode = request.getParameter("vcode");
        String password = request.getParameter("password");
        String platform = request.getParameter("platform"); // 平台 P:PC Web A:App
        if (platform == null) {
            platform = "A"; // 默认来源是App
        }

        // 0 参数都不能为空
        if (username == null || vcode == null || password == null) {
            return errorMsg(request, "缺少必要的参数");
        }
        if (password == null || password.length() < 6) {
            return errorMsg(request, "密码不能小于6位");
        }

        // 1 手机号验证,如果找不到注册信息返回
        if (ValiUtil.isMobile(username) == false) {
            return errorMsg(request, "手机号格式不正确");
        }
        Delegator delegator = DelegatorFactory.getDelegator("default");

        GenericValue user = null;

        List<GenericValue> list = null;
        try {
            list = delegator.findList("Account", EntityCondition.makeCondition("username1", EntityOperator.EQUALS, username), null, null, null, false);
        } catch (GenericEntityException e) {
            e.printStackTrace();
            return errorMsg(request, e.getMessage());
        }
        if (list == null || list.size() == 0) {
            return errorMsg(request, "未找到用户信息");
        } else {
            // TODO 多公司时需要解决
            user = list.get(0);
        }

        // 2 验证发送的密码，查询当前验证码是否有效
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("mobile", username);
        GenericValue record = null;
        try {
            record = delegator.findOne("SmsRecord", fields, false);
        } catch (GenericEntityException e) {
            e.printStackTrace();
            return errorMsg(request, e.getMessage());
        }

        if (checkSmsTime(request, record, vcode) == null) {
            // 3 设置新的密码
            if (user != null) {
                // 改库，以下换作对象都是换库后的
                delegator = DelegatorFactory.getDelegator("default");
                try {
                    list = delegator.findList("Account", EntityCondition.makeCondition("username1", EntityOperator.EQUALS, username), null, null, null, false);
                } catch (GenericEntityException e) {
                    e.printStackTrace();
                    return errorMsg(request, e.getMessage());
                }
                if (list == null || list.size() == 0) {
                    return errorMsg(request, "未找到用户信息");
                } else {
                    user.set("currentPassword", Base64.base64Encode(password));
                    if (user.getInteger("accountStatus") == 2) {
                        // 只有是2的时候改才会变成可用
                        user.set("accountStatus", 1);
                    }
                    try {
                        user.store();
                    } catch (GenericEntityException e) {
                        e.printStackTrace();
                        return errorMsg(request, e.getMessage());
                    }
                }

                request.setAttribute("success", true);
                return null;
            }
        } else {
            return "error";
        }

        // 返回
        return errorMsg(request, "这个错误很难决定，请联系客服");
    }

    private static String checkSmsTime(HttpServletRequest request, GenericValue record, String vcode) {
        if (record == null) {
            return errorMsg(request, "验证码输入错误");
        } else {
            if (!vcode.equals(record.getString("smsMessage"))) {
                return errorMsg(request, "验证码输入错误");
            }
            Long currTime = new Date().getTime();
            Long lastTime = record.getTimestamp("lastTime").getTime();
            if ((currTime - lastTime) > 300000) {
                return errorMsg(request, "验证码已超时");
            }
        }

        return null;
    }

    /**
     * 注销
     *
     * @param username
     * @return
     */
    public static String removeUser(String username) throws GenericEntityException {
        Delegator delegator = DelegatorFactory.getDelegator("default");
        List<GenericValue> list = delegator.findList("Account", EntityCondition.makeCondition("username1", EntityOperator.EQUALS, username), null, null, null, false);
        if (list != null && list.size() != 0) {
            list.get(0).remove();
        }

        return null;
    }

    private static String authErrorMsg(HttpServletRequest request, HttpServletResponse response, String errorCode, String msg) {
        request.setAttribute("success", false);
        request.setAttribute("errorCode", errorCode);
        request.setAttribute("message", msg);
        return "error";
    }

    private static String errorMsg(HttpServletRequest request, String msg) {
        request.setAttribute("success", false);
        request.setAttribute("message", msg);
        return "error";
    }
}

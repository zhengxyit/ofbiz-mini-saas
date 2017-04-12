package org.ofbiz.sso;

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
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.TicketUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.*;

/**
 * Created by Ted Zheng on 2016/4/8.
 */
public class LoginService {

    public static final String module = LoginService.class.getName();

    public static final String resourceError = "SsoErrorUiLabels";

    public static String basicLogin(HttpServletRequest request, HttpServletResponse response) {
        // 先使用默认的查用户的租户信息
        Delegator deDelegator = DelegatorFactory.getDelegator("default");

        String registrationID = request.getParameter("registrationID"); // 极光推送使用

        String auth = request.getHeader("Authorization");

        // 找到TenantId
        String tenant = TicketUtil.getTenantIdFromRequest(request);
        if (EntityUtil.isMultiTenantEnabled() && tenant == null) {
            return authErrorMsg(request, response, UtilProperties.getMessage(LoginService.resourceError, "NoTenant", UtilHttp.getLocale(request)));
        }

        if (auth != null && auth.length() > 6) {
            auth = auth.substring(6, auth.length());

            String decodedAuth = Base64.base64Decode(auth);

            String[] strs = decodedAuth.split(":");
            String username = strs[0];
            String password = strs[1];
            try {
                Delegator delegator = DelegatorFactory.getDelegator("default");
                if (EntityUtil.isMultiTenantEnabled()) {
                    delegator.setTenantId(tenant);
                }

                // 验证用户名密码
                GenericValue loginUser = checkUserLogin(username, password);
                if (loginUser != null) {
                    Map<String, Object> userLoginToken = new HashMap<String, Object>();
                    // ticket
                    String ticket = null;
                    if (EntityUtil.isMultiTenantEnabled()) {
                        ticket = TicketUtil.makeTicket() + "#" + tenant;
                    } else {
                        ticket = TicketUtil.makeTicket();
                    }

                    userLoginToken.put("ticket", ticket);
                    userLoginToken.put("expirationTime", UtilDateTime.getDayEnd(new Timestamp(Calendar.getInstance().getTimeInMillis()), 7l));
                    userLoginToken.put("unicode", ""); // 唯一标识是生成的

                    // 查询员工信息
                    String mobile = loginUser.get("username1") == null ? "" : loginUser.get("username1").toString();
                    String email = loginUser.get("username2") == null ? "" : loginUser.get("username2").toString();

                    GenericValue userInfo = loginUser.getRelatedOne("UserInfo", false);
                    // 将当前的用户信息放在缓存中 未来会用Redis代替
                    userLoginToken.put("cacheData", userInfo.toString());

                    if (registrationID != null && !"".equals(registrationID)) {
                        // 极光推送注册ID更新
                        loginUser.put("registrationId", registrationID);
                        loginUser.store();
                    }

                    // 查找权限信息并设置
                    List<GenericValue> authList = userInfo.getRelated("UserPermission", null, null, false);
                    String permissionData = "";
                    if (authList != null) {
                        for (int i = 0; i < authList.size(); i++) {
                            if (i != 0) {
                                permissionData += ",";
                            }
                            permissionData += authList.get(i).get("permissionCode").toString();
                        }
                    }
                    userLoginToken.put("permissionData", permissionData);

                    String enterpriseModules = null;
                    if (EntityUtil.isMultiTenantEnabled()) {
                        // 企业支持的模块
                        EntityFindOptions findOptions = new EntityFindOptions();
                        findOptions.setMaxRows(1);
                        List<GenericValue> enterpriseList = delegator.findList("Enterprise", EntityCondition.makeCondition(UtilMisc.toMap("domainName", tenant)), UtilMisc.toSet("configs"), null, findOptions, false);
                        if (enterpriseList == null || enterpriseList.size() != 1) {
                            throw new GenericEntityException("Enterprise[" + tenant + "] is not found");
                        }
                        enterpriseModules = enterpriseList.get(0).get("configs").toString();
                    }

                    try {
                        delegator.create("LoginToken", userLoginToken);
                        request.setAttribute("success", true);
                        request.setAttribute("ticket", ticket);
                        request.setAttribute("personal", userInfo);
                        request.setAttribute("permissionData", permissionData);
                        if (EntityUtil.isMultiTenantEnabled()) {
                            request.setAttribute("enterpriseModules", enterpriseModules);
                        }
                        return "success";
                    } catch (GenericEntityException e) {
                        return authErrorMsg(request, response, e.getMessage());
                    }
                }
            } catch (GenericEntityException e) {
                return authErrorMsg(request, response, e.getMessage());
            }
        }

        return authErrorMsg(request, response, "用户名或密码错误");
    }

    private static GenericValue checkUserLogin(String username, String password) throws GenericEntityException {
        Delegator delegator = DelegatorFactory.getDelegator("default");
        // 两种登录都支持
        List usernameList = UtilMisc.toList(new EntityExpr("username1", EntityOperator.EQUALS, username), new EntityExpr("username2", EntityOperator.EQUALS, username));
        EntityConditionList userConditions = new EntityConditionList(usernameList, EntityOperator.OR);
        List conditionList = UtilMisc.toList(userConditions, new EntityExpr("currentPassword", EntityOperator.EQUALS, Base64.base64Encode(password)));
        EntityConditionList conditions = new EntityConditionList(conditionList, EntityOperator.AND);

        // 只查一条就OK
        EntityFindOptions findOptions = new EntityFindOptions();
        findOptions.setLimit(1);

        List<GenericValue> userList = delegator.findList("LoginUser", conditions, null, null, findOptions, false);
        if (userList.size() > 0) {
            return userList.get(0);
        } else {
            throw new GenericEntityException("未找到用户");
        }
    }

    public static String register(HttpServletRequest request, HttpServletResponse response) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String vcode = request.getParameter("vcode"); // 暂时不用

        // 0 参数验证
        if (username == null || password == null || vcode == null || vcode.length() != 4) {
            return errorMsg(request, "传入参数不完整");
        }
        if (ValiUtil.isMobile(username) == false && ValiUtil.isEmail(username) == false) {
            return errorMsg(request, "用户名格式不正确");
        }
        if (password == null || password.length() < 6) {
            return errorMsg(request, "密码不能小于6位");
        }


        Delegator deDelegator = DelegatorFactory.getDelegator("default");
        // 验证码判断，在统一的库里
        try {
            Map<String, Object> fields = new HashMap<>();
            fields.put("mobile", username);
            GenericValue record = deDelegator.findOne("SmsRecord", fields, false);
            if (checkSmsTime(request, record, vcode) != null) {
                return "error";
            }
        } catch (GenericEntityException e) {
            return errorMsg(request, e.getMessage());
        }


        // 换库
        Delegator delegator = DelegatorFactory.getDelegator("default");

        // 判断有无用户信息
        try {
            List usernameList = UtilMisc.toList(new EntityExpr("mobile", EntityOperator.EQUALS, username), new EntityExpr("email", EntityOperator.EQUALS, username));
            EntityConditionList userConditions = new EntityConditionList(usernameList, EntityOperator.OR);
            List elist = delegator.findList("B2bEmployee", userConditions, null, null, null, false);
            if (elist == null || elist.size() == 0) {
                return errorMsg(request, "您的手机号不在员工信息中，请联系企业用户负责人");
            }
        } catch (GenericEntityException e) {
            return errorMsg(request, e.getMessage());
        }

        try {
            // 插入登录表
            List usernameList = UtilMisc.toList(new EntityExpr("username1", EntityOperator.EQUALS, username), new EntityExpr("username2", EntityOperator.EQUALS, username));
            EntityConditionList userConditions = new EntityConditionList(usernameList, EntityOperator.OR);

            List<GenericValue> uList = delegator.findList("LoginUser", userConditions, null, null, null, false);
            if (uList != null && uList.size() > 0) {
                return errorMsg(request, "该用户已存在");
            }
            GenericValue loginUser = delegator.makeValue("LoginUser");
            loginUser.put("apiLoginUserId", delegator.getNextSeqId("LoginUser").toString());
            if (ValiUtil.isMobile(username)) {
                loginUser.put("username1", username);
            } else {
                // 不是手机号就是邮箱
                loginUser.put("username2", username);
            }

            loginUser.put("currentPassword", Base64.base64Encode(password));
            loginUser.create();
        } catch (GenericEntityException e) {
            return errorMsg(request, e.getMessage());
        }

        request.setAttribute("success", true);
        return null;
    }

    public static String changePassword(HttpServletRequest request, HttpServletResponse response) {
        String username = request.getParameter("username");
        String vcode = request.getParameter("vcode");
        String password = request.getParameter("password");

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
            list = delegator.findList("LoginUser", EntityCondition.makeCondition("username1", EntityOperator.EQUALS, username), null, null, null, false);
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
                    list = delegator.findList("LoginUser", EntityCondition.makeCondition("username1", EntityOperator.EQUALS, username), null, null, null, false);
                } catch (GenericEntityException e) {
                    e.printStackTrace();
                    return errorMsg(request, e.getMessage());
                }
                if (list == null || list.size() == 0) {
                    return errorMsg(request, "未找到用户信息");
                } else {
                    // TODO 多公司时需要解决
                    user = list.get(0);
                    user.set("currentPassword", Base64.base64Encode(password));
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
            if (!vcode.equals(record.getString("message"))) {
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

    private static String authErrorMsg(HttpServletRequest request, HttpServletResponse response, String msg) {
        request.setAttribute("success", false);
        request.setAttribute("message", msg);
        return "error";
    }

    private static String errorMsg(HttpServletRequest request, String msg) {
        request.setAttribute("success", false);
        request.setAttribute("message", msg);
        return "error";
    }
}

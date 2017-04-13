package org.ofbiz.sso;

import com.cloopen.rest.sdk.CCPRestSDK;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.ValiUtil;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 云信通产品
 * Created by Ted Zheng on 2016/4/25.
 */
public class SmsService {

    private final static String accountSid = UtilProperties.getPropertyValue("app", "sms.accountSid", null);
    private final static String accountToken = UtilProperties.getPropertyValue("app", "sms.accountToken", null);
    private final static String APP_ID = UtilProperties.getPropertyValue("app", "sms.appId", null);
    private final static String VACODE_TMID = UtilProperties.getPropertyValue("app", "sms.module.login", null);

    public static String sendValicode(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = DelegatorFactory.getDelegator("default");

        // 安全验证
        String user = null;
        if (request.getParameter("user") == null || request.getParameter("pass") == null) {
            return errorMessage(request, "安全验证未通过");
        } else {
            user = request.getParameter("user");
            String pass = request.getParameter("pass");
            if (!"a".equals(user) || !"123".equals(pass)) {
                return errorMessage(request, "安全验证未通过");
            }
        }
        // 0 参数验证
        String phone = request.getParameter("phone");
        if (!ValiUtil.isMobile(phone)) {
            return errorMessage(request, "手机号码不正确");
        }
        // 1 验证手机号是否被记录在黑名单 或 不在指定时间短内
        GenericValue record = null;
        try {
            Map<String, Object> fields = new HashMap<>();
            fields.put("mobile", phone);
            record = delegator.findOne("SmsRecord", fields, false);
        } catch (GenericEntityException e) {
            e.printStackTrace();
            return errorMessage(request, e.getMessage());

        }
        if (record != null) {
            Long currTime = new Date().getTime();
            Timestamp lockTime = record.getTimestamp("lockTime");
            if (currTime < lockTime.getTime()) {
                return errorMessage(request, "重复发送次数过多，请等待" + (int) ((lockTime.getTime() - currTime) / 1000) + "秒");
            }
        }
        // 1.1 如果是注册的话，判断是否已注册了
        String type = request.getParameter("type");
        if ("1".equals(type)) {
            List<GenericValue> uList = null;
            try {
                uList = delegator.findList("ApiLoginUser", new EntityExpr("username1", EntityOperator.EQUALS, phone), null, null, null, false);
            } catch (GenericEntityException e) {
                e.printStackTrace();
            }
            if (uList != null && uList.size() > 0) {
                return errorMessage(request, "该用户已注册");
            }
        }

        CCPRestSDK restAPI = new CCPRestSDK();
        restAPI.init("sandboxapp.cloopen.com", "8883");
        // 初始化服务器地址和端口，沙盒环境配置成sandboxapp.cloopen.com，生产环境配置成app.cloopen.com，端口都是8883.
        restAPI.setAccount(accountSid, accountToken);
        // 初始化主账号名称和主账号令牌，登陆云通讯网站后，可在"控制台-应用"中看到开发者主账号ACCOUNT SID和
        // 主账号令牌AUTH TOKEN。
        restAPI.setAppId(APP_ID);
        // 初始化应用ID，如果是在沙盒环境开发，请配置"控制台-应用-测试DEMO"中的APPID。

        // 随机的验证码
        String code = "" + (int) ((Math.random() * 9 + 1) * 1000);

        // 如切换到生产环境，请使用自己创建应用的APPID
        Map result = restAPI.sendTemplateSMS(phone, VACODE_TMID, new String[]{
                code, "5"});
        if ("000000".equals(result.get("statusCode"))) {
            // 正常返回输出data包体信息（map）
            // HashMap data = (HashMap) result.get("data");
            // 记录在数据库中
            try {
                if (record != null) {
                    Timestamp lastTime = record.getTimestamp("lockTime");
                    SimpleDateFormat formath = new SimpleDateFormat("yyyy-MM-dd");
                    SimpleDateFormat formats = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    Long today = 0l;
                    try {
                        today = formats.parse(formath.format(new Date()) + " 00:00:00").getTime();
                    } catch (ParseException e) {
                        e.printStackTrace();
                        return errorMessage(request, "Server error");
                    }
                    // 如果是今天第一次，重置
                    if (lastTime.getTime() < today) {
                        record.set("count", 1l);
                        record.set("message", code);
                        record.set("lockTime", new Timestamp(new Date().getTime() + 60000));
                        record.set("lastTime", new Timestamp(new Date().getTime()));
                    } else {
                        // 3次以内60秒，3-9次180秒，10次锁至明天
                        Long count = record.getLong("count");
                        int second = 60000;
                        if (count > 5) {
                            return errorMessage(request, "每一部手机一天最多发送5次验证码，请务非法使用。");
                        }
                        if (count > 3 && count < 5) {
                            second = 180000;
                        }
                        record.set("message", code);
                        record.set("count", count + 1);
                        record.set("lockTime", new Timestamp(new Date().getTime() + second));
                        record.set("lastTime", new Timestamp(new Date().getTime()));
                    }

                    record.store();
                } else {
                    record = delegator.makeValue("SmsRecord");
                    record.set("mobile", phone);
                    record.set("count", 1l);
                    record.set("model", VACODE_TMID);
                    record.set("message", code);
                    record.set("lockTime", new Timestamp(new Date().getTime() + 60000));
                    record.set("lastTime", new Timestamp(new Date().getTime()));
                    record.create();
                }
            } catch (GenericEntityException e) {
                e.printStackTrace();
                return errorMessage(request, e.getMessage());
            }

            request.setAttribute("success", true);
            request.setAttribute("message", "");
            return null;
        } else {
            // 异常返回输出错误码和错误信息
            return errorMessage(request, "错误码=" + result.get("statusCode") + " 错误信息= "
                    + result.get("statusMsg"));
        }
    }

    private static String errorMessage(HttpServletRequest request, String message) {
        request.setAttribute("success", false);
        request.setAttribute("message", message);
        return "error";
    }

}

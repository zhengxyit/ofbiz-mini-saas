package org.ofbiz.service;

import org.ofbiz.base.util.UtilProperties;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.Random;

/**
 * Created by zhengxy on 2017/3/26.
 */
public class TicketUtil {

    public static final String module = TicketUtil.class.getName();

    public static final String TICKET_NAME = "X-TICKET";

    public static final String TENANT_NAME = "X-TENANT";

    private static String base = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * 检查Ticket合法性
     */
    public static boolean checkTicket(String ticket) {
        if (ticket == null) return false;
        if (!ticket.contains("#")) return false;
        if (ticket.split("#").length != 2) return false;
        return true;
    }

    public static String getTicketFormRequest(ServletRequest request) {
        String ticket = ((HttpServletRequest) request).getHeader(TICKET_NAME);
        if (checkTicket(ticket)) {
            return ticket;
        } else {
            return null;
        }
    }

    /**
     * 从Ticket中获取Tenant,Ticket必须是经过验证的
     */
    public static String getTenantFromTicket(String ticket) {
        String[] strs = ticket.split("#");
        return strs[1];
    }

    // 生成Ticket
    public static String makeTicket() {
        // 固定16位
        return randomStringByLength(16);
    }

    // 从请求中拿到tenant
    public static String getTenantIdFromRequest(HttpServletRequest request) {
        String domain = request.getServerName();

        // 方式1：从三级域名中找到tenant
        String tenant = domain.split("\\.")[0];
        String saasDomain = UtilProperties.getPropertyValue("general", "saas.home");
        if (saasDomain.equals(tenant)) {
            // 说明是服务器的域名，需要从Header中获取
            tenant = request.getHeader(TENANT_NAME);
        }

        if (saasDomain.equals(tenant) || tenant == null) {
            return null;
        }

        return tenant;
    }

    public static String randomStringByLength(int length) {
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        // 只生成8位
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }
}

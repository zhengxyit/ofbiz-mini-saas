package org.ofbiz.service;

import java.util.List;
import java.util.Random;

/**
 * Created by zhengxy on 2017/3/26.
 */
public class TicketUtil {

    public static final String module = TicketUtil.class.getName();

    public static final String TICKET_NAME = "X-TICKET";

    private static String base = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * 检查Ticket合法性
     */
    public static boolean checkTicket(String ticket) {
        if (ticket == null) return false;
        if (!ticket.contains(":")) return false;
        if (ticket.split(":").length < 3) return false;
        return true;
    }

    public static String getTenantFromTicket(String ticket) {
        String[] strs = ticket.split(":");
        if (strs.length == 4) {
            return strs[3];
        }
        return null;
    }

    public static String getAccountTypeFromTicket(String ticket) {
        String[] strs = ticket.split(":");
        return strs[2];
    }

    // 生成Ticket
    public static String makeTicket() {
        // 固定16位
        return randomStringByLength(16);
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

    public static String[] asArrayByString(String tenants) {
        if (tenants == null || tenants.length() < 3) {
            return new String[0];
        }
        return tenants.substring(1, tenants.length() - 1).split(",");
    }

    public static String arrayToString(String[] tenants) {
        String str = ",";
        for (int i = 0; i < tenants.length; i++) {
            str += tenants[i] + ",";
        }
        return str;
    }

    public static String arrayToString(List<String> tenants) {
        return arrayToString((String[]) tenants.toArray());
    }
}

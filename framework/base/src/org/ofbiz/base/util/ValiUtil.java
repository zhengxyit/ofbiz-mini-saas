package org.ofbiz.base.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Ted Zhengf on 2016/4/8.
 */
public class ValiUtil {

    public static boolean isEmail(String sEmail) {
        String pattern = "^([a-z0-9A-Z]+[-|\\.|_]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        return sEmail.matches(pattern);
    }

    public static boolean isMobile(String str) {
        String pattern = "^1[3,4,5,7,8][0-9]{9}$";
        return str.matches(pattern);
    }

    public static boolean isIpAddress(String host) {
        InetAddress address = null;
        boolean v4 = false;
        boolean v6 = false;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        }
        if (address instanceof Inet6Address) {
            v6 = true;
        }
        if (address instanceof Inet4Address) {
            v4 = true;
        }
        if (v4 || v6) {
            return true;
        }

        return false;
    }
}

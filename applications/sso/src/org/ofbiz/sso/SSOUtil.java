package org.ofbiz.sso;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ted Zheng on 2016/4/8.
 */
public class SSOUtil {

    public static Map<String, Object> returnSUCCESS(HashMap<String,Object> map) {
        map.put("success", true);
        map.put("message", "");
        return map;
    }

    public static Map<String, Object> returnSUCCESS() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("success", true);
        map.put("message", "");
        return map;
    }

    public static Map<String, Object> returnFAIL(String message) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("success", false);
        map.put("message", message);
        return map;
    }

    public static Map<String, Object> returnFAIL() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("success", false);
        map.put("message", "");
        return map;
    }
}

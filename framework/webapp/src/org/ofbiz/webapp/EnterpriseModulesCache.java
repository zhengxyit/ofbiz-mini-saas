package org.ofbiz.webapp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2017/4/1.
 */
public class EnterpriseModulesCache {
    // 暂时用一个Map缓存
    private static Map<String, String> cacheMap = new ConcurrentHashMap<String, String>();

    public static String getModulesByTenant(String tenant) {
        return cacheMap.get(tenant);
    }

    public static void putModules(String tenant, String supportModules) {
        cacheMap.put(tenant, supportModules);
    }
}

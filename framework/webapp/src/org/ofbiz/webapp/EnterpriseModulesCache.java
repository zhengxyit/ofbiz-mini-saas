package org.ofbiz.webapp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2017/4/1.
 */
public class EnterpriseModulesCache {
    // 暂时用一个Map缓存
    private static Map<String, String> flatFieldsCacheMap = new ConcurrentHashMap<String, String>();

    private static Map<String, String> treeFieldsCacheMap = new ConcurrentHashMap<String, String>();

    public static String getFlatFieldsByTenant(String tenant) {
        return flatFieldsCacheMap.get(tenant);
    }

    public static String getTreeFieldsByTenant(String tenant) {
        return treeFieldsCacheMap.get(tenant);
    }

    public static void setFlatFieldsByTenant(String tenant, String fields) {
        flatFieldsCacheMap.put(tenant, fields);
    }

    public static void setTreeFieldsByTenant(String tenant, String fields) {
        treeFieldsCacheMap.put(tenant, fields);
    }
}

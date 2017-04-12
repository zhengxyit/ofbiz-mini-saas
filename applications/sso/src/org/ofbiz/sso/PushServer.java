package org.ofbiz.sso;

import cn.jpush.api.JPushClient;
import cn.jpush.api.push.PushResult;
import cn.jpush.api.push.model.Platform;
import cn.jpush.api.push.model.PushPayload;
import cn.jpush.api.push.model.audience.Audience;
import cn.jpush.api.push.model.notification.Notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PushServer {
    private static String appKey = "xxxxxxxxxxx";
    private static String masterSecret = "xxxxxxxxx";
    private static JPushClient client = null;

    /**
     * @param msgContent      消息内容
     * @param registrationIDs 注册ID组
     * @see <b> 发送消息到指定别名 </b>
     */
    public static boolean sendNotifyMessage(String msgContent, String orderCode, List<String> registrationIDs) {
        JPushClient jpushClient = new JPushClient(masterSecret, appKey);

        // 传值
        Map<String, String> map = new HashMap<String, String>();
        map.put("orderCode", orderCode);
        PushPayload payload = PushPayload.newBuilder()
                .setPlatform(Platform.android())
                .setAudience(Audience.registrationId(registrationIDs))
                .setNotification(Notification.android(msgContent, "云扳手", map))
                .build();

        try {
            PushResult result = jpushClient.sendPush(payload);
            return result.isResultOK();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return false;
    }

}

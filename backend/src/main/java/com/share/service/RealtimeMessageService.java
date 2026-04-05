package com.share.service;

import java.util.Collection;
import java.util.Map;

public interface RealtimeMessageService {

    void pushInteractionNotification(Long receiverId, Map<String, Object> payload);

    void pushChatNotification(Long receiverId, Map<String, Object> payload);

    /**
     * 推送公告类实时事件到指定在线用户集合。
     * 说明：公告是“系统广播”场景，不依赖单个接收者业务实体，可按在线用户批量推送。
     */
    void pushAnnouncementEvent(Collection<Long> receiverIds, Map<String, Object> payload);
}

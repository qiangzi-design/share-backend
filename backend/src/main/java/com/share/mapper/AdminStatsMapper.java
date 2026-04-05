package com.share.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
/**
 * Mapper职责：管理仪表盘统计数据访问，提供总览计数聚合。
 */
public interface AdminStatsMapper {

    @Select("SELECT COUNT(*) FROM users")
    Long countUsers();

    @Select("SELECT COUNT(*) FROM users WHERE status NOT IN (0, 3)")
    Long countActiveUsers();

    @Select("SELECT COUNT(*) FROM contents WHERE status = 1 AND review_status = 'approved'")
    Long countApprovedContents();

    @Select("SELECT COUNT(*) FROM comments WHERE status = 1 AND review_status = 'approved'")
    Long countApprovedComments();

    @Select("SELECT COUNT(*) FROM chat_messages")
    Long countChatMessages();

    @Select("SELECT COUNT(*) FROM reports WHERE status IN ('pending', 'assigned')")
    Long countPendingReports();

    @Select("SELECT COUNT(*) FROM reports")
    Long countReports();

    @Select("SELECT create_time FROM users WHERE create_time >= #{fromTime}")
    List<LocalDateTime> listUserCreateTimes(LocalDateTime fromTime);

    @Select("SELECT create_time FROM contents WHERE create_time >= #{fromTime} AND status = 1 AND review_status = 'approved'")
    List<LocalDateTime> listContentCreateTimes(LocalDateTime fromTime);

    @Select("SELECT create_time FROM comments WHERE create_time >= #{fromTime} AND status = 1 AND review_status = 'approved'")
    List<LocalDateTime> listCommentCreateTimes(LocalDateTime fromTime);

    @Select("SELECT create_time FROM reports WHERE create_time >= #{fromTime}")
    List<LocalDateTime> listReportCreateTimes(LocalDateTime fromTime);
}

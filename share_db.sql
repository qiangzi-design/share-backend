/*
 Navicat Premium Dump SQL

 Source Server         : qiangzi
 Source Server Type    : MySQL
 Source Server Version : 80037 (8.0.37)
 Source Host           : localhost:3306
 Source Schema         : share_db

 Target Server Type    : MySQL
 Target Server Version : 80037 (8.0.37)
 File Encoding         : 65001

 Date: 02/04/2026 18:52:51
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin_audit_logs
-- ----------------------------
DROP TABLE IF EXISTS `admin_audit_logs`;
CREATE TABLE `admin_audit_logs`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '审计日志ID',
  `operator_user_id` bigint NOT NULL COMMENT '操作者用户ID',
  `action` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作编码',
  `target_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标类型',
  `target_id` bigint NULL DEFAULT NULL COMMENT '目标ID',
  `detail_before` json NULL COMMENT '变更前快照',
  `detail_after` json NULL COMMENT '变更后快照',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '客户端IP',
  `user_agent` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '客户端UA',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_admin_audit_operator_time`(`operator_user_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_admin_audit_target`(`target_type` ASC, `target_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 28 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理操作审计日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of admin_audit_logs
-- ----------------------------
INSERT INTO `admin_audit_logs` VALUES (1, 6, 'admin.user.mute', 'user', 5, '{\"id\": 5, \"status\": 1, \"banTime\": null, \"banReason\": null, \"muteUntil\": null, \"updateTime\": \"2026-03-31T11:34:25\"}', '{\"id\": 5, \"status\": 1, \"banTime\": null, \"muteMeta\": {\"reason\": \"看他不爽\", \"minutes\": 60}, \"banReason\": \"看他不爽\", \"muteUntil\": \"2026-03-31T13:21:50.8714719\", \"updateTime\": \"2026-03-31T12:21:50.8714719\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 12:21:51');
INSERT INTO `admin_audit_logs` VALUES (2, 6, 'admin.user.ban', 'user', 5, '{\"id\": 5, \"status\": 1, \"banTime\": null, \"banReason\": \"看他不爽\", \"muteUntil\": \"2026-03-31T13:21:51\", \"updateTime\": \"2026-03-31T12:21:51\"}', '{\"id\": 5, \"status\": 0, \"banTime\": \"2026-03-31T12:22:29.4612204\", \"banReason\": null, \"muteUntil\": \"2026-03-31T13:21:51\", \"updateTime\": \"2026-03-31T12:22:29.4612204\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 12:22:29');
INSERT INTO `admin_audit_logs` VALUES (3, 6, 'admin.user.unban', 'user', 5, '{\"id\": 5, \"status\": 0, \"banTime\": \"2026-03-31T12:22:29\", \"banReason\": \"看他不爽\", \"muteUntil\": \"2026-03-31T13:21:51\", \"updateTime\": \"2026-03-31T12:22:29\"}', '{\"id\": 5, \"status\": 1, \"banTime\": null, \"banReason\": null, \"muteUntil\": \"2026-03-31T13:21:51\", \"updateTime\": \"2026-03-31T12:28:50.4774747\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 12:28:51');
INSERT INTO `admin_audit_logs` VALUES (4, 6, 'admin.user.ban', 'user', 5, '{\"id\": 5, \"status\": 1, \"banTime\": \"2026-03-31T12:22:29\", \"banReason\": \"看他不爽\", \"muteUntil\": \"2026-03-31T13:21:51\", \"updateTime\": \"2026-03-31T12:28:50\"}', '{\"id\": 5, \"status\": 0, \"banTime\": \"2026-03-31T12:29:16.3230368\", \"banReason\": \"看你不爽\", \"muteUntil\": \"2026-03-31T13:21:51\", \"updateTime\": \"2026-03-31T12:29:16.3230368\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 12:29:16');
INSERT INTO `admin_audit_logs` VALUES (5, 6, 'admin.user.unban', 'user', 5, '{\"id\": 5, \"status\": 0, \"banTime\": \"2026-03-31T12:29:16\", \"banReason\": \"看你不爽\", \"muteUntil\": \"2026-03-31T13:21:51\", \"updateTime\": \"2026-03-31T12:29:16\"}', '{\"id\": 5, \"status\": 1, \"banTime\": null, \"banReason\": null, \"muteUntil\": \"2026-03-31T13:21:51\", \"updateTime\": \"2026-03-31T12:30:30.1084428\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 12:30:30');
INSERT INTO `admin_audit_logs` VALUES (6, 6, 'admin.user.mute', 'user', 5, '{\"id\": 5, \"status\": 1, \"banTime\": \"2026-03-31T12:29:16\", \"banReason\": \"看你不爽\", \"muteUntil\": \"2026-03-31T13:21:51\", \"updateTime\": \"2026-03-31T12:30:30\"}', '{\"id\": 5, \"status\": 2, \"banTime\": \"2026-03-31T12:29:16\", \"muteMeta\": {\"reason\": \"看他不爽\", \"minutes\": 60}, \"banReason\": \"看他不爽\", \"muteUntil\": \"2026-03-31T17:39:44.2061723\", \"updateTime\": \"2026-03-31T16:39:44.2061723\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 16:39:44');
INSERT INTO `admin_audit_logs` VALUES (7, 6, 'admin.announcement.create', 'announcement', 1, NULL, '{\"id\": 1, \"body\": \"新年快乐\", \"title\": \"新年\", \"status\": \"draft\", \"endTime\": \"2026-03-31T00:00:00\", \"isPinned\": false, \"creatorId\": 6, \"startTime\": \"2026-03-01T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T19:34:00.2475727\", \"updateTime\": \"2026-03-31T19:34:00.2475727\", \"publishTime\": null}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 19:34:00');
INSERT INTO `admin_audit_logs` VALUES (8, 6, 'admin.announcement.publish', 'announcement', 1, '{\"id\": 1, \"body\": \"新年快乐\", \"title\": \"新年\", \"status\": \"draft\", \"endTime\": \"2026-03-31T00:00:00\", \"isPinned\": false, \"creatorId\": 6, \"startTime\": \"2026-03-01T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T19:34:00\", \"updateTime\": \"2026-03-31T19:34:00\", \"publishTime\": null}', '{\"id\": 1, \"body\": \"新年快乐\", \"title\": \"新年\", \"status\": \"published\", \"endTime\": \"2026-03-31T00:00:00\", \"isPinned\": false, \"creatorId\": 6, \"startTime\": \"2026-03-01T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T19:34:00\", \"updateTime\": \"2026-03-31T19:34:14.7582126\", \"publishTime\": \"2026-03-31T19:34:14.7582126\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 19:34:15');
INSERT INTO `admin_audit_logs` VALUES (9, 6, 'admin.announcement.update', 'announcement', 1, '{\"id\": 1, \"body\": \"新年快乐\", \"title\": \"新年\", \"status\": \"published\", \"endTime\": \"2026-03-31T00:00:00\", \"isPinned\": false, \"creatorId\": 6, \"startTime\": \"2026-03-01T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T19:34:00\", \"updateTime\": \"2026-03-31T19:34:15\", \"publishTime\": \"2026-03-31T19:34:15\"}', '{\"id\": 1, \"body\": \"新年快乐\", \"title\": \"新年\", \"status\": \"published\", \"endTime\": \"2026-04-09T00:00:00\", \"isPinned\": false, \"creatorId\": 6, \"startTime\": \"2026-03-01T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T19:34:00\", \"updateTime\": \"2026-03-31T19:37:06.7795967\", \"publishTime\": \"2026-03-31T19:34:15\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 19:37:07');
INSERT INTO `admin_audit_logs` VALUES (10, 6, 'admin.announcement.update', 'announcement', 1, '{\"id\": 1, \"body\": \"新年快乐\", \"title\": \"新年\", \"status\": \"published\", \"endTime\": \"2026-04-09T00:00:00\", \"isPinned\": false, \"creatorId\": 6, \"startTime\": \"2026-03-01T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T19:34:00\", \"updateTime\": \"2026-03-31T19:37:07\", \"publishTime\": \"2026-03-31T19:34:15\"}', '{\"id\": 1, \"body\": \"新年快乐\", \"title\": \"新年\", \"status\": \"published\", \"endTime\": \"2026-04-09T00:00:00\", \"isPinned\": false, \"creatorId\": 6, \"startTime\": \"2026-03-01T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T19:34:00\", \"updateTime\": \"2026-03-31T19:37:06.8831716\", \"publishTime\": \"2026-03-31T19:34:15\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 19:37:07');
INSERT INTO `admin_audit_logs` VALUES (11, 6, 'admin.announcement.create', 'announcement', 2, NULL, '{\"id\": 2, \"body\": \"端午快乐\", \"title\": \"端午\", \"status\": \"draft\", \"endTime\": \"2026-04-17T23:59:59\", \"isPinned\": true, \"creatorId\": 6, \"startTime\": \"2026-03-02T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T19:38:07.8861414\", \"updateTime\": \"2026-03-31T19:38:07.8861414\", \"publishTime\": null}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 19:38:08');
INSERT INTO `admin_audit_logs` VALUES (12, 6, 'admin.announcement.publish', 'announcement', 2, '{\"id\": 2, \"body\": \"端午快乐\", \"title\": \"端午\", \"status\": \"draft\", \"endTime\": \"2026-04-17T23:59:59\", \"isPinned\": true, \"creatorId\": 6, \"startTime\": \"2026-03-02T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T19:38:08\", \"updateTime\": \"2026-03-31T19:38:08\", \"publishTime\": null}', '{\"id\": 2, \"body\": \"端午快乐\", \"title\": \"端午\", \"status\": \"published\", \"endTime\": \"2026-04-17T23:59:59\", \"isPinned\": true, \"creatorId\": 6, \"startTime\": \"2026-03-02T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T19:38:08\", \"updateTime\": \"2026-03-31T19:38:09.6350695\", \"publishTime\": \"2026-03-31T19:38:09.6350695\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 19:38:10');
INSERT INTO `admin_audit_logs` VALUES (13, 6, 'admin.announcement.offline', 'announcement', 2, '{\"id\": 2, \"body\": \"端午快乐\", \"title\": \"端午\", \"status\": \"published\", \"endTime\": \"2026-04-17T23:59:59\", \"isPinned\": true, \"creatorId\": 6, \"startTime\": \"2026-03-02T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T19:38:08\", \"updateTime\": \"2026-03-31T19:38:10\", \"publishTime\": \"2026-03-31T19:38:10\"}', '{\"id\": 2, \"body\": \"端午快乐\", \"title\": \"端午\", \"status\": \"offline\", \"endTime\": \"2026-04-17T23:59:59\", \"isPinned\": true, \"creatorId\": 6, \"startTime\": \"2026-03-02T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T19:38:08\", \"updateTime\": \"2026-03-31T19:38:20.7775657\", \"publishTime\": \"2026-03-31T19:38:10\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 19:38:21');
INSERT INTO `admin_audit_logs` VALUES (14, 6, 'admin.announcement.publish', 'announcement', 2, '{\"id\": 2, \"body\": \"端午快乐\", \"title\": \"端午\", \"status\": \"offline\", \"endTime\": \"2026-04-17T23:59:59\", \"isPinned\": true, \"creatorId\": 6, \"startTime\": \"2026-03-02T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T19:38:08\", \"updateTime\": \"2026-03-31T19:38:21\", \"publishTime\": \"2026-03-31T19:38:10\"}', '{\"id\": 2, \"body\": \"端午快乐\", \"title\": \"端午\", \"status\": \"published\", \"endTime\": \"2026-04-17T23:59:59\", \"isPinned\": true, \"creatorId\": 6, \"startTime\": \"2026-03-02T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T19:38:08\", \"updateTime\": \"2026-03-31T19:38:28.1301233\", \"publishTime\": \"2026-03-31T19:38:10\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 19:38:28');
INSERT INTO `admin_audit_logs` VALUES (15, 6, 'admin.comment.hide', 'comment', 8, '{\"id\": 8, \"status\": 1, \"reviewTime\": null, \"reviewerId\": null, \"reviewReason\": null, \"reviewStatus\": \"approved\"}', '{\"id\": 8, \"status\": 1, \"reviewTime\": \"2026-03-31T19:53:07.6454508\", \"reviewerId\": 6, \"reviewReason\": \"不合法\", \"reviewStatus\": \"rejected\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 19:53:08');
INSERT INTO `admin_audit_logs` VALUES (16, 6, 'admin.comment.restore', 'comment', 8, '{\"id\": 8, \"status\": 1, \"reviewTime\": \"2026-03-31T19:53:08\", \"reviewerId\": 6, \"reviewReason\": \"不合法\", \"reviewStatus\": \"rejected\"}', '{\"id\": 8, \"status\": 1, \"reviewTime\": \"2026-03-31T20:02:29.0772769\", \"reviewerId\": 6, \"reviewReason\": null, \"reviewStatus\": \"approved\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 20:02:29');
INSERT INTO `admin_audit_logs` VALUES (17, 6, 'admin.announcement.create', 'announcement', 3, NULL, '{\"id\": 3, \"body\": \"中秋快乐\", \"title\": \"中秋\", \"status\": \"draft\", \"endTime\": \"2026-08-15T23:59:59\", \"isPinned\": false, \"creatorId\": 6, \"startTime\": \"2026-03-02T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T22:46:48.717087\", \"updateTime\": \"2026-03-31T22:46:48.717087\", \"publishTime\": null}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 22:46:49');
INSERT INTO `admin_audit_logs` VALUES (18, 6, 'admin.announcement.publish', 'announcement', 3, '{\"id\": 3, \"body\": \"中秋快乐\", \"title\": \"中秋\", \"status\": \"draft\", \"endTime\": \"2026-08-15T23:59:59\", \"isPinned\": false, \"creatorId\": 6, \"startTime\": \"2026-03-02T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T22:46:49\", \"updateTime\": \"2026-03-31T22:46:49\", \"publishTime\": null}', '{\"id\": 3, \"body\": \"中秋快乐\", \"title\": \"中秋\", \"status\": \"published\", \"endTime\": \"2026-08-15T23:59:59\", \"isPinned\": false, \"creatorId\": 6, \"startTime\": \"2026-03-02T00:00:00\", \"updaterId\": 6, \"createTime\": \"2026-03-31T22:46:49\", \"updateTime\": \"2026-03-31T22:46:50.8483544\", \"publishTime\": \"2026-03-31T22:46:50.8483544\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 22:46:51');
INSERT INTO `admin_audit_logs` VALUES (19, 6, 'admin.report.assign', 'report', 1, '{\"id\": 1, \"status\": \"pending\", \"assigneeId\": null, \"handleNote\": null, \"handleTime\": null, \"updateTime\": \"2026-04-01T11:34:39\", \"resolveAction\": null}', '{\"id\": 1, \"status\": \"assigned\", \"assigneeId\": 6, \"handleNote\": \"接单处理中\", \"handleTime\": \"2026-04-01T11:36:15.9414369\", \"updateTime\": \"2026-04-01T11:36:15.9414369\", \"resolveAction\": null}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 11:36:16');
INSERT INTO `admin_audit_logs` VALUES (20, 6, 'admin.report.resolve', 'report', 1, '{\"id\": 1, \"status\": \"assigned\", \"assigneeId\": 6, \"handleNote\": \"接单处理中\", \"handleTime\": \"2026-04-01T11:36:16\", \"updateTime\": \"2026-04-01T11:36:16\", \"resolveAction\": null}', '{\"id\": 1, \"status\": \"resolved\", \"assigneeId\": 6, \"handleNote\": \"已处理\", \"handleTime\": \"2026-04-01T11:36:36.1823059\", \"updateTime\": \"2026-04-01T11:36:36.1823059\", \"resolveAction\": \"resolved\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 11:36:36');
INSERT INTO `admin_audit_logs` VALUES (21, 6, 'admin.content.restore', 'content', 6, '{\"id\": 6, \"status\": 1, \"reviewTime\": null, \"reviewerId\": null, \"reviewReason\": null, \"reviewStatus\": \"pending\"}', '{\"id\": 6, \"status\": 1, \"reviewTime\": \"2026-04-01T16:30:02.8811962\", \"reviewerId\": 6, \"reviewReason\": null, \"reviewStatus\": \"approved\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 16:30:03');
INSERT INTO `admin_audit_logs` VALUES (22, 6, 'admin.content.restore', 'content', 7, '{\"id\": 7, \"status\": 1, \"reviewTime\": null, \"reviewerId\": null, \"reviewReason\": null, \"reviewStatus\": \"pending\"}', '{\"id\": 7, \"status\": 1, \"reviewTime\": \"2026-04-01T22:22:04.8433554\", \"reviewerId\": 6, \"reviewReason\": null, \"reviewStatus\": \"approved\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 22:22:05');
INSERT INTO `admin_audit_logs` VALUES (23, 6, 'admin.content.restore', 'content', 7, '{\"id\": 7, \"status\": 1, \"reviewTime\": \"2026-04-01T22:22:05\", \"reviewerId\": 6, \"reviewReason\": null, \"reviewStatus\": \"pending\"}', '{\"id\": 7, \"status\": 1, \"reviewTime\": \"2026-04-01T22:23:39.6811954\", \"reviewerId\": 6, \"reviewReason\": null, \"reviewStatus\": \"approved\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 22:23:40');
INSERT INTO `admin_audit_logs` VALUES (24, 6, 'admin.content.restore', 'content', 7, '{\"id\": 7, \"status\": 1, \"reviewTime\": \"2026-04-01T22:23:40\", \"reviewerId\": 6, \"reviewReason\": null, \"reviewStatus\": \"pending\"}', '{\"id\": 7, \"status\": 1, \"reviewTime\": \"2026-04-01T22:41:32.9082521\", \"reviewerId\": 6, \"reviewReason\": null, \"reviewStatus\": \"approved\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 22:41:33');
INSERT INTO `admin_audit_logs` VALUES (25, 6, 'admin.content.off_shelf', 'content', 7, '{\"id\": 7, \"status\": 1, \"reviewTime\": \"2026-04-01T22:41:33\", \"reviewerId\": 6, \"reviewReason\": null, \"reviewStatus\": \"approved\"}', '{\"id\": 7, \"status\": 1, \"reviewTime\": \"2026-04-01T23:46:57.65336\", \"reviewerId\": 6, \"reviewReason\": \"血腥暴力\", \"reviewStatus\": \"rejected\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 23:46:58');
INSERT INTO `admin_audit_logs` VALUES (26, 6, 'admin.report.handle', 'report', 2, '{\"id\": 2, \"status\": \"pending\", \"assigneeId\": null, \"handleNote\": null, \"handleTime\": null, \"updateTime\": \"2026-04-01T23:45:16\", \"resolveAction\": null}', '{\"id\": 2, \"status\": \"resolved\", \"decision\": \"valid\", \"assigneeId\": 6, \"handleNote\": \"涉嫌违规\", \"handleTime\": \"2026-04-01T23:46:57.6617363\", \"updateTime\": \"2026-04-01T23:46:57.6617363\", \"resolveAction\": \"off_shelf_content\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 23:46:58');
INSERT INTO `admin_audit_logs` VALUES (27, 6, 'admin.content.restore', 'content', 7, '{\"id\": 7, \"status\": 1, \"reviewTime\": \"2026-04-01T23:46:58\", \"reviewerId\": 6, \"reviewReason\": \"血腥暴力\", \"reviewStatus\": \"rejected\"}', '{\"id\": 7, \"status\": 1, \"reviewTime\": \"2026-04-01T23:47:44.9657011\", \"reviewerId\": 6, \"reviewReason\": null, \"reviewStatus\": \"approved\"}', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 23:47:45');

-- ----------------------------
-- Table structure for admin_permissions
-- ----------------------------
DROP TABLE IF EXISTS `admin_permissions`;
CREATE TABLE `admin_permissions`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限编码',
  `name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '权限名称',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_admin_permissions_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of admin_permissions
-- ----------------------------
INSERT INTO `admin_permissions` VALUES (1, 'admin.dashboard.read', 'Dashboard Read', '2026-03-31 12:07:26', '2026-03-31 12:07:26');
INSERT INTO `admin_permissions` VALUES (2, 'admin.user.read', 'User Read', '2026-03-31 12:07:26', '2026-03-31 12:07:26');
INSERT INTO `admin_permissions` VALUES (3, 'admin.user.ban', 'User Ban', '2026-03-31 12:07:26', '2026-03-31 12:07:26');
INSERT INTO `admin_permissions` VALUES (4, 'admin.user.mute', 'User Mute', '2026-03-31 12:07:26', '2026-03-31 12:07:26');
INSERT INTO `admin_permissions` VALUES (5, 'admin.content.read', 'Content Read', '2026-03-31 12:07:26', '2026-03-31 12:07:26');
INSERT INTO `admin_permissions` VALUES (6, 'admin.content.off_shelf', 'Content Off Shelf', '2026-03-31 12:07:26', '2026-03-31 12:07:26');
INSERT INTO `admin_permissions` VALUES (7, 'admin.content.restore', 'Content Restore', '2026-03-31 12:07:26', '2026-03-31 12:07:26');
INSERT INTO `admin_permissions` VALUES (8, 'admin.comment.read', 'Comment Read', '2026-03-31 12:07:26', '2026-03-31 12:07:26');
INSERT INTO `admin_permissions` VALUES (9, 'admin.comment.hide', 'Comment Hide', '2026-03-31 12:07:26', '2026-03-31 12:07:26');
INSERT INTO `admin_permissions` VALUES (10, 'admin.report.read', 'Report Read', '2026-03-31 12:07:26', '2026-03-31 12:07:26');
INSERT INTO `admin_permissions` VALUES (11, 'admin.report.handle', 'Report Handle', '2026-03-31 12:07:26', '2026-03-31 12:07:26');
INSERT INTO `admin_permissions` VALUES (12, 'admin.audit.read', 'Audit Read', '2026-03-31 12:07:26', '2026-03-31 12:07:26');
INSERT INTO `admin_permissions` VALUES (13, 'admin.category.read', 'Admin category read', '2026-03-31 19:22:43', '2026-03-31 19:25:07');
INSERT INTO `admin_permissions` VALUES (14, 'admin.category.write', 'Admin category write', '2026-03-31 19:22:43', '2026-03-31 19:25:07');
INSERT INTO `admin_permissions` VALUES (15, 'admin.tag.read', 'Admin tag read', '2026-03-31 19:22:43', '2026-03-31 19:25:07');
INSERT INTO `admin_permissions` VALUES (16, 'admin.tag.write', 'Admin tag write', '2026-03-31 19:22:43', '2026-03-31 19:25:07');
INSERT INTO `admin_permissions` VALUES (17, 'admin.announcement.read', 'Admin announcement read', '2026-03-31 19:22:43', '2026-03-31 19:25:07');
INSERT INTO `admin_permissions` VALUES (18, 'admin.announcement.write', 'Admin announcement write', '2026-03-31 19:22:43', '2026-03-31 19:25:07');
INSERT INTO `admin_permissions` VALUES (19, 'admin.template.read', 'Admin template read', '2026-03-31 19:22:43', '2026-03-31 19:25:07');
INSERT INTO `admin_permissions` VALUES (20, 'admin.template.write', 'Admin template write', '2026-03-31 19:22:43', '2026-03-31 19:25:07');
INSERT INTO `admin_permissions` VALUES (21, 'admin.analytics.read', 'Admin analytics read', '2026-03-31 19:22:43', '2026-03-31 19:25:07');
INSERT INTO `admin_permissions` VALUES (22, 'admin.user.risk_mark', 'Admin user risk mark', '2026-03-31 19:22:43', '2026-03-31 19:25:07');

-- ----------------------------
-- Table structure for admin_role_permissions
-- ----------------------------
DROP TABLE IF EXISTS `admin_role_permissions`;
CREATE TABLE `admin_role_permissions`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色权限关联ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_admin_role_permission`(`role_id` ASC, `permission_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 55 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色权限关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of admin_role_permissions
-- ----------------------------
INSERT INTO `admin_role_permissions` VALUES (1, 1, 12, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (2, 1, 9, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (3, 1, 8, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (4, 1, 6, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (5, 1, 5, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (6, 1, 7, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (7, 1, 1, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (8, 1, 11, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (9, 1, 10, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (10, 1, 3, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (11, 1, 4, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (12, 1, 2, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (16, 2, 9, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (17, 2, 8, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (18, 2, 6, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (19, 2, 5, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (20, 2, 7, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (21, 2, 1, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (22, 2, 11, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (23, 2, 10, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (31, 3, 1, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (32, 3, 11, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (33, 3, 10, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (34, 3, 3, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (35, 3, 4, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (36, 3, 2, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (38, 4, 12, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (39, 4, 8, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (40, 4, 5, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (41, 4, 1, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (42, 4, 10, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (43, 4, 2, '2026-03-31 12:07:26');
INSERT INTO `admin_role_permissions` VALUES (45, 1, 21, '2026-03-31 19:25:07');
INSERT INTO `admin_role_permissions` VALUES (46, 1, 17, '2026-03-31 19:25:07');
INSERT INTO `admin_role_permissions` VALUES (47, 1, 18, '2026-03-31 19:25:07');
INSERT INTO `admin_role_permissions` VALUES (48, 1, 13, '2026-03-31 19:25:07');
INSERT INTO `admin_role_permissions` VALUES (49, 1, 14, '2026-03-31 19:25:07');
INSERT INTO `admin_role_permissions` VALUES (50, 1, 15, '2026-03-31 19:25:07');
INSERT INTO `admin_role_permissions` VALUES (51, 1, 16, '2026-03-31 19:25:07');
INSERT INTO `admin_role_permissions` VALUES (52, 1, 19, '2026-03-31 19:25:07');
INSERT INTO `admin_role_permissions` VALUES (53, 1, 20, '2026-03-31 19:25:07');
INSERT INTO `admin_role_permissions` VALUES (54, 1, 22, '2026-03-31 19:25:07');

-- ----------------------------
-- Table structure for admin_roles
-- ----------------------------
DROP TABLE IF EXISTS `admin_roles`;
CREATE TABLE `admin_roles`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色编码',
  `name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_admin_roles_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of admin_roles
-- ----------------------------
INSERT INTO `admin_roles` VALUES (1, 'SUPER_ADMIN', 'Super Admin', '2026-03-31 12:07:26', '2026-03-31 12:07:26');
INSERT INTO `admin_roles` VALUES (2, 'CONTENT_MODERATOR', 'Content Moderator', '2026-03-31 12:07:26', '2026-03-31 12:07:26');
INSERT INTO `admin_roles` VALUES (3, 'USER_OPS', 'User Operations', '2026-03-31 12:07:26', '2026-03-31 12:07:26');
INSERT INTO `admin_roles` VALUES (4, 'AUDITOR_READONLY', 'Auditor Readonly', '2026-03-31 12:07:26', '2026-03-31 12:07:26');

-- ----------------------------
-- Table structure for admin_user_roles
-- ----------------------------
DROP TABLE IF EXISTS `admin_user_roles`;
CREATE TABLE `admin_user_roles`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户角色关联ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_admin_user_role`(`user_id` ASC, `role_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of admin_user_roles
-- ----------------------------
INSERT INTO `admin_user_roles` VALUES (1, 6, 1, '2026-03-31 12:14:06');

-- ----------------------------
-- Table structure for announcement_reads
-- ----------------------------
DROP TABLE IF EXISTS `announcement_reads`;
CREATE TABLE `announcement_reads`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '已读记录ID',
  `announcement_id` bigint NOT NULL COMMENT '公告ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `read_time` datetime NOT NULL COMMENT '已读时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_ann_reads_ann_user`(`announcement_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_ann_reads_user_time`(`user_id` ASC, `read_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '公告已读记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of announcement_reads
-- ----------------------------
INSERT INTO `announcement_reads` VALUES (1, 1, 1, '2026-03-31 19:37:21', '2026-03-31 19:37:21');
INSERT INTO `announcement_reads` VALUES (2, 2, 1, '2026-03-31 19:38:18', '2026-03-31 19:38:18');
INSERT INTO `announcement_reads` VALUES (3, 2, 6, '2026-03-31 19:41:26', '2026-03-31 19:41:26');
INSERT INTO `announcement_reads` VALUES (4, 1, 6, '2026-03-31 19:41:26', '2026-03-31 19:41:26');
INSERT INTO `announcement_reads` VALUES (5, 3, 1, '2026-03-31 22:46:59', '2026-03-31 22:46:59');
INSERT INTO `announcement_reads` VALUES (6, 3, 6, '2026-04-01 22:27:34', '2026-04-01 22:27:34');
INSERT INTO `announcement_reads` VALUES (7, 3, 2, '2026-04-01 22:27:46', '2026-04-01 22:27:46');
INSERT INTO `announcement_reads` VALUES (8, 2, 2, '2026-04-01 22:27:46', '2026-04-01 22:27:46');
INSERT INTO `announcement_reads` VALUES (9, 1, 2, '2026-04-01 22:27:48', '2026-04-01 22:27:48');
INSERT INTO `announcement_reads` VALUES (10, 2, 4, '2026-04-01 22:50:03', '2026-04-01 22:50:03');
INSERT INTO `announcement_reads` VALUES (11, 3, 4, '2026-04-01 22:50:04', '2026-04-01 22:50:04');
INSERT INTO `announcement_reads` VALUES (12, 1, 4, '2026-04-01 22:50:04', '2026-04-01 22:50:04');

-- ----------------------------
-- Table structure for announcements
-- ----------------------------
DROP TABLE IF EXISTS `announcements`;
CREATE TABLE `announcements`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `body` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '正文',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft' COMMENT '状态：草稿/已发布/已下线',
  `is_pinned` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `start_time` datetime NULL DEFAULT NULL COMMENT '生效开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '生效结束时间',
  `publish_time` datetime NULL DEFAULT NULL COMMENT '发布时间',
  `creator_id` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `updater_id` bigint NULL DEFAULT NULL COMMENT '更新人用户ID',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ann_status_time`(`status` ASC, `is_pinned` ASC, `publish_time` ASC, `start_time` ASC, `end_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '站内公告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of announcements
-- ----------------------------
INSERT INTO `announcements` VALUES (1, '新年', '新年快乐', 'published', 0, '2026-03-01 00:00:00', '2026-04-09 00:00:00', '2026-03-31 19:34:15', 6, 6, '2026-03-31 19:34:00', '2026-03-31 19:37:07');
INSERT INTO `announcements` VALUES (2, '端午', '端午快乐', 'published', 1, '2026-03-02 00:00:00', '2026-04-17 23:59:59', '2026-03-31 19:38:10', 6, 6, '2026-03-31 19:38:08', '2026-03-31 19:38:28');
INSERT INTO `announcements` VALUES (3, '中秋', '中秋快乐', 'published', 0, '2026-03-02 00:00:00', '2026-08-15 23:59:59', '2026-03-31 22:46:51', 6, 6, '2026-03-31 22:46:49', '2026-03-31 22:46:51');

-- ----------------------------
-- Table structure for categories
-- ----------------------------
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分类描述',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序权重',
  `status` int NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name` ASC) USING BTREE,
  INDEX `idx_categories_sort_order`(`sort_order` ASC) USING BTREE,
  INDEX `idx_categories_status_sort`(`status` ASC, `sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of categories
-- ----------------------------
INSERT INTO `categories` VALUES (1, '生活趣事', '分享日常生活中的有趣事情', 1, 1, '2026-03-29 18:35:18', '2026-03-29 18:35:18');
INSERT INTO `categories` VALUES (2, '工作感悟', '分享工作中的心得体会', 2, 1, '2026-03-29 18:35:18', '2026-03-29 18:35:18');
INSERT INTO `categories` VALUES (3, '情感故事', '分享情感经历和感悟', 3, 1, '2026-03-29 18:35:18', '2026-03-29 18:35:18');
INSERT INTO `categories` VALUES (4, '学习笔记', '分享学习心得和笔记', 4, 1, '2026-03-29 18:35:18', '2026-03-29 18:35:18');
INSERT INTO `categories` VALUES (5, '其他', '其他类型的分享', 5, 1, '2026-03-29 18:35:18', '2026-03-29 18:35:18');

-- ----------------------------
-- Table structure for chat_conversations
-- ----------------------------
DROP TABLE IF EXISTS `chat_conversations`;
CREATE TABLE `chat_conversations`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_low_id` bigint NOT NULL COMMENT '较小用户ID',
  `user_high_id` bigint NOT NULL COMMENT '较大用户ID',
  `last_message_id` bigint NULL DEFAULT NULL COMMENT '最后一条消息ID',
  `last_message_time` timestamp NULL DEFAULT NULL COMMENT '最后消息时间',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_chat_conversation_pair`(`user_low_id` ASC, `user_high_id` ASC) USING BTREE,
  INDEX `idx_chat_conversation_last_time`(`last_message_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '私聊会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_conversations
-- ----------------------------
INSERT INTO `chat_conversations` VALUES (1, 1, 2, 11, '2026-03-31 00:11:50', '2026-03-30 17:32:36', '2026-03-31 00:11:49');
INSERT INTO `chat_conversations` VALUES (6, 1, 4, 15, '2026-04-01 22:46:03', '2026-03-30 22:39:29', '2026-04-01 22:46:03');
INSERT INTO `chat_conversations` VALUES (13, 2, 5, 14, '2026-03-31 11:41:41', '2026-03-31 11:41:16', '2026-03-31 11:41:40');

-- ----------------------------
-- Table structure for chat_messages
-- ----------------------------
DROP TABLE IF EXISTS `chat_messages`;
CREATE TABLE `chat_messages`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` bigint NOT NULL COMMENT '会话ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `message_type` tinyint NOT NULL DEFAULT 1 COMMENT '消息类型：1文本',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
  `read_time` timestamp NULL DEFAULT NULL COMMENT '已读时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_chat_messages_conversation_id`(`conversation_id` ASC, `id` ASC) USING BTREE,
  INDEX `idx_chat_messages_receiver_time`(`receiver_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_chat_messages_receiver_read`(`receiver_id` ASC, `is_read` ASC, `conversation_id` ASC) USING BTREE,
  INDEX `idx_chat_messages_conversation_receiver_read`(`conversation_id` ASC, `receiver_id` ASC, `is_read` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '私聊消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_messages
-- ----------------------------
INSERT INTO `chat_messages` VALUES (1, 1, 2, 1, '你好', 1, '2026-03-30 17:32:36', 1, '2026-03-30 17:32:36');
INSERT INTO `chat_messages` VALUES (2, 1, 1, 2, '😀', 1, '2026-03-30 22:00:07', 1, '2026-03-30 22:00:07');
INSERT INTO `chat_messages` VALUES (3, 1, 1, 2, '在干嘛', 1, '2026-03-30 22:00:49', 1, '2026-03-30 22:00:49');
INSERT INTO `chat_messages` VALUES (4, 1, 2, 1, '在学习🥳🥳', 1, '2026-03-30 22:01:12', 1, '2026-03-30 22:01:12');
INSERT INTO `chat_messages` VALUES (5, 1, 2, 1, '好的', 1, '2026-03-30 22:33:52', 1, '2026-03-30 22:33:58');
INSERT INTO `chat_messages` VALUES (6, 6, 4, 1, '在吗', 1, '2026-03-30 22:39:29', 1, '2026-03-30 22:39:33');
INSERT INTO `chat_messages` VALUES (7, 1, 2, 1, '吃了吗', 1, '2026-03-30 22:40:13', 1, '2026-03-30 22:40:44');
INSERT INTO `chat_messages` VALUES (8, 6, 4, 1, '吃了吗', 1, '2026-03-30 22:40:28', 1, '2026-03-30 22:40:42');
INSERT INTO `chat_messages` VALUES (9, 6, 4, 1, '睡了吗', 1, '2026-03-30 23:42:04', 1, '2026-03-30 23:42:27');
INSERT INTO `chat_messages` VALUES (10, 1, 2, 1, '睡了吗', 1, '2026-03-30 23:49:44', 1, '2026-03-31 00:10:30');
INSERT INTO `chat_messages` VALUES (11, 1, 2, 1, '我要睡了😄', 1, '2026-03-31 00:11:50', 1, '2026-03-31 00:12:24');
INSERT INTO `chat_messages` VALUES (12, 6, 4, 1, '我也要睡了', 1, '2026-03-31 00:12:01', 1, '2026-03-31 00:12:19');
INSERT INTO `chat_messages` VALUES (13, 13, 2, 5, '哥们你现在在家干嘛', 1, '2026-03-31 11:41:16', 1, '2026-03-31 11:41:25');
INSERT INTO `chat_messages` VALUES (14, 13, 5, 2, '在家躺平😂', 1, '2026-03-31 11:41:41', 1, '2026-03-31 11:43:48');
INSERT INTO `chat_messages` VALUES (15, 6, 1, 4, '/api/uploads/chat/1ac7ddf4-cc05-43d9-9ab0-03ef518052c7.jpg', 2, '2026-04-01 22:46:03', 1, '2026-04-01 22:47:11');

-- ----------------------------
-- Table structure for chat_oneway_quota
-- ----------------------------
DROP TABLE IF EXISTS `chat_oneway_quota`;
CREATE TABLE `chat_oneway_quota`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '单向消息配额ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID',
  `used_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首条消息使用时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_chat_oneway_pair`(`sender_id` ASC, `receiver_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '单向关注私聊配额表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat_oneway_quota
-- ----------------------------
INSERT INTO `chat_oneway_quota` VALUES (1, 2, 1, '2026-03-30 17:32:36');
INSERT INTO `chat_oneway_quota` VALUES (2, 1, 2, '2026-03-30 22:00:07');
INSERT INTO `chat_oneway_quota` VALUES (3, 4, 1, '2026-03-30 22:39:29');
INSERT INTO `chat_oneway_quota` VALUES (4, 2, 5, '2026-03-31 11:41:16');
INSERT INTO `chat_oneway_quota` VALUES (5, 5, 2, '2026-03-31 11:41:40');

-- ----------------------------
-- Table structure for collections
-- ----------------------------
DROP TABLE IF EXISTS `collections`;
CREATE TABLE `collections`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `content_id` bigint NOT NULL COMMENT '内容ID',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_collections_user_content`(`user_id` ASC, `content_id` ASC) USING BTREE,
  INDEX `idx_collections_content_id`(`content_id` ASC) USING BTREE,
  INDEX `idx_collections_content_time`(`content_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of collections
-- ----------------------------
INSERT INTO `collections` VALUES (1, 1, 3, '2026-03-30 15:55:29');
INSERT INTO `collections` VALUES (3, 5, 2, '2026-03-31 11:35:29');
INSERT INTO `collections` VALUES (4, 2, 4, '2026-03-31 11:39:27');
INSERT INTO `collections` VALUES (5, 1, 4, '2026-03-31 19:51:37');
INSERT INTO `collections` VALUES (13, 1, 7, '2026-04-01 23:18:56');

-- ----------------------------
-- Table structure for comment_likes
-- ----------------------------
DROP TABLE IF EXISTS `comment_likes`;
CREATE TABLE `comment_likes`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论点赞ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `comment_id` bigint NOT NULL COMMENT '评论ID',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC, `comment_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of comment_likes
-- ----------------------------
INSERT INTO `comment_likes` VALUES (3, 1, 1, '2026-03-30 11:28:54');
INSERT INTO `comment_likes` VALUES (4, 1, 6, '2026-03-31 19:52:00');
INSERT INTO `comment_likes` VALUES (5, 1, 10, '2026-04-02 00:09:50');

-- ----------------------------
-- Table structure for comments
-- ----------------------------
DROP TABLE IF EXISTS `comments`;
CREATE TABLE `comments`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `user_id` bigint NOT NULL COMMENT '评论者ID',
  `content_id` bigint NOT NULL COMMENT '内容ID',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '父评论ID（空表示顶级评论）',
  `comment_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞次数',
  `status` int NULL DEFAULT 1 COMMENT '状态：0-删除，1-正常',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `review_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'approved' COMMENT '审核状态',
  `review_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核说明',
  `reviewer_id` bigint NULL DEFAULT NULL COMMENT '审核人用户ID',
  `review_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_comments_content_id`(`content_id` ASC) USING BTREE,
  INDEX `idx_comments_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_comments_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_comments_review_status`(`review_status` ASC, `status` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_comments_content_parent_status_review_time`(`content_id` ASC, `parent_id` ASC, `status` ASC, `review_status` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of comments
-- ----------------------------
INSERT INTO `comments` VALUES (1, 1, 3, NULL, '看起来很有趣', 1, 1, '2026-03-29 23:59:59', '2026-03-29 23:59:59', 'approved', NULL, NULL, NULL);
INSERT INTO `comments` VALUES (2, 1, 3, NULL, '????', 0, 0, '2026-03-30 11:18:04', '2026-03-30 11:18:04', 'approved', NULL, NULL, NULL);
INSERT INTO `comments` VALUES (3, 1, 3, 1, '????', 0, 0, '2026-03-30 11:18:15', '2026-03-30 11:18:15', 'approved', NULL, NULL, NULL);
INSERT INTO `comments` VALUES (4, 1, 3, 1, '确实', 0, 1, '2026-03-30 11:29:03', '2026-03-30 11:29:03', 'approved', NULL, NULL, NULL);
INSERT INTO `comments` VALUES (5, 2, 3, 1, '我也是这么觉得，而且这个作者太帅了，简直是三亿少女的梦！！', 0, 1, '2026-03-30 16:01:34', '2026-03-30 16:01:34', 'approved', NULL, NULL, NULL);
INSERT INTO `comments` VALUES (6, 2, 4, NULL, '我也想躺平，不想努力了！！😭😭', 1, 1, '2026-03-31 11:40:18', '2026-03-31 11:40:18', 'approved', NULL, NULL, NULL);
INSERT INTO `comments` VALUES (7, 1, 4, NULL, '羡慕😍', 0, 1, '2026-03-31 19:51:54', '2026-03-31 19:51:54', 'approved', NULL, NULL, NULL);
INSERT INTO `comments` VALUES (8, 1, 4, 6, '一起吗兄弟', 0, 1, '2026-03-31 19:52:15', '2026-03-31 20:02:29', 'approved', '不合法', 6, '2026-03-31 20:02:29');
INSERT INTO `comments` VALUES (9, 4, 5, NULL, '真不错😍😍', 0, 1, '2026-03-31 20:33:04', '2026-03-31 20:33:04', 'approved', NULL, NULL, NULL);
INSERT INTO `comments` VALUES (10, 1, 7, NULL, '很好', 1, 1, '2026-04-01 23:04:09', '2026-04-01 23:04:09', 'approved', NULL, NULL, NULL);
INSERT INTO `comments` VALUES (11, 1, 7, 10, '期待新作品', 0, 1, '2026-04-01 23:08:46', '2026-04-01 23:08:46', 'approved', NULL, NULL, NULL);

-- ----------------------------
-- Table structure for content_counter_fix_backup_phase15
-- ----------------------------
DROP TABLE IF EXISTS `content_counter_fix_backup_phase15`;
CREATE TABLE `content_counter_fix_backup_phase15`  (
  `content_id` bigint NOT NULL COMMENT '内容ID',
  `like_count` int NOT NULL DEFAULT 0 COMMENT '旧点赞数',
  `collection_count` int NOT NULL DEFAULT 0 COMMENT '旧收藏数',
  `comment_count` int NOT NULL DEFAULT 0 COMMENT '旧评论数',
  `backup_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '备份时间',
  PRIMARY KEY (`content_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '内容计数修复备份表（第15阶段）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of content_counter_fix_backup_phase15
-- ----------------------------
INSERT INTO `content_counter_fix_backup_phase15` VALUES (1, 0, 0, 0, '2026-04-01 23:35:18');
INSERT INTO `content_counter_fix_backup_phase15` VALUES (2, 1, 1, 0, '2026-04-01 23:35:18');
INSERT INTO `content_counter_fix_backup_phase15` VALUES (3, 3, 1, 1, '2026-04-01 23:35:18');
INSERT INTO `content_counter_fix_backup_phase15` VALUES (4, 2, 2, 2, '2026-04-01 23:35:18');
INSERT INTO `content_counter_fix_backup_phase15` VALUES (5, 1, 0, 1, '2026-04-01 23:35:18');
INSERT INTO `content_counter_fix_backup_phase15` VALUES (6, 0, 0, 0, '2026-04-01 23:35:18');
INSERT INTO `content_counter_fix_backup_phase15` VALUES (7, 1, 1, 1, '2026-04-01 23:35:18');

-- ----------------------------
-- Table structure for content_view_events
-- ----------------------------
DROP TABLE IF EXISTS `content_view_events`;
CREATE TABLE `content_view_events`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '浏览事件ID',
  `content_id` bigint NOT NULL COMMENT '内容ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '浏览用户ID（未登录为空）',
  `viewer_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '浏览器标识（匿名去重）',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端IP',
  `user_agent` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户端UA',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_cve_content_time`(`content_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_cve_user_content_time`(`user_id` ASC, `content_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_cve_viewer_content_time`(`viewer_key` ASC, `content_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '内容浏览事件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of content_view_events
-- ----------------------------
INSERT INTO `content_view_events` VALUES (1, 2, 2, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-30 17:09:44');
INSERT INTO `content_view_events` VALUES (2, 3, 2, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-30 17:10:01');
INSERT INTO `content_view_events` VALUES (3, 4, 2, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 11:39:49');
INSERT INTO `content_view_events` VALUES (4, 2, 5, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 12:22:15');
INSERT INTO `content_view_events` VALUES (5, 4, 1, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 19:51:42');
INSERT INTO `content_view_events` VALUES (6, 5, 4, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-03-31 20:32:21');
INSERT INTO `content_view_events` VALUES (7, 4, 1, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 11:35:35');
INSERT INTO `content_view_events` VALUES (8, 5, 6, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 11:35:57');
INSERT INTO `content_view_events` VALUES (9, 4, 1, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 15:54:58');
INSERT INTO `content_view_events` VALUES (10, 6, 2, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 17:27:19');
INSERT INTO `content_view_events` VALUES (11, 6, 6, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 17:32:05');
INSERT INTO `content_view_events` VALUES (12, 6, 2, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 17:59:56');
INSERT INTO `content_view_events` VALUES (13, 6, 2, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 22:40:16');
INSERT INTO `content_view_events` VALUES (14, 3, 2, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 22:49:07');
INSERT INTO `content_view_events` VALUES (15, 7, 1, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 23:04:01');
INSERT INTO `content_view_events` VALUES (16, 7, 1, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 23:37:08');
INSERT INTO `content_view_events` VALUES (17, 7, 4, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-01 23:45:11');
INSERT INTO `content_view_events` VALUES (18, 7, 1, 'v_mncyvp4y_4cjrhc6k', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0', '2026-04-02 00:09:44');

-- ----------------------------
-- Table structure for contents
-- ----------------------------
DROP TABLE IF EXISTS `contents`;
CREATE TABLE `contents`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '内容ID',
  `user_id` bigint NOT NULL COMMENT '发布者ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '内容',
  `category_id` bigint NULL DEFAULT NULL COMMENT '分类ID',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标签列表（逗号分隔）',
  `images` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片列表（逗号分隔）',
  `videos` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '视频地址列表（逗号分隔）',
  `image_size` bigint NULL DEFAULT 0 COMMENT '图片总大小（字节）',
  `view_count` int NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞次数',
  `comment_count` int NULL DEFAULT 0 COMMENT '评论次数',
  `collection_count` int NULL DEFAULT 0 COMMENT '收藏次数',
  `status` int NULL DEFAULT 1 COMMENT '状态：0-草稿，1-发布，2-删除',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `review_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'approved' COMMENT '审核状态',
  `review_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核说明',
  `reviewer_id` bigint NULL DEFAULT NULL COMMENT '审核人用户ID',
  `review_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_contents_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_contents_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_contents_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_contents_status`(`status` ASC) USING BTREE,
  INDEX `idx_contents_title`(`title` ASC) USING BTREE,
  INDEX `idx_contents_review_status`(`review_status` ASC, `status` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_contents_user_review_status_time`(`user_id` ASC, `review_status` ASC, `status` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of contents
-- ----------------------------
INSERT INTO `contents` VALUES (1, 1, '天气好', '今天天气真好', 1, '分享', '/api/uploads/b175fbb8-a9e2-4dbd-b07e-b2202ae7bc9f.webp', NULL, 316004, 3, 0, 0, 0, 1, '2026-03-29 22:08:56', '2026-04-01 23:32:14', 'approved', NULL, NULL, NULL);
INSERT INTO `contents` VALUES (2, 1, 'java', '今天学习了spring', 4, '工作,学习,成长', '/api/uploads/3a21ce8e-96f9-40be-9bb9-e79c11505808.jpeg', NULL, 31373, 2, 1, 0, 1, 1, '2026-03-29 23:00:33', '2026-03-30 15:00:26', 'approved', NULL, NULL, NULL);
INSERT INTO `contents` VALUES (3, 1, 'VibeCoding', '今天开始尝试使用VibeCoding做一些有趣的设计', 4, '学习,经验,分享,生活,工作', '/api/uploads/f7990df5-64a5-4a55-af56-4e3598833846.webp', NULL, 0, 2, 3, 1, 1, 1, '2026-03-29 23:58:53', '2026-03-29 23:58:53', 'approved', NULL, NULL, NULL);
INSERT INTO `contents` VALUES (4, 5, '躺平', '今天又是躺平的一天', 1, '生活', '/api/uploads/3840ab17-b14b-448b-8bb6-534391380ac4.jpg', NULL, 40402, 4, 2, 2, 2, 1, '2026-03-31 11:37:50', '2026-03-31 11:37:50', 'approved', NULL, NULL, NULL);
INSERT INTO `contents` VALUES (5, 1, '美食', '今天吃麻辣小龙虾，再整瓶小麦果汁，美滋滋', 1, '生活', '/api/uploads/d9d3aff0-b17a-48d0-bf02-08c5df506eea.jpg,/api/uploads/5dd2cfc9-0eb8-493f-add9-12baf18c3861.jpeg', NULL, 783520, 2, 1, 1, 0, 1, '2026-03-31 19:57:49', '2026-03-31 19:57:49', 'approved', NULL, NULL, NULL);
INSERT INTO `contents` VALUES (6, 1, '没钱了~', '我的gpt-plus明天就要到期了，肿么办，我还想要继续VibeCoding呢', 1, '工作,学习', '/api/uploads/2abfcf83-5ac4-4af3-a245-92a509a5722e.png', NULL, 369045, 4, 0, 0, 0, 1, '2026-04-01 16:29:35', '2026-04-01 16:30:03', 'approved', NULL, 6, '2026-04-01 16:30:03');
INSERT INTO `contents` VALUES (7, 2, '问道星穹', '这是一段很精彩的视频', 1, '分享', '', '/api/uploads/videos/861065cf-4e4b-4019-b5de-1f445135f65a.mp4', 0, 4, 1, 1, 1, 1, '2026-04-01 22:21:39', '2026-04-01 23:47:45', 'approved', '血腥暴力', 6, '2026-04-01 23:47:45');

-- ----------------------------
-- Table structure for follow_events
-- ----------------------------
DROP TABLE IF EXISTS `follow_events`;
CREATE TABLE `follow_events`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '事件ID',
  `user_id` bigint NOT NULL COMMENT '操作用户ID',
  `target_user_id` bigint NOT NULL COMMENT '被关注用户ID',
  `event_type` tinyint NOT NULL COMMENT '事件类型：1-关注，2-取关',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_follow_events_target_time`(`target_user_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_follow_events_target_type_time`(`target_user_id` ASC, `event_type` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_follow_events_user_time`(`user_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of follow_events
-- ----------------------------
INSERT INTO `follow_events` VALUES (1, 2, 1, 1, '2026-03-30 16:09:53');
INSERT INTO `follow_events` VALUES (2, 2, 1, 2, '2026-03-30 16:10:04');
INSERT INTO `follow_events` VALUES (3, 2, 1, 1, '2026-03-30 16:33:26');
INSERT INTO `follow_events` VALUES (4, 1, 2, 1, '2026-03-30 22:00:33');
INSERT INTO `follow_events` VALUES (5, 4, 1, 1, '2026-03-30 22:39:19');
INSERT INTO `follow_events` VALUES (6, 1, 4, 1, '2026-03-30 22:40:04');
INSERT INTO `follow_events` VALUES (7, 5, 1, 1, '2026-03-31 11:35:31');
INSERT INTO `follow_events` VALUES (8, 2, 5, 1, '2026-03-31 11:39:32');
INSERT INTO `follow_events` VALUES (9, 5, 2, 1, '2026-03-31 11:41:49');
INSERT INTO `follow_events` VALUES (10, 5, 2, 2, '2026-03-31 11:41:49');
INSERT INTO `follow_events` VALUES (11, 5, 2, 1, '2026-03-31 11:41:53');
INSERT INTO `follow_events` VALUES (12, 1, 5, 1, '2026-03-31 19:53:46');

-- ----------------------------
-- Table structure for follows
-- ----------------------------
DROP TABLE IF EXISTS `follows`;
CREATE TABLE `follows`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关注关系ID',
  `user_id` bigint NOT NULL COMMENT '关注者ID',
  `target_user_id` bigint NOT NULL COMMENT '被关注用户ID',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_follows_user_target`(`user_id` ASC, `target_user_id` ASC) USING BTREE,
  INDEX `idx_follows_target_user_id`(`target_user_id` ASC) USING BTREE,
  INDEX `idx_follows_target_create_time`(`target_user_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of follows
-- ----------------------------
INSERT INTO `follows` VALUES (3, 2, 1, '2026-03-30 16:33:26');
INSERT INTO `follows` VALUES (4, 1, 2, '2026-03-30 22:00:33');
INSERT INTO `follows` VALUES (5, 4, 1, '2026-03-30 22:39:19');
INSERT INTO `follows` VALUES (6, 1, 4, '2026-03-30 22:40:04');
INSERT INTO `follows` VALUES (7, 5, 1, '2026-03-31 11:35:32');
INSERT INTO `follows` VALUES (8, 2, 5, '2026-03-31 11:39:32');
INSERT INTO `follows` VALUES (10, 5, 2, '2026-03-31 11:41:53');
INSERT INTO `follows` VALUES (11, 1, 5, '2026-03-31 19:53:46');

-- ----------------------------
-- Table structure for likes
-- ----------------------------
DROP TABLE IF EXISTS `likes`;
CREATE TABLE `likes`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `content_id` bigint NOT NULL COMMENT '内容ID',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC, `content_id` ASC) USING BTREE,
  INDEX `idx_likes_content_time`(`content_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of likes
-- ----------------------------
INSERT INTO `likes` VALUES (12, 1, 3, '2026-03-30 11:22:07');
INSERT INTO `likes` VALUES (15, 2, 3, '2026-03-30 15:09:34');
INSERT INTO `likes` VALUES (23, 4, 3, '2026-03-30 23:40:19');
INSERT INTO `likes` VALUES (24, 5, 2, '2026-03-31 11:35:24');
INSERT INTO `likes` VALUES (25, 2, 4, '2026-03-31 11:39:26');
INSERT INTO `likes` VALUES (26, 1, 4, '2026-03-31 19:51:36');
INSERT INTO `likes` VALUES (27, 1, 5, '2026-03-31 20:05:57');
INSERT INTO `likes` VALUES (30, 1, 7, '2026-04-01 23:15:18');

-- ----------------------------
-- Table structure for notification_templates
-- ----------------------------
DROP TABLE IF EXISTS `notification_templates`;
CREATE TABLE `notification_templates`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板编码',
  `name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
  `title_template` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题模板',
  `body_template` varchar(800) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '正文模板',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1启用0停用',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_notification_templates_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统通知模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notification_templates
-- ----------------------------
INSERT INTO `notification_templates` VALUES (1, 'USER_BANNED', 'User banned notice', '账号封禁通知', '你的账号已被管理员封禁。原因：{{reason}}', 1, '2026-03-31 19:22:43', '2026-03-31 19:25:07');
INSERT INTO `notification_templates` VALUES (2, 'USER_MUTED', 'User muted notice', '账号禁言通知', '你的账号已被禁言 {{minutes}} 分钟。原因：{{reason}}', 1, '2026-03-31 19:22:43', '2026-03-31 19:25:07');
INSERT INTO `notification_templates` VALUES (3, 'CONTENT_OFF_SHELF', 'Content off shelf notice', '内容下架通知', '你的内容《{{contentTitle}}》已下架。原因：{{reason}}', 1, '2026-03-31 19:22:43', '2026-03-31 19:25:07');
INSERT INTO `notification_templates` VALUES (4, 'COMMENT_HIDDEN', 'Comment hidden notice', '评论隐藏通知', '你在《{{contentTitle}}》下的评论已被隐藏。原因：{{reason}}', 1, '2026-03-31 19:22:43', '2026-03-31 19:25:07');

-- ----------------------------
-- Table structure for notifications
-- ----------------------------
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知类型',
  `related_id` bigint(20) UNSIGNED ZEROFILL NOT NULL DEFAULT 00000000000000000000 COMMENT '关联ID（评论ID或内容ID）',
  `related_user_id` bigint NULL DEFAULT NULL COMMENT '关联用户ID',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '通知内容',
  `status` int NULL DEFAULT 0 COMMENT '状态：0-未读，1-已读',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `receiver_id` bigint NOT NULL COMMENT '接收者用户ID',
  `actor_id` bigint NOT NULL COMMENT '触发者用户ID',
  `content_id` bigint NULL DEFAULT NULL COMMENT '关联内容ID',
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知标题',
  `body` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知内容',
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
  `read_time` timestamp NULL DEFAULT NULL COMMENT '已读时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notifications_status`(`status` ASC) USING BTREE,
  INDEX `idx_notifications_receiver_read_time`(`receiver_id` ASC, `is_read` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_notifications_receiver_time`(`receiver_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 28 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notifications
-- ----------------------------
INSERT INTO `notifications` VALUES (1, 'content_like', 00000000000000000000, NULL, NULL, 0, '2026-03-30 23:40:19', 1, 4, 3, '收到新的点赞', '刘华强 点赞了 《VibeCoding》', 1, '2026-03-30 23:41:28');
INSERT INTO `notifications` VALUES (2, 'content_like', 00000000000000000000, NULL, NULL, 0, '2026-03-31 11:35:24', 1, 5, 2, '收到新的点赞', '路人甲 点赞了 《java》', 1, '2026-04-01 11:08:34');
INSERT INTO `notifications` VALUES (3, 'content_collection', 00000000000000000000, NULL, NULL, 0, '2026-03-31 11:35:27', 1, 5, 2, '收到新的收藏', '路人甲 收藏了 《java》', 1, '2026-04-01 11:08:29');
INSERT INTO `notifications` VALUES (4, 'content_collection', 00000000000000000000, NULL, NULL, 0, '2026-03-31 11:35:29', 1, 5, 2, '收到新的收藏', '路人甲 收藏了 《java》', 1, '2026-03-31 17:21:45');
INSERT INTO `notifications` VALUES (5, 'content_like', 00000000000000000000, NULL, NULL, 0, '2026-03-31 11:39:26', 5, 2, 4, '收到新的点赞', '少年凌云志 点赞了 《躺平》', 1, '2026-03-31 11:43:27');
INSERT INTO `notifications` VALUES (6, 'content_collection', 00000000000000000000, NULL, NULL, 0, '2026-03-31 11:39:27', 5, 2, 4, '收到新的收藏', '少年凌云志 收藏了 《躺平》', 1, '2026-03-31 11:43:20');
INSERT INTO `notifications` VALUES (7, 'content_comment', 00000000000000000000, NULL, NULL, 0, '2026-03-31 11:40:18', 5, 2, 4, '收到新的评论', '少年凌云志 评论了 《躺平》：我也想躺平，不想努力了！！😭😭', 1, '2026-03-31 11:40:42');
INSERT INTO `notifications` VALUES (8, 'content_like', 00000000000000000000, NULL, NULL, 0, '2026-03-31 19:51:36', 5, 1, 4, '收到新的点赞', 'qiangzi 点赞了《躺平》', 0, NULL);
INSERT INTO `notifications` VALUES (9, 'content_collection', 00000000000000000000, NULL, NULL, 0, '2026-03-31 19:51:37', 5, 1, 4, '收到新的收藏', 'qiangzi 收藏了《躺平》', 0, NULL);
INSERT INTO `notifications` VALUES (10, 'content_comment', 00000000000000000000, NULL, NULL, 0, '2026-03-31 19:51:54', 5, 1, 4, '收到新的评论', 'qiangzi 评论了《躺平》：羡慕😍', 0, NULL);
INSERT INTO `notifications` VALUES (11, 'content_comment', 00000000000000000000, NULL, NULL, 0, '2026-03-31 19:52:15', 5, 1, 4, '收到新的评论', 'qiangzi 评论了《躺平》：一起吗兄弟', 0, NULL);
INSERT INTO `notifications` VALUES (12, 'system_notice', 00000000000000000000, NULL, NULL, 0, '2026-03-31 19:53:08', 1, 0, NULL, '评论隐藏通知', '你在《躺平》下的评论已被隐藏。原因：不合法', 1, '2026-03-31 19:54:05');
INSERT INTO `notifications` VALUES (13, 'content_comment', 00000000000000000000, NULL, NULL, 0, '2026-03-31 20:33:04', 1, 4, 5, '收到新的评论', '刘华强 评论了《美食》：真不错😍😍', 1, '2026-03-31 22:45:03');
INSERT INTO `notifications` VALUES (14, 'content_like', 00000000000000000000, NULL, NULL, 0, '2026-04-01 22:28:03', 2, 1, 7, '收到新的点赞', '少年凌云志 点赞了《问道星穹》', 1, '2026-04-01 22:42:53');
INSERT INTO `notifications` VALUES (15, 'content_collection', 00000000000000000000, NULL, NULL, 0, '2026-04-01 22:28:04', 2, 1, 7, '收到新的收藏', '少年凌云志 收藏了《问道星穹》', 1, '2026-04-01 22:28:18');
INSERT INTO `notifications` VALUES (16, 'content_comment', 00000000000000000000, NULL, NULL, 0, '2026-04-01 23:04:09', 2, 1, 7, '收到新的评论', '少年凌云志 评论了《问道星穹》：很好', 1, '2026-04-01 23:04:20');
INSERT INTO `notifications` VALUES (17, 'content_collection', 00000000000000000000, NULL, NULL, 0, '2026-04-01 23:06:24', 2, 1, 7, '收到新的收藏', '少年凌云志 收藏了《问道星穹》', 1, '2026-04-01 23:07:03');
INSERT INTO `notifications` VALUES (18, 'content_comment', 00000000000000000000, NULL, NULL, 0, '2026-04-01 23:08:46', 2, 1, 7, '收到新的评论', '少年凌云志 评论了《问道星穹》：期待新作品', 1, '2026-04-01 23:11:44');
INSERT INTO `notifications` VALUES (19, 'content_collection', 00000000000000000000, NULL, NULL, 0, '2026-04-01 23:15:04', 2, 1, 7, '收到新的收藏', '少年凌云志 收藏了《问道星穹》', 1, '2026-04-01 23:15:12');
INSERT INTO `notifications` VALUES (20, 'content_like', 00000000000000000000, NULL, NULL, 0, '2026-04-01 23:15:05', 2, 1, 7, '收到新的点赞', '少年凌云志 点赞了《问道星穹》', 1, '2026-04-01 23:15:12');
INSERT INTO `notifications` VALUES (21, 'content_collection', 00000000000000000000, NULL, NULL, 0, '2026-04-01 23:15:17', 2, 1, 7, '收到新的收藏', '少年凌云志 收藏了《问道星穹》', 1, '2026-04-01 23:15:23');
INSERT INTO `notifications` VALUES (22, 'content_like', 00000000000000000000, NULL, NULL, 0, '2026-04-01 23:15:18', 2, 1, 7, '收到新的点赞', '少年凌云志 点赞了《问道星穹》', 1, '2026-04-01 23:15:23');
INSERT INTO `notifications` VALUES (23, 'content_collection', 00000000000000000000, NULL, NULL, 0, '2026-04-01 23:15:19', 2, 1, 7, '收到新的收藏', '少年凌云志 收藏了《问道星穹》', 1, '2026-04-01 23:15:23');
INSERT INTO `notifications` VALUES (24, 'content_collection', 00000000000000000000, NULL, NULL, 0, '2026-04-01 23:18:52', 2, 1, 7, '收到新的收藏', '少年凌云志 收藏了《问道星穹》', 1, '2026-04-01 23:25:05');
INSERT INTO `notifications` VALUES (25, 'content_collection', 00000000000000000000, NULL, NULL, 0, '2026-04-01 23:18:54', 2, 1, 7, '收到新的收藏', '少年凌云志 收藏了《问道星穹》', 1, '2026-04-01 23:25:05');
INSERT INTO `notifications` VALUES (26, 'content_collection', 00000000000000000000, NULL, NULL, 0, '2026-04-01 23:18:56', 2, 1, 7, '收到新的收藏', '少年凌云志 收藏了《问道星穹》', 1, '2026-04-01 23:25:04');
INSERT INTO `notifications` VALUES (27, 'system_notice', 00000000000000000000, NULL, NULL, 0, '2026-04-01 23:46:58', 2, 0, NULL, '内容下架通知', '你的内容《问道星穹》已下架。原因：血腥暴力', 1, '2026-04-01 23:49:41');

-- ----------------------------
-- Table structure for report_violation_templates
-- ----------------------------
DROP TABLE IF EXISTS `report_violation_templates`;
CREATE TABLE `report_violation_templates`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板编码',
  `label` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板标签',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模板描述',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1启用0停用',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `is_system` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1系统模板0自定义模板',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_report_templates_code`(`code` ASC) USING BTREE,
  INDEX `idx_report_templates_status_sort`(`status` ASC, `sort_order` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '举报违规模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of report_violation_templates
-- ----------------------------
INSERT INTO `report_violation_templates` VALUES (1, 'pornography', '色情低俗', '包含色情暗示、低俗露骨内容', 1, 10, 1, '2026-04-01 17:52:44', '2026-04-01 17:52:44');
INSERT INTO `report_violation_templates` VALUES (2, 'violence', '血腥暴力', '包含血腥画面、暴力鼓吹内容', 1, 20, 1, '2026-04-01 17:52:44', '2026-04-01 17:52:44');
INSERT INTO `report_violation_templates` VALUES (3, 'subversion', '反动言论', '包含煽动颠覆、破坏公共秩序言论', 1, 30, 1, '2026-04-01 17:52:44', '2026-04-01 17:52:44');
INSERT INTO `report_violation_templates` VALUES (4, 'extremism', '极端言论', '包含极端主义、仇恨煽动内容', 1, 40, 1, '2026-04-01 17:52:44', '2026-04-01 17:52:44');
INSERT INTO `report_violation_templates` VALUES (5, 'borderline', '擦边低俗', '存在擦边内容、明显不适宜传播', 1, 50, 1, '2026-04-01 17:52:44', '2026-04-01 17:52:44');
INSERT INTO `report_violation_templates` VALUES (6, 'gender_conflict', '性别对立', '恶意制造性别对立、群体攻击', 1, 60, 1, '2026-04-01 17:52:44', '2026-04-01 17:52:44');
INSERT INTO `report_violation_templates` VALUES (7, 'anti_learning', '读书无用论', '恶意传播学习无价值等误导内容', 1, 70, 1, '2026-04-01 17:52:44', '2026-04-01 17:52:44');
INSERT INTO `report_violation_templates` VALUES (8, 'abusive', '弱智发言', '侮辱性、恶意贬损与低质攻击内容', 1, 80, 1, '2026-04-01 17:52:44', '2026-04-01 17:52:44');
INSERT INTO `report_violation_templates` VALUES (9, 'spam_ad', '广告引流', '含广告导流、恶意推广、刷屏信息', 1, 90, 1, '2026-04-01 17:52:44', '2026-04-01 17:52:44');
INSERT INTO `report_violation_templates` VALUES (10, 'rumor', '谣言虚假', '传播未经证实或明显虚假信息', 1, 100, 1, '2026-04-01 17:52:44', '2026-04-01 17:52:44');
INSERT INTO `report_violation_templates` VALUES (11, 'privacy', '隐私泄露', '泄露他人隐私、身份信息或联系方式', 1, 110, 1, '2026-04-01 17:52:44', '2026-04-01 17:52:44');

-- ----------------------------
-- Table structure for reports
-- ----------------------------
DROP TABLE IF EXISTS `reports`;
CREATE TABLE `reports`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '举报工单ID',
  `reporter_id` bigint NOT NULL COMMENT '举报人用户ID',
  `target_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标类型：内容/评论/用户',
  `target_id` bigint NOT NULL COMMENT '目标ID',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '举报原因',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '状态：待处理/处理中/已处理/已驳回',
  `assignee_id` bigint NULL DEFAULT NULL COMMENT '处理人用户ID',
  `handle_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '处理备注',
  `resolve_action` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '处理动作',
  `handle_time` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `target_snapshot` json NULL COMMENT '目标快照（结构化数据）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_reports_status_time`(`status` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_reports_target`(`target_type` ASC, `target_id` ASC) USING BTREE,
  INDEX `idx_reports_status_handle_time`(`status` ASC, `create_time` ASC, `handle_time` ASC) USING BTREE,
  INDEX `idx_reports_target_status_time`(`target_type` ASC, `target_id` ASC, `status` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '举报工单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of reports
-- ----------------------------
INSERT INTO `reports` VALUES (1, 1, 'comment', 9, '违规', 'resolved', 6, '已处理', 'resolved', '2026-04-01 11:36:36', '2026-04-01 11:34:39', '2026-04-01 11:36:36', '{\"id\": 9, \"status\": 1, \"userId\": 4, \"contentId\": 5, \"createTime\": \"2026-03-31T20:33:04\", \"reviewStatus\": \"approved\", \"commentContent\": \"真不错😍😍\"}');
INSERT INTO `reports` VALUES (2, 4, 'content', 7, '血腥暴力', 'resolved', 6, '涉嫌违规', 'off_shelf_content', '2026-04-01 23:46:58', '2026-04-01 23:45:16', '2026-04-01 23:46:58', '{\"id\": 7, \"title\": \"问道星穹\", \"status\": 1, \"userId\": 2, \"content\": \"这是一段很精彩的视频\", \"createTime\": \"2026-04-01T22:21:39\", \"reviewStatus\": \"approved\"}');

-- ----------------------------
-- Table structure for tags
-- ----------------------------
DROP TABLE IF EXISTS `tags`;
CREATE TABLE `tags`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标签名称',
  `use_count` int NULL DEFAULT 0 COMMENT '使用次数',
  `status` int NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name` ASC) USING BTREE,
  INDEX `idx_tags_name`(`name` ASC) USING BTREE,
  INDEX `idx_tags_status_use`(`status` ASC, `use_count` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tags
-- ----------------------------
INSERT INTO `tags` VALUES (1, '生活', 0, 1, '2026-03-29 18:35:18', '2026-03-29 18:35:18');
INSERT INTO `tags` VALUES (2, '工作', 0, 1, '2026-03-29 18:35:18', '2026-03-29 18:35:18');
INSERT INTO `tags` VALUES (3, '学习', 0, 1, '2026-03-29 18:35:18', '2026-03-29 18:35:18');
INSERT INTO `tags` VALUES (4, '情感', 0, 1, '2026-03-29 18:35:18', '2026-03-29 18:35:18');
INSERT INTO `tags` VALUES (5, '感悟', 0, 1, '2026-03-29 18:35:18', '2026-03-29 18:35:18');
INSERT INTO `tags` VALUES (6, '成长', 0, 1, '2026-03-29 18:35:18', '2026-03-29 18:35:18');
INSERT INTO `tags` VALUES (7, '分享', 0, 1, '2026-03-29 18:35:18', '2026-03-29 18:35:18');
INSERT INTO `tags` VALUES (8, '经验', 0, 1, '2026-03-29 18:35:18', '2026-03-29 18:35:18');
INSERT INTO `tags` VALUES (9, '技巧', 0, 1, '2026-03-29 18:35:18', '2026-03-29 18:35:18');
INSERT INTO `tags` VALUES (10, '思考', 0, 1, '2026-03-29 18:35:18', '2026-03-29 18:35:18');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码（加密存储）',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '邮箱',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像地址',
  `bio` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '个人简介',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态：1-正常，2-禁言，3-封禁',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `mute_until` datetime NULL DEFAULT NULL COMMENT '禁言截止时间',
  `ban_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封禁原因',
  `ban_time` datetime NULL DEFAULT NULL COMMENT '封禁时间',
  `risk_level` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '风险等级：低/中/高',
  `risk_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '风险备注',
  `risk_mark_time` datetime NULL DEFAULT NULL COMMENT '风险标记时间',
  `risk_mark_by` bigint NULL DEFAULT NULL COMMENT '风险标记人用户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `email`(`email` ASC) USING BTREE,
  INDEX `idx_users_username`(`username` ASC) USING BTREE,
  INDEX `idx_users_email`(`email` ASC) USING BTREE,
  INDEX `idx_users_risk_level`(`risk_level` ASC, `status` ASC, `update_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, '少年凌云志', '$2a$10$CQdU7nuIyV4d4in7pzIRweihqR5FDFRIodkTD3IdwyMINFueXV.kq', '3479590764@qq.com', '少年凌云志', '/api/uploads/avatars/b1311ebc-3f94-45e9-a48e-f29eab68b79c.png', '不要迷恋哥，哥只是一个传说', 1, '2026-03-29 21:48:47', '2026-04-01 18:23:25', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `users` VALUES (2, 'qiangzi', '$2a$10$NeVvAxfjiVlAZVvaP7pNTO74l6Aklqx2OaIvRF5OASRMLI0gleWC6', '1838958493@qq.com', 'qiangzi', '/api/uploads/avatars/c78443ba-9c1e-4e72-80e0-90d0edc2cee6.webp', '从未见过如此之帅的男人', 1, '2026-03-30 11:36:39', '2026-03-30 22:44:43', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `users` VALUES (3, 'newuser1', '$2a$10$CYxL95shB9gfMhQ5ZDnGPedu26HyIgYT8l4taVSbtEJqptzab1woe', 'qiangzi@qq.com', 'n1', '', NULL, 1, '2026-03-30 11:43:51', '2026-03-30 11:43:51', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `users` VALUES (4, '刘华强', '$2a$10$Yy2dqyjrkZ0Fn6lT2A8XvuF1SeIQJcMjrEwSTNW3T6VE1MRwEe1GW', '3479590765@qq.com', '刘华强', '/api/uploads/avatars/d58dab4e-d663-420b-9b3f-ff75f099cebb.webp', '跟我刘华强拼，你有那个实力吗？', 1, '2026-03-30 11:53:51', '2026-04-01 23:37:51', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `users` VALUES (5, '路人甲', '$2a$10$V6HbQiaH69zB4JAGlFoPSO3xqrIaTLjP/fsz1iXgipymgQy7MZhea', '3479590766@qq.com', '路人甲', '/api/uploads/avatars/d61fdbd6-39cf-4224-a088-847a9fc7f210.jpg', '我是路人甲', 2, '2026-03-31 11:32:55', '2026-03-31 16:39:44', '2026-03-31 17:39:44', '看他不爽', '2026-03-31 12:29:16', NULL, NULL, NULL, NULL);
INSERT INTO `users` VALUES (6, 'admin', '$2a$10$5Y/xBkiHnJ128qQDUNa7Z.rfj25ryjpHuf9LShIyP.FoZNgot0Wuy', '3479590767@qq.com', 'admin', NULL, NULL, 1, '2026-03-31 12:13:43', '2026-03-31 12:13:43', NULL, NULL, NULL, NULL, NULL, NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;


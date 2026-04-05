package com.share.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.entity.DailyAiBrief;
import com.share.entity.DailyAiBriefItem;
import com.share.exception.BusinessException;
import com.share.mapper.DailyAiBriefItemMapper;
import com.share.mapper.DailyAiBriefMapper;
import com.share.service.AdminAuditService;
import com.share.service.DailyAiBriefService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 每日AI快讯服务实现：
 * 1. 通过 Python 脚本抓取外部热点；
 * 2. 负责把抓取结果转换为系统可读结构并落库；
 * 3. 对外提供今日快讯与历史快讯查询。
 */
@Service
public class DailyAiBriefServiceImpl implements DailyAiBriefService {

    private static final Logger log = LoggerFactory.getLogger(DailyAiBriefServiceImpl.class);

    private static final String STATUS_READY = "ready";
    private static final String STATUS_FAILED = "failed";
    private static final int DEFAULT_HISTORY_DAYS = 7;
    private static final int MAX_HISTORY_DAYS = 30;

    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    };

    private final DailyAiBriefMapper dailyAiBriefMapper;
    private final DailyAiBriefItemMapper dailyAiBriefItemMapper;
    private final ObjectMapper objectMapper;
    private final AdminAuditService adminAuditService;

    /** Python 可执行文件路径，支持绝对路径和系统 PATH 命令名。 */
    @Value("${ai-brief.python-path:python}")
    private String pythonPath;

    /** 抓取脚本路径。 */
    @Value("${ai-brief.script-path:scripts/ai_brief/fetch_ai_hotspots.py}")
    private String scriptPath;

    /** 每次抓取的最大条目数。 */
    @Value("${ai-brief.limit:8}")
    private Integer fetchLimit;

    /** 外部抓取超时时间（秒）。 */
    @Value("${ai-brief.timeout-seconds:45}")
    private Integer timeoutSeconds;

    public DailyAiBriefServiceImpl(DailyAiBriefMapper dailyAiBriefMapper,
                                   DailyAiBriefItemMapper dailyAiBriefItemMapper,
                                   ObjectMapper objectMapper,
                                   AdminAuditService adminAuditService) {
        this.dailyAiBriefMapper = dailyAiBriefMapper;
        this.dailyAiBriefItemMapper = dailyAiBriefItemMapper;
        this.objectMapper = objectMapper;
        this.adminAuditService = adminAuditService;
    }

    @Override
    public Map<String, Object> getTodayBrief() {
        LocalDate today = LocalDate.now();
        DailyAiBrief todayBrief = findByDate(today);

        if (todayBrief != null && STATUS_READY.equalsIgnoreCase(todayBrief.getStatus())) {
            return toDetailView(todayBrief, false);
        }

        DailyAiBrief fallback = findLatestReadyBrief(today);
        if (fallback == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("date", today);
            empty.put("title", "每日AI快讯");
            empty.put("summary", "今日暂未生成快讯，请稍后再试");
            empty.put("isFallback", false);
            empty.put("generatedAt", null);
            empty.put("items", List.of());
            return empty;
        }

        return toDetailView(fallback, true);
    }

    @Override
    public List<Map<String, Object>> getHistoryBriefs(Integer days) {
        int validDays = normalizeDays(days);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(validDays - 1L);

        LambdaQueryWrapper<DailyAiBrief> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DailyAiBrief::getStatus, STATUS_READY)
                .between(DailyAiBrief::getBriefDate, start, end)
                .orderByDesc(DailyAiBrief::getBriefDate);

        List<DailyAiBrief> briefs = dailyAiBriefMapper.selectList(queryWrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DailyAiBrief brief : briefs) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", brief.getId());
            row.put("date", brief.getBriefDate());
            row.put("title", brief.getTitle());
            row.put("summary", brief.getSummary());
            row.put("sourceCount", brief.getSourceCount());
            row.put("itemCount", brief.getItemCount());
            row.put("generatedAt", brief.getGeneratedAt());
            result.add(row);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> refreshBrief(LocalDate targetDate, Long operatorUserId, String ip, String userAgent) {
        LocalDate date = targetDate == null ? LocalDate.now() : targetDate;
        LocalDateTime now = LocalDateTime.now();

        ScriptResult scriptResult;
        try {
            scriptResult = runPythonScript(date);
        } catch (Exception ex) {
            // 管理员手动触发时，失败也要留下可追踪的审计信息。
            saveFailureBrief(date, now);
            if (operatorUserId != null) {
                Map<String, Object> after = Map.of(
                        "date", date,
                        "status", STATUS_FAILED,
                        "reason", truncate(ex.getMessage(), 500)
                );
                adminAuditService.log(operatorUserId, "admin.ai_brief.refresh.failed", "daily_ai_brief", null, null, after, ip, userAgent);
            }
            throw new BusinessException(HttpStatus.BAD_GATEWAY, 502, "AI快讯抓取失败：" + truncate(ex.getMessage(), 160));
        }

        ParsedBrief parsedBrief = parseScriptPayload(scriptResult.output(), date);
        DailyAiBrief brief = upsertBrief(date, parsedBrief, now);
        replaceBriefItems(brief.getId(), date, parsedBrief.items());

        Map<String, Object> after = toDetailView(brief, false);
        if (operatorUserId != null) {
            adminAuditService.log(operatorUserId, "admin.ai_brief.refresh", "daily_ai_brief", brief.getId(), null, after, ip, userAgent);
        }

        return after;
    }

    /**
     * 执行 Python 抓取脚本。
     * 输出必须是 JSON 文本，超时或非0退出码都视为失败。
     */
    private ScriptResult runPythonScript(LocalDate date) throws Exception {
        int limit = fetchLimit == null || fetchLimit < 1 ? 8 : Math.min(fetchLimit, 20);
        int timeout = timeoutSeconds == null || timeoutSeconds < 5 ? 45 : timeoutSeconds;
        String resolvedScriptPath = resolveScriptPath();
        List<List<String>> interpreterCandidates = buildInterpreterCandidates();
        List<String> failureReasons = new ArrayList<>();

        // 解释器兜底策略：依次尝试配置解释器、python、py -3、python3，避免不同机器命令差异导致补跑失败。
        for (List<String> interpreterPrefix : interpreterCandidates) {
            List<String> command = new ArrayList<>(interpreterPrefix);
            command.add(resolvedScriptPath);
            command.add("--date");
            command.add(date.toString());
            command.add("--limit");
            command.add(String.valueOf(limit));

            try {
                ScriptResult result = executeScriptCommand(command, timeout);
                log.info("AI快讯脚本执行成功，interpreter={}, script={}, exitCode={}",
                        String.join(" ", interpreterPrefix), resolvedScriptPath, result.exitCode());
                return result;
            } catch (Exception ex) {
                String interpreterLabel = String.join(" ", interpreterPrefix);
                String reason = truncate(ex.getMessage(), 200);
                failureReasons.add(interpreterLabel + " => " + reason);
                log.warn("AI快讯脚本执行失败，interpreter={}, reason={}", interpreterLabel, reason);
            }
        }

        throw new IllegalStateException("未找到可用Python解释器，尝试结果：" + String.join(" | ", failureReasons));
    }

    /**
     * 执行具体命令并返回脚本输出。
     */
    private ScriptResult executeScriptCommand(List<String> command, int timeoutSeconds) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        // 强制 Python 进程使用 UTF-8 I/O，避免 Windows 默认编码导致中文乱码。
        processBuilder.environment().put("PYTHONUTF8", "1");
        processBuilder.environment().put("PYTHONIOENCODING", "UTF-8");

        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Python脚本执行超时");
        }

        int exitCode = process.exitValue();
        String outputText = output.toString().trim();
        if (exitCode != 0) {
            throw new IllegalStateException("Python脚本执行失败，退出码=" + exitCode + "，输出=" + truncate(outputText, 500));
        }
        if (outputText.isBlank()) {
            throw new IllegalStateException("Python脚本未返回有效内容");
        }
        return new ScriptResult(exitCode, outputText);
    }

    /**
     * 构建解释器候选列表：
     * - 优先使用配置项 ai-brief.python-path；
     * - 再回退常见命令，提升在 Windows/macOS/Linux 上的兼容性。
     */
    private List<List<String>> buildInterpreterCandidates() {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (pythonPath != null && !pythonPath.trim().isEmpty()) {
            String normalized = pythonPath.trim();
            // 支持配置值里带外层引号（例如 "C:\\Python\\python.exe"）。
            if ((normalized.startsWith("\"") && normalized.endsWith("\"")) ||
                    (normalized.startsWith("'") && normalized.endsWith("'"))) {
                normalized = normalized.substring(1, normalized.length() - 1);
            }
            candidates.add(normalized);
        }
        candidates.add("python");
        candidates.add("py");
        candidates.add("python3");

        List<List<String>> result = new ArrayList<>();
        for (String candidate : candidates) {
            if ("py".equalsIgnoreCase(candidate)) {
                // Windows Python Launcher 建议带 -3，避免落到 Python2 或错误解释器。
                result.add(List.of("py", "-3"));
                result.add(List.of("py"));
                continue;
            }
            result.add(List.of(candidate));
        }
        return result;
    }

    /**
     * 解析 Python 输出。
     * 为了兼容脚本中可能带有日志前缀，这里会自动提取最外层 JSON。
     */
    private ParsedBrief parseScriptPayload(String output, LocalDate targetDate) {
        try {
            String payload = extractJsonPayload(output);
            JsonNode root = objectMapper.readTree(payload);

            String title = trimOrDefault(root.path("title").asText(null), "每日AI快讯（" + targetDate + "）", 120);
            String summary = trimOrDefault(root.path("summary").asText(null), "已为你整理今日AI热点", 800);
            int sourceCount = Math.max(0, root.path("sourceCount").asInt(0));

            JsonNode itemsNode = root.path("items");
            if (!itemsNode.isArray() || itemsNode.isEmpty()) {
                throw new IllegalStateException("抓取结果为空，未返回热点条目");
            }

            List<ParsedItem> items = new ArrayList<>();
            int rank = 1;
            for (JsonNode node : itemsNode) {
                String itemTitle = trimOrDefault(node.path("title").asText(null), null, 300);
                if (itemTitle == null) {
                    continue;
                }
                ParsedItem item = new ParsedItem(
                        rank++,
                        parseScore(node.path("score")),
                        itemTitle,
                        trimOrDefault(node.path("summary").asText(null), null, 2000),
                        trimOrDefault(node.path("sourceName").asText(null), "未知来源", 120),
                        trimOrDefault(node.path("sourceUrl").asText(null), null, 600),
                        parseDateTime(node.path("eventTime").asText(null))
                );
                items.add(item);
            }

            if (items.isEmpty()) {
                throw new IllegalStateException("抓取结果不合法：热点标题全部为空");
            }

            return new ParsedBrief(title, summary, sourceCount, items);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, 502, "AI快讯解析失败：" + truncate(ex.getMessage(), 180));
        }
    }

    /**
     * 使用“先删后插”方式更新条目明细，保证排序与内容完全一致。
     */
    private void replaceBriefItems(Long briefId, LocalDate date, List<ParsedItem> items) {
        LambdaQueryWrapper<DailyAiBriefItem> removeWrapper = new LambdaQueryWrapper<>();
        removeWrapper.eq(DailyAiBriefItem::getBriefId, briefId);
        dailyAiBriefItemMapper.delete(removeWrapper);

        for (ParsedItem parsedItem : items) {
            DailyAiBriefItem item = new DailyAiBriefItem();
            item.setBriefId(briefId);
            item.setBriefDate(date);
            item.setRankOrder(parsedItem.rankOrder());
            item.setHotScore(parsedItem.score());
            item.setTitle(parsedItem.title());
            item.setSummary(parsedItem.summary());
            item.setSourceName(parsedItem.sourceName());
            item.setSourceUrl(parsedItem.sourceUrl());
            item.setEventTime(parsedItem.eventTime());
            item.setCreateTime(LocalDateTime.now());
            dailyAiBriefItemMapper.insert(item);
        }
    }

    /**
     * 主表采用按日期幂等更新：同一天重复执行只会覆盖，不会新增多条记录。
     */
    private DailyAiBrief upsertBrief(LocalDate date, ParsedBrief parsedBrief, LocalDateTime now) {
        DailyAiBrief existing = findByDate(date);
        if (existing == null) {
            DailyAiBrief created = new DailyAiBrief();
            created.setBriefDate(date);
            created.setTitle(parsedBrief.title());
            created.setSummary(parsedBrief.summary());
            created.setStatus(STATUS_READY);
            created.setSourceCount(parsedBrief.sourceCount());
            created.setItemCount(parsedBrief.items().size());
            created.setGeneratedAt(now);
            created.setCreateTime(now);
            created.setUpdateTime(now);
            dailyAiBriefMapper.insert(created);
            return created;
        }

        existing.setTitle(parsedBrief.title());
        existing.setSummary(parsedBrief.summary());
        existing.setStatus(STATUS_READY);
        existing.setSourceCount(parsedBrief.sourceCount());
        existing.setItemCount(parsedBrief.items().size());
        existing.setGeneratedAt(now);
        existing.setUpdateTime(now);
        dailyAiBriefMapper.updateById(existing);
        return existing;
    }

    /**
     * 抓取失败时仍落一条 failed 记录，便于管理端排障与审计。
     */
    private void saveFailureBrief(LocalDate date, LocalDateTime now) {
        DailyAiBrief existing = findByDate(date);
        if (existing == null) {
            DailyAiBrief failed = new DailyAiBrief();
            failed.setBriefDate(date);
            failed.setTitle("每日AI快讯（" + date + "）");
            failed.setSummary("抓取失败，请查看服务日志");
            failed.setStatus(STATUS_FAILED);
            failed.setSourceCount(0);
            failed.setItemCount(0);
            failed.setGeneratedAt(now);
            failed.setCreateTime(now);
            failed.setUpdateTime(now);
            dailyAiBriefMapper.insert(failed);
            return;
        }

        existing.setStatus(STATUS_FAILED);
        existing.setSummary("抓取失败，请查看服务日志");
        existing.setGeneratedAt(now);
        existing.setUpdateTime(now);
        dailyAiBriefMapper.updateById(existing);
    }

    /**
     * 组装快讯详情给前端展示。
     */
    private Map<String, Object> toDetailView(DailyAiBrief brief, boolean fallback) {
        LambdaQueryWrapper<DailyAiBriefItem> itemQuery = new LambdaQueryWrapper<>();
        itemQuery.eq(DailyAiBriefItem::getBriefId, brief.getId())
                .orderByAsc(DailyAiBriefItem::getRankOrder)
                .orderByAsc(DailyAiBriefItem::getId);

        List<Map<String, Object>> items = dailyAiBriefItemMapper.selectList(itemQuery).stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", item.getRankOrder());
            row.put("score", item.getHotScore());
            row.put("title", item.getTitle());
            row.put("summary", item.getSummary());
            row.put("sourceName", item.getSourceName());
            row.put("sourceUrl", item.getSourceUrl());
            row.put("eventTime", item.getEventTime());
            return row;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", brief.getId());
        result.put("date", brief.getBriefDate());
        result.put("title", brief.getTitle());
        result.put("summary", brief.getSummary());
        result.put("status", brief.getStatus());
        result.put("sourceCount", brief.getSourceCount());
        result.put("itemCount", brief.getItemCount());
        result.put("generatedAt", brief.getGeneratedAt());
        result.put("isFallback", fallback);
        result.put("items", items);
        return result;
    }

    private DailyAiBrief findByDate(LocalDate date) {
        LambdaQueryWrapper<DailyAiBrief> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DailyAiBrief::getBriefDate, date).last("LIMIT 1");
        return dailyAiBriefMapper.selectOne(queryWrapper);
    }

    private DailyAiBrief findLatestReadyBrief(LocalDate upToDate) {
        LambdaQueryWrapper<DailyAiBrief> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DailyAiBrief::getStatus, STATUS_READY)
                .le(DailyAiBrief::getBriefDate, upToDate)
                .orderByDesc(DailyAiBrief::getBriefDate)
                .last("LIMIT 1");
        return dailyAiBriefMapper.selectOne(queryWrapper);
    }

    private int normalizeDays(Integer days) {
        if (days == null || days < 1) {
            return DEFAULT_HISTORY_DAYS;
        }
        return Math.min(days, MAX_HISTORY_DAYS);
    }

    private String extractJsonPayload(String raw) {
        String text = raw == null ? "" : raw.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalStateException("脚本输出中未找到合法JSON");
        }
        return text.substring(start, end + 1);
    }

    private BigDecimal parseScore(JsonNode scoreNode) {
        if (scoreNode == null || scoreNode.isNull()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(scoreNode.asText("0")).max(BigDecimal.ZERO);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim();
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // 继续尝试下一个格式。
            }
        }

        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String trimOrDefault(String value, String defaultValue, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        String trimmed = value.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    /**
     * 解析脚本路径：
     * 1. 优先使用配置路径；
     * 2. 若后端以 backend/ 为工作目录启动，则尝试 ../scripts 兜底；
     * 3. 都不存在时返回原配置并在后续执行阶段抛错。
     */
    private String resolveScriptPath() {
        Path direct = Paths.get(scriptPath);
        if (Files.exists(direct)) {
            return direct.toAbsolutePath().toString();
        }

        Path parentCandidate = Paths.get("..").resolve(scriptPath).normalize();
        if (Files.exists(parentCandidate)) {
            return parentCandidate.toAbsolutePath().toString();
        }

        return scriptPath;
    }

    /** 脚本执行结果。 */
    private record ScriptResult(int exitCode, String output) {
    }

    /** 解析后的快讯结构。 */
    private record ParsedBrief(String title, String summary, int sourceCount, List<ParsedItem> items) {
    }

    /** 解析后的热点条目。 */
    private record ParsedItem(int rankOrder,
                              BigDecimal score,
                              String title,
                              String summary,
                              String sourceName,
                              String sourceUrl,
                              LocalDateTime eventTime) {
    }
}

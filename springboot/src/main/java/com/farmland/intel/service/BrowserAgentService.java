package com.farmland.intel.service;

import com.farmland.intel.entity.Statistic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class BrowserAgentService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_SESSIONS = 20;
    private static final String AGENT_MODE = "business_agent";

    @Value("${browser-agent.artifact-dir:${java.io.tmpdir}/bangbang-agro/browser-agent}")
    private String artifactDir;

    @Resource
    private IStatisticService statisticService;

    @Resource
    private AutoPatrolService autoPatrolService;

    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "browser-agent-runner");
        thread.setDaemon(true);
        return thread;
    });

    public Map<String, Object> startPatrol(String operator) {
        cleanupOldSessions();

        String sessionId = UUID.randomUUID().toString();
        AgentSession session = new AgentSession(sessionId, operator);
        sessions.put(sessionId, session);

        executor.submit(() -> runPatrol(session));
        return session.toMap();
    }

    public Map<String, Object> getSession(String sessionId) {
        AgentSession session = sessions.get(sessionId);
        return session == null ? null : session.toMap();
    }

    public Map<String, Object> getLatestSession() {
        return sessions.values().stream()
                .max((a, b) -> a.startedAt.compareTo(b.startedAt))
                .map(AgentSession::toMap)
                .orElse(null);
    }

    private void runPatrol(AgentSession session) {
        try {
            session.status = "running";
            session.mode = AGENT_MODE;
            session.artifactBaseUrl = "/api/browser-agent/sessions/" + session.sessionId + "/artifacts/";
            emit(session, 8, "open-dashboard", "总控台", "/unmanned-dashboard",
                    "打开无人农场总控台",
                    "进入总控仪表盘，读取巡检状态、待处理事件和近期决策链。",
                    List.of("运行状态", "任务队列", "决策链"));
            sleep(800);

            List<Statistic> farms = loadFarms();
            FarmSnapshot snapshot = summarizeFarms(farms);
            emit(session, 28, "read-sensors", "环境监测", "/dashbordnew",
                    "读取环境监测数据",
                    String.format("读取 %d 块农田数据：平均温度 %s°C，低土壤湿度 %d 块，高温 %d 块，弱光 %d 块。",
                            snapshot.totalFarms, snapshot.avgTemperatureText,
                            snapshot.lowSoilHumidityCount, snapshot.highTemperatureCount, snapshot.lowLightCount),
                    List.of(
                            "农田 " + snapshot.totalFarms + " 块",
                            "均温 " + snapshot.avgTemperatureText + "°C",
                            "低湿 " + snapshot.lowSoilHumidityCount + " 块",
                            "弱光 " + snapshot.lowLightCount + " 块"
                    ));
            sleep(900);

            emit(session, 48, "inspect-vision", "视觉巡检", "/fruit-detect",
                    "查看视觉巡检结果",
                    "打开视觉巡检与异常识别页面，复核最近图像识别结果并等待高风险目标。",
                    List.of("图像识别", "病虫害风险", "成熟度"));
            sleep(850);

            emit(session, 66, "check-map", "GIS 指挥", "/farm-map-gaode",
                    "定位地块与巡检区域",
                    String.format("打开 GIS 地块指挥页，关联 %d 块地块的空间位置与巡检范围。", snapshot.totalFarms),
                    List.of("地块位置", "巡检轨迹", "区域状态"));
            sleep(850);

            emit(session, 82, "execute-policy", "策略执行", "/auto-patrol",
                    "执行自主巡检策略",
                    "调用自主巡检规则引擎，执行灌溉、补光、通知和 AI 分析等白名单动作。",
                    List.of("规则引擎", "Agent 决策", "执行日志"));

            Map<String, Object> result = autoPatrolService.doPatrol("browser_agent");
            session.finalResult = result;
            session.progress = 100;
            session.status = "completed";
            session.finishedAt = LocalDateTime.now();
            addEvent(session, "execute-policy", "策略执行", "/auto-patrol",
                    "巡检执行完成",
                    String.format("检查 %s 块农田，执行 %s 项操作，新增 %s 个事件。",
                            valueOrZero(result.get("farmsChecked")),
                            valueOrZero(result.get("actionsExecuted")),
                            valueOrZero(result.get("newEvents"))),
                    "completed",
                    List.of("完成", "日志已写入", "结果已回传"));
        } catch (Exception e) {
            log.warn("巡检智能体执行失败", e);
            session.status = "failed";
            session.finishedAt = LocalDateTime.now();
            session.errorMessage = e.getMessage();
            addEvent(session, "failed", "异常", "/auto-patrol",
                    "巡检智能体执行失败",
                    e.getMessage() != null ? e.getMessage() : "未知异常",
                    "failed",
                    List.of("执行中断"));
        }
    }

    private List<Statistic> loadFarms() {
        try {
            List<Statistic> farms = statisticService.list();
            return farms != null ? farms : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private FarmSnapshot summarizeFarms(List<Statistic> farms) {
        FarmSnapshot snapshot = new FarmSnapshot();
        snapshot.totalFarms = farms.size();
        BigDecimal totalTemp = BigDecimal.ZERO;
        int tempCount = 0;
        int soilHumidityWarn = autoPatrolService.getSoilHumidityWarn();
        double temperatureWarn = autoPatrolService.getTemperatureWarn();
        int lightWarn = autoPatrolService.getLightWarn();

        for (Statistic farm : farms) {
            if (farm.getTemperature() != null) {
                totalTemp = totalTemp.add(farm.getTemperature());
                tempCount++;
                if (farm.getTemperature().compareTo(BigDecimal.valueOf(temperatureWarn)) > 0) {
                    snapshot.highTemperatureCount++;
                }
            }
            if (farm.getSoilhumidity() != null && farm.getSoilhumidity() < soilHumidityWarn) {
                snapshot.lowSoilHumidityCount++;
            }
            if (farm.getLight() != null && farm.getLight() < lightWarn) {
                snapshot.lowLightCount++;
            }
        }

        if (tempCount == 0) {
            snapshot.avgTemperatureText = "--";
        } else {
            snapshot.avgTemperatureText = totalTemp
                    .divide(BigDecimal.valueOf(tempCount), 1, java.math.RoundingMode.HALF_UP)
                    .toPlainString();
        }
        return snapshot;
    }

    private void emit(AgentSession session, int progress, String stepKey, String module, String path,
                      String title, String detail, List<String> signals) {
        session.progress = progress;
        addEvent(session, stepKey, module, path, title, detail, "running", signals);
    }

    private void addEvent(AgentSession session, String stepKey, String module, String path,
                          String title, String detail, String state, List<String> signals) {
        int sequence = session.nextEvidenceSequence();
        String artifactName = writeEvidenceCard(session, sequence, stepKey, module, path, title, detail, state, signals);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("id", UUID.randomUUID().toString());
        event.put("time", LocalDateTime.now().format(TIME_FORMATTER));
        event.put("sequence", sequence);
        event.put("stepKey", stepKey);
        event.put("module", module);
        event.put("path", path);
        event.put("title", title);
        event.put("detail", detail);
        event.put("state", state);
        event.put("progress", session.progress);
        event.put("signals", signals);
        event.put("mode", session.mode);
        if (artifactName != null) {
            String artifactUrl = session.artifactBaseUrl + artifactName;
            event.put("artifactType", "execution_snapshot");
            event.put("artifactName", artifactName);
            event.put("screenshotUrl", artifactUrl);
            event.put("artifactUrl", artifactUrl);
        }
        session.events.add(event);
    }

    public byte[] getArtifact(String sessionId, String artifactName) {
        AgentSession session = sessions.get(sessionId);
        if (session == null || artifactName == null || artifactName.contains("..")
                || artifactName.contains("/") || artifactName.contains("\\")) {
            return null;
        }
        Path path = sessionArtifactDir(sessionId).resolve(artifactName).normalize();
        if (!path.startsWith(sessionArtifactDir(sessionId)) || !Files.exists(path)) {
            return null;
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            return null;
        }
    }

    private String writeEvidenceCard(AgentSession session, int sequence, String stepKey, String module, String path,
                                     String title, String detail, String state, List<String> signals) {
        try {
            Path dir = sessionArtifactDir(session.sessionId);
            Files.createDirectories(dir);
            String artifactName = String.format("%02d-%s-%s.svg", sequence, safeArtifactPart(stepKey), safeArtifactPart(state));
            String svg = buildEvidenceSvg(session, sequence, module, path, title, detail, state, signals);
            Files.writeString(dir.resolve(artifactName), svg, StandardCharsets.UTF_8);
            return artifactName;
        } catch (Exception e) {
            log.debug("生成智能体证据图失败: {}", e.getMessage());
            return null;
        }
    }

    private Path sessionArtifactDir(String sessionId) {
        return Paths.get(artifactDir).toAbsolutePath().normalize().resolve(sessionId).normalize();
    }

    private String buildEvidenceSvg(AgentSession session, int sequence, String module, String path, String title,
                                    String detail, String state, List<String> signals) {
        String signalText = String.join(" / ", signals == null ? Collections.emptyList() : signals);
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String accent = "failed".equals(state) ? "#ef4444" : "completed".equals(state) ? "#16a34a" : "#0f9f66";
        String badgeSvg = buildSignalBadges(signals);
        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="960" height="540" viewBox="0 0 960 540">
                  <defs>
                    <linearGradient id="bg" x1="0" x2="1" y1="0" y2="1">
                      <stop offset="0" stop-color="#f5faf7"/>
                      <stop offset="1" stop-color="#eaf5ee"/>
                    </linearGradient>
                    <linearGradient id="hero" x1="0" x2="1" y1="0" y2="1">
                      <stop offset="0" stop-color="#0b3f2c"/>
                      <stop offset="0.62" stop-color="#0f7a4d"/>
                      <stop offset="1" stop-color="#38a169"/>
                    </linearGradient>
                    <filter id="shadow" x="0" y="0" width="960" height="540" filterUnits="userSpaceOnUse">
                      <feDropShadow dx="0" dy="14" stdDeviation="16" flood-color="#0b2a1c" flood-opacity="0.16"/>
                    </filter>
                  </defs>
                  <rect width="960" height="540" fill="url(#bg)"/>
                  <rect x="34" y="32" width="892" height="476" rx="18" fill="#ffffff" filter="url(#shadow)"/>
                  <rect x="34" y="32" width="892" height="92" rx="18" fill="url(#hero)"/>
                  <rect x="34" y="96" width="892" height="28" fill="#0f7a4d"/>
                  <circle cx="74" cy="76" r="8" fill="#ff6b6b"/>
                  <circle cx="100" cy="76" r="8" fill="#f6c453"/>
                  <circle cx="126" cy="76" r="8" fill="#50d890"/>
                  <text x="158" y="83" fill="#dffbea" font-size="19" font-family="Microsoft YaHei, Arial">%s</text>
                  <rect x="736" y="56" width="146" height="38" rx="19" fill="%s" fill-opacity="0.96"/>
                  <text x="774" y="81" fill="#ffffff" font-size="16" font-weight="700" font-family="Microsoft YaHei, Arial">%s</text>
                  <text x="72" y="168" fill="#0f7a4d" font-size="16" font-weight="700" font-family="Microsoft YaHei, Arial">第 %02d 步 · %s</text>
                  <text x="72" y="218" fill="#10231a" font-size="38" font-weight="800" font-family="Microsoft YaHei, Arial">%s</text>
                  <foreignObject x="72" y="242" width="816" height="82">
                    <div xmlns="http://www.w3.org/1999/xhtml" style="font-family:'Microsoft YaHei',Arial;color:#52645a;font-size:20px;line-height:1.55;">%s</div>
                  </foreignObject>
                  <rect x="72" y="352" width="816" height="76" rx="12" fill="#f5faf7"/>
                  <text x="96" y="382" fill="#6b7d72" font-size="15" font-weight="700" font-family="Microsoft YaHei, Arial">关键信号</text>
                  <text x="96" y="409" fill="#24382e" font-size="18" font-family="Microsoft YaHei, Arial">%s</text>
                  %s
                  <text x="72" y="468" fill="#7b8b82" font-size="15" font-family="Microsoft YaHei, Arial">会话 %s · 操作人 %s · %s · 进度 %d%%</text>
                </svg>
                """.formatted(
                escapeXml(path),
                accent,
                escapeXml(stateLabel(state)),
                sequence,
                escapeXml(module + " / " + modeLabel(session.mode)),
                escapeXml(title),
                escapeXml(truncate(detail, 120)),
                escapeXml(signalText),
                badgeSvg,
                escapeXml(session.sessionId.substring(0, Math.min(8, session.sessionId.length()))),
                escapeXml(session.operator),
                escapeXml(time),
                session.progress
        );
    }

    private String buildSignalBadges(List<String> signals) {
        if (signals == null || signals.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int x = 72;
        int y = 452;
        int count = Math.min(signals.size(), 4);
        for (int i = 0; i < count; i++) {
            String signal = truncate(signals.get(i), 12);
            if (signal == null || signal.isBlank()) {
                continue;
            }
            int width = Math.max(82, Math.min(160, 34 + signal.length() * 14));
            builder.append(String.format(
                    "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"30\" rx=\"15\" fill=\"#ecfdf3\"/>"
                            + "<text x=\"%d\" y=\"%d\" fill=\"#0f7a4d\" font-size=\"14\" font-weight=\"700\" font-family=\"Microsoft YaHei, Arial\">%s</text>",
                    x, y, width, x + 16, y + 20, escapeXml(signal)));
            x += width + 10;
        }
        return builder.toString();
    }

    private String stateLabel(String state) {
        if ("completed".equals(state)) return "已完成";
        if ("failed".equals(state)) return "失败";
        return "执行中";
    }

    private String modeLabel(String mode) {
        if ("playwright".equals(mode)) return "浏览器实控";
        if ("controlled".equals(mode) || "business_agent".equals(mode)) return "业务智能体";
        return "智能体";
    }

    private String safeArtifactPart(String value) {
        if (value == null || value.isBlank()) {
            return "event";
        }
        return value.replaceAll("[^a-zA-Z0-9_-]", "-");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String valueOrZero(Object value) {
        return value == null ? "0" : String.valueOf(value);
    }

    private void cleanupOldSessions() {
        if (sessions.size() < MAX_SESSIONS) {
            return;
        }
        sessions.values().stream()
                .min((a, b) -> a.startedAt.compareTo(b.startedAt))
                .ifPresent(oldest -> sessions.remove(oldest.sessionId));
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private static class FarmSnapshot {
        int totalFarms;
        int lowSoilHumidityCount;
        int highTemperatureCount;
        int lowLightCount;
        String avgTemperatureText = "--";
    }

    private static class AgentSession {
        private final String sessionId;
        private final String operator;
        private final LocalDateTime startedAt;
        private volatile LocalDateTime finishedAt;
        private volatile String status = "queued";
        private volatile String mode = AGENT_MODE;
        private volatile String artifactBaseUrl;
        private volatile int progress = 0;
        private volatile String errorMessage;
        private volatile Map<String, Object> finalResult;
        private int evidenceSequence = 0;
        private final List<Map<String, Object>> events = Collections.synchronizedList(new ArrayList<>());

        AgentSession(String sessionId, String operator) {
            this.sessionId = sessionId;
            this.operator = operator;
            this.startedAt = LocalDateTime.now();
        }

        synchronized int nextEvidenceSequence() {
            evidenceSequence++;
            return evidenceSequence;
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sessionId", sessionId);
            map.put("operator", operator);
            map.put("status", status);
            map.put("mode", mode);
            map.put("progress", progress);
            map.put("startedAt", startedAt.toString());
            map.put("finishedAt", finishedAt != null ? finishedAt.toString() : null);
            map.put("errorMessage", errorMessage);
            map.put("artifactBaseUrl", artifactBaseUrl);
            map.put("finalResult", finalResult);
            synchronized (events) {
                map.put("events", new ArrayList<>(events));
            }
            return map;
        }
    }
}

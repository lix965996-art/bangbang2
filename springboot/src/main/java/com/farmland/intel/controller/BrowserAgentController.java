package com.farmland.intel.controller;

import com.farmland.intel.common.Constants;
import com.farmland.intel.common.Result;
import com.farmland.intel.config.interceptor.AuthAccess;
import com.farmland.intel.entity.User;
import com.farmland.intel.service.BrowserAgentService;
import com.farmland.intel.utils.TokenUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/browser-agent")
public class BrowserAgentController {

    @Resource
    private BrowserAgentService browserAgentService;

    @PostMapping("/patrol/run")
    public Result runPatrol() {
        User user = TokenUtils.getCurrentUser();
        if (user == null) {
            return Result.error(Constants.CODE_401, "未登录");
        }
        return Result.success(browserAgentService.startPatrol(user.getUsername()));
    }

    @GetMapping("/sessions/latest")
    public Result latestSession() {
        Map<String, Object> session = browserAgentService.getLatestSession();
        return Result.success(session);
    }

    @GetMapping("/sessions/{sessionId}")
    public Result getSession(@PathVariable String sessionId) {
        Map<String, Object> session = browserAgentService.getSession(sessionId);
        if (session == null) {
            return Result.error(Constants.CODE_404, "智能体会话不存在");
        }
        return Result.success(session);
    }

    @AuthAccess
    @GetMapping(value = "/sessions/{sessionId}/artifacts/{artifactName}", produces = "image/svg+xml")
    public ResponseEntity<byte[]> getArtifact(@PathVariable String sessionId, @PathVariable String artifactName) {
        byte[] bytes = browserAgentService.getArtifact(sessionId, artifactName);
        if (bytes == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(bytes);
    }
}

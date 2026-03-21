package com.farmland.intel.controller;

import com.farmland.intel.agent.AgentAction;
import com.farmland.intel.agent.AgentActionResult;
import com.farmland.intel.agent.AgentPlan;
import com.farmland.intel.common.Constants;
import com.farmland.intel.common.Result;
import com.farmland.intel.service.AgentService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agent 编排接口：
 * 1) /api/agent/plan 生成计划（仅建议 + 动作列表），需要前端确认。
 * 2) /api/agent/execute 执行已确认的动作（受白名单限制）。
 */
@RestController
@RequestMapping("/api/agent")
@CrossOrigin
@Slf4j
public class AgentController {

    @Autowired
    private AgentService agentService;

    @PostMapping("/plan")
    public Result plan(@RequestBody Map<String, Object> payload) {
        String question = payload == null ? null : (String) payload.get("question");
        if (question == null || question.trim().isEmpty()) {
            return Result.error(Constants.CODE_400, "question 不能为空");
        }

        AgentPlan plan = agentService.buildPlan(question);
        // 确保每个动作有唯一 id
        plan.getActions().forEach(a -> {
            if (a.getId() == null || a.getId().trim().isEmpty()) {
                a.setId("act-" + UUID.randomUUID());
            }
        });
        return Result.success(plan);
    }

    @PostMapping("/execute")
    public Result execute(@RequestBody ExecuteRequest request) {
        if (request == null || request.getActions() == null || request.getActions().isEmpty()) {
            return Result.error(Constants.CODE_400, "actions 不能为空");
        }

        // 只保留白名单类型
        List<AgentAction> actions = request.getActions().stream()
                .filter(a -> a != null && a.getType() != null)
                .collect(Collectors.toList());

        List<AgentActionResult> results = agentService.executeActions(actions);
        return Result.success(results);
    }

    @Data
    private static class ExecuteRequest {
        private List<AgentAction> actions;
    }
}

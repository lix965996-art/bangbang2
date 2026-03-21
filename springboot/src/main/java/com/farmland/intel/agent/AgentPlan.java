package com.farmland.intel.agent;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 规划结果：建议文本 + 动作列表（待用户确认）。
 */
@Data
public class AgentPlan {
    private String advice;
    private List<AgentAction> actions = new ArrayList<>();
}

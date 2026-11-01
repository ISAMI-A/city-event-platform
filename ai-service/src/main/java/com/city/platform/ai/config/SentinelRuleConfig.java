package com.city.platform.ai.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * AI 分析调用保护：
 * - 限流：5 QPS，避免瞬时打满模型配额；
 * - 熔断：慢调用比例（RT > 3000ms 且比例 >= 50%，10 秒窗口，最少 2 次），熔断 10 秒。
 */
@Configuration
public class SentinelRuleConfig {

    @PostConstruct
    public void initRules() {
        FlowRule flowRule = new FlowRule("aiAnalyze");
        flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        flowRule.setCount(5);
        flowRule.setLimitApp("default");

        // 视觉核实调用成本更高，单独限制 3 QPS
        FlowRule verifyFlowRule = new FlowRule("aiVerifyRectify");
        verifyFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        verifyFlowRule.setCount(3);
        verifyFlowRule.setLimitApp("default");

        // 紧急度复评每次上报都会触发，限制 5 QPS 防止批量上报打满配额
        FlowRule reviewFlowRule = new FlowRule("aiReviewEvent");
        reviewFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        reviewFlowRule.setCount(5);
        reviewFlowRule.setLimitApp("default");
        FlowRuleManager.loadRules(List.of(flowRule, verifyFlowRule, reviewFlowRule));

        DegradeRule degradeRule = new DegradeRule("aiAnalyze");
        degradeRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        degradeRule.setCount(3000);
        degradeRule.setSlowRatioThreshold(1.0);
        // 单次调用耗时较长，10 秒统计窗口内请求数少，最小请求数设为 1，保证单个慢调用即可熔断
        degradeRule.setMinRequestAmount(1);
        degradeRule.setStatIntervalMs(10000);
        degradeRule.setTimeWindow(10);

        // 视觉模型耗时更长，慢调用阈值放宽到 15 秒
        DegradeRule verifyDegradeRule = new DegradeRule("aiVerifyRectify");
        verifyDegradeRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        verifyDegradeRule.setCount(15000);
        verifyDegradeRule.setSlowRatioThreshold(1.0);
        verifyDegradeRule.setMinRequestAmount(1);
        verifyDegradeRule.setStatIntervalMs(10000);
        verifyDegradeRule.setTimeWindow(15);

        DegradeRule reviewDegradeRule = new DegradeRule("aiReviewEvent");
        reviewDegradeRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        reviewDegradeRule.setCount(8000);
        reviewDegradeRule.setSlowRatioThreshold(1.0);
        reviewDegradeRule.setMinRequestAmount(1);
        reviewDegradeRule.setStatIntervalMs(10000);
        reviewDegradeRule.setTimeWindow(15);
        DegradeRuleManager.loadRules(List.of(degradeRule, verifyDegradeRule, reviewDegradeRule));
    }
}

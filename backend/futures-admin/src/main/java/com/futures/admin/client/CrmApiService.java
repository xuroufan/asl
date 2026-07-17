package com.futures.admin.client;

import com.futures.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * CRM API 客户端 — 封装对客户关系管理系统的 REST 调用（降级模式）。
 * <p>提供客户信息管理、等级管理、标签管理、沟通记录、反馈处理等功能的模拟数据。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmApiService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DTF_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==================== 客户信息管理 ====================

    public Result<?> getCustomers(String keyword, String level, String kycStatus, String status, int page, int size) {
        return Result.success(buildMockCustomers(keyword, level, kycStatus, status, page));
    }

    public Result<?> getCustomerDetail(Long customerId) {
        return Result.success(buildMockCustomerDetail(customerId));
    }

    public Result<?> getCustomerPortfolio(Long customerId) {
        return Result.success(buildMockCustomerPortfolio(customerId));
    }

    // ==================== 客户等级管理 ====================

    public Result<?> getLevelDefinitions() {
        return Result.success(buildMockLevelDefinitions());
    }

    public Result<?> getLevelRules() {
        return Result.success(buildMockLevelRules());
    }

    public Result<?> getLevelHistory(Long customerId, int page, int size) {
        return Result.success(buildMockLevelHistory(customerId, page));
    }

    public Result<?> updateCustomerLevel(Long customerId, String newLevel, String reason) {
        log.info("更新客户等级: customerId={}, newLevel={}, reason={}", customerId, newLevel, reason);
        return Result.success("客户等级已更新为 " + newLevel);
    }

    // ==================== 客户标签管理 ====================

    public Result<?> getTags(String category) {
        return Result.success(buildMockTags(category));
    }

    public Result<?> createTag(Map<String, Object> tag) {
        log.info("创建标签: {}", tag);
        return Result.success("标签创建成功");
    }

    public Result<?> deleteTag(Long tagId) {
        log.info("删除标签: {}", tagId);
        return Result.success("标签已删除");
    }

    public Result<?> getCustomerTags(Long customerId) {
        return Result.success(buildMockCustomerTags(customerId));
    }

    public Result<?> assignTagToCustomer(Long customerId, Long tagId) {
        log.info("为客户打标签: customerId={}, tagId={}", customerId, tagId);
        return Result.success("标签已添加");
    }

    public Result<?> removeTagFromCustomer(Long customerId, Long tagId) {
        log.info("为客户去标签: customerId={}, tagId={}", customerId, tagId);
        return Result.success("标签已移除");
    }

    // ==================== 客户沟通记录 ====================

    public Result<?> getCommunications(Long customerId, int page, int size) {
        return Result.success(buildMockCommunications(customerId, page));
    }

    public Result<?> createCommunication(Map<String, Object> comm) {
        log.info("创建沟通记录: {}", comm);
        return Result.success("沟通记录已保存");
    }

    public Result<?> getFollowUpReminders(String staffName, int page, int size) {
        return Result.success(buildMockFollowUps(staffName, page));
    }

    // ==================== 客户反馈处理 ====================

    public Result<?> getFeedbacks(String status, String type, String assignee, int page, int size) {
        return Result.success(buildMockFeedbacks(status, type, assignee, page));
    }

    public Result<?> createFeedback(Map<String, Object> feedback) {
        log.info("创建反馈: {}", feedback);
        return Result.success("反馈已提交");
    }

    public Result<?> assignFeedback(Long feedbackId, String assignee) {
        log.info("分配反馈: feedbackId={}, assignee={}", feedbackId, assignee);
        return Result.success("反馈已分配给 " + assignee);
    }

    public Result<?> updateFeedbackStatus(Long feedbackId, String status, String resolution) {
        log.info("更新反馈状态: feedbackId={}, status={}, resolution={}", feedbackId, status, resolution);
        return Result.success("反馈状态已更新为 " + status);
    }

    public Result<?> getFeedbackStats() {
        return Result.success(buildMockFeedbackStats());
    }

    // ==================== 模拟数据 ====================

    private Map<String, Object> buildMockCustomers(String keyword, String level, String kycStatus, String status, int page) {
        String[] levels = {"普通", "白银", "黄金", "钻石"};
        String[] kycStatuses = {"已通过", "审核中", "未提交", "已拒绝"};
        String[] statuses = {"正常", "正常", "正常", "冻结", "正常", "正常", "正常", "正常"};
        String[] symbols = {"HSI2309", "ESM23", "GCQ23", "CLZ23"};

        List<Map<String, Object>> records = new ArrayList<>();
        int startIdx = (page - 1) * 10;
        for (int i = 1; i <= 10; i++) {
            int idx = startIdx + i;
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("customerId", 1000L + idx);
            c.put("username", "user_" + String.format("%04d", idx));
            c.put("realName", "测试客户" + String.format("%02d", idx));
            c.put("phone", "138" + String.format("%08d", 10000000 + idx));
            c.put("email", "user" + idx + "@example.com");
            c.put("level", levels[idx % 4]);
            c.put("kycStatus", kycStatuses[idx % 4]);
            c.put("status", statuses[idx % 8]);
            c.put("balance", BigDecimal.valueOf(50000 + idx * 15000).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
            c.put("available", BigDecimal.valueOf(30000 + idx * 10000).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
            c.put("totalTradeVolume", 100 + idx * 25);
            c.put("totalTradeAmount", BigDecimal.valueOf(500000 + idx * 150000).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
            c.put("openPositions", idx % 5);
            c.put("registerTime", LocalDate.now().minusDays(30 + idx * 3).format(DTF_DATE));
            c.put("lastTradeTime", LocalDateTime.now().minusHours(idx * 6).format(DTF));
            c.put("tags", List.of("VIP", idx % 3 == 0 ? "高频交易" : "长期持有"));
            records.add(c);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", 156);
        result.put("page", page);
        result.put("size", 10);
        return result;
    }

    private Map<String, Object> buildMockCustomerDetail(Long customerId) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("customerId", customerId);
        detail.put("username", "user_" + customerId);
        detail.put("realName", "测试客户" + (customerId % 100));
        detail.put("phone", "1381234" + String.format("%04d", customerId % 10000));
        detail.put("email", "user" + customerId + "@example.com");
        detail.put("idCardNo", "11010119900101" + String.format("%04d", customerId % 10000));
        detail.put("level", customerId % 4 == 0 ? "钻石" : (customerId % 3 == 0 ? "黄金" : (customerId % 2 == 0 ? "白银" : "普通")));
        detail.put("kycStatus", customerId % 4 == 3 ? "审核中" : "已通过");
        detail.put("status", customerId % 7 == 0 ? "冻结" : "正常");
        detail.put("registerTime", LocalDate.now().minusDays(60 + customerId * 2).format(DTF_DATE));
        detail.put("lastLoginTime", LocalDateTime.now().minusHours(customerId % 24).format(DTF));
        detail.put("totalDeposit", BigDecimal.valueOf(200000 + customerId * 5000).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
        detail.put("totalWithdraw", BigDecimal.valueOf(80000 + customerId * 2000).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
        detail.put("totalFee", BigDecimal.valueOf(12000 + customerId * 300).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
        detail.put("totalProfit", BigDecimal.valueOf(35000 + customerId * 800).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
        detail.put("riskRatio", BigDecimal.valueOf(45 + (customerId % 20)).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue());
        detail.put("tags", List.of("VIP", customerId % 3 == 0 ? "高频交易" : "长期持有", "自选客户"));
        detail.put("assignedStaff", customerId % 3 == 0 ? "客服张三" : (customerId % 2 == 0 ? "客服李四" : "客服王五"));
        detail.put("remark", "重要机构客户");
        return detail;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildMockCustomerPortfolio(Long customerId) {
        Map<String, Object> data = new LinkedHashMap<>();

        // 持仓
        data.put("positions", List.of(
                Map.of("symbol", "HSI2309", "direction", "多", "volume", 3, "avgPrice", 18450.00,
                        "currentPrice", 18520.00, "floatPnl", 2100.00, "margin", 37500.00),
                Map.of("symbol", "ESM23", "direction", "空", "volume", 2, "avgPrice", 4520.50,
                        "currentPrice", 4500.00, "floatPnl", 410.00, "margin", 18000.00),
                Map.of("symbol", "GCQ23", "direction", "多", "volume", 1, "avgPrice", 1980.20,
                        "currentPrice", 1975.00, "floatPnl", -520.00, "margin", 8000.00)
        ));

        // 近期交易
        data.put("recentTrades", List.of(
                Map.of("tradeId", "TRD20240712001", "symbol", "HSI2309", "direction", "买", "price", 18520.00, "volume", 1, "time", LocalDateTime.now().minusHours(2).format(DTF)),
                Map.of("tradeId", "TRD20240712002", "symbol", "ESM23", "direction", "卖", "price", 4500.00, "volume", 2, "time", LocalDateTime.now().minusHours(4).format(DTF)),
                Map.of("tradeId", "TRD20240711003", "symbol", "HSI2309", "direction", "买", "price", 18480.00, "volume", 2, "time", LocalDateTime.now().minusDays(1).format(DTF)),
                Map.of("tradeId", "TRD20240711004", "symbol", "GCQ23", "direction", "买", "price", 1975.50, "volume", 1, "time", LocalDateTime.now().minusDays(1).format(DTF))
        ));

        // 资金流水
        data.put("fundFlows", List.of(
                Map.of("flowId", "FL20240712001", "type", "入金", "amount", 200000.00, "balance", 523450.00, "time", LocalDateTime.now().minusHours(3).format(DTF)),
                Map.of("flowId", "FL20240711002", "type", "扣款", "amount", -37500.00, "balance", 323450.00, "time", LocalDateTime.now().minusDays(1).format(DTF)),
                Map.of("flowId", "FL20240710003", "type", "出金", "amount", -50000.00, "balance", 360950.00, "time", LocalDateTime.now().minusDays(2).format(DTF)),
                Map.of("flowId", "FL20240709004", "type", "入金", "amount", 100000.00, "balance", 410950.00, "time", LocalDateTime.now().minusDays(3).format(DTF))
        ));

        return data;
    }

    private List<Map<String, Object>> buildMockLevelDefinitions() {
        List<Map<String, Object>> defs = new ArrayList<>();
        defs.add(Map.of("levelId", 1, "levelName", "普通", "levelOrder", 1, "color", "#909399",
                "minVolume", 0, "minBalance", 0, "minTrades", 0, "description", "新注册或交易量较小的客户"));
        defs.add(Map.of("levelId", 2, "levelName", "白银", "levelOrder", 2, "color", "#C0C0C0",
                "minVolume", 100, "minBalance", 100000.00, "minTrades", 20, "description", "有稳定交易记录和一定资金量的客户"));
        defs.add(Map.of("levelId", 3, "levelName", "黄金", "levelOrder", 3, "color", "#E6A23C",
                "minVolume", 500, "minBalance", 500000.00, "minTrades", 100, "description", "交易活跃且资金量较大的重要客户"));
        defs.add(Map.of("levelId", 4, "levelName", "钻石", "levelOrder", 4, "color", "#409EFF",
                "minVolume", 2000, "minBalance", 2000000.00, "minTrades", 500, "description", "核心高端客户，享有专属服务和最优费率"));
        // Extra fields using HashMap
        for (int i = 0; i < defs.size(); i++) {
            Map<String, Object> m = new LinkedHashMap<>(defs.get(i));
            double[] discounts = {0.0, 0.1, 0.2, 0.3};
            int[] limits = {100, 200, 500, 1000};
            boolean[] supports = {false, false, false, true};
            String[] icons = {"CircleCheck", "Star", "Crown", "Coffee"};
            m.put("feeDiscount", discounts[i]);
            m.put("positionLimit", limits[i]);
            m.put("dedicatedSupport", supports[i]);
            m.put("icon", icons[i]);
            defs.set(i, m);
        }
        return defs;
    }

    private List<Map<String, Object>> buildMockLevelRules() {
        return List.of(
                Map.of("ruleId", 1, "ruleName", "交易量升级", "metric", "totalVolume", "operator", ">=",
                        "thresholds", Map.of("白银", 100, "黄金", 500, "钻石", 2000),
                        "autoUpgrade", true, "checkCycle", "MONTHLY", "enabled", true),
                Map.of("ruleId", 2, "ruleName", "资金量升级", "metric", "balance", "operator", ">=",
                        "thresholds", Map.of("白银", 100000.00, "黄金", 500000.00, "钻石", 2000000.00),
                        "autoUpgrade", true, "checkCycle", "DAILY", "enabled", true),
                Map.of("ruleId", 3, "ruleName", "交易次数升级", "metric", "totalTrades", "operator", ">=",
                        "thresholds", Map.of("白银", 20, "黄金", 100, "钻石", 500),
                        "autoUpgrade", false, "checkCycle", "QUARTERLY", "enabled", false)
        );
    }

    private List<Map<String, Object>> buildMockLevelHistory(Long customerId, int page) {
        List<Map<String, Object>> history = new ArrayList<>();
        String[] fromLevels = {"普通", "普通", "白银", "白银", "黄金"};
        String[] toLevels = {"白银", "白银", "黄金", "黄金", "钻石"};
        String[] reasons = {"累计交易量达到100手", "日均资金量超过10万", "累计交易量达到500手",
                "资金量达到50万", "综合评分达到钻石标准"};
        String[] operators = {"系统自动", "系统自动", "系统自动", "系统自动", "管理员张三"};

        for (int i = 0; i < Math.min(5, page * 5); i++) {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("historyId", (long) (page * 100 + i + 1));
            h.put("customerId", customerId);
            h.put("fromLevel", fromLevels[i]);
            h.put("toLevel", toLevels[i]);
            h.put("reason", reasons[i]);
            h.put("operator", operators[i]);
            h.put("changeTime", LocalDateTime.now().minusDays(30 * (5 - i)).format(DTF));
            history.add(h);
        }
        return history;
    }

    private List<Map<String, Object>> buildMockTags(String category) {
        List<Map<String, Object>> allTags = List.of(
                Map.of("tagId", 1, "tagName", "VIP客户", "category", "等级", "color", "#E6A23C", "customerCount", 56, "createdAt", "2024-01-15"),
                Map.of("tagId", 2, "tagName", "高频交易者", "category", "行为", "color", "#F56C6C", "customerCount", 128, "createdAt", "2024-01-20"),
                Map.of("tagId", 3, "tagName", "长期持有者", "category", "行为", "color", "#409EFF", "customerCount", 89, "createdAt", "2024-02-01"),
                Map.of("tagId", 4, "tagName", "机构客户", "category", "类型", "color", "#67C23A", "customerCount", 34, "createdAt", "2024-02-15"),
                Map.of("tagId", 5, "tagName", "风险偏好型", "category", "风险", "color", "#F56C6C", "customerCount", 72, "createdAt", "2024-03-01"),
                Map.of("tagId", 6, "tagName", "保守型投资者", "category", "风险", "color", "#909399", "customerCount", 45, "createdAt", "2024-03-10"),
                Map.of("tagId", 7, "tagName", "新注册客户", "category", "阶段", "color", "#409EFF", "customerCount", 210, "createdAt", "2024-04-01"),
                Map.of("tagId", 8, "tagName", "休眠客户", "category", "阶段", "color", "#909399", "customerCount", 68, "createdAt", "2024-04-15"),
                Map.of("tagId", 9, "tagName", "自选客户", "category", "自定义", "color", "#E6A23C", "customerCount", 23, "createdAt", "2024-05-01"),
                Map.of("tagId", 10, "tagName", "需跟进", "category", "自定义", "color", "#F56C6C", "customerCount", 15, "createdAt", "2024-05-10")
        );
        if (category != null && !category.isEmpty()) {
            return allTags.stream().filter(t -> category.equals(t.get("category"))).toList();
        }
        return allTags;
    }

    private List<Map<String, Object>> buildMockCustomerTags(Long customerId) {
        return List.of(
                Map.of("tagId", 1, "tagName", "VIP客户", "color", "#E6A23C", "assignedAt", LocalDateTime.now().minusDays(30).format(DTF)),
                Map.of("tagId", 2, "tagName", "高频交易者", "color", "#F56C6C", "assignedAt", LocalDateTime.now().minusDays(15).format(DTF)),
                Map.of("tagId", 9, "tagName", "自选客户", "color", "#E6A23C", "assignedAt", LocalDateTime.now().minusDays(7).format(DTF))
        );
    }

    private Map<String, Object> buildMockCommunications(Long customerId, int page) {
        String[] methods = {"电话", "在线聊天", "邮件", "面谈", "微信"};
        String[] contents = {
                "客户咨询HSI2309合约的交易时间和保证金要求，已详细解答",
                "回访客户了解近期交易情况，客户表示对平台服务满意",
                "客户反馈App登录闪退问题，已记录并提交技术部门处理",
                "向客户介绍新推出的钻石等级权益，客户表示有兴趣",
                "处理客户出金申请，指导客户完成银行信息确认",
                "客户咨询手续费优惠政策，已根据其等级说明折扣比例",
                "跟进客户KYC认证进度，提醒补充身份证背面照片"
        };
        String[] staffs = {"客服张三", "客服李四", "客服王五", "客服赵六"};

        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Map<String, Object> comm = new LinkedHashMap<>();
            int idx = (page - 1) * 8 + i;
            comm.put("commId", (long) (idx + 1));
            comm.put("customerId", customerId);
            comm.put("method", methods[i % methods.length]);
            comm.put("content", contents[i % contents.length]);
            comm.put("staff", staffs[i % staffs.length]);
            comm.put("contactTime", LocalDateTime.now().minusDays(idx).minusHours(i).format(DTF));
            comm.put("duration", 5 + i * 3);
            comm.put("nextFollowUp", i % 3 == 0 ? LocalDateTime.now().plusDays(3).format(DTF_DATE) : null);
            comm.put("satisfaction", i % 5 == 0 ? 4 : (i % 3 == 0 ? 5 : 5));
            records.add(comm);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", 48);
        result.put("page", page);
        result.put("size", 8);
        return result;
    }

    private List<Map<String, Object>> buildMockFollowUps(String staffName, int page) {
        List<Map<String, Object>> followUps = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("followUpId", (long) (page * 10 + i));
            f.put("customerId", 1000L + i);
            f.put("customerName", "测试客户" + String.format("%02d", i));
            f.put("content", "跟进客户关于" + (i % 2 == 0 ? "新产品咨询" : "账户问题") + "的回复");
            f.put("followUpDate", LocalDate.now().plusDays(i).format(DTF_DATE));
            f.put("priority", i % 3 == 0 ? "高" : (i % 2 == 0 ? "中" : "低"));
            f.put("status", "待跟进");
            followUps.add(f);
        }
        return followUps;
    }

    private Map<String, Object> buildMockFeedbacks(String status, String type, String assignee, int page) {
        String[] types = {"功能建议", "问题反馈", "服务投诉", "产品咨询", "其他"};
        String[] statuses = {"待处理", "处理中", "已解决", "已关闭"};
        String[] staffs = {"客服张三", "客服李四", "客服王五", ""};
        String[] sources = {"App", "Web", "客服电话", "邮件"};
        String[] titles = {
                "建议增加市价单功能", "登录页面加载缓慢", "App闪退问题",
                "咨询保证金计算方式", "投诉客服响应速度慢",
                "希望增加更多K线周期", "出金到账延迟", "账户资产显示异常"
        };

        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Map<String, Object> fb = new LinkedHashMap<>();
            int idx = (page - 1) * 8 + i;
            fb.put("feedbackId", (long) (idx + 1));
            fb.put("title", titles[i % titles.length]);
            fb.put("type", types[i % types.length]);
            fb.put("source", sources[i % sources.length]);
            fb.put("status", statuses[i % 4]);
            fb.put("customerId", 1000L + i);
            fb.put("customerName", "客户" + String.format("%02d", i));
            fb.put("assignee", staffs[i % staffs.length].isEmpty() ? null : staffs[i % staffs.length]);
            fb.put("description", "详细描述：" + titles[i % titles.length] + "的具体情况说明");
            fb.put("resolution", i < 4 ? null : "已处理完毕，客户确认满意");
            fb.put("satisfaction", i < 4 ? null : (4 + (i % 2)));
            fb.put("createTime", LocalDateTime.now().minusDays(idx).format(DTF));
            fb.put("resolveTime", i < 4 ? null : LocalDateTime.now().minusDays(idx - 2).format(DTF));
            fb.put("priority", i % 3 == 0 ? "高" : (i % 2 == 0 ? "中" : "低"));
            records.add(fb);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", 64);
        result.put("page", page);
        result.put("size", 8);
        return result;
    }

    private Map<String, Object> buildMockFeedbackStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalFeedbacks", 128);
        stats.put("pendingCount", 15);
        stats.put("processingCount", 22);
        stats.put("resolvedCount", 68);
        stats.put("closedCount", 23);
        stats.put("avgResolutionTime", 48.5); // 小时
        stats.put("satisfactionRate", 92.5);

        // 反馈类型分布
        stats.put("typeDistribution", List.of(
                Map.of("type", "功能建议", "count", 42),
                Map.of("type", "问题反馈", "count", 35),
                Map.of("type", "服务投诉", "count", 18),
                Map.of("type", "产品咨询", "count", 25),
                Map.of("type", "其他", "count", 8)
        ));

        // 来源分布
        stats.put("sourceDistribution", List.of(
                Map.of("source", "App", "count", 55),
                Map.of("source", "Web", "count", 32),
                Map.of("source", "客服电话", "count", 28),
                Map.of("source", "邮件", "count", 13)
        ));

        return stats;
    }
}

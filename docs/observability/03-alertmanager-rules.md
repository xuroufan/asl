# 告警规则与通知通道

> AlertManager + 钉钉/PagerDuty/邮件 分级告警

---

## 1. Prometheus 告警规则

```yaml
# prometheus-rules.yaml
groups:
  # ═══════════════════════════════════════
  # 1. 服务可用性告警
  # ═══════════════════════════════════════
  - name: service-availability
    interval: 30s
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
          team: backend
        annotations:
          summary: "{{ $labels.service }} 不可用"
          description: "{{ $labels.instance }} 已离线超过 1 分钟"

      - alert: HighErrorRate
        expr: |
          sum(rate(http_requests_duration_seconds_count{status=~"5.."}[2m]))
          / sum(rate(http_requests_duration_seconds_count[2m]))
          > 0.05
        for: 2m
        labels:
          severity: warning
          team: backend
        annotations:
          summary: "{{ $labels.service }} 错误率 > 5%"
          description: "当前错误率 {{ $value | humanizePercentage }}"

  # ═══════════════════════════════════════
  # 2. 延迟告警
  # ═══════════════════════════════════════
  - name: latency-alerts
    interval: 30s
    rules:
      - alert: MatchingLatencyHigh
        expr: |
          histogram_quantile(0.99,
            rate(matching_latency_microseconds_bucket[3m])
          ) > 100000
        for: 3m
        labels:
          severity: warning
          team: matching
        annotations:
          summary: "撮合延迟 P99 > 100ms"
          description: "当前 P99={{ $value | humanizeDuration }}"

      - alert: OrderLatencyHigh
        expr: |
          histogram_quantile(0.99,
            rate(order_latency_seconds_bucket[3m])
          ) > 10
        for: 3m
        labels:
          severity: warning
          team: backend
        annotations:
          summary: "订单处理延迟 P99 > 10s"
          description: "当前 P99={{ $value | humanizeDuration }}"

  # ═══════════════════════════════════════
  # 3. 消息队列告警
  # ═══════════════════════════════════════
  - name: mq-alerts
    interval: 30s
    rules:
      - alert: MqBacklogHigh
        expr: rabbitmq_queue_messages_ready > 10000
        for: 2m
        labels:
          severity: critical
          team: backend
        annotations:
          summary: "消息队列积压 > 10000 条"
          description: "队列 {{ $labels.queue }} 积压 {{ $value }} 条"

      - alert: MqBacklogWarning
        expr: rabbitmq_queue_messages_ready > 5000
        for: 2m
        labels:
          severity: warning
          team: backend
        annotations:
          summary: "消息队列积压 > 5000 条"
          description: "队列 {{ $labels.queue }} 积压 {{ $value }} 条"

  # ═══════════════════════════════════════
  # 4. 数据库告警
  # ═══════════════════════════════════════
  - name: database-alerts
    interval: 30s
    rules:
      - alert: DbConnectionPoolHigh
        expr: |
          hikaricp_active_connections / hikaricp_max_connections > 0.8
        for: 2m
        labels:
          severity: warning
          team: backend
        annotations:
          summary: "连接池使用率 > 80%"
          description: "{{ $labels.pool }} 使用率 {{ $value | humanizePercentage }}"

      - alert: ReplicationLag
        expr: mysql_slave_status_seconds_behind_master > 5
        for: 1m
        labels:
          severity: critical
          team: dba
        annotations:
          summary: "主从延迟 > 5s"
          description: "延迟 {{ $value }} 秒"

  # ═══════════════════════════════════════
  # 5. 业务指标告警
  # ═══════════════════════════════════════
  - name: business-alerts
    interval: 30s
    rules:
      - alert: FundFreezeErrors
        expr: rate(fund_freeze_errors_total[5m]) > 10
        for: 2m
        labels:
          severity: warning
          team: fund
        annotations:
          summary: "资金冻结错误频率过高"
          description: "最近 5 分钟错误数: {{ $value }}"

      - alert: NoOrderPlaced
        expr: rate(order_place_total[5m]) == 0
        for: 10m
        labels:
          severity: warning
          team: trading
        annotations:
          summary: "连续 10 分钟无新订单"
          description: "交易可能异常停滞，请人工检查"

  # ═══════════════════════════════════════
  # 6. 资源告警
  # ═══════════════════════════════════════
  - name: resource-alerts
    interval: 30s
    rules:
      - alert: HighCpuUsage
        expr: rate(process_cpu_usage[5m]) > 0.8
        for: 5m
        labels:
          severity: warning
          team: backend
        annotations:
          summary: "{{ $labels.service }} CPU 使用率 > 80%"

      - alert: JvmMemoryHigh
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 5m
        labels:
          severity: warning
          team: backend
        annotations:
          summary: "{{ $labels.service }} 堆内存 > 85%"
```

---

## 2. AlertManager 配置

```yaml
# alertmanager.yml
global:
  resolve_timeout: 5m
  # 钉钉 Webhook
  # 通知模板在 /etc/alertmanager/templates/

route:
  receiver: 'default'
  group_by: ['alertname', 'severity', 'service']
  group_wait: 10s           # 同一组告警的等待时间
  group_interval: 2m        # 同一组告警的发送间隔
  repeat_interval: 4h       # 已恢复告警的重复间隔

  # 分级路由
  routes:
    - match:
        severity: critical
      receiver: 'critical-pipeline'
      repeat_interval: 15m   # Critical 级别 15 分钟重报一次

    - match:
        severity: warning
      receiver: 'warning-pipeline'
      repeat_interval: 1h

receivers:
  # ═══════════════════════════════════════
  # Critical 通道：钉钉 + 电话 + 短信
  # ═══════════════════════════════════════
  - name: 'critical-pipeline'
    webhook_configs:
      - url: 'http://alert-bridge:8080/dingtalk/critical'
        send_resolved: true
    pagerduty_configs:
      - routing_key: ${PAGERDUTY_KEY}
        severity: critical
        description: '{{ .GroupLabels.alertname }}'
    victorops_configs: []    # 可选电话通知

  # ═══════════════════════════════════════
  # Warning 通道：钉钉 + 邮件
  # ═══════════════════════════════════════
  - name: 'warning-pipeline'
    webhook_configs:
      - url: 'http://alert-bridge:8080/dingtalk/warning'
        send_resolved: true
    email_configs:
      - to: '${ALERT_EMAIL_TO}'
        from: '${ALERT_EMAIL_FROM}'
        smarthost: 'smtp.example.com:587'
        auth_username: '${SMTP_USER}'
        auth_password: '${SMTP_PASS}'

  # ═══════════════════════════════════════
  # 默认通道：钉钉
  # ═══════════════════════════════════════
  - name: 'default'
    webhook_configs:
      - url: 'http://alert-bridge:8080/dingtalk/default'
        send_resolved: true
```

---

## 3. 钉钉告警模板

```json
// 钉钉 Markdown 消息模板示例
{
    "msgtype": "markdown",
    "markdown": {
        "title": "【{{ .Status | toUpper }}】{{ .GroupLabels.alertname }}",
        "text": "### {{ .Status | toUpper }} {{ .GroupLabels.alertname }}\n\n" +
                "**服务**: {{ .GroupLabels.service }}\n" +
                "**严重级别**: {{ .GroupLabels.severity }}\n" +
                "**详情**: {{ .CommonAnnotations.description }}\n" +
                "**开始时间**: {{ .StartsAt }}\n" +
                "**告警数**: {{ .Alerts | len }}\n" +
                "\n---\n" +
                "{{ range .Alerts }}" +
                "- **实例**: {{ .Labels.instance }}\n" +
                "  **当前值**: {{ .Annotations.value }}\n" +
                "{{ end }}\n\n" +
                "🔗 [Grafana 大盘](https://grafana.example.com)"
    }
}
```

---

## 4. 静默规则（防止告警风暴）

```yaml
# alertmanager-silences.yaml
# 部署维护时提前创建静默

silences:
  - comment: "计划内扩容，预期 2026-07-20 02:00-04:00"
    createdBy: "sre-team"
    startsAt: "2026-07-20T02:00:00+08:00"
    endsAt: "2026-07-20T04:00:00+08:00"
    matchers:
      - name: severity
        value: warning
        isRegex: false
      - name: alertname
        value: ".*(Cpu|Memory|Latency).*"
        isRegex: true
```

---

## 5. 告警分级与响应

| 级别 | 定义 | 通知渠道 | 响应时间 | 通知对象 |
|:----:|------|----------|:--------:|----------|
| **P0 Critical** | 服务不可用、撮合引擎宕机、资金数据不一致 | 电话 + 钉钉 + 短信 | 5 分钟 | 值班 SRE + 技术负责人 |
| **P1 Warning** | 错误率 > 5%、延迟升高、连接池满 | 钉钉 + 邮件 | 15 分钟 | 相关微服务负责人 |
| **P2 Info** | 慢查询、磁盘接近满、证书即将到期 | 钉钉（不响铃） | 24 小时 | SRE 团队 |

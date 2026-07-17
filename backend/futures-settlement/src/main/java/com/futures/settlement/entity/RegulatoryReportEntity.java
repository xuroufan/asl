package com.futures.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 监管报表实体。
 * <p>记录生成的SFC等监管机构所需的日报/月报信息。</p>
 */
@Data
@TableName("regulatory_report")
public class RegulatoryReportEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报表日期 */
    private LocalDate reportDate;

    /** 报表类型：DAILY / MONTHLY */
    private String reportType;

    /** 报表格式：PDF / EXCEL */
    private String format;

    /** 报表文件路径 */
    private String filePath;

    /** 报表状态：GENERATING / COMPLETED / FAILED */
    private String status;

    /** 报表摘要 */
    private String summary;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime generatedAt;

    @TableLogic
    private Integer deleted;
}

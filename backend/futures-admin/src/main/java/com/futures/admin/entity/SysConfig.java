package com.futures.admin.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("sys_config")
public class SysConfig {
    @TableId(type = IdType.AUTO)
    private Long configId;
    private String configName;
    private String configKey;
    private String configValue;
    private Integer configType;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.futures.admin.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("sys_oper_log")
public class SysOperLog {
    @TableId(type = IdType.AUTO)
    private Long operId;
    private String title;
    private Integer operType;
    private String method;
    private String requestMethod;
    private String operName;
    private String deptName;
    private String operUrl;
    private String operIp;
    private String operLocation;
    private String operParam;
    private String jsonResult;
    private Integer status;
    private Long costTime;
    private String errorMsg;
    private LocalDateTime operTime;
}

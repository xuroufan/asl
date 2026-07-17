package com.futures.admin.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("sys_login_log")
public class SysLoginLog {
    @TableId(type = IdType.AUTO)
    private Long infoId;
    private String username;
    private Integer status;
    private String ip;
    private String location;
    private String msg;
    private String browser;
    private String os;
    private LocalDateTime loginTime;
}

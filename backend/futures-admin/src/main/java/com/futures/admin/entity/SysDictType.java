package com.futures.admin.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("sys_dict_type")
public class SysDictType {
    @TableId(type = IdType.AUTO)
    private Long dictId;
    private String dictName;
    private String dictType;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

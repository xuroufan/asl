package com.futures.admin.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("sys_dict_data")
public class SysDictData {
    @TableId(type = IdType.AUTO)
    private Long dictCode;
    private Integer dictSort;
    private String dictLabel;
    private String dictValue;
    private String dictType;
    private String cssClass;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

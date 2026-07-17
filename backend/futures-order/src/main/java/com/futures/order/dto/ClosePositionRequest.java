package com.futures.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 平仓请求
 */
@Data
public class ClosePositionRequest {
    @NotBlank(message = "合约代码不能为空")
    private String symbol;

    @NotBlank(message = "方向不能为空")
    private String direction;

    @NotNull(message = "手数不能为空")
    @Min(value = 1, message = "手数必须大于0")
    private Integer volume;
}

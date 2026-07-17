package com.futures.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * KYC 提交请求 DTO。
 */
@Data
public class KycSubmitRequest {

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @NotBlank(message = "身份证号不能为空")
    private String idCardNo;

    /** 身份证正面照URL */
    private String idCardFrontUrl;

    /** 身份证背面照URL */
    private String idCardBackUrl;
}

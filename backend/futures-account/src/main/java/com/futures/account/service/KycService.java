package com.futures.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.futures.account.dto.KycSubmitRequest;
import com.futures.account.entity.KycRecordEntity;
import com.futures.account.entity.UserEntity;
import com.futures.account.mapper.KycRecordMapper;
import com.futures.account.mapper.UserMapper;
import com.futures.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * KYC（实名认证）服务。
 * <p>用户提交 KYC 资料，管理员审核通过/拒绝。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KycService {

    private final KycRecordMapper kycRecordMapper;
    private final UserMapper userMapper;

    /**
     * 用户提交 KYC 认证材料。
     *
     * @param userId  用户 ID
     * @param request KYC 提交请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitKyc(Long userId, KycSubmitRequest request) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }

        // 检查是否已有审核中的 KYC 记录
        Long pendingCount = kycRecordMapper.selectCount(
                new LambdaQueryWrapper<KycRecordEntity>()
                        .eq(KycRecordEntity::getUserId, userId)
                        .eq(KycRecordEntity::getStatus, 1));
        if (pendingCount > 0) {
            throw BizException.badRequest("已有审核中的 KYC 申请，请耐心等待");
        }

        // 若用户已通过 KYC，不允许重复提交
        if (user.getKycStatus() != null && user.getKycStatus() == 2) {
            throw BizException.badRequest("用户已通过实名认证");
        }

        // 创建 KYC 记录
        KycRecordEntity record = new KycRecordEntity();
        record.setUserId(userId);
        record.setRealName(request.getRealName());
        record.setIdCardNo(request.getIdCardNo());
        record.setIdCardFrontUrl(request.getIdCardFrontUrl());
        record.setIdCardBackUrl(request.getIdCardBackUrl());
        record.setStatus(1); // 审核中
        record.setCreatedAt(LocalDateTime.now());
        kycRecordMapper.insert(record);

        // 更新用户 KYC 状态
        user.setKycStatus(1);
        userMapper.updateById(user);

        log.info("用户 {} 提交 KYC 认证请求，真实姓名：{}", userId, request.getRealName());
    }

    /**
     * 管理员审批通过 KYC。
     *
     * @param recordId  KYC 记录 ID
     * @param reviewer  审核人
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveKyc(Long recordId, String reviewer) {
        KycRecordEntity record = kycRecordMapper.selectById(recordId);
        if (record == null) {
            throw BizException.notFound("KYC 记录不存在");
        }
        if (record.getStatus() != 1) {
            throw BizException.badRequest("KYC 记录状态异常，无法审核");
        }

        // 更新 KYC 记录
        record.setStatus(2); // 已通过
        record.setReviewer(reviewer);
        record.setReviewedAt(LocalDateTime.now());
        kycRecordMapper.updateById(record);

        // 更新用户信息
        UserEntity user = userMapper.selectById(record.getUserId());
        user.setKycStatus(2);
        user.setRealName(record.getRealName());
        user.setIdCardNo(record.getIdCardNo());
        userMapper.updateById(user);

        log.info("KYC 记录 {} 审核通过，审核人：{}", recordId, reviewer);
    }

    /**
     * 管理员拒绝 KYC。
     *
     * @param recordId KYC 记录 ID
     * @param reviewer 审核人
     * @param remark   拒绝原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectKyc(Long recordId, String reviewer, String remark) {
        KycRecordEntity record = kycRecordMapper.selectById(recordId);
        if (record == null) {
            throw BizException.notFound("KYC 记录不存在");
        }
        if (record.getStatus() != 1) {
            throw BizException.badRequest("KYC 记录状态异常，无法审核");
        }

        record.setStatus(3); // 已拒绝
        record.setReviewer(reviewer);
        record.setRemark(remark);
        record.setReviewedAt(LocalDateTime.now());
        kycRecordMapper.updateById(record);

        // 回退用户 KYC 状态
        UserEntity user = userMapper.selectById(record.getUserId());
        user.setKycStatus(3);
        userMapper.updateById(user);

        log.warn("KYC 记录 {} 审核拒绝，原因：{}，审核人：{}", recordId, remark, reviewer);
    }

    /**
     * 分页查询待审核的 KYC 记录（管理员用）。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    public IPage<KycRecordEntity> getPendingList(int page, int size) {
        return kycRecordMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<KycRecordEntity>()
                        .eq(KycRecordEntity::getStatus, 1)
                        .orderByAsc(KycRecordEntity::getCreatedAt));
    }

    /**
     * 获取用户的 KYC 状态。
     *
     * @param userId 用户 ID
     * @return KYC 状态映射
     */
    public Map<String, Object> getKycStatus(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return Map.of(
                "kycStatus", user.getKycStatus(),
                "realName", user.getRealName() != null ? user.getRealName() : "",
                "idCardNo", user.getIdCardNo() != null ? maskIdCard(user.getIdCardNo()) : ""
        );
    }

    /** 脱敏身份证号 */
    private String maskIdCard(String idCard) {
        if (idCard.length() < 8) return idCard;
        return idCard.substring(0, 3) + "********" + idCard.substring(idCard.length() - 4);
    }
}

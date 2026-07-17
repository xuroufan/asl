package com.futures.account.service;

import com.futures.account.dto.KycSubmitRequest;
import com.futures.account.entity.KycRecordEntity;
import com.futures.account.entity.UserEntity;
import com.futures.account.mapper.KycRecordMapper;
import com.futures.account.mapper.UserMapper;
import com.futures.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    @Mock
    private KycRecordMapper kycRecordMapper;

    @Mock
    private UserMapper userMapper;

    private KycService kycService;

    @BeforeEach
    void setUp() {
        kycService = new KycService(kycRecordMapper, userMapper);
    }

    @Test
    void submitKyc_shouldCreateRecord() {
        Long userId = 1L;
        UserEntity user = createTestUser(userId);
        user.setKycStatus(0);

        when(userMapper.selectById(userId)).thenReturn(user);
        when(kycRecordMapper.selectCount(any())).thenReturn(0L);
        when(kycRecordMapper.insert(any())).thenReturn(1);

        KycSubmitRequest request = new KycSubmitRequest();
        request.setRealName("张三");
        request.setIdCardNo("110101199001011234");
        request.setIdCardFrontUrl("https://example.com/front.jpg");
        request.setIdCardBackUrl("https://example.com/back.jpg");

        assertDoesNotThrow(() -> kycService.submitKyc(userId, request));

        verify(userMapper).updateById(argThat(u -> u.getKycStatus() == 1));
        verify(kycRecordMapper).insert(argThat(r -> "张三".equals(r.getRealName())));
    }

    @Test
    void submitKyc_withPendingRecord_shouldThrowException() {
        Long userId = 1L;
        UserEntity user = createTestUser(userId);
        user.setKycStatus(1);

        when(userMapper.selectById(userId)).thenReturn(user);
        when(kycRecordMapper.selectCount(any())).thenReturn(1L);

        KycSubmitRequest request = new KycSubmitRequest();
        request.setRealName("张三");
        request.setIdCardNo("110101199001011234");

        assertThrows(BizException.class, () -> kycService.submitKyc(userId, request));
    }

    @Test
    void submitKyc_withApprovedKyc_shouldThrowException() {
        Long userId = 1L;
        UserEntity user = createTestUser(userId);
        user.setKycStatus(2);

        when(userMapper.selectById(userId)).thenReturn(user);

        KycSubmitRequest request = new KycSubmitRequest();
        request.setRealName("张三");
        request.setIdCardNo("110101199001011234");

        assertThrows(BizException.class, () -> kycService.submitKyc(userId, request));
    }

    @Test
    void approveKyc_shouldUpdateUserInfo() {
        Long userId = 1L;
        Long recordId = 1L;
        String reviewer = "admin";

        KycRecordEntity record = createTestRecord(recordId, userId, 1);
        UserEntity user = createTestUser(userId);

        when(kycRecordMapper.selectById(recordId)).thenReturn(record);
        when(userMapper.selectById(userId)).thenReturn(user);

        assertDoesNotThrow(() -> kycService.approveKyc(recordId, reviewer));

        verify(kycRecordMapper).updateById(argThat(r -> r.getStatus() == 2 && reviewer.equals(r.getReviewer())));
        verify(userMapper).updateById(argThat(u -> u.getKycStatus() == 2 && "张三".equals(u.getRealName())));
    }

    @Test
    void rejectKyc_shouldUpdateRecord() {
        Long userId = 1L;
        Long recordId = 1L;
        String reviewer = "admin";
        String remark = "身份证照片不清晰";

        KycRecordEntity record = createTestRecord(recordId, userId, 1);
        UserEntity user = createTestUser(userId);

        when(kycRecordMapper.selectById(recordId)).thenReturn(record);
        when(userMapper.selectById(userId)).thenReturn(user);

        assertDoesNotThrow(() -> kycService.rejectKyc(recordId, reviewer, remark));

        verify(kycRecordMapper).updateById(argThat(r -> r.getStatus() == 3 && remark.equals(r.getRemark())));
        verify(userMapper).updateById(argThat(u -> u.getKycStatus() == 3));
    }

    @Test
    void getKycStatus_shouldReturnMaskedIdCard() {
        Long userId = 1L;
        UserEntity user = createTestUser(userId);
        user.setKycStatus(2);
        user.setRealName("张三");
        user.setIdCardNo("110101199001011234");

        when(userMapper.selectById(userId)).thenReturn(user);

        Map<String, Object> status = kycService.getKycStatus(userId);

        assertEquals(2, status.get("kycStatus"));
        assertEquals("张三", status.get("realName"));
        String masked = (String) status.get("idCardNo");
        assertNotNull(masked);
        assertTrue(masked.contains("********"));
    }

    private UserEntity createTestUser(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername("testuser");
        user.setPassword("encoded");
        user.setDisplayName("testuser");
        user.setRole("USER");
        user.setStatus(0);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    private KycRecordEntity createTestRecord(Long id, Long userId, int status) {
        KycRecordEntity record = new KycRecordEntity();
        record.setId(id);
        record.setUserId(userId);
        record.setRealName("张三");
        record.setIdCardNo("110101199001011234");
        record.setIdCardFrontUrl("https://example.com/front.jpg");
        record.setIdCardBackUrl("https://example.com/back.jpg");
        record.setStatus(status);
        return record;
    }
}

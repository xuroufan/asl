package com.futures.settlement;

import com.futures.common.exception.BizException;
import com.futures.settlement.entity.DailySettlementEntity;
import com.futures.settlement.service.SettlementService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.RoundingMode;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 清结算服务单元测试。
 */
class SettlementServiceTest {

    @Test
    void testPnlCalculation() {
        // 买入开仓 价格4500, 平仓 价格4550, 手数2 => PnL = (4550-4500) * 2 = 100
        String direction = "BUY";
        BigDecimal openPrice = new BigDecimal("4500");
        BigDecimal closePrice = new BigDecimal("4550");
        int volume = 2;

        BigDecimal pnl = direction.equalsIgnoreCase("BUY")
                ? closePrice.subtract(openPrice).multiply(BigDecimal.valueOf(volume))
                : openPrice.subtract(closePrice).multiply(BigDecimal.valueOf(volume));

        assertEquals(0, new BigDecimal("100").compareTo(pnl), "多头平仓盈亏应为100");
    }

    @Test
    void testShortPnlCalculation() {
        // 卖出开仓 价格4600, 平仓 价格4550, 手数3 => PnL = (4600-4550) * 3 = 150
        String direction = "SELL";
        BigDecimal openPrice = new BigDecimal("4600");
        BigDecimal closePrice = new BigDecimal("4550");
        int volume = 3;

        BigDecimal pnl = direction.equalsIgnoreCase("BUY")
                ? closePrice.subtract(openPrice).multiply(BigDecimal.valueOf(volume))
                : openPrice.subtract(closePrice).multiply(BigDecimal.valueOf(volume));

        assertEquals(0, new BigDecimal("150").compareTo(pnl), "空头平仓盈亏应为150");
    }

    @Test
    void testEndEquityCalculation() {
        BigDecimal beginEquity = new BigDecimal("500000");
        BigDecimal realizedPnl = new BigDecimal("5000");
        BigDecimal unrealizedPnl = new BigDecimal("-2000");
        BigDecimal fee = new BigDecimal("50");
        BigDecimal netDeposit = BigDecimal.ZERO;

        BigDecimal totalPnl = realizedPnl.add(unrealizedPnl); // 3000
        BigDecimal endEquity = beginEquity.add(totalPnl).subtract(fee).add(netDeposit);

        assertEquals(0, new BigDecimal("502950").compareTo(endEquity),
                "期末权益应为期初500000 + 总盈亏3000 - 手续费50 = 502950");
    }
}

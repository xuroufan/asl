package com.hackfuture.trading.baselineprofile

import androidx.benchmark.macro.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collectBaselineProfile(
            packageName = "com.hackfuture.trading",
            profileBlock = {
                pressHome()
                startActivityAndWait()

                // 等待行情列表加载
                device.waitForIdle()
                // 切换合约
                // 浏览 K 线
                // 进入交易页
                // 查看持仓
            },
        )
    }
}

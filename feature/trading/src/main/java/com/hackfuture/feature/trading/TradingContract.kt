package com.hackfuture.feature.trading

import com.hackfuture.core.model.MarketData
import com.hackfuture.core.model.Order
import com.hackfuture.core.model.OrderSide
import com.hackfuture.core.model.OrderType

data class TradingState(
    val isLoading: Boolean = false,
    val symbol: String = "BTCUSDT",
    val marketData: MarketData? = null,
    val currentPrice: Double = 0.0,
    val orderSide: OrderSide = OrderSide.BUY,
    val orderType: OrderType = OrderType.LIMIT,
    val price: String = "",
    val quantity: String = "",
    val totalAmount: String = "",
    val recentOrders: List<Order> = emptyList(),
    val isSubmitting: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val confirmOrder: PendingOrder? = null,
    val error: String? = null,
    val successMessage: String? = null,
) {
    val canSubmit: Boolean
        get() = !isSubmitting && quantity.isNotBlank() &&
            (quantity.toDoubleOrNull() ?: 0.0) > 0 &&
            (if (orderType == OrderType.MARKET) true
            else (price.toDoubleOrNull() ?: 0.0) > 0)

}

data class PendingOrder(
    val symbol: String,
    val side: OrderSide,
    val type: OrderType,
    val price: Double,
    val quantity: Double,
    val total: Double,
)

sealed class TradingIntent {
    data class SetSymbol(val symbol: String) : TradingIntent()
    data class SetSide(val side: OrderSide) : TradingIntent()
    data class SetOrderType(val type: OrderType) : TradingIntent()
    data class SetPrice(val price: String) : TradingIntent()
    data class SetQuantity(val quantity: String) : TradingIntent()
    data object PreviewOrder : TradingIntent()
    data object ConfirmOrder : TradingIntent()
    data object DismissConfirm : TradingIntent()
    data object PlaceOrder : TradingIntent()
    data object LoadMarketData : TradingIntent()
    data object ClearMessage : TradingIntent()
}

sealed class TradingEffect {
    data class OrderPlaced(val order: Order) : TradingEffect()
    data class ShowError(val message: String) : TradingEffect()
    data object NavigateBack : TradingEffect()
}

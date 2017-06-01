package micdm.btce_trader.model

import java.math.BigDecimal

data class OrderData(val type: OrderType, val price: BigDecimal, val amount: BigDecimal) {

    val secondAmount: BigDecimal
        get() = price * amount
}

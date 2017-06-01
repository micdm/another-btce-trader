package micdm.btce_trader.model

import java.math.BigDecimal

data class Balance(val first: BigDecimal, val second: BigDecimal) {

    fun asFirst(price: BigDecimal): BigDecimal {
        return first + second / price
    }

    fun asSecond(price: BigDecimal): BigDecimal {
        return first * price + second
    }
}

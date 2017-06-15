package micdm.btce_trader.model

import java.math.BigDecimal

data class CurrencyPair(val first: Currency, val second: Currency, val decimalPlaces: Int, val minOrderAmount: BigDecimal)

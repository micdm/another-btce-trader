package micdm.btce_trader.model

import java.math.BigDecimal
import java.time.ZonedDateTime

data class Price(val value: BigDecimal, val updated: ZonedDateTime)

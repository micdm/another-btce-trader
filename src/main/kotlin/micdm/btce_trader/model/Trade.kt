package micdm.btce_trader.model

import java.time.ZonedDateTime

data class Trade(val data: OrderData, val created: ZonedDateTime)

package micdm.btce_trader.model

import java.time.ZonedDateTime

data class Trade(val id: String, val orderId: String, val data: OrderData, val created: ZonedDateTime)

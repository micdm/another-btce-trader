package micdm.btce_trader

import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Config @Inject constructor() {

    fun getApiKey(): String {
        return System.getenv("API_KEY")
    }

    fun getApiSecret(): String {
        return System.getenv("API_SECRET")
    }

    fun getPriceDelta(): BigDecimal {
        return BigDecimal(System.getenv("PRICE_DELTA"))
    }

    fun getPriceThreshold(): BigDecimal {
        return BigDecimal(System.getenv("PRICE_THRESHOLD"))
    }

    fun getOrderAmount(): BigDecimal {
        return BigDecimal(System.getenv("ORDER_AMOUNT"))
    }
}

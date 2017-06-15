package micdm.btce_trader

import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Config @Inject constructor() {

    fun getFirstCurrency(): String = System.getenv("FIRST_CURRENCY")

    fun getSecondCurrency(): String = System.getenv("SECOND_CURRENCY")

    fun getDecimalPlaces(): Int = System.getenv("DECIMAL_PLACES").toInt()

    fun getMinOrderAmount(): BigDecimal = BigDecimal(System.getenv("MIN_ORDER_AMOUNT"))

    fun getTradingStrategy(): Int = System.getenv("TRADING_STRATEGY").toInt()

    fun getPriceDelta(): BigDecimal = BigDecimal(System.getenv("PRICE_DELTA"))

    fun getPriceThreshold(): BigDecimal = BigDecimal(System.getenv("PRICE_THRESHOLD"))

    fun getOrderAmount(): BigDecimal = BigDecimal(System.getenv("ORDER_AMOUNT"))
}

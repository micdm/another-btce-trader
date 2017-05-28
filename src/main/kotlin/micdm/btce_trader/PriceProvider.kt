package micdm.btce_trader

import io.reactivex.Observable
import java.math.BigDecimal

interface PriceProvider {

    fun getPrices(): Observable<BigDecimal>
    fun start()
}

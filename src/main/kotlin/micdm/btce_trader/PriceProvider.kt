package micdm.btce_trader

import io.reactivex.Observable
import micdm.btce_trader.model.Price

interface PriceProvider {

    fun start()
    fun getPrices(): Observable<Price>
}

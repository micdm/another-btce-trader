package micdm.btce_trader

import io.reactivex.Observable
import micdm.btce_trader.model.Trade

interface TradeHistoryProvider {

    fun getTradeHistory(): Observable<Collection<Trade>>
}

package micdm.btce_trader.local

import io.reactivex.Observable
import micdm.btce_trader.TradeHistoryProvider
import micdm.btce_trader.model.Trade
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalTradeHistoryProvider @Inject constructor(private val tradeHistoryBuffer: TradeHistoryBuffer): TradeHistoryProvider {

    override fun getTradeHistory(): Observable<Collection<Trade>> = tradeHistoryBuffer.get()
}

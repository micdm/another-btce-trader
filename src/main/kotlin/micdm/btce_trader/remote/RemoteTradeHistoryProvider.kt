package micdm.btce_trader.remote

import io.reactivex.Observable
import micdm.btce_trader.TradeHistoryProvider
import micdm.btce_trader.model.Trade
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RemoteTradeHistoryProvider @Inject constructor(): TradeHistoryProvider {

    override fun getTradeHistory(): Observable<Collection<Trade>> {
        return Observable.just(Collections.emptyList())
    }
}

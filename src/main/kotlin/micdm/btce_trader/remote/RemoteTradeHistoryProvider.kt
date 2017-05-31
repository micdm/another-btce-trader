package micdm.btce_trader.remote

import io.reactivex.Observable
import micdm.btce_trader.TradeHistoryProvider
import micdm.btce_trader.model.Trade
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RemoteTradeHistoryProvider @Inject constructor(private val tradeApiConnector: TradeApiConnector): TradeHistoryProvider {

    override fun getTradeHistory(): Observable<Collection<Trade>> {
        return tradeApiConnector.getTradeHistory()
            .toObservable()
            .doOnError {
                println("Cannot get trade history: $it")
            }
            .onErrorResumeNext(Observable.empty())
    }
}

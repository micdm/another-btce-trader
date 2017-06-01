package micdm.btce_trader.local

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import micdm.btce_trader.model.Trade
import org.slf4j.Logger
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TradeHistoryBuffer @Inject constructor(private val logger: Logger) {

    private val trades = BehaviorSubject.createDefault<Collection<Trade>>(Collections.emptyList())

    fun get(): Observable<Collection<Trade>> = trades

    fun add(trade: Trade) {
        logger.info("Adding trade $trade")
        val trades = ArrayList(this.trades.getValue())
        trades.add(trade)
        this.trades.onNext(trades)
    }
}

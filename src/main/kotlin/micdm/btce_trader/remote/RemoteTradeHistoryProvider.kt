package micdm.btce_trader.remote

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.TradeHistoryProvider
import micdm.btce_trader.model.Trade
import org.slf4j.Logger
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RemoteTradeHistoryProvider @Inject constructor(tradeApiConnector: TradeApiConnector,
                                                              logger: Logger): TradeHistoryProvider {

    private val POLL_INTERVAL = Duration.ofSeconds(30)

    private val trades: Subject<Collection<Trade>> = BehaviorSubject.create()

    init {
        Observable
            .interval(0, POLL_INTERVAL.seconds, TimeUnit.SECONDS)
            .switchMap {
                tradeApiConnector.getTradeHistory()
                    .toObservable()
                    .doOnError { logger.warn("Cannot get trade history: $it") }
                    .onErrorResumeNext(Observable.empty())
            }
            .distinctUntilChanged()
            .doOnNext { logger.info("Trades are $it") }
            .subscribe(trades::onNext)
    }

    override fun getTradeHistory(): Observable<Collection<Trade>> = trades
}

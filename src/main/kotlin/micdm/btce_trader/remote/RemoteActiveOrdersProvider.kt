package micdm.btce_trader.remote

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.ActiveOrdersProvider
import micdm.btce_trader.TradeHistoryProvider
import micdm.btce_trader.model.Order
import org.slf4j.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RemoteActiveOrdersProvider @Inject constructor(logger: Logger,
                                                              tradeApiConnector: TradeApiConnector,
                                                              orderStatusBuffer: OrderStatusBuffer,
                                                              tradeHistoryProvider: TradeHistoryProvider): ActiveOrdersProvider {

    private val orders: Subject<Collection<Order>> = BehaviorSubject.create()

    init {
        Observable
            .merge(
                orderStatusBuffer.getCreates(),
                orderStatusBuffer.getCancels(),
                tradeHistoryProvider.getTradeHistory().distinctUntilChanged()
            )
            .startWith(Any())
            .switchMap {
                tradeApiConnector.getActiveOrders()
                    .toObservable()
                    .doOnError { logger.warn("Cannot get orders: $it") }
                    .onErrorResumeNext(Observable.empty())
            }
            .doOnNext { logger.info("Active orders are $it") }
            .subscribe(orders::onNext)
    }

    override fun getActiveOrders(): Observable<Collection<Order>> = orders
}

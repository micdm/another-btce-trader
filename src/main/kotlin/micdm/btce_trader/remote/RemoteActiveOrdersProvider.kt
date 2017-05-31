package micdm.btce_trader.remote

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.ActiveOrdersProvider
import micdm.btce_trader.model.Order
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RemoteActiveOrdersProvider @Inject constructor(private val tradeApiConnector: TradeApiConnector): ActiveOrdersProvider {

    private val POLL_INTERVAL = Duration.ofSeconds(300)

    private val orders: Subject<Collection<Order>> = PublishSubject.create()

    init {
        Observable
            .interval(0, POLL_INTERVAL.seconds, TimeUnit.SECONDS)
            .switchMap {
                tradeApiConnector.getActiveOrders()
                    .toObservable()
                    .doOnError {
                        println("Cannot get orders: $it")
                    }
                    .onErrorResumeNext(Observable.empty())
            }
            .subscribe(orders::onNext)
    }

    override fun getActiveOrders(): Observable<Collection<Order>> = orders
}

package micdm.btce_trader.strategies

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.OrderStrategy
import micdm.btce_trader.PriceProvider
import micdm.btce_trader.model.OrderData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LazyOrderStrategy @Inject constructor(private val priceProvider: PriceProvider): OrderStrategy {

    private val createRequests: Subject<Collection<OrderData>> = PublishSubject.create()

    override fun start() {

    }

    override fun getCreateRequests(): Observable<Collection<OrderData>> = createRequests

    override fun getCancelRequests(): Observable<Collection<String>> = Observable.empty()
}

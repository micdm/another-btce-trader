package micdm.btce_trader.remote

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.ActiveOrdersProvider
import micdm.btce_trader.BalanceProvider
import micdm.btce_trader.model.Balance
import micdm.btce_trader.model.Order
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RemoteBalanceProvider @Inject constructor(tradeApiConnector: TradeApiConnector,
                                                         activeOrdersProvider: ActiveOrdersProvider): BalanceProvider {

    private val balance: Subject<Balance> = BehaviorSubject.create()

    init {
        activeOrdersProvider.getActiveOrders()
            .startWith(Collections.emptyList<Order>())
            .distinctUntilChanged()
            .switchMap {
                tradeApiConnector.getBalance()
                    .toObservable()
                    .doOnError { println("Cannot get balance: $it") }
                    .onErrorResumeNext(Observable.empty())
            }
            .doOnNext { println("Balance is $it") }
            .subscribe(balance::onNext)
    }

    override fun getBalance(): Observable<Balance> = balance
}

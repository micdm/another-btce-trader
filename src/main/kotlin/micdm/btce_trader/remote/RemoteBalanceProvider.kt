package micdm.btce_trader.remote

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.BalanceProvider
import micdm.btce_trader.model.Balance
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RemoteBalanceProvider @Inject constructor(private val tradeApiConnector: TradeApiConnector): BalanceProvider {

    private val POLL_INTERVAL = Duration.ofSeconds(300)

    private val balance: Subject<Balance> = PublishSubject.create()

    init {
        Observable
            .interval(0, POLL_INTERVAL.seconds, TimeUnit.SECONDS)
            .switchMap {
                tradeApiConnector.getBalance()
                    .toObservable()
                    .doOnError {
                        println("Cannot get balance: $it")
                    }
                    .onErrorResumeNext(Observable.empty())
            }
            .subscribe(balance::onNext)
    }

    override fun getBalance(): Observable<Balance> = balance
}

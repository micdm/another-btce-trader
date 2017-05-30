package micdm.btce_trader.remote

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.BalanceProvider
import micdm.btce_trader.model.Currency
import java.math.BigDecimal
import java.time.Duration
import java.util.concurrent.TimeUnit

internal class RemoteBalanceProvider constructor(private val currency: Currency,
                                                 private val firstCurrency: Currency,
                                                 private val secondCurrency: Currency,
                                                 private val tradeApiConnector: TradeApiConnector): BalanceProvider {

    private val POLL_INTERVAL = Duration.ofSeconds(5)

    private val balance: Subject<BigDecimal> = PublishSubject.create()

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
            .map {
                when (currency) {
                    firstCurrency -> it.first
                    secondCurrency -> it.second
                    else -> throw IllegalStateException("unknown currency $currency")
                }
            }
            .subscribe(balance::onNext)
    }

    override fun getBalance(): Observable<BigDecimal> = balance
}

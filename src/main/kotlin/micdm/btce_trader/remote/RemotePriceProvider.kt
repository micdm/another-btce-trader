package micdm.btce_trader.remote

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.PriceProvider
import java.math.BigDecimal
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RemotePriceProvider @Inject constructor(private val publicApiConnector: PublicApiConnector): PriceProvider {

    private val POLL_INTERVAL = Duration.ofSeconds(2)

    private val prices: Subject<BigDecimal> = PublishSubject.create()

    override fun getPrices(): Observable<BigDecimal> = prices

    override fun start() {
        Observable
            .interval(0, POLL_INTERVAL.seconds, TimeUnit.SECONDS)
            .switchMap {
                publicApiConnector.getPrice()
                    .toObservable()
                    .doOnError {
                        println("Cannot get price: $it")
                    }
                    .onErrorResumeNext(Observable.empty())
            }
            .distinctUntilChanged()
            .doOnNext {
                println("New price is $it")
            }
            .subscribe(prices::onNext)
    }
}

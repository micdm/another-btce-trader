package micdm.btce_trader.remote

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.PriceProvider
import micdm.btce_trader.misc.DummyWorker
import micdm.btce_trader.model.Price
import org.slf4j.Logger
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RemotePriceProvider @Inject constructor(private val publicApiConnector: PublicApiConnector,
                                                       private val logger: Logger,
                                                       private val dummyWorker: DummyWorker): PriceProvider {

    private val POLL_INTERVAL = Duration.ofSeconds(5)

    private val prices: Subject<Price> = PublishSubject.create()

    override fun getPrices(): Observable<Price> = prices

    override fun start() {
        Observable
            .interval(0, POLL_INTERVAL.seconds, TimeUnit.SECONDS)
            .switchMap {
                publicApiConnector.getPrice()
                    .toObservable()
                    .doOnError { logger.warn("Cannot get price: $it") }
                    .onErrorResumeNext(Observable.empty())
            }
            .distinctUntilChanged()
            .doOnNext { logger.info("Price is $it") }
            .subscribe(prices::onNext)
        dummyWorker.run()
    }
}

package micdm.btce_trader.local

import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.PriceProvider
import org.slf4j.Logger
import java.io.File
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class LocalPriceProvider @Inject constructor(@Named("common") private val gson: Gson,
                                                      private val logger: Logger): PriceProvider {

    data class Pairs(val btc_usd: Pair)

    data class Pair(val last: BigDecimal)

    private val prices: Subject<BigDecimal> = PublishSubject.create()

    override fun getPrices(): Observable<BigDecimal> = prices

    override fun start() {
        Observable
            .create<BigDecimal> { source ->
                File("data/btce_prices2.log").forEachLine {
                    source.onNext(gson.fromJson(it, Pairs::class.java).btc_usd.last)
                }
                source.onComplete()
            }
            .distinctUntilChanged()
            .doOnNext { logger.info("New price is $it") }
            .subscribe(prices::onNext)
    }
}

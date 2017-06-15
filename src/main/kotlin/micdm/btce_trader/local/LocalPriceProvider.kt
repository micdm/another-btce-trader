package micdm.btce_trader.local

import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.PriceProvider
import micdm.btce_trader.model.CurrencyPair
import micdm.btce_trader.model.Price
import org.slf4j.Logger
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class LocalPriceProvider @Inject constructor(@Named("common") private val gson: Gson,
                                                      private val logger: Logger,
                                                      private val currencyPair: CurrencyPair): PriceProvider {

    class Pairs: HashMap<String, Pair>()

    data class Pair(val last: BigDecimal, val updated: Long)

    private val prices: Subject<Price> = PublishSubject.create()

    override fun getPrices(): Observable<Price> = prices

    override fun start() {
        Observable
            .create<Price> { source ->
                File("data/btce_prices.log").forEachLine {
                    try {
                        val pairs = gson.fromJson(it, Pairs::class.java)
                        val pair = pairs["${currencyPair.first.name.toLowerCase()}_${currencyPair.second.name.toLowerCase()}"]
                        source.onNext(Price(pair!!.last, ZonedDateTime.ofInstant(Instant.ofEpochSecond(pair.updated), ZoneId.of("Europe/Moscow"))))
                    } catch (e: Exception) {

                    }
                }
                source.onComplete()
            }
            .distinctUntilChanged { (value) -> value }
            .doOnNext { logger.info("New price is $it") }
            .subscribe(prices)
    }
}

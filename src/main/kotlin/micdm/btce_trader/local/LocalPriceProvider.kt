package micdm.btce_trader.local

import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.PriceProvider
import micdm.btce_trader.model.CurrencyPair
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

    data class Price(val amount: BigDecimal, val updated: ZonedDateTime)

    private val prices: Subject<BigDecimal> = PublishSubject.create()

    override fun getPrices(): Observable<BigDecimal> = prices

    override fun start() {
        Observable
            .create<Price> { source ->
                File("data/btce_prices.log").forEachLine {
                    try {
                        val pair = gson.fromJson(it, Pairs::class.java).get("${currencyPair.first.name.toLowerCase()}_${currencyPair.second.name.toLowerCase()}")
                        source.onNext(Price(pair!!.last, ZonedDateTime.ofInstant(Instant.ofEpochSecond(pair.updated), ZoneId.of("Europe/Moscow"))))
                    } catch (e: Exception) {

                    }
                }
                source.onComplete()
            }
            .distinctUntilChanged { it -> it.amount }
            .doOnNext { logger.info("New price is ${it.amount} at ${it.updated}") }
            .map { it.amount }
            .subscribe(prices)
    }
}

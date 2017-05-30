package micdm.btce_trader.local

import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.PriceProvider
import java.io.File
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class LocalPriceProvider @Inject constructor(@Named("common") private val gson: Gson): PriceProvider {

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
            .doOnNext {
                println("New price is $it")
            }
            .subscribe(prices)
//        Observable.just(
//            BigDecimal("1000"),
//            BigDecimal("900"),
//            BigDecimal("1010"),
//            BigDecimal("1050"),
//            BigDecimal("1060")
//        )

//            .subscribe(prices)
    }
}

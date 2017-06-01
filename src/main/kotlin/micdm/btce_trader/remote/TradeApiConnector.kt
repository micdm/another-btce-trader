package micdm.btce_trader.remote

import com.google.gson.Gson
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.SingleEmitter
import micdm.btce_trader.Config
import micdm.btce_trader.model.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.lang.reflect.Type
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.collections.ArrayList

@Singleton
internal class TradeApiConnector @Inject constructor(private val config: Config,
                                                     private val okHttpClient: OkHttpClient,
                                                     private val dataSigner: DataSigner,
                                                     @Named("trade") private val gson: Gson,
                                                     private val currencyPair: CurrencyPair,
                                                     private val nonceKeeper: NonceKeeper,
                                                     @Named("single") private val singleScheduler: Scheduler) {

    open class Result<T>(val data: Optional<T>, val error: Optional<String>)

    class GetInfoResult(data: Optional<Info>, error: Optional<String>): Result<Info>(data, error)

    data class Info(val funds: Map<String, BigDecimal>)

    class GetActiveOrdersResult(data: Optional<ActiveOrders>, error: Optional<String>) : Result<ActiveOrders>(data, error)

    class ActiveOrders: HashMap<String, ActiveOrder>()

    data class ActiveOrder(val type: String, val rate: BigDecimal, val amount: BigDecimal)

    class GetTradeHistoryResult(data: Optional<Trades>, error: Optional<String>) : Result<Trades>(data, error)

    class Trades: HashMap<String, TradeInfo>()

    data class TradeInfo(val orderId: String, val type: String, val rate: BigDecimal, val amount: BigDecimal, val timestamp: Long)

    fun getBalance(): Single<Balance> {
        return doRequest<Info>("getInfo", type=GetInfoResult::class.java).map {
            Balance(it.funds.get(currencyPair.first.name.toLowerCase())!!,
                    it.funds.get(currencyPair.second.name.toLowerCase())!!)
        }
    }

    fun getActiveOrders(): Single<Collection<Order>> {
        return doRequest<ActiveOrders>("ActiveOrders", mapOf(
            "pair" to "${currencyPair.first.name.toLowerCase()}_${currencyPair.second.name.toLowerCase()}"
        ), GetActiveOrdersResult::class.java)
            .map<Collection<Order>> {
                val orders = ArrayList<Order>()
                for ((id, data) in it) {
                    orders.add(Order(id, OrderData(if (data.type == "buy") OrderType.BUY else OrderType.SELL, data.rate, data.amount)))
                }
                orders.sortedByDescending { it.id }
            }
            .onErrorResumeNext {
                when {
                    it is RuntimeException && it.message == "no orders" -> Single.just(Collections.emptyList<Order>())
                    else -> Single.error(it)
                }
            }
    }

    fun getTradeHistory(): Single<Collection<Trade>> {
        return doRequest<Trades>("TradeHistory", mapOf(
            "pair" to "${currencyPair.first.name.toLowerCase()}_${currencyPair.second.name.toLowerCase()}",
            "count" to "10"
        ), GetTradeHistoryResult::class.java)
            .map<Collection<Trade>> {
                val trades = ArrayList<Trade>()
                for ((id, data) in it) {
                    trades.add(Trade(id, data.orderId, OrderData(if (data.type == "buy") OrderType.BUY else OrderType.SELL, data.rate, data.amount),
                                     ZonedDateTime.ofInstant(Instant.ofEpochSecond(data.timestamp), ZoneId.of("Europe/Moscow"))))
                }
                trades.sortedByDescending { it.id }
            }
    }

    private fun <T1> doRequest(method: String, params: Map<String, String> = Collections.emptyMap(), type: Type): Single<T1> {
        return Single
            .create { source: SingleEmitter<T1> ->
                val body = getFormBody(method, params)
                try {
                    val response = okHttpClient
                        .newCall(
                            Request.Builder()
                                .url("https://btc-e.nz/tapi")
                                .post(body)
                                .header("Key", config.getApiKey())
                                .header("Sign", dataSigner.getSignature(formBodyToString(body), config.getApiSecret()))
                                .build()
                        )
                        .execute()
                    val text = response.body().string()
                    val result = gson.fromJson<Result<T1>>(text, type)
                    if (result.error.isPresent()) {
                        source.onError(RuntimeException(result.error.get()))
                    } else if (result.data.isPresent()) {
                        source.onSuccess(result.data.get())
                    } else {
                        source.onError(IllegalStateException("no data or error"))
                    }
                } catch (e: IOException) {
                    source.onError(e)
                }
            }
            .subscribeOn(singleScheduler)
    }

    private fun getFormBody(method: String, params: Map<String, String>): FormBody {
        val builder = FormBody.Builder()
            .add("nonce", nonceKeeper.get().toString())
            .add("method", method)
        for ((key, value) in params) {
            builder.add(key, value)
        }
        return builder.build()
    }

    private fun formBodyToString(body: FormBody): String {
        val parts = ArrayList<String>()
        for (i in 0 until body.size()) {
            parts.add("${body.encodedName(i)}=${body.encodedValue(i)}")
        }
        return parts.joinToString("&")
    }
}

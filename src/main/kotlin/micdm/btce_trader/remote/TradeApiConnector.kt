package micdm.btce_trader.remote

import com.google.gson.Gson
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.Config
import micdm.btce_trader.model.Balance
import micdm.btce_trader.model.CurrencyPair
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TradeApiConnector @Inject constructor(private val config: Config,
                                            private val okHttpClient: OkHttpClient,
                                            private val dataSigner: DataSigner,
                                            @Named("trade") private val gson: Gson,
                                            private val currencyPair: CurrencyPair,
                                            private val nonceKeeper: NonceKeeper) {

    open class Result<T>(val data: Optional<T>, val error: Optional<String>)

    class GetInfoResult(data: Optional<Info>, error: Optional<String>): Result<Info>(data, error)

    data class Info(val funds: Map<String, BigDecimal>)

    private data class Task(val method: String, val source: SingleEmitter<Balance>)

    private val tasks: Subject<Task> = PublishSubject.create()

    init {
        tasks.subscribe {
            val body = FormBody.Builder().add("nonce", nonceKeeper.get().toString()).add("method", it.method).build()
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
                val result = gson.fromJson(response.body().string(), GetInfoResult::class.java)
                if (result.error.isPresent()) {
                    it.source.onError(RuntimeException(result.error.get()))
                }
                if (result.data.isPresent()) {
                    it.source.onSuccess(Balance(result.data.get().funds.get(currencyPair.first.name.toLowerCase())!!, result.data.get().funds.get(currencyPair.second.name.toLowerCase())!!))
                }
            } catch (e: IOException) {
                it.source.onError(e)
            }
        }
    }

    fun getBalance(): Single<Balance> {
        return Single.create { source ->
            tasks.onNext(Task("getInfo", source))
        }
    }

    private fun formBodyToString(body: FormBody): String {
        val parts = ArrayList<String>()
        for (i in 0 until body.size()) {
            parts.add("${body.encodedName(i)}=${body.encodedValue(i)}")
        }
        return parts.joinToString("&")
    }
}

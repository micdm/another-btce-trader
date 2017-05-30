package micdm.btce_trader.remote

import com.google.gson.Gson
import io.reactivex.Single
import micdm.btce_trader.model.CurrencyPair
import okhttp3.*
import java.io.IOException
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class PublicApiConnector @Inject constructor(private val currencyPair: CurrencyPair,
                                                      @Named("common") private val gson: Gson,
                                                      private val okHttpClient: OkHttpClient) {

    private class Pairs: HashMap<String, Pair>()

    private data class Pair(val last: BigDecimal)

    fun getPrice(): Single<BigDecimal> {
        return Single.create { source ->
            val key = "${currencyPair.first.name.toLowerCase()}_${currencyPair.second.name.toLowerCase()}"
            okHttpClient
                .newCall(
                    Request.Builder()
                        .url("https://btc-e.nz/api/3/ticker/$key")
                        .build()
                )
                .enqueue(object: Callback {
                    override fun onFailure(call: Call?, e: IOException?) {
                        source.onError(e)
                    }
                    override fun onResponse(call: Call?, response: Response?) {
                        val pairs = gson.fromJson(response!!.body().string(), Pairs::class.java)
                        source.onSuccess(pairs.get(key)!!.last)
                    }
                })
        }
    }
}

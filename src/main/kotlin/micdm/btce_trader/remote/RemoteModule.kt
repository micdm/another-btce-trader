package micdm.btce_trader.remote

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import dagger.Binds
import dagger.Module
import dagger.Provides
import micdm.btce_trader.*
import okhttp3.OkHttpClient
import java.util.*
import javax.inject.Named
import javax.inject.Singleton

@Module(includes = arrayOf(ImplModule::class))
class RemoteModule {

    @Provides
    @Singleton
    internal fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor {
                val now = System.currentTimeMillis()
                val response = it.proceed(it.request())
                println("${it.request()} to $response in ${System.currentTimeMillis() - now}ms")
                response
            }
            .build()
    }

    @Provides
    @Singleton
    @Named("trade")
    internal fun provideTradeGson(): Gson {
        return GsonBuilder()
            .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(TradeApiConnector.GetInfoResult::class.java, JsonDeserializer<TradeApiConnector.GetInfoResult> { json, typeOfT, context ->
                val root = json.getAsJsonObject()
                val success = root.getAsJsonPrimitive("success").getAsInt()
                when (success) {
                    0 -> TradeApiConnector.GetInfoResult(Optional.empty(), Optional.of(root.getAsJsonPrimitive("error").getAsString()))
                    1 -> TradeApiConnector.GetInfoResult(Optional.of(context.deserialize(root.getAsJsonObject("return"), TradeApiConnector.Info::class.java)), Optional.empty())
                    else -> throw IllegalStateException("unknown success value $success")
                }
            })
            .registerTypeAdapter(TradeApiConnector.GetActiveOrdersResult::class.java, JsonDeserializer<TradeApiConnector.GetActiveOrdersResult> { json, typeOfT, context ->
                val root = json.getAsJsonObject()
                val success = root.getAsJsonPrimitive("success").getAsInt()
                when (success) {
                    0 -> TradeApiConnector.GetActiveOrdersResult(Optional.empty(), Optional.of(root.getAsJsonPrimitive("error").getAsString()))
                    1 -> TradeApiConnector.GetActiveOrdersResult(Optional.of(context.deserialize(root.getAsJsonObject("return"), TradeApiConnector.ActiveOrders::class.java)), Optional.empty())
                    else -> throw IllegalStateException("unknown success value $success")
                }
            })
            .registerTypeAdapter(TradeApiConnector.GetTradeHistoryResult::class.java, JsonDeserializer<TradeApiConnector.GetTradeHistoryResult> { json, typeOfT, context ->
                val root = json.getAsJsonObject()
                val success = root.getAsJsonPrimitive("success").getAsInt()
                when (success) {
                    0 -> TradeApiConnector.GetTradeHistoryResult(Optional.empty(), Optional.of(root.getAsJsonPrimitive("error").getAsString()))
                    1 -> TradeApiConnector.GetTradeHistoryResult(Optional.of(context.deserialize(root.getAsJsonObject("return"), TradeApiConnector.Trades::class.java)), Optional.empty())
                    else -> throw IllegalStateException("unknown success value $success")
                }
            })
            .create()
    }
}

@Module
internal abstract class ImplModule {

    @Singleton
    @Binds
    internal abstract fun providePriceProvider(priceProvider: RemotePriceProvider): PriceProvider

    @Singleton
    @Binds
    internal abstract fun provideOrderHandler(orderHandler: RemoteOrderHandler): OrderHandler

    @Singleton
    @Binds
    internal abstract fun provideActiveOrdersProvider(activeOrdersProvider: RemoteActiveOrdersProvider): ActiveOrdersProvider

    @Singleton
    @Binds
    internal abstract fun provideTradeHistoryProvider(tradeHistoryProvider: RemoteTradeHistoryProvider): TradeHistoryProvider

    @Singleton
    @Binds
    internal abstract fun provideBalanceProvider(balanceProvider: RemoteBalanceProvider): BalanceProvider
}

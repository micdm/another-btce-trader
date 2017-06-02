package micdm.btce_trader

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import micdm.btce_trader.model.Currency
import micdm.btce_trader.model.CurrencyPair
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
class CommonModule {

    @Singleton
    @Provides
    @Named("first")
    fun provideFirstCurrency(config: Config): Currency = Currency.valueOf(config.getFirstCurrency())

    @Singleton
    @Provides
    @Named("second")
    fun provideSecondCurrency(config: Config): Currency = Currency.valueOf(config.getSecondCurrency())

    @Singleton
    @Provides
    fun provideCurrencyPair(@Named("first") first: Currency, @Named("second") second: Currency, config: Config): CurrencyPair = CurrencyPair(first, second, config.getDecimalPlaces())

    @Provides
    @Singleton
    @Named("common")
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    @Named("single")
    fun provideSingleScheduler(): Scheduler = Schedulers.single()

    @Provides
    @Singleton
    fun provideLogger(): Logger {
        return LoggerFactory.getLogger("main")
    }
}

package micdm.btce_trader

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import micdm.btce_trader.model.Currency
import micdm.btce_trader.model.CurrencyPair
import javax.inject.Named
import javax.inject.Singleton

@Module
class CommonModule {

    @Singleton
    @Provides
    @Named("first")
    fun provideFirstCurrency(): Currency = Currency.BTC

    @Singleton
    @Provides
    @Named("second")
    fun provideSecondCurrency(): Currency = Currency.USD

    @Singleton
    @Provides
    fun provideCurrencyPair(@Named("first") first: Currency, @Named("second") second: Currency): CurrencyPair {
        return CurrencyPair(first, second, 3)
    }

    @Provides
    @Singleton
    @Named("common")
    fun provideGson(): Gson = GsonBuilder().create()
}

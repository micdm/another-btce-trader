package micdm.btce_trader

import dagger.Module
import dagger.Provides
import micdm.btce_trader.model.Currency
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
}
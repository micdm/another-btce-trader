package micdm.btce_trader.local

import dagger.Binds
import dagger.Module
import dagger.Provides
import micdm.btce_trader.*
import micdm.btce_trader.model.Currency
import javax.inject.Named
import javax.inject.Singleton

@Module(includes = arrayOf(ImplModule::class))
class LocalModule {

    @Singleton
    @Provides
    @Named("first")
    fun provideFirstCurrencyBalanceProvider(@Named("first") currency: Currency,
                                            @Named("first") balanceBuffer: LocalBalanceBuffer): BalanceProvider = LocalBalanceProvider(currency, balanceBuffer)

    @Singleton
    @Provides
    @Named("second")
    fun provideSecondCurrencyBalanceProvider(@Named("second") currency: Currency,
                                             @Named("second") balanceBuffer: LocalBalanceBuffer): BalanceProvider = LocalBalanceProvider(currency, balanceBuffer)

    @Singleton
    @Provides
    @Named("first")
    fun provideFirstCurrencyBalanceBuffer(@Named("first") currency: Currency): LocalBalanceBuffer = LocalBalanceBuffer(currency)

    @Singleton
    @Provides
    @Named("second")
    fun provideSecondCurrencyBalanceBuffer(@Named("second") currency: Currency): LocalBalanceBuffer = LocalBalanceBuffer(currency)
}

@Module
abstract class ImplModule {

    @Singleton
    @Binds
    internal abstract fun provideActiveOrdersProvider(activeOrdersProvider: LocalActiveOrdersProvider): ActiveOrdersProvider

    @Singleton
    @Binds
    internal abstract fun provideOrderHandler(orderHandler: LocalOrderHandler): OrderHandler

    @Singleton
    @Binds
    internal abstract fun provideOrderMaker(orderMaker: LocalOrderMaker): OrderMaker

    @Singleton
    @Binds
    internal abstract fun providePriceProvider(priceProvider: LocalPriceProvider): PriceProvider

    @Singleton
    @Binds
    internal abstract fun provideTradeHistoryProvider(tradeHistoryProvider: LocalTradeHistoryProvider): TradeHistoryProvider
}

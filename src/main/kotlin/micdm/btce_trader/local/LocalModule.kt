package micdm.btce_trader.local

import dagger.Binds
import dagger.Module
import dagger.Provides
import micdm.btce_trader.*
import micdm.btce_trader.model.Balance
import java.math.BigDecimal
import javax.inject.Singleton

@Module(includes = arrayOf(ImplModule::class))
class LocalModule {

    @Singleton
    @Provides
    internal fun provideBalance(): Balance = Balance(BigDecimal.ZERO, BigDecimal("400"))
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
    internal abstract fun providePriceProvider(priceProvider: LocalPriceProvider): PriceProvider

    @Singleton
    @Binds
    internal abstract fun provideTradeHistoryProvider(tradeHistoryProvider: LocalTradeHistoryProvider): TradeHistoryProvider

    @Singleton
    @Binds
    internal abstract fun provideBalanceProvider(balanceProvider: LocalBalanceProvider): BalanceProvider
}

package micdm.btce_trader

import dagger.Component
import micdm.btce_trader.misc.MainThreadExecutor
import micdm.btce_trader.remote.RemoteModule
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(CommonModule::class, RemoteModule::class))
interface RemoteComponent {

    fun getActiveOrdersProvider(): ActiveOrdersProvider
    @Named("first") fun getFirstCurrencyBalanceProvider(): BalanceProvider
    @Named("second") fun getSecondCurrencyBalanceProvider(): BalanceProvider
    fun getOrderHandler(): OrderHandler
    fun getOrderMaker(): OrderMaker
    fun getPriceProvider(): PriceProvider
    fun getMainThreadExecutor(): MainThreadExecutor
}

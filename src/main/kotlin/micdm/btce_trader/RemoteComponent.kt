package micdm.btce_trader

import dagger.Component
import micdm.btce_trader.remote.RemoteModule
import org.slf4j.Logger
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(CommonModule::class, RemoteModule::class))
interface RemoteComponent {

    fun getActiveOrdersProvider(): ActiveOrdersProvider
    fun getBalanceProvider(): BalanceProvider
    fun getOrderHandler(): OrderHandler
    fun getOrderStrategy(): OrderStrategy
    fun getPriceProvider(): PriceProvider
    fun getTradeHistoryProvider(): TradeHistoryProvider
    fun getLogger(): Logger
    fun getRunner(): Runner
}

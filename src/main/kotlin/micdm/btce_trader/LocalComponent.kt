package micdm.btce_trader

import dagger.Component
import micdm.btce_trader.local.LocalModule
import org.slf4j.Logger
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(CommonModule::class, LocalModule::class))
interface LocalComponent {

    fun getActiveOrdersProvider(): ActiveOrdersProvider
    fun getBalanceProvider(): BalanceProvider
    fun getOrderHandler(): OrderHandler
    fun getOrderStrategy(): OrderStrategy
    fun getPriceProvider(): PriceProvider
    fun getLogger(): Logger
    fun getRunner(): Runner
}

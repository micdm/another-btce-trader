package micdm.btce_trader

import dagger.Component
import micdm.btce_trader.local.LocalModule
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(CommonModule::class, LocalModule::class, UtilModule::class))
interface LocalComponent {

    @Named("first") fun getFirstCurrencyBalanceProvider(): BalanceProvider
    @Named("second") fun getSecondCurrencyBalanceProvider(): BalanceProvider
    fun getOrderHandler(): OrderHandler
    fun getOrderMaker(): OrderMaker
    fun getPriceProvider(): PriceProvider
}

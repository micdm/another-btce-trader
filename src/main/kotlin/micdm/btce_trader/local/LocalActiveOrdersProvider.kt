package micdm.btce_trader.local

import io.reactivex.Observable
import micdm.btce_trader.ActiveOrdersProvider
import micdm.btce_trader.model.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalActiveOrdersProvider @Inject constructor(private val activeOrdersBuffer: LocalActiveOrdersBuffer): ActiveOrdersProvider {

    override fun getActiveOrders(): Observable<Collection<Order>> = activeOrdersBuffer.get()
}

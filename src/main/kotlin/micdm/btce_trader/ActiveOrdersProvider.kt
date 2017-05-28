package micdm.btce_trader

import io.reactivex.Observable
import micdm.btce_trader.model.Order

interface ActiveOrdersProvider {

    fun getActiveOrders(): Observable<Collection<Order>>
}

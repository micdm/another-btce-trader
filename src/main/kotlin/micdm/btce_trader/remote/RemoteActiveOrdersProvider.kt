package micdm.btce_trader.remote

import io.reactivex.Observable
import micdm.btce_trader.ActiveOrdersProvider
import micdm.btce_trader.model.Order
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RemoteActiveOrdersProvider @Inject constructor(): ActiveOrdersProvider {

    override fun getActiveOrders(): Observable<Collection<Order>> {
        return Observable.just(Collections.emptyList())
    }
}

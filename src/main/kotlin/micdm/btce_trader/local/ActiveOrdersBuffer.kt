package micdm.btce_trader.local

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import micdm.btce_trader.model.Order
import org.slf4j.Logger
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ActiveOrdersBuffer @Inject constructor(private val logger: Logger) {

    private val orders = BehaviorSubject.createDefault<Collection<Order>>(Collections.emptyList())

    fun get(): Observable<Collection<Order>> = orders

    fun getOrder(id: String): Optional<Order> {
        return orders.value.stream().filter{ it.id == id }.findFirst()
    }

    fun add(order: Order) {
        logger.info("Creating order $order")
        val orders = ArrayList(this.orders.value)
        orders.add(order)
        this.orders.onNext(orders)
    }

    fun remove(id: String) {
        logger.info("Removing order $id")
        val orders = ArrayList(this.orders.value)
        orders.removeIf{ it.id == id }
        this.orders.onNext(orders)
    }
}

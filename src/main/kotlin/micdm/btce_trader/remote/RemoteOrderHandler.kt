package micdm.btce_trader.remote

import io.reactivex.Observable
import micdm.btce_trader.OrderHandler
import micdm.btce_trader.OrderMaker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RemoteOrderHandler @Inject constructor(private val orderMaker: OrderMaker,
                                                      private val tradeApiConnector: TradeApiConnector,
                                                      private val orderStatusBuffer: OrderStatusBuffer): OrderHandler {

    override fun start() {
        orderMaker.getCreateRequests()
            .doOnNext { println("Creating order $it")}
            .switchMap {
                tradeApiConnector.createOrder(it)
                    .toObservable()
                    .doOnError { println("Cannot create order: $it") }
                    .onErrorResumeNext(Observable.empty())
            }
            .doOnNext { println("Order created") }
            .subscribe { orderStatusBuffer.created() }
        orderMaker.getCancelRequests()
            .doOnNext { println("Cancelling order $it") }
            .switchMap {
                tradeApiConnector.cancelOrder(it)
                    .toObservable()
                    .doOnError { println("Cannot cancel order: $it") }
                    .onErrorResumeNext(Observable.empty())
            }
            .doOnNext { println("Order canceled") }
            .subscribe { orderStatusBuffer.canceled() }
    }
}

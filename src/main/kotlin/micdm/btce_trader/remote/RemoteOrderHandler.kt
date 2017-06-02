package micdm.btce_trader.remote

import io.reactivex.Observable
import micdm.btce_trader.OrderHandler
import micdm.btce_trader.OrderMaker
import org.slf4j.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RemoteOrderHandler @Inject constructor(private val orderMaker: OrderMaker,
                                                      private val tradeApiConnector: TradeApiConnector,
                                                      private val orderStatusBuffer: OrderStatusBuffer,
                                                      private val logger: Logger): OrderHandler {

    override fun start() {
        orderMaker.getCreateRequests()
            .flatMap { Observable.fromIterable(it) }
            .doOnNext { logger.info("Creating order $it")}
            .flatMap {
                tradeApiConnector.createOrder(it)
                    .toObservable()
                    .doOnError { logger.warn("Cannot create order: $it") }
                    .onErrorResumeNext(Observable.empty())
            }
            .doOnNext { logger.info("Order created") }
            .subscribe { orderStatusBuffer.created() }
        orderMaker.getCancelRequests()
            .flatMap { Observable.fromIterable(it) }
            .doOnNext { logger.info("Cancelling order $it") }
            .flatMap {
                tradeApiConnector.cancelOrder(it)
                    .toObservable()
                    .doOnError { logger.warn("Cannot cancel order: $it") }
                    .onErrorResumeNext(Observable.empty())
            }
            .doOnNext { logger.info("Order canceled") }
            .subscribe { orderStatusBuffer.canceled() }
    }
}

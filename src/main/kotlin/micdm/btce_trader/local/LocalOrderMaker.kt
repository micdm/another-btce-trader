package micdm.btce_trader.local

import io.reactivex.Observable
import io.reactivex.functions.Function3
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.ActiveOrdersProvider
import micdm.btce_trader.OrderMaker
import micdm.btce_trader.PriceProvider
import micdm.btce_trader.TradeHistoryProvider
import micdm.btce_trader.model.Order
import micdm.btce_trader.model.OrderData
import micdm.btce_trader.model.OrderType
import micdm.btce_trader.model.Trade
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalOrderMaker @Inject constructor(activeOrdersProvider: ActiveOrdersProvider,
                                                   priceProvider: PriceProvider,
                                                   tradeHistoryProvider: TradeHistoryProvider): OrderMaker {

    private val createRequests: Subject<OrderData> = PublishSubject.create()
    private val cancelRequests: Subject<String> = PublishSubject.create()

    init {
        priceProvider.getPrices()
            .withLatestFrom(
                activeOrdersProvider.getActiveOrders(),
                tradeHistoryProvider.getTradeHistory(),
                Function3<BigDecimal, Collection<Order>, Collection<Trade>, Optional<OrderData>> { price, activeOrders, trades ->
                    getDataToCreateOrder(price, activeOrders, trades)
                }
            )
            .filter { it.isPresent() }
            .map { it.get() }
            .subscribe(createRequests::onNext)
        priceProvider.getPrices()
            .withLatestFrom(
                activeOrdersProvider.getActiveOrders(),
                tradeHistoryProvider.getTradeHistory(),
                Function3<BigDecimal, Collection<Order>, Collection<Trade>, Optional<String>> { price, activeOrders, trades ->
                    getOrderIdToCancel(price, activeOrders, trades)
                }
            )
            .filter { it.isPresent() }
            .map { it.get() }
            .subscribe(cancelRequests::onNext)
    }

    private fun getDataToCreateOrder(price: BigDecimal, activeOrders: Collection<Order>, trades: Collection<Trade>): Optional<OrderData> {
        if (activeOrders.isEmpty()) {
            if (trades.isEmpty()) {
                println("No trades made yet, creating a new one")
                return Optional.of(OrderData(OrderType.BUY, price, BigDecimal.ONE))
            } else {
                val (data) = trades.last()
                if (data.type == OrderType.BUY) {
                    return Optional.of(OrderData(OrderType.SELL, data.price * BigDecimal("1.03"), data.amount))
                }
                if (data.type == OrderType.SELL) {
                    return Optional.of(OrderData(OrderType.BUY, data.price * BigDecimal("0.97"), data.amount))
                }
            }
        }
        return Optional.empty()
    }

    private fun getOrderIdToCancel(price: BigDecimal, activeOrders: Collection<Order>, trades: Collection<Trade>): Optional<String> {
        if (activeOrders.isEmpty() || trades.isEmpty()) {
            return Optional.empty()
        }
        val order = activeOrders.first()
        val trade = trades.last()
        if (order.data.type == OrderType.BUY && price / trade.data.price > BigDecimal("1.01")) {
            println("Price is going to get bigger ($price > ${trade.data.price})")
            return Optional.of(order.id)
        }
        if (order.data.type == OrderType.SELL && trade.data.price / price > BigDecimal("1.01")) {
            println("Price is going to get smaller (${trade.data.price} > $price)")
            return Optional.of(order.id)
        }
        return Optional.empty()
    }

    override fun getCreateRequests(): Observable<OrderData> = createRequests

    override fun getCancelRequests(): Observable<String>  = cancelRequests
}

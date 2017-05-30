package micdm.btce_trader

import io.reactivex.Observable
import io.reactivex.functions.Function3
import io.reactivex.functions.Function5
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.model.Order
import micdm.btce_trader.model.OrderData
import micdm.btce_trader.model.OrderType
import micdm.btce_trader.model.Trade
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class OrderMaker @Inject constructor(activeOrdersProvider: ActiveOrdersProvider,
                                     @Named("first") firstCurrencyBalanceProvider: BalanceProvider,
                                     @Named("second") secondCurrencyBalanceProvider: BalanceProvider,
                                     priceProvider: PriceProvider,
                                     tradeHistoryProvider: TradeHistoryProvider) {

    private val PRICE_DELTA = BigDecimal("0.03")
    private val PRICE_THRESHOLD = BigDecimal("0.01")
    private val ORDER_AMOUNT = BigDecimal("0.1")

    private val createRequests: Subject<OrderData> = PublishSubject.create()
    private val cancelRequests: Subject<String> = PublishSubject.create()

    init {
        priceProvider.getPrices()
            .withLatestFrom(
                activeOrdersProvider.getActiveOrders(),
                tradeHistoryProvider.getTradeHistory(),
                firstCurrencyBalanceProvider.getBalance(),
                secondCurrencyBalanceProvider.getBalance(),
                Function5<BigDecimal, Collection<Order>, Collection<Trade>, BigDecimal, BigDecimal, Optional<OrderData>> { price, activeOrders, trades, firstCurrencyBalance, secondCurrencyBalance ->
                    getDataToCreateOrder(price, activeOrders, trades, firstCurrencyBalance, secondCurrencyBalance)
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

    private fun getDataToCreateOrder(price: BigDecimal, activeOrders: Collection<Order>, trades: Collection<Trade>,
                                     firstCurrencyBalance: BigDecimal, secondCurrencyBalance: BigDecimal): Optional<OrderData> {
        if (activeOrders.isEmpty()) {
            if (trades.isEmpty()) {
                println("No trades made yet, creating a new one")
                return getDataToCreateFirstOrder(price, secondCurrencyBalance)
            } else {
                val (data) = trades.last()
                if (data.type == OrderType.BUY) {
                    println("Creating SELL order")
                    return getDataToCreateSellOrder(data.price, data.amount, firstCurrencyBalance)
                }
                if (data.type == OrderType.SELL) {
                    println("Creating BUY order")
                    return getDataToCreateBuyOrder(data.price, data.amount, secondCurrencyBalance)
                }
            }
        }
        return Optional.empty()
    }

    private fun getDataToCreateFirstOrder(price: BigDecimal, secondCurrencyBalance: BigDecimal): Optional<OrderData> {
        if (secondCurrencyBalance >= price * ORDER_AMOUNT) {
            return Optional.of(OrderData(OrderType.BUY, price, ORDER_AMOUNT))
        } else {
            println("Not enough money")
            return Optional.empty()
        }
    }

    private fun getDataToCreateBuyOrder(price: BigDecimal, amount: BigDecimal, secondCurrencyBalance: BigDecimal): Optional<OrderData> {
        val newPrice = price * (BigDecimal.ONE - PRICE_DELTA)
        if (secondCurrencyBalance >= newPrice * amount) {
            return Optional.of(OrderData(OrderType.BUY, newPrice, amount))
        } else {
            println("Not enough money")
            return Optional.empty()
        }
    }

    private fun getDataToCreateSellOrder(price: BigDecimal, amount: BigDecimal, firstCurrencyBalance: BigDecimal): Optional<OrderData> {
        if (firstCurrencyBalance >= amount) {
            return Optional.of(OrderData(OrderType.SELL, price * (BigDecimal.ONE + PRICE_DELTA), amount))
        } else {
            println("Not enough money")
            return Optional.empty()
        }
    }

    private fun getOrderIdToCancel(price: BigDecimal, activeOrders: Collection<Order>, trades: Collection<Trade>): Optional<String> {
        if (activeOrders.isEmpty() || trades.isEmpty()) {
            return Optional.empty()
        }
        val order = activeOrders.first()
        val trade = trades.last()
        if (order.data.type == OrderType.BUY && (price - trade.data.price) / trade.data.price > PRICE_THRESHOLD) {
            println("Price is going to get bigger (previous=${trade.data.price}, new=$price)")
            return Optional.of(order.id)
        }
        if (order.data.type == OrderType.SELL && (trade.data.price - price) / trade.data.price > PRICE_THRESHOLD) {
            println("Price is going to get smaller (previous=${trade.data.price}, new=$price)")
            return Optional.of(order.id)
        }
        return Optional.empty()
    }

    fun getCreateRequests(): Observable<OrderData> = createRequests

    fun getCancelRequests(): Observable<String> = cancelRequests
}

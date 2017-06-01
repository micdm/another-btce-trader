package micdm.btce_trader

import io.reactivex.Observable
import io.reactivex.functions.Function3
import io.reactivex.functions.Function4
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.model.*
import org.slf4j.Logger
import java.math.BigDecimal
import java.math.MathContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderMaker @Inject constructor(private val logger: Logger,
                                     currencyPair: CurrencyPair,
                                     activeOrdersProvider: ActiveOrdersProvider,
                                     balanceProvider: BalanceProvider,
                                     priceProvider: PriceProvider,
                                     tradeHistoryProvider: TradeHistoryProvider) {

    private val PRICE_ROUNDING = MathContext(currencyPair.decimalPlaces)
    private val AMOUNT_ROUNDING = MathContext(currencyPair.decimalPlaces)
    private val PRICE_DELTA = BigDecimal("0.03")
    private val PRICE_THRESHOLD = BigDecimal("0.01")
    private val ORDER_AMOUNT = BigDecimal("0.001")

    private val createRequests: Subject<OrderData> = PublishSubject.create()
    private val cancelRequests: Subject<String> = PublishSubject.create()

    init {
        priceProvider.getPrices()
            .withLatestFrom(
                activeOrdersProvider.getActiveOrders(),
                tradeHistoryProvider.getTradeHistory(),
                balanceProvider.getBalance(),
                Function4<BigDecimal, Collection<Order>, Collection<Trade>, Balance, Optional<OrderData>> { price, activeOrders, trades, balance ->
                    getDataToCreateOrder(price, activeOrders, trades, balance)
                }
            )
            .filter { it.isPresent() }
            .map { it.get() }
            .map { (type, price, amount) ->
                OrderData(type, price.round(PRICE_ROUNDING), amount.round(AMOUNT_ROUNDING))
            }
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

    private fun getDataToCreateOrder(price: BigDecimal, activeOrders: Collection<Order>, trades: Collection<Trade>, balance: Balance): Optional<OrderData> {
        if (activeOrders.isEmpty()) {
            if (trades.isEmpty()) {
                logger.info("No trades made yet, creating a new one")
                return getDataToCreateFirstOrder(price, balance.second)
            } else {
                val data = trades.first().data
                if (data.type == OrderType.BUY) {
                    logger.info("Creating SELL order")
                    return getDataToCreateSellOrder(data.price, ORDER_AMOUNT, balance.first)
                }
                if (data.type == OrderType.SELL) {
                    logger.info("Creating BUY order")
                    return getDataToCreateBuyOrder(data.price, ORDER_AMOUNT, balance.second)
                }
            }
        }
        return Optional.empty()
    }

    private fun getDataToCreateFirstOrder(price: BigDecimal, balance: BigDecimal): Optional<OrderData> {
        if (balance >= price * ORDER_AMOUNT) {
            return Optional.of(OrderData(OrderType.BUY, price, ORDER_AMOUNT))
        } else {
            logger.info("Not enough money")
            return Optional.empty()
        }
    }

    private fun getDataToCreateBuyOrder(price: BigDecimal, amount: BigDecimal, balance: BigDecimal): Optional<OrderData> {
        val newPrice = price * (BigDecimal.ONE - PRICE_DELTA)
        if (balance >= newPrice * amount) {
            return Optional.of(OrderData(OrderType.BUY, newPrice, amount))
        } else {
            logger.info("Not enough money")
            return Optional.empty()
        }
    }

    private fun getDataToCreateSellOrder(price: BigDecimal, amount: BigDecimal, balance: BigDecimal): Optional<OrderData> {
        if (balance >= amount) {
            return Optional.of(OrderData(OrderType.SELL, price * (BigDecimal.ONE + PRICE_DELTA), amount))
        } else {
            logger.info("Not enough money")
            return Optional.empty()
        }
    }

    private fun getOrderIdToCancel(price: BigDecimal, activeOrders: Collection<Order>, trades: Collection<Trade>): Optional<String> {
        if (activeOrders.isEmpty() || trades.isEmpty()) {
            return Optional.empty()
        }
        val order = activeOrders.first()
        val trade = trades.first()
        if (order.data.type == OrderType.BUY && (price - trade.data.price) / trade.data.price > PRICE_THRESHOLD) {
            logger.info("Price is going to get bigger (previous=${trade.data.price}, new=$price)")
            return Optional.of(order.id)
        }
        if (order.data.type == OrderType.SELL && (trade.data.price - price) / trade.data.price > PRICE_THRESHOLD) {
            logger.info("Price is going to get smaller (previous=${trade.data.price}, new=$price)")
            return Optional.of(order.id)
        }
        return Optional.empty()
    }

    fun getCreateRequests(): Observable<OrderData> = createRequests

    fun getCancelRequests(): Observable<String> = cancelRequests
}

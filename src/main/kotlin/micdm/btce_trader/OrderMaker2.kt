package micdm.btce_trader

import io.reactivex.Observable
import io.reactivex.functions.Function3
import io.reactivex.functions.Function4
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import micdm.btce_trader.model.*
import org.slf4j.Logger
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.ArrayList

@Singleton
class OrderMaker2 @Inject constructor(private val logger: Logger,
                                      private val currencyPair: CurrencyPair,
                                      private val activeOrdersProvider: ActiveOrdersProvider,
                                      private val balanceProvider: BalanceProvider,
                                      private val priceProvider: PriceProvider,
                                      private val tradeHistoryProvider: TradeHistoryProvider,
                                      config: Config): OrderMaker {

    private val PRICE_DELTA = config.getPriceDelta()
    private val PRICE_THRESHOLD = config.getPriceThreshold()
    private val ORDER_AMOUNT = config.getOrderAmount()

    private val createRequests: Subject<Collection<OrderData>> = PublishSubject.create()
    private val cancelRequests: Subject<Collection<String>> = PublishSubject.create()

    override fun start() {
        priceProvider.getPrices()
            .withLatestFrom(
                activeOrdersProvider.getActiveOrders(),
                tradeHistoryProvider.getTradeHistory(),
                balanceProvider.getBalance(),
                Function4<BigDecimal, Collection<Order>, Collection<Trade>, Balance, Collection<OrderData>> { price, activeOrders, trades, balance ->
                    getDataToCreateSellOrders(price, activeOrders, trades, balance) + getDataToCreateBuyOrders(price, activeOrders, balance)
                }
            )
            .map { orders ->
                orders.map { it.copy(price=it.price.setScale(currencyPair.decimalPlaces, RoundingMode.HALF_UP),
                                     amount=it.amount.setScale(currencyPair.decimalPlaces, RoundingMode.HALF_UP)) }
            }
            .subscribe(createRequests::onNext)
        priceProvider.getPrices()
            .withLatestFrom(
                activeOrdersProvider.getActiveOrders(),
                tradeHistoryProvider.getTradeHistory(),
                Function3<BigDecimal, Collection<Order>, Collection<Trade>, Collection<String>> { price, activeOrders, trades ->
                    getSellOrderIdsToCancel(activeOrders, trades) + getBuyOrderIdsToCancel(price, activeOrders)
                }
            )
            .subscribe(cancelRequests::onNext)
    }

    private fun getDataToCreateSellOrders(price: BigDecimal, activeOrders: Collection<Order>, trades: Collection<Trade>, balance: Balance): Collection<OrderData> {
        val sellOrders = activeOrders.filter { it.data.type == OrderType.SELL }
        if (sellOrders.isEmpty()) {
            if (balance.first > currencyPair.minOrderAmount) {
                if (trades.takeWhile { it.data.type == OrderType.BUY } .isEmpty()) {
                    return listOf(OrderData(OrderType.SELL, price, balance.first))
                } else {
                    val newPrice = getPriceToSell(trades)
                    if (newPrice.isPresent) {
                        return listOf(OrderData(OrderType.SELL, newPrice.get(), balance.first))
                    } else {
                        logger.debug("No average price available (probably strange?)")
                    }
                }
            }
        }
        return emptyList()
    }

    private fun getPriceToSell(trades: Collection<Trade>): Optional<BigDecimal> {
        val buys = trades.takeWhile { it.data.type == OrderType.BUY }
        if (buys.isEmpty()) {
            return Optional.empty()
        }
        val average = (buys.fold(BigDecimal.ZERO, { accumulated, trade -> accumulated + trade.data.price * (BigDecimal.ONE + PRICE_DELTA) }) / BigDecimal(buys.size)).setScale(currencyPair.decimalPlaces, RoundingMode.HALF_UP)
        logger.debug("Average sell price is $average")
        return Optional.of(average)
    }

    private fun getDataToCreateBuyOrders(price: BigDecimal, activeOrders: Collection<Order>, balance: Balance): Collection<OrderData> {
        val orders = ArrayList<OrderData>()
        var buyPrice = (activeOrders.filter { it.data.type == OrderType.BUY } .map { it.data.price } .min() ?: price) * (BigDecimal.ONE - PRICE_DELTA)
        var amount = balance.second
        while (amount >= buyPrice * ORDER_AMOUNT) {
            orders.add(OrderData(OrderType.BUY, buyPrice, ORDER_AMOUNT))
            amount -= buyPrice * ORDER_AMOUNT
            buyPrice *= (BigDecimal.ONE - PRICE_DELTA)
        }
        return orders
    }

    private fun getSellOrderIdsToCancel(activeOrders: Collection<Order>, trades: Collection<Trade>): Collection<String> {
        val sellOrders = activeOrders.filter { it.data.type == OrderType.SELL }
        if (sellOrders.isNotEmpty()) {
            val sellPrice = sellOrders[0].data.price
            val newSellPrice = getPriceToSell(trades)
            if (newSellPrice.isPresent && newSellPrice.get().compareTo(sellPrice) != 0) {
                logger.debug("Sell price does not match (current=$sellPrice, new=${newSellPrice.get()}), we have to cancel current sell order")
                return listOf(sellOrders[0].id)
            }
        }
        return emptyList()
    }

    private fun getBuyOrderIdsToCancel(price: BigDecimal, activeOrders: Collection<Order>): Collection<String> {
        val buyOrders = activeOrders.filter { it.data.type == OrderType.BUY }
        val buyPrice =  buyOrders.map { it.data.price } .max()
        if (buyPrice != null && (price - buyPrice) / buyPrice > PRICE_THRESHOLD) {
            logger.debug("Price is going to be too high, we have to cancel all current buy orders")
            return buyOrders.map { it.id }
        }
        return emptyList()
    }

    override fun getCreateRequests(): Observable<Collection<OrderData>> = createRequests

    override fun getCancelRequests(): Observable<Collection<String>> = cancelRequests
}

package micdm.btce_trader.strategies

import micdm.btce_trader.model.*
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SlidingOrderStrategy @Inject constructor(private val logger: org.slf4j.Logger,
                                               private val currencyPair: micdm.btce_trader.model.CurrencyPair,
                                               private val activeOrdersProvider: micdm.btce_trader.ActiveOrdersProvider,
                                               private val balanceProvider: micdm.btce_trader.BalanceProvider,
                                               private val priceProvider: micdm.btce_trader.PriceProvider,
                                               private val tradeHistoryProvider: micdm.btce_trader.TradeHistoryProvider,
                                               config: micdm.btce_trader.Config): micdm.btce_trader.OrderStrategy {

    private val PRICE_DELTA = config.getPriceDelta()
    private val PRICE_THRESHOLD = config.getPriceThreshold()
    private val ORDER_AMOUNT = config.getOrderAmount()

    private val createRequests: io.reactivex.subjects.Subject<Collection<OrderData>> = io.reactivex.subjects.PublishSubject.create()
    private val cancelRequests: io.reactivex.subjects.Subject<Collection<String>> = io.reactivex.subjects.PublishSubject.create()

    override fun start() {
        priceProvider.getPrices()
            .withLatestFrom(
                activeOrdersProvider.getActiveOrders(),
                tradeHistoryProvider.getTradeHistory(),
                balanceProvider.getBalance(),
                io.reactivex.functions.Function4<BigDecimal, Collection<Order>, Collection<Trade>, Balance, Collection<OrderData>> { price, activeOrders, trades, balance ->
                    getDataToCreateSellOrders(price, activeOrders, trades, balance) + getDataToCreateBuyOrders(price, activeOrders, balance)
                }
            )
            .map { orders ->
                orders.map { it.copy(price=it.price.setScale(currencyPair.decimalPlaces, java.math.RoundingMode.HALF_UP),
                                     amount=it.amount.setScale(currencyPair.decimalPlaces, java.math.RoundingMode.HALF_UP)) }
            }
            .subscribe(createRequests::onNext)
        priceProvider.getPrices()
            .withLatestFrom(
                activeOrdersProvider.getActiveOrders(),
                tradeHistoryProvider.getTradeHistory(),
                io.reactivex.functions.Function3<BigDecimal, Collection<Order>, Collection<Trade>, Collection<String>> { price, activeOrders, trades ->
                    getSellOrderIdsToCancel(activeOrders, trades) + getBuyOrderIdsToCancel(price, activeOrders)
                }
            )
            .subscribe(cancelRequests::onNext)
    }

    private fun getDataToCreateSellOrders(price: java.math.BigDecimal, activeOrders: Collection<micdm.btce_trader.model.Order>, trades: Collection<micdm.btce_trader.model.Trade>, balance: micdm.btce_trader.model.Balance): Collection<micdm.btce_trader.model.OrderData> {
        val sellOrders = activeOrders.filter { it.data.type == micdm.btce_trader.model.OrderType.SELL }
        if (sellOrders.isEmpty()) {
            if (balance.first > currencyPair.minOrderAmount) {
                if (trades.takeWhile { it.data.type == micdm.btce_trader.model.OrderType.BUY } .isEmpty()) {
                    return listOf(micdm.btce_trader.model.OrderData(OrderType.SELL, price, balance.first))
                } else {
                    val newPrice = getPriceToSell(trades)
                    if (newPrice.isPresent) {
                        return listOf(micdm.btce_trader.model.OrderData(OrderType.SELL, newPrice.get(), balance.first))
                    } else {
                        logger.debug("No average price available (probably strange?)")
                    }
                }
            }
        }
        return emptyList()
    }

    private fun getPriceToSell(trades: Collection<micdm.btce_trader.model.Trade>): java.util.Optional<BigDecimal> {
        val buys = trades.takeWhile { it.data.type == micdm.btce_trader.model.OrderType.BUY }
        if (buys.isEmpty()) {
            return java.util.Optional.empty()
        }
        val average = (buys.fold(java.math.BigDecimal.ZERO, { accumulated, trade -> accumulated + trade.data.price * (java.math.BigDecimal.ONE + PRICE_DELTA) }) / java.math.BigDecimal(buys.size)).setScale(currencyPair.decimalPlaces, java.math.RoundingMode.HALF_UP)
        logger.debug("Average sell price is $average")
        return java.util.Optional.of(average)
    }

    private fun getDataToCreateBuyOrders(price: java.math.BigDecimal, activeOrders: Collection<micdm.btce_trader.model.Order>, balance: micdm.btce_trader.model.Balance): Collection<micdm.btce_trader.model.OrderData> {
        val orders = kotlin.collections.ArrayList<OrderData>()
        var buyPrice = (activeOrders.filter { it.data.type == micdm.btce_trader.model.OrderType.BUY } .map { it.data.price } .min() ?: price) * (java.math.BigDecimal.ONE - PRICE_DELTA)
        var amount = balance.second
        while (amount >= buyPrice * ORDER_AMOUNT) {
            orders.add(micdm.btce_trader.model.OrderData(OrderType.BUY, buyPrice, ORDER_AMOUNT))
            amount -= buyPrice * ORDER_AMOUNT
            buyPrice *= (java.math.BigDecimal.ONE - PRICE_DELTA)
        }
        return orders
    }

    private fun getSellOrderIdsToCancel(activeOrders: Collection<micdm.btce_trader.model.Order>, trades: Collection<micdm.btce_trader.model.Trade>): Collection<String> {
        val sellOrders = activeOrders.filter { it.data.type == micdm.btce_trader.model.OrderType.SELL }
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

    private fun getBuyOrderIdsToCancel(price: java.math.BigDecimal, activeOrders: Collection<micdm.btce_trader.model.Order>): Collection<String> {
        val buyOrders = activeOrders.filter { it.data.type == micdm.btce_trader.model.OrderType.BUY }
        val buyPrice =  buyOrders.map { it.data.price } .max()
        if (buyPrice != null && (price - buyPrice) / buyPrice > PRICE_THRESHOLD) {
            logger.debug("Price is going to be too high, we have to cancel all current buy orders")
            return buyOrders.map { it.id }
        }
        return emptyList()
    }

    override fun getCreateRequests(): io.reactivex.Observable<Collection<OrderData>> = createRequests

    override fun getCancelRequests(): io.reactivex.Observable<Collection<String>> = cancelRequests
}

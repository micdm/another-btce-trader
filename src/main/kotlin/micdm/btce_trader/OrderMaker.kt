package micdm.btce_trader

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
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
class OrderMaker @Inject constructor(private val logger: Logger,
                                     currencyPair: CurrencyPair,
                                     activeOrdersProvider: ActiveOrdersProvider,
                                     balanceProvider: BalanceProvider,
                                     priceProvider: PriceProvider,
                                     config: Config,
                                     tradeHistoryProvider: TradeHistoryProvider) {

    private val PRICE_DELTA = config.getPriceDelta()
    private val PRICE_THRESHOLD = config.getPriceThreshold()
    private val ORDER_AMOUNT = config.getOrderAmount()

    private val createRequests: Subject<Collection<OrderData>> = PublishSubject.create()
    private val cancelRequests: Subject<Collection<String>> = PublishSubject.create()

    init {
        logger.info("Trading options are: PAIR=$currencyPair, PRICE_DELTA=$PRICE_DELTA, PRICE_THRESHOLD=$PRICE_THRESHOLD, ORDER_AMOUNT=$ORDER_AMOUNT")
        priceProvider.getPrices()
            .withLatestFrom(
                activeOrdersProvider.getActiveOrders(),
                tradeHistoryProvider.getTradeHistory(),
                balanceProvider.getBalance(),
                Function4<BigDecimal, Collection<Order>, Collection<Trade>, Balance, Collection<OrderData>> { price, activeOrders, trades, balance ->
                    getDataToCreateOrders(price, activeOrders, trades, balance)
                }
            )
            .map { orders ->
                orders.map { it.copy(price=it.price.setScale(currencyPair.decimalPlaces, RoundingMode.HALF_UP), amount=it.amount.setScale(currencyPair.decimalPlaces, RoundingMode.HALF_UP)) }
            }
            .subscribe(createRequests::onNext)
        priceProvider.getPrices()
            .withLatestFrom(
                activeOrdersProvider.getActiveOrders(),
                BiFunction<BigDecimal, Collection<Order>, Collection<String>> { price, activeOrders ->
                    getOrderIdsToCancel(price, activeOrders)
                }
            )
            .subscribe(cancelRequests::onNext)
    }

    private fun getDataToCreateOrders(price: BigDecimal, activeOrders: Collection<Order>, trades: Collection<Trade>, balance: Balance): Collection<OrderData> {
        if (activeOrders.size == 2) {
            return emptyList()
        }
        val datas = ArrayList<OrderData>()
        if (activeOrders.find { it.data.type == OrderType.BUY } == null) {
            logger.info("Creating BUY order")
            val data = getDataToCreateBuyOrder(price, ORDER_AMOUNT, balance.second)
            if (data.isPresent()) {
                datas.add(data.get())
            }
        }
        if (activeOrders.find { it.data.type == OrderType.SELL } == null) {
            logger.info("Creating SELL order")
            val data = getDataToCreateSellOrder(price, ORDER_AMOUNT, balance.first)
            if (data.isPresent()) {
                datas.add(data.get())
            }
        }
        return datas
    }

    private fun getDataToCreateBuyOrder(price: BigDecimal, amount: BigDecimal, balance: BigDecimal): Optional<OrderData> {
        val newPrice = price * (BigDecimal.ONE - PRICE_DELTA)
        if (balance >= newPrice * amount) {
            return Optional.of(OrderData(OrderType.BUY, newPrice, amount))
        } else {
            logger.warn("Not enough money")
            return Optional.empty()
        }
    }

    private fun getDataToCreateSellOrder(price: BigDecimal, amount: BigDecimal, balance: BigDecimal): Optional<OrderData> {
        if (balance >= amount) {
            return Optional.of(OrderData(OrderType.SELL, price * (BigDecimal.ONE + PRICE_DELTA), amount))
        } else {
            logger.warn("Not enough money")
            return Optional.empty()
        }
    }

    private fun getOrderIdsToCancel(price: BigDecimal, activeOrders: Collection<Order>): Collection<String> {
        if (activeOrders.isEmpty()) {
            return emptyList()
        }
        val (id, data) = activeOrders.first()
        if (data.type == OrderType.BUY && (price - data.price) / data.price > PRICE_DELTA + PRICE_THRESHOLD) {
            logger.info("Price is going to get too big (previous=${data.price}, new=$price)")
            return arrayListOf(id)
        }
        if (data.type == OrderType.SELL && (data.price - price) / data.price > PRICE_DELTA + PRICE_THRESHOLD) {
            logger.info("Price is going to get too small (previous=${data.price}, new=$price)")
            return arrayListOf(id)
        }
        return emptyList()
    }

    fun getCreateRequests(): Observable<Collection<OrderData>> = createRequests

    fun getCancelRequests(): Observable<Collection<String>> = cancelRequests
}

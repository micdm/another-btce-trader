package micdm.btce_trader

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
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
                                     config: Config) {

    private val PRICE_ROUNDING = MathContext(currencyPair.decimalPlaces)
    private val AMOUNT_ROUNDING = MathContext(currencyPair.decimalPlaces)
    private val PRICE_DELTA = config.getPriceDelta()
    private val PRICE_THRESHOLD = config.getPriceThreshold()
    private val ORDER_AMOUNT = config.getOrderAmount()

    private val createRequests: Subject<OrderData> = PublishSubject.create()
    private val cancelRequests: Subject<String> = PublishSubject.create()

    init {
        logger.info("Trading options are: PRICE_DELTA=$PRICE_DELTA, PRICE_THRESHOLD=$PRICE_THRESHOLD, ORDER_AMOUNT=$ORDER_AMOUNT")
        priceProvider.getPrices()
            .withLatestFrom(
                activeOrdersProvider.getActiveOrders(),
                balanceProvider.getBalance(),
                Function3<BigDecimal, Collection<Order>, Balance, Optional<OrderData>> { price, activeOrders, balance ->
                    getDataToCreateOrder(price, activeOrders, balance)
                }
            )
            .filter { it.isPresent() }
            .map { it.get() }
            .map { (type, price, amount) -> OrderData(type, price.round(PRICE_ROUNDING), amount.round(AMOUNT_ROUNDING))}
            .subscribe(createRequests::onNext)
        priceProvider.getPrices()
            .withLatestFrom(
                activeOrdersProvider.getActiveOrders(),
                BiFunction<BigDecimal, Collection<Order>, Optional<String>> { price, activeOrders ->
                    getOrderIdToCancel(price, activeOrders)
                }
            )
            .filter { it.isPresent() }
            .map { it.get() }
            .subscribe(cancelRequests::onNext)
    }

    private fun getDataToCreateOrder(price: BigDecimal, activeOrders: Collection<Order>, balance: Balance): Optional<OrderData> {
        if (!activeOrders.isEmpty()) {
            return Optional.empty()
        }
        if (balance.first > balance.second * price) {
            logger.info("Creating SELL order")
            return getDataToCreateSellOrder(price, ORDER_AMOUNT, balance.first)
        } else {
            logger.info("Creating BUY order")
            return getDataToCreateBuyOrder(price, ORDER_AMOUNT, balance.second)
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

    private fun getOrderIdToCancel(price: BigDecimal, activeOrders: Collection<Order>): Optional<String> {
        if (activeOrders.isEmpty()) {
            return Optional.empty()
        }
        val (id, data) = activeOrders.first()
        if (data.type == OrderType.BUY && (price - data.price) / data.price > PRICE_DELTA + PRICE_THRESHOLD) {
            logger.info("Price is going to get too big (previous=${data.price}, new=$price)")
            return Optional.of(id)
        }
        if (data.type == OrderType.SELL && (data.price - price) / data.price > PRICE_DELTA + PRICE_THRESHOLD) {
            logger.info("Price is going to get too small (previous=${data.price}, new=$price)")
            return Optional.of(id)
        }
        return Optional.empty()
    }

    fun getCreateRequests(): Observable<OrderData> = createRequests

    fun getCancelRequests(): Observable<String> = cancelRequests
}

package micdm.btce_trader

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import micdm.btce_trader.model.Balance
import micdm.btce_trader.model.CurrencyPair
import micdm.btce_trader.model.OrderType
import micdm.btce_trader.model.Trade
import org.slf4j.Logger
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Runner @Inject constructor(private val logger: Logger,
                                 private val config: Config,
                                 private val currencyPair: CurrencyPair,
                                 private val balanceProvider: BalanceProvider,
                                 private val activeOrdersProvider: ActiveOrdersProvider,
                                 private val priceProvider: PriceProvider,
                                 private val orderHandler: OrderHandler,
                                 private val tradeHistoryProvider: TradeHistoryProvider,
                                 private val orderStrategy: OrderStrategy) {

    fun start() {
        logger.info("Trading options are: PAIR=$currencyPair, TRADING_STRATEGY=${config.getTradingStrategy()} PRICE_DELTA=${config.getPriceDelta()}, PRICE_THRESHOLD=${config.getPriceThreshold()}, ORDER_AMOUNT=${config.getOrderAmount()}")
        Observable
            .combineLatest(
                balanceProvider.getBalance(),
                Observable.zip(
                    activeOrdersProvider.getActiveOrders().map { orders ->
                        orders
                            .filter { it.data.type == OrderType.SELL }
                            .fold(BigDecimal.ZERO, { accumulated, item -> accumulated + item.data.amount })
                    },
                    activeOrdersProvider.getActiveOrders().map { orders ->
                        orders
                            .filter { it.data.type == OrderType.BUY }
                            .fold(BigDecimal.ZERO, { accumulated, item -> accumulated + item.data.secondAmount })
                    },
                    BiFunction<BigDecimal, BigDecimal, Balance> { first, second -> Balance(first, second) }
                ),
                priceProvider.getPrices(),
                Function3<Balance, Balance, BigDecimal, String> { balance, orderBalance, price ->
                    "Balances are ${balance.first}/${balance.second}, ${orderBalance.first}/${orderBalance.second} on orders, " +
                        "${balance.asFirst(price) + orderBalance.asFirst(price)}/${balance.asSecond(price) + orderBalance.asSecond(price)} in total"
                }
            )
            .subscribe(logger::info)
        tradeHistoryProvider.getTradeHistory()
            .subscribe { logger.info("Trades are $it") }
        priceProvider.getPrices()
            .takeLast(1)
            .withLatestFrom(tradeHistoryProvider.getTradeHistory(), BiFunction<BigDecimal, Collection<Trade>, Int> { _, trades -> trades.count() })
            .subscribe { logger.info("Final trade count is $it") }
        orderHandler.start()
        orderStrategy.start()
        priceProvider.start()
    }
}

package micdm.btce_trader.local

import micdm.btce_trader.OrderHandler
import micdm.btce_trader.OrderMaker
import micdm.btce_trader.PriceProvider
import micdm.btce_trader.model.*
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.ZonedDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class LocalOrderHandler @Inject constructor(private val currencyPair: CurrencyPair,
                                                     private val activeOrdersBuffer: LocalActiveOrdersBuffer,
                                                     @Named("first") private val firstCurrencyBalanceBuffer: LocalBalanceBuffer,
                                                     @Named("second") private val secondCurrencyBalanceBuffer: LocalBalanceBuffer,
                                                     private val orderMaker: OrderMaker,
                                                     private val priceProvider: PriceProvider,
                                                     private val tradeHistoryBuffer: LocalTradeHistoryBuffer): OrderHandler {

    private val PRICE_ROUNDING = MathContext(currencyPair.decimalPlaces)
    private val AMOUNT_ROUNDING = MathContext(currencyPair.decimalPlaces)
    private val PRIZE_UP_ROUNDING = MathContext(8, RoundingMode.UP)
    private val PRIZE_DOWN_ROUNDING = MathContext(8, RoundingMode.DOWN)
    private val PRIZE_PART = BigDecimal("0.998")

    override fun start() {
        orderMaker.getCreateRequests()
            .map { (type, price, amount) ->
                OrderData(type, price.round(PRICE_ROUNDING), amount.round(AMOUNT_ROUNDING))
            }
            .doOnNext { (type, price, amount) ->
                if (type == OrderType.BUY) {
                    secondCurrencyBalanceBuffer.change((-price * amount).round(PRIZE_UP_ROUNDING))
                }
                if (type == OrderType.SELL) {
                    firstCurrencyBalanceBuffer.change(-amount)
                }
            }
            .map {
                Order(UUID.randomUUID().toString(), it)
            }
            .doOnNext {
                activeOrdersBuffer.add(it)
            }
            .switchMap { (id, data) ->
                priceProvider.getPrices()
                    .filter { price -> (data.type == OrderType.BUY && data.price >= price) || (data.type == OrderType.SELL && data.price <= price) }
                    .map { Trade(OrderData(data.type, data.price, (data.amount * PRIZE_PART).round(PRIZE_DOWN_ROUNDING)), ZonedDateTime.now()) }
                    .doOnNext { trade ->
                        println("Removing order $id: complete")
                        activeOrdersBuffer.remove(id)
                        if (data.type == OrderType.BUY) {
                            firstCurrencyBalanceBuffer.change(data.amount)
                        }
                        if (data.type == OrderType.SELL) {
                            secondCurrencyBalanceBuffer.change(data.price * data.amount)
                        }
                        tradeHistoryBuffer.add(trade)
                    }
                    .take(1)
            }
            .subscribe()
        orderMaker.getCancelRequests()
            .map { activeOrdersBuffer.getOrder(it) }
            .filter { it.isPresent() }
            .map { it.get() }
            .subscribe { (id, data) ->
                if (data.type == OrderType.BUY) {
                    secondCurrencyBalanceBuffer.change((data.price * data.amount).round(PRIZE_DOWN_ROUNDING))
                }
                if (data.type == OrderType.SELL) {
                    firstCurrencyBalanceBuffer.change(data.amount)
                }
                println("Removing order $id: canceled")
                activeOrdersBuffer.remove(id)
            }
    }
}

package micdm.btce_trader.local

import io.reactivex.Observable
import micdm.btce_trader.OrderHandler
import micdm.btce_trader.OrderMaker
import micdm.btce_trader.PriceProvider
import micdm.btce_trader.TradeHistoryProvider
import micdm.btce_trader.model.Order
import micdm.btce_trader.model.OrderData
import micdm.btce_trader.model.OrderType
import micdm.btce_trader.model.Trade
import org.slf4j.Logger
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.ZonedDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalOrderHandler @Inject constructor(private val activeOrdersBuffer: ActiveOrdersBuffer,
                                                     private val balanceBuffer: BalanceBuffer,
                                                     private val orderMaker: OrderMaker,
                                                     private val priceProvider: PriceProvider,
                                                     private val tradeHistoryBuffer: TradeHistoryBuffer,
                                                     private val tradeHistoryProvider: TradeHistoryProvider,
                                                     private val logger: Logger): OrderHandler {


    private val PRIZE_UP_ROUNDING = MathContext(8, RoundingMode.UP)
    private val PRIZE_DOWN_ROUNDING = MathContext(8, RoundingMode.DOWN)
    private val PRIZE_PART = BigDecimal("0.998")

    override fun start() {
        orderMaker.getCreateRequests()
            .flatMap { Observable.fromIterable(it) }
            .doOnNext { (type, price, amount) ->
                if (type == OrderType.BUY) {
                    balanceBuffer.changeSecond((-price * amount).round(PRIZE_UP_ROUNDING))
                }
                if (type == OrderType.SELL) {
                    balanceBuffer.changeFirst(-amount)
                }
            }
            .map { Order(UUID.randomUUID().toString(), it) }
            .doOnNext { activeOrdersBuffer.add(it) }
            .flatMap { (id, data) ->
                priceProvider.getPrices()
                    .filter { price -> (data.type == OrderType.BUY && data.price >= price) || (data.type == OrderType.SELL && data.price <= price) }
                    .map { Trade(UUID.randomUUID().toString(), id, OrderData(data.type, data.price, (data.amount * PRIZE_PART).round(PRIZE_DOWN_ROUNDING)), ZonedDateTime.now()) }
                    .doOnNext { trade ->
                        logger.info("Removing order $id: complete")
                        activeOrdersBuffer.remove(id)
                        if (data.type == OrderType.BUY) {
                            balanceBuffer.changeFirst(data.amount)
                        }
                        if (data.type == OrderType.SELL) {
                            balanceBuffer.changeSecond(data.secondAmount)
                        }
                        tradeHistoryBuffer.add(trade)
                    }
                    .takeUntil(orderMaker.getCancelRequests().filter { it.contains(id) })
                    .take(1)
            }
            .subscribe()
        orderMaker.getCancelRequests()
            .flatMap { Observable.fromIterable(it) }
            .map { activeOrdersBuffer.getOrder(it) }
            .filter { it.isPresent() }
            .map { it.get() }
            .subscribe { (id, data) ->
                if (data.type == OrderType.BUY) {
                    balanceBuffer.changeSecond(data.secondAmount.round(PRIZE_DOWN_ROUNDING))
                }
                if (data.type == OrderType.SELL) {
                    balanceBuffer.changeFirst(data.amount)
                }
                logger.info("Removing order $id: canceled")
                activeOrdersBuffer.remove(id)
            }
    }
}

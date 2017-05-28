package micdm.btce_trader.local

import micdm.btce_trader.OrderHandler
import micdm.btce_trader.OrderMaker
import micdm.btce_trader.PriceProvider
import micdm.btce_trader.model.Order
import micdm.btce_trader.model.OrderType
import micdm.btce_trader.model.Trade
import java.time.ZonedDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class LocalOrderHandler @Inject constructor(private val activeOrdersBuffer: LocalActiveOrdersBuffer,
                                                     @Named("first") private val firstCurrencyBalanceBuffer: LocalBalanceBuffer,
                                                     @Named("second") private val secondCurrencyBalanceBuffer: LocalBalanceBuffer,
                                                     private val orderMaker: OrderMaker,
                                                     private val priceProvider: PriceProvider,
                                                     private val tradeHistoryBuffer: LocalTradeHistoryBuffer): OrderHandler {

    override fun start() {
        orderMaker.getCreateRequests()
            .doOnNext { (type, price, amount) ->
                if (type == OrderType.BUY) {
                    secondCurrencyBalanceBuffer.change(-price * amount)
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
                    .map { Trade(data, ZonedDateTime.now()) }
                    .doOnNext { trade ->
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
                    secondCurrencyBalanceBuffer.change(data.price * data.amount)
                }
                if (data.type == OrderType.SELL) {
                    firstCurrencyBalanceBuffer.change(data.amount)
                }
                activeOrdersBuffer.remove(id)
            }
    }
}

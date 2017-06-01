package micdm.btce_trader

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import micdm.btce_trader.model.Balance
import micdm.btce_trader.model.OrderType
import org.slf4j.Logger
import java.math.BigDecimal

fun main(args: Array<String>) {
    if (System.getenv("USE_LOCAL_DATA") != null) {
        val component = DaggerLocalComponent.builder().build()
        run(component.getBalanceProvider(), component.getActiveOrdersProvider(), component.getPriceProvider(),
            component.getLogger(), component.getOrderHandler())
    } else {
        val component = DaggerRemoteComponent.builder().build()
        run(component.getBalanceProvider(), component.getActiveOrdersProvider(), component.getPriceProvider(),
            component.getLogger(), component.getOrderHandler())
    }
}

private fun run(balanceProvider: BalanceProvider, activeOrdersProvider: ActiveOrdersProvider, priceProvider: PriceProvider, logger: Logger, orderHandler: OrderHandler) {
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
    orderHandler.start()
    priceProvider.start()
}

package micdm.btce_trader

import io.reactivex.Observable
import io.reactivex.functions.Function5
import micdm.btce_trader.model.OrderType
import java.math.BigDecimal

fun main(args: Array<String>) {
    val component = DaggerLocalComponent.builder().build()
    Observable
        .combineLatest(
            component.getFirstCurrencyBalanceProvider().getBalance(),
            component.getSecondCurrencyBalanceProvider().getBalance(),
            component.getActiveOrdersProvider().getActiveOrders()
                .map { orders ->
                    orders
                        .filter { it.data.type == OrderType.SELL }
                        .fold(BigDecimal.ZERO, { accumulated, item -> accumulated + item.data.amount })
                },
            component.getActiveOrdersProvider().getActiveOrders()
                .map { orders ->
                    orders
                        .filter { it.data.type == OrderType.BUY }
                        .fold(BigDecimal.ZERO, { accumulated, item -> accumulated + item.data.amount })
                },
            component.getPriceProvider().getPrices(),
            Function5<BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal, String> { firstCurrencyBalance, secondCurrencyBalance, firstCurrencyBalanceOnOrders, secondCurrencyBalanceOnOrders, price ->
                "Balances are $firstCurrencyBalance and $secondCurrencyBalance, $firstCurrencyBalanceOnOrders and $secondCurrencyBalanceOnOrders on orders, ${firstCurrencyBalance * price + secondCurrencyBalance + firstCurrencyBalanceOnOrders * price + secondCurrencyBalanceOnOrders} in total"
            }
        )
        .subscribe {
            println(it)
        }
    component.getOrderHandler().start()
    component.getPriceProvider().start()
}

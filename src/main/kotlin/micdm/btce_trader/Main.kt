package micdm.btce_trader

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import micdm.btce_trader.model.Balance
import micdm.btce_trader.model.OrderType
import java.math.BigDecimal

fun main(args: Array<String>) {
    val component = DaggerLocalComponent.builder().build()
    Observable
        .combineLatest(
            component.getBalanceProvider().getBalance(),
            Observable.zip(
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
                            .fold(BigDecimal.ZERO, { accumulated, item -> accumulated + item.data.secondAmount })
                    },
                BiFunction<BigDecimal, BigDecimal, Balance> { first, second -> Balance(first, second) }
            ),
            component.getPriceProvider().getPrices(),
            Function3<Balance, Balance, BigDecimal, String> { balance, orderBalance, price ->
                "Balances are ${balance.first}/${balance.second}, ${orderBalance.first}/${orderBalance.second} on orders, " +
                    "${balance.asFirst(price) + orderBalance.asFirst(price)}/${balance.asSecond(price) + orderBalance.asSecond(price)} in total"
            }
        )
        .subscribe(::println)
    component.getOrderHandler().start()
    component.getPriceProvider().start()
//    component.getMainThreadExecutor().run()
}

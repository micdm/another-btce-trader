package micdm.btce_trader

import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import java.math.BigDecimal

fun main(args: Array<String>) {
    val component = DaggerLocalComponent.builder().build()
    Observable
        .combineLatest(
            component.getFirstCurrencyBalanceProvider().getBalance(),
            component.getSecondCurrencyBalanceProvider().getBalance(),
            BiFunction<BigDecimal, BigDecimal, Pair<BigDecimal, BigDecimal>> { first, second -> Pair(first, second) }
        )
        .subscribe {
            println("Balances are ${it.first} and ${it.second}")
        }
    component.getOrderHandler().start()
    component.getPriceProvider().start()
}

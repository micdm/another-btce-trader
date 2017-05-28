package micdm.btce_trader.local

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import micdm.btce_trader.model.Currency
import java.math.BigDecimal

class LocalBalanceBuffer constructor(private val currency: Currency) {

    private val balance = BehaviorSubject.createDefault(BigDecimal.ZERO)

    fun get(): Observable<BigDecimal> = balance

    fun change(value: BigDecimal) {
        println("Changing balance for $value$currency")
        balance.onNext(balance.value + value)
    }
}

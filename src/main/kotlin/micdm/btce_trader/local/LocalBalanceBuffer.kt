package micdm.btce_trader.local

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import micdm.btce_trader.model.Balance
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalBalanceBuffer @Inject constructor(initial: Balance) {

    private val balance = BehaviorSubject.createDefault(initial)

    fun get(): Observable<Balance> = balance

    fun changeFirst(value: BigDecimal) {
        println("Changing first balance for $value")
        balance.onNext(balance.value.copy(first=balance.value.first + value))
    }

    fun changeSecond(value: BigDecimal) {
        println("Changing second balance for $value")
        balance.onNext(balance.value.copy(second=balance.value.second + value))
    }
}

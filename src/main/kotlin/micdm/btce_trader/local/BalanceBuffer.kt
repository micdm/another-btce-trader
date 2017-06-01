package micdm.btce_trader.local

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import micdm.btce_trader.model.Balance
import org.slf4j.Logger
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BalanceBuffer @Inject constructor(private val logger: Logger,
                                                 initial: Balance) {

    private val balance = BehaviorSubject.createDefault(initial)

    fun get(): Observable<Balance> = balance

    fun changeFirst(value: BigDecimal) {
        logger.info("Changing first balance for $value")
        balance.onNext(balance.value.copy(first=balance.value.first + value))
    }

    fun changeSecond(value: BigDecimal) {
        logger.info("Changing second balance for $value")
        balance.onNext(balance.value.copy(second=balance.value.second + value))
    }
}

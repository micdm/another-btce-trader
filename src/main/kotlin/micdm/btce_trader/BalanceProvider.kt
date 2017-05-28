package micdm.btce_trader

import io.reactivex.Observable
import java.math.BigDecimal

interface BalanceProvider {

    fun getBalance(): Observable<BigDecimal>
}

package micdm.btce_trader

import io.reactivex.Observable
import micdm.btce_trader.model.Balance

interface BalanceProvider {

    fun getBalance(): Observable<Balance>
}

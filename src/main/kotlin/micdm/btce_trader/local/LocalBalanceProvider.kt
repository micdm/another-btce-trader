package micdm.btce_trader.local

import io.reactivex.Observable
import micdm.btce_trader.BalanceProvider
import micdm.btce_trader.model.Balance
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalBalanceProvider @Inject constructor(private val balanceBuffer: BalanceBuffer): BalanceProvider {

    override fun getBalance(): Observable<Balance> = balanceBuffer.get()
}

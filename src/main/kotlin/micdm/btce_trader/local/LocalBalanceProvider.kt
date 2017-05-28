package micdm.btce_trader.local

import io.reactivex.Observable
import micdm.btce_trader.BalanceProvider
import micdm.btce_trader.model.Currency
import java.math.BigDecimal

internal class LocalBalanceProvider constructor(private val currency: Currency,
                                                private val balanceBuffer: LocalBalanceBuffer): BalanceProvider {

    override fun getBalance(): Observable<BigDecimal> = balanceBuffer.get()
}
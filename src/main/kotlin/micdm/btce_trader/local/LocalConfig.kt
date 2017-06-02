package micdm.btce_trader.local

import micdm.btce_trader.model.Balance
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalConfig @Inject constructor() {

    fun getInitialBalance(): Balance {
        val parts = System.getenv("INITIAL_BALANCE").split(",")
        return Balance(BigDecimal(parts[0]), BigDecimal(parts[1]))
    }
}

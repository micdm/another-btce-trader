package micdm.btce_trader.remote

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NonceKeeper @Inject constructor() {

    fun get(): Int {
        val file = File("data/nonce.data")
        if (!file.exists()) {
            file.writeText("0")
        }
        val nonce = file.readText().toInt()
        file.writeText((nonce + 1).toString())
        return nonce
    }
}

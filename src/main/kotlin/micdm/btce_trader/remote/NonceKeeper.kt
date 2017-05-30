package micdm.btce_trader.remote

import java.io.File
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NonceKeeper @Inject constructor() {

    private val lock = ReentrantLock()

    fun get(): Int {
        val file = File("data/nonce.data")
        if (!file.exists()) {
            file.writeText("0")
        }
        lock.lock()
        try {
            val nonce = file.readText().toInt()
            file.writeText((nonce + 1).toString())
            return nonce
        } finally {
            lock.unlock()
        }
    }
}

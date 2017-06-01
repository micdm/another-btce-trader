package micdm.btce_trader.misc

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DummyWorker @Inject constructor() {

    fun run() {
        while (!Thread.interrupted()) {
            try {
                Thread.sleep(1)
            } catch (e: InterruptedException) {
                break
            }
        }
    }
}

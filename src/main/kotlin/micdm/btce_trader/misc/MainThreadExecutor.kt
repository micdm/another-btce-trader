package micdm.btce_trader.misc

import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainThreadExecutor @Inject constructor(): Executor {

    private val tasks = LinkedBlockingQueue<Runnable>()

    override fun execute(task: Runnable) {
        tasks.add(task)
    }

    fun run() {
        while (!Thread.interrupted()) {
            try {
                tasks.take().run()
            } catch (e: InterruptedException) {
                break
            }
        }
    }
}

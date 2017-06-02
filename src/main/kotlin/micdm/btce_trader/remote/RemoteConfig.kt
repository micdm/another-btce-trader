package micdm.btce_trader.remote

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RemoteConfig @Inject constructor() {

    fun getApiKey(): String = System.getenv("API_KEY")

    fun getApiSecret(): String = System.getenv("API_SECRET")
}

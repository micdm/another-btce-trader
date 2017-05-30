package micdm.btce_trader

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Config @Inject constructor() {

    fun getApiKey(): String {
        return System.getenv("API_KEY")
    }

    fun getApiSecret(): String {
        return System.getenv("API_SECRET")
    }
}

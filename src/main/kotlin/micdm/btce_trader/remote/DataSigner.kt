package micdm.btce_trader.remote

import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataSigner @Inject constructor() {

    fun getSignature(data: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA512"))
        return toHexString(mac.doFinal(data.toByteArray()))
    }

    private fun toHexString(bytes: ByteArray): String {
        val formatter = Formatter()
        for (byte in bytes) {
            formatter.format("%02x", byte)
        }
        return formatter.toString()
    }
}

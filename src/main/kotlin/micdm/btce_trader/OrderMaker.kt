package micdm.btce_trader

import io.reactivex.Observable
import micdm.btce_trader.model.OrderData

interface OrderMaker {

    fun getCreateRequests(): Observable<OrderData>
    fun getCancelRequests(): Observable<String>
}

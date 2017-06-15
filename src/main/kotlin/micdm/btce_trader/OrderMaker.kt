package micdm.btce_trader

import io.reactivex.Observable
import micdm.btce_trader.model.OrderData

interface OrderMaker {

    fun start()
    fun getCreateRequests(): Observable<Collection<OrderData>>
    fun getCancelRequests(): Observable<Collection<String>>
}

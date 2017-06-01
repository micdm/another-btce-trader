package micdm.btce_trader.remote

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OrderStatusBuffer @Inject constructor() {

    private val creates: Subject<Any> = PublishSubject.create()
    private val cancels: Subject<Any> = PublishSubject.create()

    fun created() = creates.onNext(Any())

    fun getCreates(): Observable<Any> = creates

    fun canceled() = cancels.onNext(Any())

    fun getCancels(): Observable<Any> = cancels
}

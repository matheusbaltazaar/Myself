package br.com.myself.observer

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class Trigger {
    companion object {
        
        private val trigger = PublishSubject.create<Any>()
        
        fun launch(vararg t: Any) {
            t.forEach { trigger.onNext(it) }
        }
        
        
        fun watcher(): Observable<Any> {
            return trigger
        }
        
    }
}
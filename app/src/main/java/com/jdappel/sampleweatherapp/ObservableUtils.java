package com.jdappel.sampleweatherapp;

import java.util.concurrent.Callable;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Convenience class for creating {@link Observable} from a {@link Callable} and
 * scheduling it on a thread for IO bound work.
 * 
 * @author jappel
 */
class ObservableUtils {

    private ObservableUtils() {
        throw new AssertionError();
    }

    static <T> Observable<T> createObservable(final Callable<T> callable) {
        return Observable.create(new Observable.OnSubscribe<T>() {

            @Override
            public void call(Subscriber<? super T> subscriber) {
                try {
                    subscriber.onNext(callable.call());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }
}

package com.yy.core.yyp.smart;

import io.reactivex.Observer;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;

/**
 * Created by liyong on 2017-07-07.
 */

public abstract class SmartObserverResult<T> implements Observer<T> {
    @Override
    public void onSubscribe(Disposable d) {

    }

    @Override
    public abstract void onNext(@Nullable T t);

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onComplete() {

    }
}

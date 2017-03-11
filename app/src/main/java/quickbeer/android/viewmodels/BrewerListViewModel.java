/**
 * This file is part of QuickBeer.
 * Copyright (C) 2017 Antti Poikela <antti.poikela@iki.fi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package quickbeer.android.viewmodels;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import io.reark.reark.data.DataStreamNotification;
import ix.Ix;
import quickbeer.android.data.DataLayer;
import quickbeer.android.data.pojos.ItemList;
import rx.Observable;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static io.reark.reark.utils.Preconditions.get;

public abstract class BrewerListViewModel extends NetworkViewModel<ItemList<String>> {

    @NonNull
    private final DataLayer.GetBrewer getBrewer;

    @NonNull
    private final PublishSubject<List<BrewerViewModel>> brewers = PublishSubject.create();

    protected BrewerListViewModel(@NonNull final DataLayer.GetBrewer getBrewer) {
        this.getBrewer = get(getBrewer);
    }

    @NonNull
    protected abstract Observable<DataStreamNotification<ItemList<String>>> sourceObservable();

    @NonNull
    public Observable<List<BrewerViewModel>> getBrewers() {
        return brewers.asObservable();
    }

    @Override
    protected void bind(@NonNull final CompositeSubscription subscription) {
        ConnectableObservable<DataStreamNotification<ItemList<String>>> sharedObservable =
                sourceObservable()
                        .publish();

        // Construct progress status. Completed with value means we'll receive onNext.
        subscription.add(sharedObservable
                .filter(notification -> !notification.isFetchingCompletedWithValue())
                .map(toProgressStatus())
                .startWith(ProgressStatus.LOADING)
                .distinctUntilChanged()
                .subscribe(this::setProgressStatus));

        // Clear list on fetching start
        subscription.add(sharedObservable
                .subscribeOn(Schedulers.computation())
                .filter(DataStreamNotification::isFetchingStart)
                .map(__ -> Collections.<BrewerViewModel>emptyList())
                .subscribe(brewers::onNext));

        // Actual update
        subscription.add(sharedObservable
                .subscribeOn(Schedulers.computation())
                .filter(DataStreamNotification::isOnNext)
                .map(DataStreamNotification::getValue)
                .doOnNext(brewerSearch -> Timber.d("Search finished"))
                .map(ItemList::getItems)
                .map(toBrewerViewModelList())
                .doOnNext(list -> Timber.d("Publishing " + list.size() + " brewers"))
                .subscribe(brewers::onNext));

        subscription.add(sharedObservable.connect());
    }

    @NonNull
    private Func1<List<Integer>, List<BrewerViewModel>> toBrewerViewModelList() {
        return brewerIds -> Ix.from(brewerIds)
                .map(integer -> new BrewerViewModel(integer, getBrewer))
                .toList();
    }

    @Override
    protected boolean hasValue(@Nullable final ItemList<String> item) {
        return !get(item).getItems().isEmpty();
    }
}

/**
 * This file is part of QuickBeer.
 * Copyright (C) 2016 Antti Poikela <antti.poikela@iki.fi>
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

import java.util.List;

import io.reark.reark.data.DataStreamNotification;
import io.reark.reark.utils.RxUtils;
import quickbeer.android.data.DataLayer;
import quickbeer.android.data.pojos.ItemList;
import quickbeer.android.data.pojos.Review;
import rx.Observable;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static io.reark.reark.utils.Preconditions.get;

public class ReviewListViewModel extends BaseViewModel {

    private final DataLayer.GetReviews getReviews;
    private final DataLayer.GetReview getReview;

    private final BehaviorSubject<List<Review>> reviews = BehaviorSubject.create();
    private final int beerId;

    public ReviewListViewModel(final int beerId,
                               @NonNull final DataLayer.GetReviews getReviews,
                               @NonNull final DataLayer.GetReview getReview) {
        this.getReviews = get(getReviews);
        this.getReview = get(getReview);
        this.beerId = beerId;
    }

    @NonNull
    public Observable<List<Review>> getReviews() {
        return reviews.asObservable();
    }

    @Override
    public void subscribeToDataStoreInternal(@NonNull final CompositeSubscription compositeSubscription) {
        Timber.v("subscribeToDataStoreInternal");

        ConnectableObservable<DataStreamNotification<ItemList<Integer>>> reviewSource =
                getReviews.call(beerId).publish();

        compositeSubscription.add(reviewSource
                .map(toProgressStatus())
                .subscribe(this::setNetworkStatusText));

        compositeSubscription.add(reviewSource
                .filter(DataStreamNotification::isOnNext)
                .map(DataStreamNotification::getValue)
                .doOnNext(search -> Timber.d("Review get finished"))
                .map(ItemList<Integer>::getItems)
                .flatMap(toReviewList())
                .doOnNext(list -> Timber.d("Publishing " + list.size() + " reviews from the view model"))
                .subscribe(reviews::onNext));

        compositeSubscription.add(reviewSource
                .connect());
    }

    @NonNull
    private Func1<List<Integer>, Observable<List<Review>>> toReviewList() {
        return reviewIds -> Observable.from(reviewIds)
                .map(this::getReviewObservable)
                .toList()
                .flatMap(RxUtils::toObservableList);
    }

    @NonNull
    private Observable<Review> getReviewObservable(@NonNull final Integer reviewId) {
        return getReview
                .call(get(reviewId))
                .compose(quickbeer.android.rx.RxUtils::pickValue)
                .doOnNext((review) -> Timber.v("Received review " + review.id()));
    }
}
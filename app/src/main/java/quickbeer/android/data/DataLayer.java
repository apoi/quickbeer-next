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
package quickbeer.android.data;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import io.reark.reark.data.DataStreamNotification;
import io.reark.reark.data.utils.DataLayerUtils;
import io.reark.reark.pojo.NetworkRequestStatus;
import polanski.option.Option;
import quickbeer.android.Constants;
import quickbeer.android.data.pojos.Beer;
import quickbeer.android.data.pojos.BeerMetadata;
import quickbeer.android.data.pojos.Brewer;
import quickbeer.android.data.pojos.BrewerMetadata;
import quickbeer.android.data.pojos.ItemList;
import quickbeer.android.data.pojos.Review;
import quickbeer.android.data.pojos.User;
import quickbeer.android.data.stores.BeerListStore;
import quickbeer.android.data.stores.BeerMetadataStore;
import quickbeer.android.data.stores.BeerStore;
import quickbeer.android.data.stores.BrewerListStore;
import quickbeer.android.data.stores.BrewerMetadataStore;
import quickbeer.android.data.stores.BrewerStore;
import quickbeer.android.data.stores.NetworkRequestStatusStore;
import quickbeer.android.data.stores.ReviewListStore;
import quickbeer.android.data.stores.ReviewStore;
import quickbeer.android.data.stores.UserStore;
import quickbeer.android.network.NetworkService;
import quickbeer.android.network.RateBeerService;
import quickbeer.android.network.fetchers.BeerFetcher;
import quickbeer.android.network.fetchers.BeerSearchFetcher;
import quickbeer.android.network.fetchers.BrewerFetcher;
import quickbeer.android.network.fetchers.LoginFetcher;
import quickbeer.android.network.fetchers.ReviewFetcher;
import quickbeer.android.network.fetchers.TickBeerFetcher;
import quickbeer.android.rx.RxUtils;
import rx.Observable;
import timber.log.Timber;

import static io.reark.reark.utils.Preconditions.checkNotNull;
import static io.reark.reark.utils.Preconditions.get;
import static quickbeer.android.data.stores.NetworkRequestStatusStore.requestIdForUri;

public class DataLayer extends DataLayerBase {

    @NonNull
    private final Context context;

    @NonNull
    private final UserStore userStore;

    public DataLayer(@NonNull Context context,
                     @NonNull UserStore userStore,
                     @NonNull NetworkRequestStatusStore requestStatusStore,
                     @NonNull BeerStore beerStore,
                     @NonNull BeerListStore beerListStore,
                     @NonNull BeerMetadataStore beerMetadataStore,
                     @NonNull ReviewStore reviewStore,
                     @NonNull ReviewListStore reviewListStore,
                     @NonNull BrewerStore brewerStore,
                     @NonNull BrewerListStore brewerListStore,
                     @NonNull BrewerMetadataStore brewerMetadataStore) {
        super(requestStatusStore,
                beerStore, beerListStore, beerMetadataStore,
                reviewStore, reviewListStore,
                brewerStore, brewerListStore, brewerMetadataStore);

        this.context = get(context);
        this.userStore = get(userStore);
    }

    //// GET USER SETTINGS

    @NonNull
    public Observable<DataStreamNotification<User>> login(@NonNull String username, @NonNull String password) {
        Timber.v("login(%s)", get(username));
        checkNotNull(password);

        String uri = LoginFetcher.getUniqueUri();

        int listenerId = createListenerId();

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.LOGIN.toString());
        intent.putExtra("listenerId", listenerId);
        intent.putExtra("username", username);
        intent.putExtra("password", password);
        context.startService(intent);

        return getLoginStatus(listenerId);
    }

    @NonNull
    public Observable<DataStreamNotification<User>> getLoginStatus() {
        // Note -- returns old and new statuses alike
        return getLoginStatus(null);
    }

    @NonNull
    private Observable<DataStreamNotification<User>> getLoginStatus(@Nullable Integer listenerId) {
        Timber.v("getLoginStatus()");

        String uri = LoginFetcher.getUniqueUri();

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                        .compose(RxUtils::pickValue)
                        .filter(status -> status.forListener(listenerId));

        Observable<User> userObservable =
                userStore.getOnceAndStream(Constants.DEFAULT_USER_ID)
                        .compose(RxUtils::pickValue);

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, userObservable);
    }

    @NonNull
    public Observable<Option<User>> getUser() {
        return userStore.getOnceAndStream(Constants.DEFAULT_USER_ID);
    }

    //// GET BEER DETAILS

    @NonNull
    public Observable<DataStreamNotification<Beer>> getBeer(int beerId, boolean fullDetails) {
        Timber.v("getBeer(%s)", get(beerId));

        // Trigger a fetch only if full details haven't been fetched
        Observable<Option<Beer>> triggerFetchIfEmpty =
                beerStore.getOnce(beerId)
                        .toObservable()
                        .filter(option -> option.match(beer -> !beer.hasDetails(fullDetails), () -> true))
                        .doOnNext(__ -> {
                            Timber.v("Beer not cached, fetching");
                            fetchBeer(beerId);
                        });

        return getBeerResultStream(beerId)
                .mergeWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()))
                .distinctUntilChanged();
    }

    @NonNull
    public Observable<DataStreamNotification<Beer>> getBeerResultStream(int beerId) {
        Timber.v("getBeerResultStream(%s)", get(beerId));

        String uri = BeerFetcher.getUniqueUri(beerId);

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                        .compose(RxUtils::pickValue); // No need to filter stale statuses

        Observable<Beer> beerObservable =
                beerStore.getOnceAndStream(beerId)
                        .compose(RxUtils::pickValue);

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, beerObservable);
    }

    private int fetchBeer(int beerId) {
        Timber.v("fetchBeer(%s)", get(beerId));

        int listenerId = createListenerId();

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.BEER.toString());
        intent.putExtra("listenerId", listenerId);
        intent.putExtra("id", beerId);
        context.startService(intent);

        return listenerId;
    }

    //// ACCESS BREWER

    public void accessBeer(int beerId) {
        Timber.v("accessBeer(%s)", get(beerId));

        beerMetadataStore.put(BeerMetadata.newAccess(beerId));
    }

    //// ACCESSED BEERS

    @NonNull
    public Observable<DataStreamNotification<ItemList<String>>> getAccessedBeers() {
        Timber.v("getAccessedBeers");

        return beerMetadataStore.getAccessedIdsOnce()
                .map(ids -> new ItemList<String>(null, ids, null))
                .map(DataStreamNotification::onNext);
    }

    //// SEARCH BEERS

    @NonNull
    public Observable<List<String>> getBeerSearchQueriesOnce() {
        Timber.v("getBeerSearchQueriesOnce");

        return beerListStore.getOnce()
                .toObservable()
                .flatMap(Observable::from)
                .map(ItemList::getKey)
                .filter(search -> !search.startsWith(Constants.META_QUERY_PREFIX))
                .toList();
    }

    @NonNull
    public Observable<DataStreamNotification<ItemList<String>>> getBeerSearch(@NonNull String searchString) {
        Timber.v("getBeerSearch(%s)", get(searchString));

        // Trigger a fetch only if there was no cached result
        Observable<Option<ItemList<String>>> triggerFetchIfEmpty =
                beerListStore.getOnce(BeerSearchFetcher.getQueryId(RateBeerService.SEARCH, searchString))
                        .toObservable()
                        .filter(RxUtils::isNoneOrEmpty)
                        .doOnNext(__ -> {
                            Timber.v("Search not cached, fetching");
                            fetchBeerSearch(searchString);
                        });

        return getBeerSearchResultStream(searchString)
                .mergeWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()));
    }

    @NonNull
    private Observable<DataStreamNotification<ItemList<String>>> getBeerSearchResultStream(@NonNull String searchString) {
        Timber.v("getBeerSearchResultStream(%s)", get(searchString));

        String queryId = BeerSearchFetcher.getQueryId(RateBeerService.SEARCH, searchString);
        String uri = BeerSearchFetcher.getUniqueUri(queryId);

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                        .compose(RxUtils::pickValue); // No need to filter stale statuses?

        Observable<ItemList<String>> beerSearchObservable =
                beerListStore.getOnceAndStream(queryId)
                        .compose(RxUtils::pickValue);

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, beerSearchObservable);
    }

    private int fetchBeerSearch(@NonNull String searchString) {
        Timber.v("fetchBeerSearch(%s)", get(searchString));

        int listenerId = createListenerId();

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.SEARCH.toString());
        intent.putExtra("listenerId", listenerId);
        intent.putExtra("searchString", searchString);
        context.startService(intent);

        return listenerId;
    }

    //// BARCODE SEARCH

    @NonNull
    public Observable<DataStreamNotification<ItemList<String>>> getBarcodeSearch(@NonNull String barcode) {
        Timber.v("getBarcodeSearch(%s)", get(barcode));

        // Trigger a fetch only if there was no cached result
        Observable<Option<ItemList<String>>> triggerFetchIfEmpty =
                beerListStore.getOnce(BeerSearchFetcher.getQueryId(RateBeerService.BARCODE, barcode))
                        .toObservable()
                        .filter(RxUtils::isNoneOrEmpty)
                        .doOnNext(__ -> {
                            Timber.v("Search not cached, fetching");
                            fetchBarcodeSearch(barcode);
                        });

        return getBarcodeSearchResultStream(barcode)
                .mergeWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()));
    }

    @NonNull
    private Observable<DataStreamNotification<ItemList<String>>> getBarcodeSearchResultStream(@NonNull String barcode) {
        Timber.v("getBarcodeSearchResultStream(%s)", get(barcode));

        String queryId = BeerSearchFetcher.getQueryId(RateBeerService.BARCODE, barcode);
        String uri = BeerSearchFetcher.getUniqueUri(queryId);

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                        .compose(RxUtils::pickValue); // No need to filter stale statuses?

        Observable<ItemList<String>> barcodeSearchObservable =
                beerListStore.getOnceAndStream(queryId)
                        .compose(RxUtils::pickValue);

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, barcodeSearchObservable);
    }

    private int fetchBarcodeSearch(@NonNull String barcode) {
        Timber.v("fetchBarcodeSearch(%s)", get(barcode));

        int listenerId = createListenerId();

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.BARCODE.toString());
        intent.putExtra("listenerId", listenerId);
        intent.putExtra("barcode", barcode);
        context.startService(intent);

        return listenerId;
    }

    //// TOP BEERS

    @NonNull
    public Observable<DataStreamNotification<ItemList<String>>> getTopBeers() {
        Timber.v("getTopBeers");

        // Trigger a fetch only if there was no cached result
        Observable<Option<ItemList<String>>> triggerFetchIfEmpty =
                beerListStore.getOnce(BeerSearchFetcher.getQueryId(RateBeerService.TOP50))
                        .toObservable()
                        .filter(RxUtils::isNoneOrEmpty)
                        .doOnNext(__ -> {
                            Timber.v("Search not cached, fetching");
                            fetchTopBeers();
                        });

        return getTopBeersResultStream()
                .mergeWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()));
    }

    @NonNull
    private Observable<DataStreamNotification<ItemList<String>>> getTopBeersResultStream() {
        Timber.v("getTopBeersResultStream");

        String queryId = BeerSearchFetcher.getQueryId(RateBeerService.TOP50);
        String uri = BeerSearchFetcher.getUniqueUri(queryId);

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                        .compose(RxUtils::pickValue); // No need to filter stale statuses?

        Observable<ItemList<String>> beerSearchObservable =
                beerListStore.getOnceAndStream(queryId)
                        .compose(RxUtils::pickValue);

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, beerSearchObservable);
    }

    private int fetchTopBeers() {
        Timber.v("fetchTopBeers");

        int listenerId = createListenerId();

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.TOP50.toString());
        intent.putExtra("listenerId", listenerId);
        context.startService(intent);

        return listenerId;
    }

    //// BEERS IN COUNTRY

    @NonNull
    public Observable<DataStreamNotification<ItemList<String>>> getBeersInCountry(@NonNull String countryId) {
        Timber.v("getBeersInCountry(%s)", get(countryId));

        // Trigger a fetch only if there was no cached result
        Observable<Option<ItemList<String>>> triggerFetchIfEmpty =
                beerListStore.getOnce(BeerSearchFetcher.getQueryId(RateBeerService.COUNTRY, countryId))
                        .toObservable()
                        .filter(RxUtils::isNoneOrEmpty)
                        .doOnNext(__ -> {
                            Timber.v("Search not cached, fetching");
                            fetchBeersInCountry(countryId);
                        });

        return getBeersInCountryResultStream(countryId)
                .mergeWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()));
    }

    @NonNull
    private Observable<DataStreamNotification<ItemList<String>>> getBeersInCountryResultStream(@NonNull String countryId) {
        Timber.v("getBeersInCountryResultStream(%s)", get(countryId));

        String queryId = BeerSearchFetcher.getQueryId(RateBeerService.COUNTRY, countryId);
        String uri = BeerSearchFetcher.getUniqueUri(queryId);

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                        .compose(RxUtils::pickValue); // No need to filter stale statuses?

        Observable<ItemList<String>> beerSearchObservable =
                beerListStore.getOnceAndStream(queryId)
                        .compose(RxUtils::pickValue);

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, beerSearchObservable);
    }

    private int fetchBeersInCountry(@NonNull String countryId) {
        Timber.v("fetchBeersInCountry");

        int listenerId = createListenerId();

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.COUNTRY.toString());
        intent.putExtra("listenerId", listenerId);
        intent.putExtra("countryId", countryId);
        context.startService(intent);

        return listenerId;
    }

    //// BEERS IN STYLE

    @NonNull
    public Observable<DataStreamNotification<ItemList<String>>> getBeersInStyle(@NonNull String styleId) {
        Timber.v("getBeersInStyle(%s)", get(styleId));

        // Trigger a fetch only if there was no cached result
        Observable<Option<ItemList<String>>> triggerFetchIfEmpty =
                beerListStore.getOnce(BeerSearchFetcher.getQueryId(RateBeerService.STYLE, styleId))
                        .toObservable()
                        .filter(RxUtils::isNoneOrEmpty)
                        .doOnNext(__ -> {
                            Timber.v("Search not cached, fetching");
                            fetchBeersInStyle(styleId);
                        });

        return getBeersInStyleResultStream(styleId)
                .mergeWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()));
    }

    @NonNull
    private Observable<DataStreamNotification<ItemList<String>>> getBeersInStyleResultStream(@NonNull String styleId) {
        Timber.v("getBeersInStyleResultStream(%s)", get(styleId));

        String queryId = BeerSearchFetcher.getQueryId(RateBeerService.STYLE, styleId);
        String uri = BeerSearchFetcher.getUniqueUri(queryId);

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                        .compose(RxUtils::pickValue); // No need to filter stale statuses?

        Observable<ItemList<String>> beerSearchObservable =
                beerListStore.getOnceAndStream(queryId)
                        .compose(RxUtils::pickValue);

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, beerSearchObservable);
    }

    private int fetchBeersInStyle(@NonNull String styleId) {
        Timber.v("fetchBeersInStyle(%s)", get(styleId));

        int listenerId = createListenerId();

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.STYLE.toString());
        intent.putExtra("listenerId", listenerId);
        intent.putExtra("styleId", styleId);
        context.startService(intent);

        return listenerId;
    }

    //// OWN_REVIEWS

    @NonNull
    public Observable<Option<Review>> getReview(int reviewId) {
        Timber.v("getReview(%s)", get(reviewId));

        // Reviews are never fetched one-by-one, only as a list of reviews. This method can only
        // return reviews from the local store, no fetching.
        return reviewStore.getOnceAndStream(reviewId);
    }

    @NonNull
    public Observable<DataStreamNotification<ItemList<Integer>>> getReviews(int beerId) {
        Timber.v("getReviews(%s)", beerId);

        // Trigger a fetch only if there was no cached result
        Observable<Option<ItemList<Integer>>> triggerFetchIfEmpty =
                reviewListStore.getOnce(beerId)
                        .toObservable()
                        .filter(RxUtils::isNoneOrEmpty)
                        .doOnNext(__ -> {
                            Timber.v("Reviews not cached, fetching");
                            fetchReviews(beerId);
                        });

        return getReviewsResultStream(beerId)
                .mergeWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()));
    }

    @NonNull
    private Observable<DataStreamNotification<ItemList<Integer>>> getReviewsResultStream(int beerId) {
        Timber.v("getReviewsResultStream(%s)", beerId);

        String uri = ReviewFetcher.getUniqueUri(beerId);

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                        .compose(RxUtils::pickValue); // No need to filter stale statuses?

        Observable<ItemList<Integer>> reviewListObservable =
                reviewListStore.getOnceAndStream(beerId)
                        .compose(RxUtils::pickValue);

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, reviewListObservable);
    }

    private int fetchReviews(int beerId) {
        Timber.v("fetchReviews(%s)", beerId);

        int listenerId = createListenerId();

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.REVIEWS.toString());
        intent.putExtra("listenerId", listenerId);
        intent.putExtra("beerId", beerId);
        context.startService(intent);

        return listenerId;
    }

    //// OWN BEER OWN_REVIEWS

    @NonNull
    public Observable<DataStreamNotification<ItemList<String>>> getOwnBeerReviews() {
        Timber.v("getOwnBeerReviews");

        // Trigger a fetch only if there was no cached result
        Observable<Option<ItemList<String>>> triggerFetchIfEmpty =
                beerListStore.getOnce(BeerSearchFetcher.getQueryId(RateBeerService.OWN_REVIEWS))
                        .toObservable()
                        .filter(RxUtils::isNoneOrEmpty)
                        .doOnNext(__ -> {
                            Timber.v("Search not cached, fetching");
                            fetchOwnBeerReviews();
                        });

        return getOwnBeerReviewsResultStream()
                .mergeWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()));
    }

    @NonNull
    private Observable<DataStreamNotification<ItemList<String>>> getOwnBeerReviewsResultStream() {
        Timber.v("getOwnBeerReviewsResultStream");

        String queryId = BeerSearchFetcher.getQueryId(RateBeerService.OWN_REVIEWS);
        String uri = BeerSearchFetcher.getUniqueUri(queryId);

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                        .compose(RxUtils::pickValue); // No need to filter stale statuses?

        Observable<ItemList<String>> beerSearchObservable =
                beerListStore.getOnceAndStream(queryId)
                        .compose(RxUtils::pickValue);

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, beerSearchObservable);
    }

    public int fetchOwnBeerReviews() {
        Timber.v("fetchOwnBeerReviews()");

        int listenerId = createListenerId();

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.OWN_REVIEWS.toString());
        intent.putExtra("listenerId", listenerId);
        context.startService(intent);

        return listenerId;
    }

    //// TICKS

    @NonNull
    public Observable<DataStreamNotification<ItemList<String>>> getTickedBeers(@NonNull String userId) {
        Timber.v("getTickedBeers");

        // Trigger a fetch only if there was no cached result
        Observable<Option<ItemList<String>>> triggerFetchIfEmpty =
                beerListStore.getOnce(BeerSearchFetcher.getQueryId(RateBeerService.TICKS, userId))
                             .toObservable()
                             .filter(RxUtils::isNoneOrEmpty)
                             .doOnNext(__ -> {
                                 Timber.v("Search not cached, fetching");
                                 fetchTickedBeers(userId);
                             });

        return getTickedBeersResultStream(userId)
                .mergeWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()));
    }

    @NonNull
    private Observable<DataStreamNotification<ItemList<String>>> getTickedBeersResultStream(@NonNull String userId) {
        Timber.v("getTickedBeersResultStream");

        String queryId = BeerSearchFetcher.getQueryId(RateBeerService.TICKS, userId);
        String uri = BeerSearchFetcher.getUniqueUri(queryId);

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                                  .compose(RxUtils::pickValue); // No need to filter stale statuses?

        Observable<ItemList<String>> beerSearchObservable =
                beerListStore.getOnceAndStream(queryId)
                             .compose(RxUtils::pickValue);

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, beerSearchObservable);
    }

    public int fetchTickedBeers(@NonNull String userId) {
        Timber.v("fetchTickedBeers(%s)", get(userId));

        int listenerId = createListenerId();

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.TICKS.toString());
        intent.putExtra("listenerId", listenerId);
        intent.putExtra("userId", userId);
        context.startService(intent);

        return listenerId;
    }

    public Observable<DataStreamNotification<Void>> tickBeer(int beerId, int rating) {
        Timber.v("tickBeer(%s, %s)", beerId, rating);

        int listenerId = createListenerId();

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.TICK_BEER.toString());
        intent.putExtra("listenerId", listenerId);
        intent.putExtra("beerId", beerId);
        intent.putExtra("rating", rating);
        context.startService(intent);

        String uri = TickBeerFetcher.getUniqueUri(beerId, rating);

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                        .compose(RxUtils::pickValue)
                        .filter(status -> status.forListener(listenerId));

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, Observable.never());
    }

    //// GET BREWER DETAILS

    @NonNull
    public Observable<DataStreamNotification<Brewer>> getBrewer(int brewerId) {
        Timber.v("getBrewer(%s)", brewerId);

        // Trigger a fetch only if full details haven't been fetched
        Observable<Option<Brewer>> triggerFetchIfEmpty =
                brewerStore.getOnce(brewerId)
                        .toObservable()
                        .filter(option -> option.match(brewer -> !brewer.hasDetails(), () -> true))
                        .doOnNext(__ -> {
                            Timber.v("Brewer not cached, fetching");
                            fetchBrewer(brewerId);
                        });

        return getBrewerResultStream(brewerId)
                .mergeWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()))
                .distinctUntilChanged();
    }

    @NonNull
    private Observable<DataStreamNotification<Brewer>> getBrewerResultStream(int brewerId) {
        Timber.v("getBrewerResultStream(%s)", brewerId);

        String uri = BrewerFetcher.getUniqueUri(get(brewerId));

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                        .compose(RxUtils::pickValue);

        Observable<Brewer> brewerObservable =
                brewerStore.getOnceAndStream(brewerId)
                        .compose(RxUtils::pickValue);

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, brewerObservable);
    }

    private int fetchBrewer(int brewerId) {
        Timber.v("fetchBrewer(%s)", brewerId);

        int listenerId = createListenerId();

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.BREWER.toString());
        intent.putExtra("listenerId", listenerId);
        intent.putExtra("id", brewerId);
        context.startService(intent);

        return listenerId;
    }

    //// BREWER'S BEERS

    @NonNull
    public Observable<DataStreamNotification<ItemList<String>>> getBrewerBeers(@NonNull String brewerId) {
        Timber.v("getBrewerBeers(%s)", get(brewerId));

        // Trigger a fetch only if there was no cached result
        Observable<Option<ItemList<String>>> triggerFetchIfEmpty =
                beerListStore.getOnce(BeerSearchFetcher.getQueryId(RateBeerService.BREWER_BEERS, brewerId))
                        .toObservable()
                        .filter(RxUtils::isNoneOrEmpty)
                        .doOnNext(__ -> {
                            Timber.v("Search not cached, fetching");
                            fetchBrewerBeers(brewerId);
                        });

        return getBrewerBeersResultStream(brewerId)
                .mergeWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()));
    }

    @NonNull
    private Observable<DataStreamNotification<ItemList<String>>> getBrewerBeersResultStream(@NonNull String brewerId) {
        Timber.v("getBrewerBeersResultStream(%s)", get(brewerId));

        String queryId = BeerSearchFetcher.getQueryId(RateBeerService.BREWER_BEERS, brewerId);
        String uri = BeerSearchFetcher.getUniqueUri(queryId);

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                        .compose(RxUtils::pickValue);

        Observable<ItemList<String>> beerSearchObservable =
                beerListStore.getOnceAndStream(queryId)
                        .compose(RxUtils::pickValue);

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, beerSearchObservable);
    }

    private int fetchBrewerBeers(@NonNull String brewerId) {
        Timber.v("fetchBrewerBeers(%s)", brewerId);

        int listenerId = createListenerId();

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.BREWER_BEERS.toString());
        intent.putExtra("listenerId", listenerId);
        intent.putExtra("brewerId", brewerId);
        context.startService(intent);

        return listenerId;
    }

    //// ACCESS BREWER

    public void accessBrewer(int brewerId) {
        Timber.v("accessBrewer(%s)", brewerId);

        brewerMetadataStore.put(BrewerMetadata.newAccess(brewerId));
    }

    //// ACCESSED BREWERS

    @NonNull
    public Observable<DataStreamNotification<ItemList<String>>> getAccessedBrewers() {
        Timber.v("getAccessedBrewers");

        return brewerMetadataStore.getAccessedIdsOnce()
                .map(ids -> new ItemList<String>(null, ids, null))
                .map(DataStreamNotification::onNext);
    }

    //// INTERFACES

    public interface Login {
        Observable<DataStreamNotification<User>> call(@NonNull String username, @NonNull String password);
    }

    public interface GetLoginStatus {
        @NonNull
        Observable<DataStreamNotification<User>> call();
    }

    public interface GetUser {
        @NonNull
        Observable<Option<User>> call();
    }

    public interface SetUser {
        void call(@NonNull User user);
    }

    public interface GetBeer {
        @NonNull
        Observable<DataStreamNotification<Beer>> call(int beerId, boolean fullDetails);
    }

    public interface AccessBeer {
        void call(int beerId);
    }

    public interface GetAccessedBeers {
        @NonNull
        Observable<DataStreamNotification<ItemList<String>>> call();
    }

    public interface AccessBrewer {
        void call(int brewerId);
    }

    public interface GetAccessedBrewers {
        @NonNull
        Observable<DataStreamNotification<ItemList<String>>> call();
    }

    public interface GetBeerSearchQueries {
        @NonNull
        Observable<List<String>> call();
    }

    public interface GetBeerSearch {
        @NonNull
        Observable<DataStreamNotification<ItemList<String>>> call(@NonNull String search);
    }

    public interface GetBarcodeSearch {
        @NonNull
        Observable<DataStreamNotification<ItemList<String>>> call(@NonNull String barcode);
    }

    public interface GetTopBeers {
        @NonNull
        Observable<DataStreamNotification<ItemList<String>>> call();
    }

    public interface GetBeersInCountry {
        @NonNull
        Observable<DataStreamNotification<ItemList<String>>> call(@NonNull String countryId);
    }

    public interface GetBeersInStyle {
        @NonNull
        Observable<DataStreamNotification<ItemList<String>>> call(@NonNull String styleId);
    }

    public interface GetReview {
        @NonNull
        Observable<Option<Review>> call(int reviewId);
    }

    public interface GetReviews {
        @NonNull
        Observable<DataStreamNotification<ItemList<Integer>>> call(int beerId);
    }

    public interface GetOwnBeerReviews {
        @NonNull
        Observable<DataStreamNotification<ItemList<String>>> call();
    }

    public interface GetTickedBeers {
        @NonNull
        Observable<DataStreamNotification<ItemList<String>>> call(String userId);
    }

    public interface FetchTickedBeers {
        void call(String userId);
    }

    public interface TickBeer {
        @NonNull
        Observable<DataStreamNotification<Void>> call(int beerId, int rating);
    }

    public interface GetBrewer {
        @NonNull
        Observable<DataStreamNotification<Brewer>> call(int brewerId);
    }

    public interface GetBrewerBeers {
        @NonNull
        Observable<DataStreamNotification<ItemList<String>>> call(@NonNull String brewerId);
    }

}

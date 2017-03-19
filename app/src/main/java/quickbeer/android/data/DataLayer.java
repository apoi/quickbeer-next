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
import quickbeer.android.rx.RxUtils;
import rx.Observable;
import timber.log.Timber;

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

    public void login(@NonNull String username, @NonNull String password) {
        Timber.v("login(%s)", get(username));

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.LOGIN.toString());
        intent.putExtra("username", username);
        intent.putExtra("password", password);
        context.startService(intent);
    }

    @NonNull
    public Observable<DataStreamNotification<User>> getLoginResultStream() {
        Timber.v("getLoginResultStream()");

        String uri = LoginFetcher.getUniqueUri();

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                        .compose(RxUtils::pickValue);

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

    public void setUser(@NonNull User user) {
        userStore.put(get(user));
    }

    //// GET BEER DETAILS

    @NonNull
    public Observable<DataStreamNotification<Beer>> getBeer(int beerId) {
        Timber.v("getBeer(%s)", get(beerId));

        // Trigger a fetch only if full details haven't been fetched
        Observable<Option<Beer>> triggerFetchIfEmpty =
                beerStore.getOnce(beerId)
                        .filter(option -> option.match(beer -> !beer.hasDetails(), () -> true))
                        .doOnNext(__ -> {
                            Timber.v("Beer not cached, fetching");
                            fetchBeer(beerId);
                        });

        // Does not emit a new notification when only beer metadata changes.
        // This avoids unnecessary view redraws.
        return getBeerResultStream(beerId)
                .startWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()))
                .distinctUntilChanged();
    }

    @NonNull
    public Observable<DataStreamNotification<Beer>> getBeerResultStream(int beerId) {
        Timber.v("getBeerResultStream(%s)", get(beerId));

        String uri = BeerFetcher.getUniqueUri(beerId);

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                        .compose(RxUtils::pickValue);

        Observable<Beer> beerObservable =
                beerStore.getOnceAndStream(beerId)
                        .compose(RxUtils::pickValue);

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, beerObservable);
    }

    private void fetchBeer(int beerId) {
        Timber.v("fetchBeer(%s)", get(beerId));

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.BEER.toString());
        intent.putExtra("id", beerId);
        context.startService(intent);
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

        return beerListStore.getAllOnce()
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
                        .filter(RxUtils::isNoneOrEmpty)
                        .doOnNext(__ -> {
                            Timber.v("Search not cached, fetching");
                            fetchBeerSearch(searchString);
                        });

        return getBeerSearchResultStream(searchString)
                .startWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()));
    }

    @NonNull
    private Observable<DataStreamNotification<ItemList<String>>> getBeerSearchResultStream(@NonNull String searchString) {
        Timber.v("getBeerSearchResultStream(%s)", get(searchString));

        String queryId = BeerSearchFetcher.getQueryId(RateBeerService.SEARCH, searchString);
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

    private void fetchBeerSearch(@NonNull String searchString) {
        Timber.v("fetchBeerSearch(%s)", get(searchString));

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.SEARCH.toString());
        intent.putExtra("searchString", searchString);
        context.startService(intent);
    }

    //// BARCODE SEARCH

    @NonNull
    public Observable<DataStreamNotification<ItemList<String>>> getBarcodeSearch(@NonNull String barcode) {
        Timber.v("getBarcodeSearch(%s)", get(barcode));

        // Trigger a fetch only if there was no cached result
        Observable<Option<ItemList<String>>> triggerFetchIfEmpty =
                beerListStore.getOnce(BeerSearchFetcher.getQueryId(RateBeerService.BARCODE, barcode))
                        .filter(RxUtils::isNoneOrEmpty)
                        .doOnNext(__ -> {
                            Timber.v("Search not cached, fetching");
                            fetchBarcodeSearch(barcode);
                        });

        return getBarcodeSearchResultStream(barcode)
                .startWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()));
    }

    @NonNull
    private Observable<DataStreamNotification<ItemList<String>>> getBarcodeSearchResultStream(@NonNull String barcode) {
        Timber.v("getBarcodeSearchResultStream(%s)", get(barcode));

        String queryId = BeerSearchFetcher.getQueryId(RateBeerService.BARCODE, barcode);
        String uri = BeerSearchFetcher.getUniqueUri(queryId);

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                        .compose(RxUtils::pickValue);

        Observable<ItemList<String>> barcodeSearchObservable =
                beerListStore.getOnceAndStream(queryId)
                        .compose(RxUtils::pickValue);

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, barcodeSearchObservable);
    }

    private void fetchBarcodeSearch(@NonNull String barcode) {
        Timber.v("fetchBarcodeSearch(%s)", get(barcode));

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.BARCODE.toString());
        intent.putExtra("barcode", barcode);
        context.startService(intent);
    }

    //// TOP BEERS

    @NonNull
    public Observable<DataStreamNotification<ItemList<String>>> getTopBeers() {
        Timber.v("getTopBeers");

        // Trigger a fetch only if there was no cached result
        Observable<Option<ItemList<String>>> triggerFetchIfEmpty =
                beerListStore.getOnce(BeerSearchFetcher.getQueryId(RateBeerService.TOP50))
                        .filter(RxUtils::isNoneOrEmpty)
                        .doOnNext(__ -> {
                            Timber.v("Search not cached, fetching");
                            fetchTopBeers();
                        });

        return getTopBeersResultStream()
                .startWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()));
    }

    @NonNull
    private Observable<DataStreamNotification<ItemList<String>>> getTopBeersResultStream() {
        Timber.v("getTopBeersResultStream");

        String queryId = BeerSearchFetcher.getQueryId(RateBeerService.TOP50);
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

    private void fetchTopBeers() {
        Timber.v("fetchTopBeers");

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.TOP50.toString());
        context.startService(intent);
    }

    //// BEERS IN COUNTRY

    @NonNull
    public Observable<DataStreamNotification<ItemList<String>>> getBeersInCountry(@NonNull String countryId) {
        Timber.v("getBeersInCountry(%s)", get(countryId));

        // Trigger a fetch only if there was no cached result
        Observable<Option<ItemList<String>>> triggerFetchIfEmpty =
                beerListStore.getOnce(BeerSearchFetcher.getQueryId(RateBeerService.COUNTRY, countryId))
                        .filter(RxUtils::isNoneOrEmpty)
                        .doOnNext(__ -> {
                            Timber.v("Search not cached, fetching");
                            fetchBeersInCountry(countryId);
                        });

        return getBeersInCountryResultStream(countryId)
                .startWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()));
    }

    @NonNull
    private Observable<DataStreamNotification<ItemList<String>>> getBeersInCountryResultStream(@NonNull String countryId) {
        Timber.v("getBeersInCountryResultStream(%s)", get(countryId));

        String queryId = BeerSearchFetcher.getQueryId(RateBeerService.COUNTRY, countryId);
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

    private void fetchBeersInCountry(@NonNull String countryId) {
        Timber.v("fetchBeersInCountry");

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.COUNTRY.toString());
        intent.putExtra("countryId", countryId);
        context.startService(intent);
    }

    //// BEERS IN STYLE

    @NonNull
    public Observable<DataStreamNotification<ItemList<String>>> getBeersInStyle(@NonNull String styleId) {
        Timber.v("getBeersInStyle(%s)", get(styleId));

        // Trigger a fetch only if there was no cached result
        Observable<Option<ItemList<String>>> triggerFetchIfEmpty =
                beerListStore.getOnce(BeerSearchFetcher.getQueryId(RateBeerService.STYLE, styleId))
                        .filter(RxUtils::isNoneOrEmpty)
                        .doOnNext(__ -> {
                            Timber.v("Search not cached, fetching");
                            fetchBeersInStyle(styleId);
                        });

        return getBeersInStyleResultStream(styleId)
                .startWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()));
    }

    @NonNull
    private Observable<DataStreamNotification<ItemList<String>>> getBeersInStyleResultStream(@NonNull String styleId) {
        Timber.v("getBeersInStyleResultStream(%s)", get(styleId));

        String queryId = BeerSearchFetcher.getQueryId(RateBeerService.STYLE, styleId);
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

    private void fetchBeersInStyle(@NonNull String styleId) {
        Timber.v("fetchBeersInStyle(%s)", get(styleId));

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.STYLE.toString());
        intent.putExtra("styleId", styleId);
        context.startService(intent);
    }

    //// REVIEWS

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
                        .filter(RxUtils::isNoneOrEmpty)
                        .doOnNext(__ -> {
                            Timber.v("Reviews not cached, fetching");
                            fetchReviews(beerId);
                        });

        return getReviewsResultStream(beerId)
                .startWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()));
    }

    @NonNull
    private Observable<DataStreamNotification<ItemList<Integer>>> getReviewsResultStream(int beerId) {
        Timber.v("getReviewsResultStream(%s)", beerId);

        String uri = ReviewFetcher.getUniqueUri(beerId);

        Observable<NetworkRequestStatus> requestStatusObservable =
                requestStatusStore.getOnceAndStream(requestIdForUri(uri))
                        .compose(RxUtils::pickValue);

        Observable<ItemList<Integer>> reviewListObservable =
                reviewListStore.getOnceAndStream(beerId)
                        .compose(RxUtils::pickValue);

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, reviewListObservable);
    }

    private void fetchReviews(int beerId) {
        Timber.v("fetchReviews(%s)", beerId);

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.REVIEWS.toString());
        intent.putExtra("beerId", beerId);
        context.startService(intent);
    }

    //// TICKS

    @NonNull
    public Observable<DataStreamNotification<ItemList<String>>> getTickedBeers(@NonNull String userId) {
        Timber.v("getTickedBeers(%s)", get(userId));

        return beerStore.getTickedIds()
                .map(ItemList::<String>create)
                .map(DataStreamNotification::onNext);
    }

    public void fetchTickedBeers(@NonNull String userId) {
        Timber.v("fetchTickedBeers(%s)", get(userId));

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.TICKS.toString());
        intent.putExtra("userId", userId);
        context.startService(intent);
    }

    public void tickBeer(int beerId, int rating) {
        Timber.v("tickBeer(%s, %s)", beerId, rating);

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.TICK.toString());
        intent.putExtra("beerId", beerId);
        intent.putExtra("rating", rating);
        context.startService(intent);
    }

    //// GET BREWER DETAILS

    @NonNull
    public Observable<DataStreamNotification<Brewer>> getBrewer(int brewerId) {
        Timber.v("getBrewer(%s)", brewerId);

        // Trigger a fetch only if full details haven't been fetched
        Observable<Option<Brewer>> triggerFetchIfEmpty =
                brewerStore.getOnce(brewerId)
                        .filter(option -> option.match(brewer -> !brewer.hasDetails(), () -> true))
                        .doOnNext(__ -> {
                            Timber.v("Brewer not cached, fetching");
                            fetchBrewer(brewerId);
                        });

        return getBrewerResultStream(brewerId)
                .startWith(triggerFetchIfEmpty.flatMap(__ -> Observable.empty()))
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

    private void fetchBrewer(int brewerId) {
        Timber.v("fetchBrewer(%s)", brewerId);

        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra("serviceUriString", RateBeerService.BREWER.toString());
        intent.putExtra("id", brewerId);
        context.startService(intent);
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
        void call(@NonNull String username, @NonNull String password);
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
        Observable<DataStreamNotification<Beer>> call(int beerId);
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

    public interface FetchTickedBeers {
        void call(String userId);
    }

    public interface TickBeer {
        void call(int beerId, int rating);
    }

    public interface GetTickedBeers {
        @NonNull
        Observable<DataStreamNotification<ItemList<String>>> call(String userId);
    }

    public interface GetBrewer {
        @NonNull
        Observable<DataStreamNotification<Brewer>> call(int brewerId);
    }

}

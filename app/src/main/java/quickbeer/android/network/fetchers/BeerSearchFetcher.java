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
package quickbeer.android.network.fetchers;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import java.util.List;

import io.reark.reark.network.fetchers.FetcherBase;
import io.reark.reark.pojo.NetworkRequestStatus;
import quickbeer.android.data.pojos.Beer;
import quickbeer.android.data.pojos.ItemList;
import quickbeer.android.data.stores.BeerListStore;
import quickbeer.android.data.stores.BeerStore;
import quickbeer.android.network.NetworkApi;
import quickbeer.android.network.RateBeerService;
import quickbeer.android.network.utils.NetworkUtils;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static io.reark.reark.utils.Preconditions.checkNotNull;
import static io.reark.reark.utils.Preconditions.get;

public class BeerSearchFetcher extends FetcherBase<Uri> {

    @NonNull
    protected final NetworkApi networkApi;

    @NonNull
    protected final NetworkUtils networkUtils;

    @NonNull
    private final BeerStore beerStore;

    @NonNull
    private final BeerListStore beerListStore;

    public BeerSearchFetcher(@NonNull final NetworkApi networkApi,
                             @NonNull final NetworkUtils networkUtils,
                             @NonNull final Action1<NetworkRequestStatus> updaterequestStatus,
                             @NonNull final BeerStore beerStore,
                             @NonNull final BeerListStore beerListStore) {
        super(updaterequestStatus);

        this.networkApi = get(networkApi);
        this.networkUtils = get(networkUtils);
        this.beerStore = get(beerStore);
        this.beerListStore = get(beerListStore);
    }

    @Override
    public void fetch(@NonNull final Intent intent) {
        final String searchString = get(intent).getStringExtra("searchString");

        if (searchString != null) {
            fetchBeerSearch(searchString);
        } else {
            Timber.e("No searchString provided in the intent extras");
        }
    }

    protected void fetchBeerSearch(@NonNull final String query) {
        Timber.d("fetchBeerSearch(" + query + ")");

        final String queryId = getQueryId(getServiceUri(), get(query));
        final String uri = getUniqueUri(queryId);

        if (isOngoingRequest(uri.hashCode())) {
            Timber.d("Found an ongoing request for search " + queryId);
            return;
        }

        Subscription subscription = createNetworkObservable(query)
                .subscribeOn(Schedulers.computation())
                .flatMap(Observable::from)
                .doOnNext(beerStore::put)
                .map(Beer::id)
                .toList()
                .map(beerIds -> ItemList.create(queryId, beerIds, DateTime.now()))
                .doOnSubscribe(() -> startRequest(uri))
                .doOnCompleted(() -> completeRequest(uri))
                .doOnError(doOnError(uri))
                .subscribe(beerListStore::put,
                           err -> Timber.e(err, "Error fetching beer search for %s", uri));

        addRequest(uri.hashCode(), subscription);
    }

    @NonNull
    protected Observable<List<Beer>> createNetworkObservable(@NonNull final String searchString) {
        return networkApi.search(networkUtils.createRequestParams("bn", get(searchString)));
    }

    @NonNull
    @Override
    public Uri getServiceUri() {
        return RateBeerService.SEARCH;
    }

    // Brewer search store needs separate query identifiers for normal searches and fixed searches
    // (top50, top in country, top in style). Fixed searches come attached with a service uri
    // identifier to make sure they stand apart from the normal searches.
    public static String getQueryId(@NonNull final Uri serviceUri, @NonNull final String query) {
        checkNotNull(serviceUri);
        checkNotNull(query);

        if (serviceUri.equals(RateBeerService.SEARCH)) {
            return query;
        } else if (!query.isEmpty()) {
            return String.format("%s_%s", serviceUri, query);
        } else {
            return serviceUri.toString();
        }
    }

    public static String getQueryId(@NonNull final Uri serviceUri) {
        return getQueryId(serviceUri, "");
    }

    @NonNull
    public static String getUniqueUri(@NonNull final String id) {
        return ItemList.class + "/beerSearch/" + id;
    }
}
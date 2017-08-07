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
package quickbeer.android.network.fetchers;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reark.reark.network.fetchers.FetcherBase;
import io.reark.reark.pojo.NetworkRequestStatus;
import ix.Ix;
import okhttp3.ResponseBody;
import quickbeer.android.Constants;
import quickbeer.android.data.pojos.Beer;
import quickbeer.android.data.pojos.ItemList;
import quickbeer.android.data.pojos.User;
import quickbeer.android.data.stores.BeerListStore;
import quickbeer.android.data.stores.BeerStore;
import quickbeer.android.data.stores.UserStore;
import quickbeer.android.network.NetworkApi;
import quickbeer.android.network.RateBeerService;
import quickbeer.android.network.fetchers.actions.LoginAndRetry;
import quickbeer.android.network.utils.NetworkUtils;
import quickbeer.android.rx.RxUtils;
import quickbeer.android.utils.ValueUtils;
import rx.Single;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static io.reark.reark.utils.Preconditions.checkNotNull;
import static io.reark.reark.utils.Preconditions.get;

public class TickBeerFetcher extends FetcherBase<Uri> {

    @NonNull
    private static final List<String> SUCCESS_RESPONSES =
            Arrays.asList("added", "updated", "removed", "deleted");

    @NonNull
    private final NetworkApi networkApi;

    @NonNull
    private final NetworkUtils networkUtils;

    @NonNull
    private final BeerStore beerStore;

    @NonNull
    private final BeerListStore beerListStore;

    @NonNull
    private final UserStore userStore;

    public TickBeerFetcher(@NonNull NetworkApi networkApi,
                           @NonNull NetworkUtils networkUtils,
                           @NonNull Action1<NetworkRequestStatus> networkRequestStatus,
                           @NonNull BeerStore beerStore,
                           @NonNull BeerListStore beerListStore,
                           @NonNull UserStore userStore) {
        super(networkRequestStatus);

        this.networkApi = get(networkApi);
        this.networkUtils = get(networkUtils);
        this.beerStore = get(beerStore);
        this.beerListStore = get(beerListStore);
        this.userStore = get(userStore);
    }

    @Override
    public void fetch(@NonNull Intent intent, int listenerId) {
        checkNotNull(intent);

        if (!intent.hasExtra("beerId") || !intent.hasExtra("rating")) {
            Timber.e("Missing required fetch parameters!");
            return;
        }

        int beerId = intent.getIntExtra("beerId", 0);
        int rating = intent.getIntExtra("rating", 0);
        String uri = getUniqueUri(beerId, rating);
        int requestId = uri.hashCode();

        addListener(requestId, listenerId);

        if (isOngoingRequest(requestId)) {
            Timber.d("Found an ongoing request for beer tick");
            return;
        }

        Timber.d("Tick rating of %s for beer %s", rating, beerId);

        Subscription subscription = getUserId()
                .flatMap(userId -> createNetworkObservable(beerId, rating, userId))
                .flatMap(__ -> beerStore.getOnce(beerId))
                .compose(RxUtils::valueOrError)
                .map(beer -> withRating(beer, rating))
                .doOnSuccess(beerStore::put)
                .zipWith(getTickedBeers(), TickBeerFetcher::appendTick)
                .zipWith(getQueryId(), (ticks, ticksQueryId) -> ItemList.create(ticksQueryId, ticks, ZonedDateTime.now()))
                .doOnSubscribe(() -> startRequest(requestId, uri))
                .doOnSuccess(updated -> completeRequest(requestId, uri, false))
                .doOnError(doOnError(requestId, uri))
                .subscribe(beerListStore::put,
                        error -> Timber.w(error, "Error ticking beer"));

        addRequest(requestId, subscription);
    }

    @NonNull
    private static Beer withRating(@NonNull Beer beer, int rating) {
        return beer.toBuilder()
                .tickValue(rating)
                .tickDate(ZonedDateTime.now())
                .build();
    }

    @NonNull
    private static List<Integer> appendTick(@NonNull Beer beer, @NonNull List<Integer> ticks) {
        List<Integer> list = new ArrayList<>(ticks);
        list.remove(beer.id());
        if (ValueUtils.greaterThan(beer.tickValue(), 0)) {
            list.add(0, beer.id());
        }
        return list;
    }

    @NonNull
    private Single<Integer> getUserId() {
        return userStore
                .getOnce(Constants.DEFAULT_USER_ID)
                .subscribeOn(Schedulers.io())
                .compose(RxUtils::valueOrError)
                .map(User::id);
    }

    @NonNull
    private Single<List<Integer>> getTickedBeers() {
        return getQueryId()
                .flatMap(beerListStore::getOnce)
                .map(option -> option.match(ItemList::getItems, Collections::emptyList));
    }

    @NonNull
    private Single<String> getQueryId() {
        return getUserId()
                .map(String::valueOf)
                .map(userId -> BeerSearchFetcher.getQueryId(RateBeerService.USER_TICKS, userId));
    }

    @NonNull
    private Single<Boolean> createNetworkObservable(int beerId, @IntRange(from=0,to=5) int rating, int userId) {
        Map<String, String> requestParams = networkUtils.createRequestParams("b", String.valueOf(beerId));
        requestParams.put("u", String.valueOf(userId));

        if (rating == 0) {
            requestParams.put("m", "3");
        } else {
            requestParams.put("m", "2");
            requestParams.put("l", String.valueOf(rating));
        }

        return networkApi.tickBeer(requestParams)
                .flatMap(TickBeerFetcher::requestSuccessful)
                .retryWhen(new LoginAndRetry(networkApi, userStore));
    }

    @NonNull
    private static Single<Boolean> requestSuccessful(@NonNull ResponseBody responseBody) {
        return Single.fromCallable(() -> {
            String value = responseBody.string().toLowerCase(Locale.ROOT);

            //noinspection BooleanVariableAlwaysNegated
            boolean success = Ix.from(SUCCESS_RESPONSES)
                    .any(value::contains)
                    .first();

            if (!success) {
                throw new Exception("Unexpected response: " + value);
            }

            return true;
        });
    }

    @NonNull
    @Override
    public Uri getServiceUri() {
        return RateBeerService.TICK_BEER;
    }

    @NonNull
    public static String getUniqueUri(int id, int rating) {
        return Beer.class + "/" + id + "/rate/" + rating;
    }
}

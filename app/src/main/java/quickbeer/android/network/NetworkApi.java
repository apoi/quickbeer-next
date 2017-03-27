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
package quickbeer.android.network;

import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import quickbeer.android.Constants;
import quickbeer.android.data.pojos.Beer;
import quickbeer.android.data.pojos.Brewer;
import quickbeer.android.data.pojos.Review;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Single;

import static io.reark.reark.utils.Preconditions.get;

public class NetworkApi {

    @NonNull
    private final RateBeerService rateBeerService;

    public NetworkApi(@NonNull OkHttpClient client,
                      @NonNull Gson gson) {
        Retrofit retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(get(gson)))
                .baseUrl(Constants.API_URL)
                .client(get(client))
                .build();

        rateBeerService = retrofit.create(RateBeerService.class);
    }

    @NonNull
    public Single<ResponseBody> login(String username, String password) {
        return rateBeerService
                .login(username, password, Constants.LOGIN_DEFAULT_SAVE_INFO);
    }

    @NonNull
    public Single<Beer> getBeer(Map<String, String> params) {
        return rateBeerService
                .getBeer(params)
                .map(list -> list.get(0)); // API returns a list of one beer
    }

    @NonNull
    public Single<List<Beer>> search(Map<String, String> params) {
        return rateBeerService
                .search(params);
    }

    @NonNull
    public Single<List<Beer>> barcode(Map<String, String> params) {
        return rateBeerService
                .barcode(params);
    }

    @NonNull
    public Single<List<Beer>> getBeersInCountry(Map<String, String> params) {
        return rateBeerService
                .topBeers(params);
    }

    @NonNull
    public Single<List<Beer>> getBeersInStyle(Map<String, String> params) {
        return rateBeerService
                .beersInStyle(params);
    }

    @NonNull
    public Single<List<Review>> getReviews(Map<String, String> params) {
        return rateBeerService
                .getReviews(params);
    }

    @NonNull
    public Single<List<Beer>> getOwnBeerReviews(Map<String, String> params) {
        return rateBeerService
                .getOwnBeerReviews(params);
    }

    @NonNull
    public Single<List<Beer>> getTicks(Map<String, String> params) {
        return rateBeerService
                .getTicks(params);
    }

    @NonNull
    public Single<ResponseBody> tickBeer(Map<String, String> params) {
        return rateBeerService
                .tickBeer(params);
    }

    @NonNull
    public Single<Brewer> getBrewer(Map<String, String> params) {
        return rateBeerService
                .getBrewer(params)
                .map(list -> list.get(0)); // API returns a list of one brewer
    }

    @NonNull
    public Single<List<Beer>> getBrewerBeers(Map<String, String> params) {
        return rateBeerService
                .getBrewerBeers(params);
    }
}

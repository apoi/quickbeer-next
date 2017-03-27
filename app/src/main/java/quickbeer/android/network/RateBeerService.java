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

import android.net.Uri;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import quickbeer.android.data.pojos.Beer;
import quickbeer.android.data.pojos.Brewer;
import quickbeer.android.data.pojos.Review;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;
import rx.Single;

public interface RateBeerService {
    Uri LOGIN         = Uri.parse("__login");
    Uri BEER          = Uri.parse("__beer");
    Uri SEARCH        = Uri.parse("__search");
    Uri BARCODE       = Uri.parse("__barcode");
    Uri TOP50         = Uri.parse("__top50");
    Uri COUNTRY       = Uri.parse("__country");
    Uri STYLE         = Uri.parse("__style");
    Uri REVIEWS       = Uri.parse("__reviews");
    Uri OWN_REVIEWS   = Uri.parse("__own_reviews");
    Uri TICKS         = Uri.parse("__ticks");
    Uri TICK_BEER     = Uri.parse("__tick_beer");
    Uri BREWER        = Uri.parse("__brewer");
    Uri BREWER_BEERS  = Uri.parse("__brewer_beers");

    @FormUrlEncoded
    @POST("/Signin_r.asp")
    Single<ResponseBody> login(@Field("username") String username,
                               @Field("pwd") String password,
                               @Field("saveinfo") String saveinfo);

    @GET("/json/bff.asp")
    Single<List<Beer>> getBeer(@QueryMap Map<String, String> params);

    @GET("/json/bff.asp")
    Single<List<Beer>> search(@QueryMap Map<String, String> params);

    @GET("/json/upc.asp")
    Single<List<Beer>> barcode(@QueryMap Map<String, String> params);

    @GET("/json/tb.asp")
    Single<List<Beer>> topBeers(@QueryMap Map<String, String> params);

    @GET("/json/style.asp")
    Single<List<Beer>> beersInStyle(@QueryMap Map<String, String> params);

    @GET("/json/gr.asp")
    Single<List<Review>> getReviews(@QueryMap Map<String, String> params);

    @GET("/json/revs.asp")
    Single<List<Beer>> getOwnBeerReviews(@QueryMap Map<String, String> params);

    @GET("/json/bt.asp")
    Single<List<Beer>> getTicks(@QueryMap Map<String, String> params);

    @GET("/json/bt.asp")
    Single<ResponseBody> tickBeer(@QueryMap Map<String, String> params);

    @GET("/json/bi.asp")
    Single<List<Brewer>> getBrewer(@QueryMap Map<String, String> params);

    @GET("/json/bw.asp")
    Single<List<Beer>> getBrewerBeers(@QueryMap Map<String, String> params);
}

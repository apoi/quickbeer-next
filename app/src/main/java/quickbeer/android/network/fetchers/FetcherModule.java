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

import android.net.Uri;
import android.support.annotation.NonNull;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import io.reark.reark.network.fetchers.Fetcher;
import io.reark.reark.network.fetchers.UriFetcherManager;
import quickbeer.android.Constants.Fetchers;
import quickbeer.android.data.stores.BeerListStore;
import quickbeer.android.data.stores.BeerMetadataStore;
import quickbeer.android.data.stores.BeerStore;
import quickbeer.android.data.stores.BrewerMetadataStore;
import quickbeer.android.data.stores.BrewerStore;
import quickbeer.android.data.stores.NetworkRequestStatusStore;
import quickbeer.android.data.stores.ReviewListStore;
import quickbeer.android.data.stores.ReviewStore;
import quickbeer.android.data.stores.UserStore;
import quickbeer.android.network.NetworkApi;
import quickbeer.android.network.NetworkModule;
import quickbeer.android.network.utils.NetworkUtils;

@Module(includes = NetworkModule.class)
public final class FetcherModule {

    @Provides
    @Named(Fetchers.LOGIN)
    static Fetcher<Uri> provideLoginFetcher(
            @NonNull NetworkApi networkApi,
            @NonNull ClearableCookieJar cookieJar,
            @NonNull NetworkRequestStatusStore requestStatusStore,
            @NonNull UserStore userStore) {
        return new LoginFetcher(networkApi,
                cookieJar,
                requestStatusStore::put,
                userStore);
    }

    @Provides
    @Named(Fetchers.BEER)
    static Fetcher<Uri> provideBeerFetcher(
            @NonNull NetworkApi networkApi,
            @NonNull NetworkUtils networkUtils,
            @NonNull NetworkRequestStatusStore requestStatusStore,
            @NonNull BeerStore beerStore,
            @NonNull BeerMetadataStore metadataStore) {
        return new BeerFetcher(networkApi,
                networkUtils,
                requestStatusStore::put,
                beerStore,
                metadataStore);
    }

    @Provides
    @Named(Fetchers.BEER_SEARCH)
    static Fetcher<Uri> provideBeerSearchFetcher(
            @NonNull NetworkApi networkApi,
            @NonNull NetworkUtils networkUtils,
            @NonNull NetworkRequestStatusStore requestStatusStore,
            @NonNull BeerStore beerStore,
            @NonNull BeerListStore beerListStore) {
        return new BeerSearchFetcher(networkApi,
                networkUtils,
                requestStatusStore::put,
                beerStore,
                beerListStore);
    }

    @Provides
    @Named(Fetchers.BARCODE_SEARCH)
    static Fetcher<Uri> provideBarcodeSearchFetcher(
            @NonNull NetworkApi networkApi,
            @NonNull NetworkUtils networkUtils,
            @NonNull NetworkRequestStatusStore requestStatusStore,
            @NonNull BeerStore beerStore,
            @NonNull BeerListStore beerListStore) {
        return new BarcodeSearchFetcher(networkApi,
                networkUtils,
                requestStatusStore::put,
                beerStore,
                beerListStore);
    }

    @Provides
    @Named(Fetchers.TOP_BEERS)
    static Fetcher<Uri> provideTopBeersFetcher(
            @NonNull NetworkApi networkApi,
            @NonNull NetworkUtils networkUtils,
            @NonNull NetworkRequestStatusStore requestStatusStore,
            @NonNull BeerStore beerStore,
            @NonNull BeerListStore beerListStore) {
        return new TopBeersFetcher(networkApi,
                networkUtils,
                requestStatusStore::put,
                beerStore,
                beerListStore);
    }

    @Provides
    @Named(Fetchers.BEERS_IN_COUNTRY)
    static Fetcher<Uri> provideBeersInCountryFetcher(
            @NonNull NetworkApi networkApi,
            @NonNull NetworkUtils networkUtils,
            @NonNull NetworkRequestStatusStore requestStatusStore,
            @NonNull BeerStore beerStore,
            @NonNull BeerListStore beerListStore) {
        return new BeersInCountryFetcher(networkApi,
                networkUtils,
                requestStatusStore::put,
                beerStore,
                beerListStore);
    }

    @Provides
    @Named(Fetchers.BEERS_IN_STYLE)
    static Fetcher<Uri> provideBeersInStyleFetcher(
            @NonNull NetworkApi networkApi,
            @NonNull NetworkUtils networkUtils,
            @NonNull NetworkRequestStatusStore requestStatusStore,
            @NonNull BeerStore beerStore,
            @NonNull BeerListStore beerListStore) {
        return new BeersInStyleFetcher(networkApi,
                networkUtils,
                requestStatusStore::put,
                beerStore,
                beerListStore);
    }

    @Provides
    @Named(Fetchers.BREWER)
    static Fetcher<Uri> provideBrewerFetcher(
            @NonNull NetworkApi networkApi,
            @NonNull NetworkUtils networkUtils,
            @NonNull NetworkRequestStatusStore requestStatusStore,
            @NonNull BrewerStore brewerStore,
            @NonNull BrewerMetadataStore metadataStore) {
        return new BrewerFetcher(networkApi,
                networkUtils,
                requestStatusStore::put,
                brewerStore,
                metadataStore);
    }

    @Provides
    @Named(Fetchers.BREWER_BEERS)
    static Fetcher<Uri> provideBrewerBeersFetcher(
            @NonNull NetworkApi networkApi,
            @NonNull NetworkUtils networkUtils,
            @NonNull NetworkRequestStatusStore requestStatusStore,
            @NonNull BeerStore beerStore,
            @NonNull BeerListStore beerListStore) {
        return new BrewerBeersFetcher(networkApi,
                networkUtils,
                requestStatusStore::put,
                beerStore,
                beerListStore);
    }

    @Provides
    @Named(Fetchers.REVIEW)
    static Fetcher<Uri> provideReviewFetcher(
            @NonNull NetworkApi networkApi,
            @NonNull NetworkUtils networkUtils,
            @NonNull NetworkRequestStatusStore requestStatusStore,
            ReviewStore reviewStore,
            ReviewListStore reviewListStore) {
        return new ReviewFetcher(networkApi,
                networkUtils,
                requestStatusStore::put,
                reviewStore,
                reviewListStore);
    }

    @Provides
    @Named(Fetchers.OWN_REVIEWS)
    static Fetcher<Uri> provideOwnReviewsFetcher(
            @NonNull NetworkApi networkApi,
            @NonNull NetworkUtils networkUtils,
            @NonNull NetworkRequestStatusStore requestStatusStore,
            @NonNull BeerStore beerStore,
            @NonNull BeerListStore beerListStore) {
        return new OwnReviewsFetcher(networkApi,
                networkUtils,
                requestStatusStore::put,
                beerStore,
                beerListStore);
    }

    @Provides
    @Named(Fetchers.TICKS)
    static Fetcher<Uri> provideTicksFetcher(
            @NonNull NetworkApi networkApi,
            @NonNull NetworkUtils networkUtils,
            @NonNull NetworkRequestStatusStore requestStatusStore,
            @NonNull BeerStore beerStore,
            @NonNull BeerListStore beerListStore) {
        return new TicksFetcher(networkApi,
                networkUtils,
                requestStatusStore::put,
                beerStore,
                beerListStore);
    }

    @Provides
    @Named(Fetchers.TICK)
    static Fetcher<Uri> provideTickBeerFetcher(
            @NonNull NetworkApi networkApi,
            @NonNull NetworkUtils networkUtils,
            @NonNull NetworkRequestStatusStore requestStatusStore,
            @NonNull BeerStore beerStore,
            @NonNull UserStore userStore) {
        return new TickBeerFetcher(networkApi,
                networkUtils,
                requestStatusStore::put,
                beerStore,
                userStore);
    }

    @Provides
    static UriFetcherManager provideUriFetcherManager(
            @Named(Fetchers.LOGIN) @NonNull Fetcher<Uri> loginFetcher,
            @Named(Fetchers.BEER) @NonNull Fetcher<Uri> beerFetcher,
            @Named(Fetchers.BEER_SEARCH) @NonNull Fetcher<Uri> beerSearchFetcher,
            @Named(Fetchers.BARCODE_SEARCH) @NonNull Fetcher<Uri> barcodeSearchFetcher,
            @Named(Fetchers.TOP_BEERS) @NonNull Fetcher<Uri> topBeersFetcher,
            @Named(Fetchers.BEERS_IN_COUNTRY) @NonNull Fetcher<Uri> beersInCountryFetcher,
            @Named(Fetchers.BEERS_IN_STYLE) @NonNull Fetcher<Uri> beersInStyleFetcher,
            @Named(Fetchers.BREWER) @NonNull Fetcher<Uri> brewerFetcher,
            @Named(Fetchers.BREWER_BEERS) @NonNull Fetcher<Uri> brewerBeersFetcher,
            @Named(Fetchers.REVIEW) @NonNull Fetcher<Uri> reviewFetcher,
            @Named(Fetchers.OWN_REVIEWS) @NonNull Fetcher<Uri> ownReviewsFetcher,
            @Named(Fetchers.TICKS) @NonNull Fetcher<Uri> ticksFetcher,
            @Named(Fetchers.TICK) @NonNull Fetcher<Uri> tickBeerFetcher) {
        List<Fetcher<Uri>> fetchers = Arrays.asList(
                loginFetcher,
                beerFetcher,
                beerSearchFetcher,
                barcodeSearchFetcher,
                topBeersFetcher,
                beersInCountryFetcher,
                beersInStyleFetcher,
                brewerFetcher,
                brewerBeersFetcher,
                reviewFetcher,
                ownReviewsFetcher,
                ticksFetcher,
                tickBeerFetcher
        );

        return new UriFetcherManager.Builder()
                .fetchers(fetchers)
                .build();
    }
}

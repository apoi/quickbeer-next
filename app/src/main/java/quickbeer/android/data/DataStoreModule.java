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
import android.support.annotation.NonNull;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reark.reark.network.fetchers.UriFetcherManager;
import quickbeer.android.data.stores.BeerListStore;
import quickbeer.android.data.stores.BeerMetadataStore;
import quickbeer.android.data.stores.BeerStore;
import quickbeer.android.data.stores.BrewerListStore;
import quickbeer.android.data.stores.BrewerMetadataStore;
import quickbeer.android.data.stores.BrewerStore;
import quickbeer.android.data.stores.NetworkRequestStatusStore;
import quickbeer.android.data.stores.ReviewListStore;
import quickbeer.android.data.stores.ReviewStore;
import quickbeer.android.data.stores.StoreModule;
import quickbeer.android.data.stores.UserStore;
import quickbeer.android.injections.ForApplication;
import quickbeer.android.network.fetchers.FetcherModule;

@Module(includes = { FetcherModule.class, StoreModule.class })
public final class DataStoreModule {

    @Provides
    static DataLayer.Login provideLogin(
            @NonNull DataLayer dataLayer) {
        return dataLayer::login;
    }

    @Provides
    static DataLayer.GetLoginStatus provideLoginStatus(
            @NonNull DataLayer dataLayer) {
        return dataLayer::getLoginStatus;
    }

    @Provides
    static DataLayer.GetUser provideGetUser(
            @NonNull DataLayer dataLayer) {
        return dataLayer::getUser;
    }

    @Provides
    static DataLayer.GetBeer provideGetBeer(
            @NonNull DataLayer dataLayer) {
        return dataLayer::getBeer;
    }

    @Provides
    static DataLayer.AccessBeer provideAccessBeer(
            @NonNull DataLayer dataLayer) {
        return dataLayer::accessBeer;
    }

    @Provides
    static DataLayer.GetAccessedBeers provideGetAccessedBeers(
            @NonNull DataLayer dataLayer) {
        return dataLayer::getAccessedBeers;
    }

    @Provides
    static DataLayer.AccessBrewer provideAccessBrewer(
            @NonNull DataLayer dataLayer) {
        return dataLayer::accessBrewer;
    }

    @Provides
    static DataLayer.GetAccessedBrewers provideGetAccessedBrewers(
            @NonNull DataLayer dataLayer) {
        return dataLayer::getAccessedBrewers;
    }

    @Provides
    static DataLayer.GetBeerSearchQueries provideGetBeerSearchQueries(
            @NonNull DataLayer dataLayer) {
        return dataLayer::getBeerSearchQueriesOnce;
    }

    @Provides
    static DataLayer.GetBeerSearch provideGetBeerSearch(
            @NonNull DataLayer dataLayer) {
        return dataLayer::getBeerSearch;
    }

    @Provides
    static DataLayer.GetBarcodeSearch provideGetBarcodeSearch(
            @NonNull DataLayer dataLayer) {
        return dataLayer::getBarcodeSearch;
    }

    @Provides
    static DataLayer.GetTopBeers provideGetTopBeers(
            @NonNull DataLayer dataLayer) {
        return dataLayer::getTopBeers;
    }

    @Provides
    static DataLayer.GetBeersInCountry provideGetBeersInCountry(
            @NonNull DataLayer dataLayer) {
        return dataLayer::getBeersInCountry;
    }

    @Provides
    static DataLayer.GetBeersInStyle provideGetBeersInStyle(

            @NonNull DataLayer dataLayer) {
        return dataLayer::getBeersInStyle;
    }

    @Provides
    static DataLayer.GetReview provideGetReview(
            @NonNull DataLayer dataLayer) {
        return dataLayer::getReview;
    }

    @Provides
    static DataLayer.GetReviews provideGetReviews(
            @NonNull DataLayer dataLayer) {
        return dataLayer::getReviews;
    }

    @Provides
    static DataLayer.GetOwnBeerReviews provideGetOwnBeerReviews(
            @NonNull DataLayer dataLayer) {
        return dataLayer::getOwnBeerReviews;
    }

    @Provides
    static DataLayer.GetTickedBeers provideGetTickedBeers(
            @NonNull DataLayer dataLayer) {
        return dataLayer::getTickedBeers;
    }

    @Provides
    static DataLayer.FetchTickedBeers provideFetchTickedBeers(
            @NonNull DataLayer dataLayer) {
        return dataLayer::fetchTickedBeers;
    }

    @Provides
    static DataLayer.TickBeer provideTickBeer(
            @NonNull DataLayer dataLayer) {
        return dataLayer::tickBeer;
    }

    @Provides
    static DataLayer.GetBrewer provideGetBrewer(
            @NonNull DataLayer dataLayer) {
        return dataLayer::getBrewer;
    }

    @Provides
    static DataLayer.GetBrewerBeers provideGetBrewerBeers(
            @NonNull DataLayer dataLayer) {
        return dataLayer::getBrewerBeers;
    }

    @Provides
    @Singleton
    static DataLayer provideApplicationDataLayer(
            @ForApplication @NonNull Context context,
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
        return new DataLayer(context,
                userStore,
                requestStatusStore,
                beerStore,
                beerListStore,
                beerMetadataStore,
                reviewStore,
                reviewListStore,
                brewerStore,
                brewerListStore,
                brewerMetadataStore);
    }

    @Provides
    @Singleton
    static ServiceDataLayer provideServiceDataLayer(
            @NonNull UriFetcherManager fetcherManager,
            @NonNull NetworkRequestStatusStore requestStatusStore,
            @NonNull BeerStore beerStore,
            @NonNull BeerListStore beerListStore,
            @NonNull BeerMetadataStore beerMetadataStore,
            @NonNull ReviewStore reviewStore,
            @NonNull ReviewListStore reviewListStore,
            @NonNull BrewerStore brewerStore,
            @NonNull BrewerListStore brewerListStore,
            @NonNull BrewerMetadataStore brewerMetadataStore) {
        return new ServiceDataLayer(fetcherManager,
                requestStatusStore,
                beerStore,
                beerListStore,
                beerMetadataStore,
                reviewStore,
                reviewListStore,
                brewerStore,
                brewerListStore,
                brewerMetadataStore);
    }
}

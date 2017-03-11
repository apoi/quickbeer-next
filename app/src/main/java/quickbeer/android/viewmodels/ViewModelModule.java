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

import dagger.Module;
import dagger.Provides;
import quickbeer.android.data.DataLayer;
import quickbeer.android.features.beerdetails.BeerDetailsViewModel;

@Module
public final class ViewModelModule {

    @Provides
    static RecentBeersViewModel provideRecentBeersViewModel(
            @NonNull final DataLayer.GetBeer getBeer,
            @NonNull final DataLayer.GetAccessedBeers getAccessedBeers) {
        return new RecentBeersViewModel(getBeer, getAccessedBeers);
    }

    @Provides
    static RecentBrewersViewModel provideRecentBrewersViewModel(
            @NonNull final DataLayer.GetBeer getBeer,
            @NonNull final DataLayer.GetBrewer getBrewer,
            @NonNull final DataLayer.GetAccessedBrewers getAccessedBrewers) {
        return new RecentBrewersViewModel(getBeer, getBrewer, getAccessedBrewers);
    }

    @Provides
    static BeerSearchViewModel provideBeerSearchViewModel(
            @NonNull final DataLayer.GetBeer getBeer,
            @NonNull final DataLayer.GetBeerSearch getBeerSearch,
            @NonNull final SearchViewViewModel searchViewViewModel) {
        return new BeerSearchViewModel(getBeer, getBeerSearch, searchViewViewModel);
    }

    @Provides
    static BarcodeSearchViewModel provideBarcodeSearchViewModel(
            @NonNull final DataLayer.GetBeer getBeer,
            @NonNull final DataLayer.GetBeerSearch getBeerSearch,
            @NonNull final DataLayer.GetBarcodeSearch getBarcodeSearch,
            @NonNull final SearchViewViewModel searchViewViewModel) {
        return new BarcodeSearchViewModel(getBeer, getBeerSearch, getBarcodeSearch, searchViewViewModel);
    }

    @Provides
    static TopBeersViewModel provideTopBeersViewModel(
            @NonNull final DataLayer.GetBeer getBeer,
            @NonNull final DataLayer.GetBeerSearch getBeerSearch,
            @NonNull final DataLayer.GetTopBeers getTopBeers,
            @NonNull final SearchViewViewModel searchViewViewModel) {
        return new TopBeersViewModel(getBeer, getBeerSearch, getTopBeers, searchViewViewModel);
    }

    @Provides
    static BeerDetailsViewModel provideBeerDetailsViewModel(
            @NonNull final DataLayer.GetBeer getBeer,
            @NonNull final DataLayer.GetBrewer getBrewer,
            @NonNull final DataLayer.GetReviews getReviews,
            @NonNull final DataLayer.GetReview getReview) {
        return new BeerDetailsViewModel(getBeer, getBrewer, getReviews, getReview);
    }

}

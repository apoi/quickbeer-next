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
package quickbeer.android.injections;

import dagger.Subcomponent;
import quickbeer.android.features.about.AboutDetailsFragment;
import quickbeer.android.features.beerdetails.BeerDetailsFragment;
import quickbeer.android.features.beerdetails.BeerDetailsPagerFragment;
import quickbeer.android.features.beerdetails.BeerReviewsFragment;
import quickbeer.android.features.brewerdetails.BrewerBeersFragment;
import quickbeer.android.features.brewerdetails.BrewerDetailsFragment;
import quickbeer.android.features.brewerdetails.BrewerDetailsPagerFragment;
import quickbeer.android.features.home.BeerTabFragment;
import quickbeer.android.features.home.BrewerTabFragment;
import quickbeer.android.features.home.HomeFragment;
import quickbeer.android.features.list.fragments.BarcodeSearchFragment;
import quickbeer.android.features.list.fragments.BeerListFragment;
import quickbeer.android.features.list.fragments.BeerSearchFragment;
import quickbeer.android.features.list.fragments.BeersInCountryFragment;
import quickbeer.android.features.list.fragments.BeersInStyleFragment;
import quickbeer.android.features.list.fragments.BrewerListFragment;
import quickbeer.android.features.list.fragments.CountryListFragment;
import quickbeer.android.features.list.fragments.ReviewedBeersFragment;
import quickbeer.android.features.list.fragments.StyleListFragment;
import quickbeer.android.features.list.fragments.TickedBeersFragment;
import quickbeer.android.features.list.fragments.TopBeersFragment;
import quickbeer.android.features.profile.ProfileDetailsFragment;
import quickbeer.android.features.profile.ProfileLoginFragment;

@FragmentScope
@Subcomponent(modules = FragmentModule.class)
public interface FragmentComponent {

    void inject(HomeFragment homeFragment);

    void inject(ProfileLoginFragment profileLoginFragment);

    void inject(ProfileDetailsFragment profileDetailsFragment);

    void inject(BeerTabFragment beerTabFragment);

    void inject(BrewerTabFragment brewerTabFragment);

    void inject(BeerListFragment beerListFragment);

    void inject(BrewerListFragment brewerListFragment);

    void inject(BeerSearchFragment beerSearchFragment);

    void inject(BarcodeSearchFragment barcodeSearchFragment);

    void inject(BeerDetailsPagerFragment beerDetailsPagerFragment);

    void inject(BeerDetailsFragment beerDetailsFragment);

    void inject(BeerReviewsFragment beerReviewsFragment);

    void inject(BrewerDetailsPagerFragment brewerDetailsPagerFragment);

    void inject(BrewerDetailsFragment brewerDetailsFragment);

    void inject(BrewerBeersFragment brewerBeersFragment);

    void inject(ReviewedBeersFragment reviewedBeersFragment);

    void inject(TickedBeersFragment tickedBeersFragment);

    void inject(TopBeersFragment topBeersFragment);

    void inject(BeersInCountryFragment beersInCountryFragment);

    void inject(BeersInStyleFragment beersInStyleFragment);

    void inject(CountryListFragment countryListFragment);

    void inject(StyleListFragment styleListFragment);

    void inject(AboutDetailsFragment aboutDetailsFragment);

}

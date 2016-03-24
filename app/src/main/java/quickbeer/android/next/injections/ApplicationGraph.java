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
package quickbeer.android.next.injections;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Component;
import quickbeer.android.next.QuickBeer;
import quickbeer.android.next.activities.BeerDetailsActivity;
import quickbeer.android.next.activities.base.BaseActivity;
import quickbeer.android.next.activities.base.SearchActivity;
import quickbeer.android.next.activities.base.SearchBarActivity;
import quickbeer.android.next.data.DataStoreModule;
import quickbeer.android.next.fragments.BeerDetailsFragment;
import quickbeer.android.next.fragments.BeerListFragment;
import quickbeer.android.next.fragments.BeerSearchFragment;
import quickbeer.android.next.fragments.BeersInCountryFragment;
import quickbeer.android.next.fragments.BeersInStyleFragment;
import quickbeer.android.next.fragments.MainFragment;
import quickbeer.android.next.fragments.TopBeersFragment;
import quickbeer.android.next.fragments.TopListFragment;
import quickbeer.android.next.network.NetworkModule;
import quickbeer.android.next.network.NetworkService;
import quickbeer.android.next.utils.UtilsModule;
import quickbeer.android.next.viewmodels.ViewModelModule;

@Singleton
@Component(modules = {
        ApplicationModule.class,
        NetworkModule.class,
        InstrumentationModule.class,
        UtilsModule.class,
        ViewModelModule.class,
        DataStoreModule.class,
})
public interface ApplicationGraph {
    void inject(QuickBeer quickBeer);
    void inject(NetworkService networkService);

    void inject(BaseActivity baseActivity);
    void inject(SearchBarActivity searchBarActivity);
    void inject(SearchActivity searchActivity);
    void inject(BeerDetailsActivity beerDetailsActivity);

    void inject(MainFragment mainFragment);
    void inject(BeerListFragment beerListFragment);
    void inject(BeerSearchFragment beerSearchFragment);
    void inject(BeerDetailsFragment beerDetailsFragment);
    void inject(TopListFragment topListFragment);
    void inject(TopBeersFragment topBeersFragment);
    void inject(BeersInCountryFragment beersInCountryFragment);
    void inject(BeersInStyleFragment beersInStyleFragment);

    final class Initializer {
        public static ApplicationGraph init(Application application) {
            return DaggerApplicationGraph.builder()
                    .applicationModule(new ApplicationModule(application))
                    .build();
        }
    }
}
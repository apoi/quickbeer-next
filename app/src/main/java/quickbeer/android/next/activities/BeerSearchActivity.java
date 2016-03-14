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
package quickbeer.android.next.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import java.util.List;

import javax.inject.Inject;

import quickbeer.android.next.QuickBeer;
import quickbeer.android.next.R;
import quickbeer.android.next.data.DataLayer;
import quickbeer.android.next.fragments.BeerSearchFragment;
import rx.Observable;

public class BeerSearchActivity extends SearchBarActivity {
    private static final String TAG = BeerSearchActivity.class.getSimpleName();

    private SearchType searchType;
    private String query;

    @Inject
    DataLayer.GetBeerSearchQueries getBeerSearchQueries;

    public enum SearchType {
        BEER_SEARCH,
        BEST_IN_WORLD,
        BEST_IN_COUNTRY,
        BEST_IN_STYLE;

        public static SearchType value(int index) {
            return values()[index];
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        QuickBeer.getInstance().getGraph().inject(this);

        if (savedInstanceState != null) {
            searchType = SearchType.value(savedInstanceState.getInt("launchType", 0));
            query = savedInstanceState.getString("query");
        } else {
            searchType = SearchType.value(getIntent().getIntExtra("launchType", 0));
            query = getIntent().getStringExtra("query");
        }

        if (searchType == SearchType.BEER_SEARCH) {
            getQueryObservable()
                    .doOnNext(s -> query = s)
                    .subscribe(this::setTitle);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("query", query);
        outState.putInt("searchType", searchType.ordinal());
    }

    @Override
    protected Fragment getFragment() {
        return new BeerSearchFragment();
    }

    public SearchType getSearchType() {
        return searchType;
    }

    @Override
    public Observable<String> getQueryObservable() {
        Observable<String> queryObservable = super.getQueryObservable();

        // Free search always starts with a submitted query
        if (searchType == SearchType.BEER_SEARCH) {
            queryObservable.startWith(query);
        }

        return queryObservable;
    }

    @Override
    protected Observable<List<String>> getInitialQueriesObservable() {
        return getBeerSearchQueries.call();
    }

    @Override
    protected String getSearchHint() {
        return getString(R.string.base_activity_search_hint);
    }

    @Override
    protected boolean liveFiltering() {
        return false;
    }

    @Override
    protected int minimumSearchLength() {
        return 3;
    }

    @Override
    protected void showTooShortSearchError() {
        Toast.makeText(this, R.string.search_too_short, Toast.LENGTH_SHORT).show();
    }

}

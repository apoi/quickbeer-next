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
package quickbeer.android.fragments;

import android.os.Bundle;
import android.view.View;

import javax.inject.Inject;

import quickbeer.android.data.DataLayer;
import quickbeer.android.data.pojos.User;
import quickbeer.android.rx.RxUtils;
import timber.log.Timber;

public class TickedBeersFragment extends BeerListFragment {

    @Inject
    DataLayer.GetTickedBeers getTickedBeers;

    @Inject
    DataLayer.GetUsers getUsers;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getGraph().inject(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getUsers.call()
                .compose(RxUtils::pickValue)
                .filter(User::isLogged)
                .filter(user -> !user.userId().isEmpty())
                .subscribe(user -> setProgressingSource(getTickedBeers.call(user.userId())),
                        err -> Timber.e(err, "Error getting settings"));
    }
}

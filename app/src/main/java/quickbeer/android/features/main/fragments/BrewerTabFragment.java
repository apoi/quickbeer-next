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
package quickbeer.android.features.main.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import quickbeer.android.R;
import quickbeer.android.core.viewmodel.DataBinder;
import quickbeer.android.core.viewmodel.ViewModel;
import quickbeer.android.data.pojos.Header;
import quickbeer.android.views.BeerListView;

public class BrewerTabFragment extends BeerSearchFragment {

    @Override
    public int getLayout() {
        return R.layout.brewer_tab_fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((BeerListView) getView()).setHeader(new Header(getContext().getString(R.string.recent_brewers)));
    }

    @NonNull
    @Override
    protected DataBinder dataBinder() {
        return listDataBinder();
    }
}

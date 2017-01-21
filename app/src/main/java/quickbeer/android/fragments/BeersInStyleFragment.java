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
import android.support.annotation.Nullable;

import javax.inject.Inject;

import quickbeer.android.R;
import quickbeer.android.activities.BeersInStyleActivity;
import quickbeer.android.data.DataLayer;
import quickbeer.android.utils.Styles;

public class BeersInStyleFragment extends BeerListFragment {

    @Inject
    DataLayer.GetBeersInStyle getBeersInStyle;

    @Inject
    Styles styles;

    @Override
    public int getLayout() {
        return R.layout.beer_list_fragment;
    }

    @Override
    protected void inject() {
        getComponent().inject(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String styleId = ((BeersInStyleActivity) getActivity()).getStyleId();
        setProgressingSource(getBeersInStyle.call(styleId));
    }
}

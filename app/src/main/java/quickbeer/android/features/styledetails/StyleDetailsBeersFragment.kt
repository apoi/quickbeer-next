/*
 * This file is part of QuickBeer.
 * Copyright (C) 2017 Antti Poikela <antti.poikela@iki.fi>
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
package quickbeer.android.features.styledetails

import android.os.Bundle
import polanski.option.Option.ofObj
import quickbeer.android.Constants
import quickbeer.android.R
import quickbeer.android.features.list.fragments.BeerListFragment
import quickbeer.android.injections.IdModule
import timber.log.Timber
import javax.inject.Inject

class StyleDetailsBeersFragment : BeerListFragment() {

    @Inject
    internal lateinit var beersInStyleViewModel: BeersInStyleViewModel

    private var styleId: Int = 0

    companion object {
        fun newInstance(styleId: Int): StyleDetailsBeersFragment {
            val bundle = Bundle()
            bundle.putInt(Constants.ID_KEY, styleId)
            val fragment = StyleDetailsBeersFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = savedInstanceState ?: arguments

        ofObj(bundle)
            .map { it.getInt(Constants.ID_KEY) }
            .ifSome { styleId = it }
            .ifNone { Timber.w("Expected state for initializing!") }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(Constants.ID_KEY, styleId)
        super.onSaveInstanceState(outState)
    }

    override fun getLayout(): Int {
        return R.layout.beer_list_fragment_paged
    }

    override fun inject() {
        super.inject()

        getComponent().plusId(IdModule(styleId))
            .inject(this)
    }

    override fun viewModel(): BeersInStyleViewModel {
        return beersInStyleViewModel
    }

    override fun onQuery(query: String) {
        // No action, new search replaces old results.
    }
}

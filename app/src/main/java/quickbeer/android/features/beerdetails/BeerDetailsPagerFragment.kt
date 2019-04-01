/**
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
package quickbeer.android.features.beerdetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.details_fragment_pager.*
import polanski.option.Option.ofObj
import quickbeer.android.Constants
import quickbeer.android.R
import quickbeer.android.analytics.Analytics
import quickbeer.android.analytics.Events.Screen
import quickbeer.android.core.fragment.BaseFragment
import timber.log.Timber
import javax.inject.Inject

class BeerDetailsPagerFragment : BaseFragment() {

    @Inject
    internal lateinit var analytics: Analytics

    private var beerId: Int = 0

    companion object {
        fun newInstance(beerId: Int): Fragment {
            return BeerDetailsPagerFragment().apply {
                arguments = Bundle().apply {
                    putInt(Constants.ID_KEY, beerId)
                }
            }
        }
    }

    override fun inject() {
        getComponent().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ofObj(savedInstanceState ?: arguments)
            .map { state -> state.getInt(Constants.ID_KEY) }
            .ifSome { value -> beerId = value }
            .ifNone { Timber.w("Expected state for initializing!") }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.details_fragment_pager, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        view_pager.adapter = BeerDetailsPagerAdapter(childFragmentManager, beerId)
        tab_layout.setupWithViewPager(view_pager)

        view_pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                val screen = if (position == 0)
                    Screen.BEER_DETAILS
                else
                    Screen.BEER_REVIEWS

                analytics.createEvent(screen)
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(Constants.ID_KEY, beerId)
        super.onSaveInstanceState(outState)
    }
}

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
package quickbeer.android.features.list.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.disposables.CompositeDisposable
import quickbeer.android.Constants
import quickbeer.android.R
import quickbeer.android.core.fragment.BindingBaseFragment
import quickbeer.android.core.viewmodel.DataBinder
import quickbeer.android.core.viewmodel.SimpleDataBinder
import quickbeer.android.data.stores.CountryStore
import quickbeer.android.features.countrydetails.CountryDetailsActivity
import quickbeer.android.providers.NavigationProvider
import quickbeer.android.viewmodels.SearchViewViewModel
import quickbeer.android.viewmodels.SearchViewViewModel.Mode
import quickbeer.android.views.SimpleListView
import timber.log.Timber
import javax.inject.Inject

class CountryListFragment : BindingBaseFragment() {

    @Inject
    internal lateinit var navigationProvider: NavigationProvider

    @Inject
    internal lateinit var searchViewViewModel: SearchViewViewModel

    @Inject
    internal lateinit var countryStore: CountryStore

    private val dataBinder = object : SimpleDataBinder() {
        override fun bind(disposable: CompositeDisposable) {
            disposable.add(
                viewModel()
                    .getQueryStream()
                    .subscribe({ view.setFilter(it) }, { Timber.e(it) }))

            disposable.add(
                view.selectionStream()
                    .subscribe({ navigateToCountry(it) }, { Timber.e(it) }))
        }
    }

    private fun navigateToCountry(countryId: Int) {
        Timber.d("navigateToCountry($countryId)")

        val intent = Intent(activity, CountryDetailsActivity::class.java).apply {
            putExtra(Constants.ID_KEY, countryId)
            putExtra(Constants.PAGER_INDEX, 1)
        }
        startActivity(intent)
    }

    override fun inject() {
        getComponent().inject(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        view.setListSource(countryStore)
        searchViewViewModel.setMode(Mode.FILTER, getString(R.string.search_box_hint_filter_countries))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.simple_list_fragment, container, false)
    }

    override fun getView(): SimpleListView {
        return super.getView() as SimpleListView
    }

    override fun viewModel(): SearchViewViewModel {
        return searchViewViewModel
    }

    override fun dataBinder(): DataBinder {
        return dataBinder
    }
}

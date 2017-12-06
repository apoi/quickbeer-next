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
package quickbeer.android.data.actions.impl

import android.content.Context
import android.content.Intent
import io.reactivex.Observable
import io.reactivex.Single
import io.reark.reark.data.DataStreamNotification
import io.reark.reark.data.utils.DataLayerUtils
import quickbeer.android.data.actions.BrewerActions
import quickbeer.android.data.pojos.Brewer
import quickbeer.android.data.pojos.BrewerMetadata
import quickbeer.android.data.pojos.ItemList
import quickbeer.android.data.stores.BeerListStore
import quickbeer.android.data.stores.BrewerMetadataStore
import quickbeer.android.data.stores.BrewerStore
import quickbeer.android.data.stores.NetworkRequestStatusStore
import quickbeer.android.network.NetworkService
import quickbeer.android.network.RateBeerService
import quickbeer.android.network.fetchers.BeerSearchFetcher
import quickbeer.android.network.fetchers.BrewerFetcher
import quickbeer.android.rx.RxUtils
import timber.log.Timber
import javax.inject.Inject

class BrewerActionsImpl @Inject
constructor(context: Context,
            private val requestStatusStore: NetworkRequestStatusStore,
            private val beerListStore: BeerListStore,
            private val brewerStore: BrewerStore,
            private val brewerMetadataStore: BrewerMetadataStore)
    : ApplicationDataLayer(context), BrewerActions {

    //// GET BREWER DETAILS

    override operator fun get(brewerId: Int): Observable<DataStreamNotification<Brewer>> {
        return getBrewer(brewerId, { brewer -> !brewer.hasDetails() })
    }

    override fun fetch(brewerId: Int): Single<Boolean> {
        return getBrewer(brewerId, { true })
                .filter { it.isCompleted }
                .map { it.isCompletedWithSuccess }
                .singleOrError()
    }

    fun getBrewer(brewerId: Int, needsReload: (Brewer) -> Boolean): Observable<DataStreamNotification<Brewer>> {
        Timber.v("getBrewer(%s)", brewerId)

        // Trigger a fetch only if full details haven't been fetched
        val triggerFetchIfEmpty = brewerStore.getOnce(brewerId)
                .toObservable()
                .filter { it.match({ needsReload(it) }, { true }) }
                .doOnNext { Timber.v("Fetching brewer data") }
                .doOnNext { fetchBrewer(brewerId) }
                .flatMap { Observable.empty<DataStreamNotification<Brewer>>() }

        return getBrewerResultStream(brewerId)
                .mergeWith(triggerFetchIfEmpty)
                .distinctUntilChanged()
    }

    private fun getBrewerResultStream(brewerId: Int): Observable<DataStreamNotification<Brewer>> {
        Timber.v("getBrewerResultStream(%s)", brewerId)

        val uri = BrewerFetcher.getUniqueUri(brewerId)

        val requestStatusObservable = requestStatusStore
                .getOnceAndStream(NetworkRequestStatusStore.requestIdForUri(uri))
                .compose { RxUtils.pickValue(it) }

        val brewerObservable = brewerStore
                .getOnceAndStream(brewerId)
                .compose { RxUtils.pickValue(it) }

        return DataLayerUtils.createDataStreamNotificationObservable(requestStatusObservable, brewerObservable)
    }

    private fun fetchBrewer(brewerId: Int): Int {
        Timber.v("fetchBrewer(%s)", brewerId)

        val listenerId = createListenerId()
        val intent = Intent(context, NetworkService::class.java).apply {
            putExtra("serviceUriString", RateBeerService.BREWER.toString())
            putExtra("listenerId", listenerId)
            putExtra("id", brewerId)
        }

        context.startService(intent)
        return listenerId
    }

    //// BREWER'S BEERS

    override fun beers(brewerId: Int): Observable<DataStreamNotification<ItemList<String>>> {
        return getBeers(brewerId, { list -> list.items.isEmpty() })
    }

    override fun fetchBeers(brewerId: Int): Single<Boolean> {
        return getBeers(brewerId, { true })
                .filter{ it.isCompleted }
                .map { it.isCompletedWithSuccess }
                .singleOrError()
    }

    private fun getBeers(brewerId: Int, needsReload: (ItemList<String>) -> Boolean): Observable<DataStreamNotification<ItemList<String>>> {
        Timber.v("getBeers(%s)", brewerId)

        // Trigger a fetch only if there was no cached result
        val triggerFetchIfEmpty = beerListStore
                .getOnce(BeerSearchFetcher.getQueryId(RateBeerService.BREWER_BEERS, brewerId.toString()))
                .toObservable()
                .filter { it.match({ needsReload(it) }, { true }) }
                .doOnNext { Timber.v("Search not cached, fetching") }
                .doOnNext { fetchBrewerBeers(brewerId) }
                .flatMap { Observable.empty<DataStreamNotification<ItemList<String>>>() }

        return getBrewerBeersResultStream(brewerId)
                .mergeWith(triggerFetchIfEmpty)
    }

    private fun getBrewerBeersResultStream(brewerId: Int): Observable<DataStreamNotification<ItemList<String>>> {
        Timber.v("getBrewerBeersResultStream(%s)", brewerId)

        val queryId = BeerSearchFetcher.getQueryId(RateBeerService.BREWER_BEERS, brewerId.toString())
        val uri = BeerSearchFetcher.getUniqueUri(queryId)

        val requestStatusObservable = requestStatusStore
                .getOnceAndStream(NetworkRequestStatusStore.requestIdForUri(uri))
                .compose { RxUtils.pickValue(it) }

        val beerSearchObservable = beerListStore
                .getOnceAndStream(queryId)
                .compose { RxUtils.pickValue(it) }

        return DataLayerUtils.createDataStreamNotificationObservable(
                requestStatusObservable, beerSearchObservable)
    }

    private fun fetchBrewerBeers(brewerId: Int): Int {
        Timber.v("fetchBrewerBeers(%s)", brewerId)

        val listenerId = createListenerId()
        val intent = Intent(context, NetworkService::class.java).apply {
            putExtra("serviceUriString", RateBeerService.BREWER_BEERS.toString())
            putExtra("listenerId", listenerId)
            putExtra("brewerId", brewerId)
        }

        context.startService(intent)
        return listenerId
    }

    //// ACCESS BREWER

    override fun access(brewerId: Int) {
        Timber.v("access(%s)", brewerId)

        brewerMetadataStore.put(BrewerMetadata.newAccess(brewerId))
    }
}

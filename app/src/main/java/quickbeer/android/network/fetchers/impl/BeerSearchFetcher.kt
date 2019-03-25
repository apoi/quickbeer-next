/*
 * This file is part of QuickBeer.
 * Copyright (C) 2019 Antti Poikela <antti.poikela@iki.fi>
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
package quickbeer.android.network.fetchers.impl

import android.content.Intent
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import io.reark.reark.pojo.NetworkRequestStatus
import io.reark.reark.utils.Preconditions.checkNotNull
import io.reark.reark.utils.Preconditions.get
import org.threeten.bp.ZonedDateTime
import quickbeer.android.data.pojos.Beer
import quickbeer.android.data.pojos.ItemList
import quickbeer.android.data.stores.BeerListStore
import quickbeer.android.data.stores.BeerStore
import quickbeer.android.network.NetworkApi
import quickbeer.android.network.fetchers.CheckingFetcher
import quickbeer.android.network.utils.NetworkUtils
import timber.log.Timber

open class BeerSearchFetcher @JvmOverloads constructor(
    protected val networkApi: NetworkApi,
    protected val networkUtils: NetworkUtils,
    networkRequestStatus: Consumer<NetworkRequestStatus>,
    private val beerStore: BeerStore,
    private val beerListStore: BeerListStore,
    name: String = NAME
) : CheckingFetcher(networkRequestStatus, name) {

    override fun required() = listOf(SEARCH)

    override fun fetch(intent: Intent, listenerId: Int) {
        if (!validateParams(intent)) return

        val searchString = get(intent).getStringExtra(SEARCH)
        fetchBeerSearch(searchString, listenerId)
    }

    protected fun fetchBeerSearch(query: String, listenerId: Int) {
        Timber.d("fetchBeerSearch($query)")

        val queryId = getQueryId(serviceUri, get(query))
        val uri = getUniqueUri(queryId)
        val requestId = uri.hashCode()

        addListener(requestId, listenerId)

        if (isOngoingRequest(requestId)) {
            Timber.d("Found an ongoing request for search $queryId")
            return
        }

        createNetworkObservable(query)
            .subscribeOn(Schedulers.io())
            .flatMapObservable { Observable.fromIterable(it) }
            .flatMap { beer -> beerStore.put(beer).toObservable().map { beer } }
            .toList()
            .map { sort(it) }
            .map { it.map(Beer::id) }
            .map { beerIds -> ItemList.create(queryId, beerIds, ZonedDateTime.now()) }
            .flatMap { beerListStore.put(it) }
            .doOnSubscribe { startRequest(requestId, uri) }
            .doOnSuccess { updated -> completeRequest(requestId, uri, updated) }
            .doOnError(doOnError(requestId, uri))
            .subscribe({}, { Timber.w(it, "Error fetching beer search for $uri") })
            .also { addRequest(requestId, it) }
    }

    protected open fun sort(list: List<Beer>): List<Beer> {
        return list.sortedWith(compareBy(Beer::name, Beer::id))
    }

    protected open fun createNetworkObservable(searchString: String): Single<List<Beer>> {
        return networkApi.search(networkUtils.createRequestParams("bn", get(searchString)))
    }

    companion object {
        const val NAME = "__search"
        const val SEARCH = "searchString"

        // Beer search store needs separate query identifiers for normal searches and fixed searches
        // (top50, top in country, top in style). Fixed searches come attached with a service uri
        // identifier to make sure they stand apart from the normal searches.
        @JvmOverloads
        fun getQueryId(serviceUri: String, query: String = ""): String {
            checkNotNull(serviceUri)
            checkNotNull(query)

            return if (serviceUri === NAME) {
                query
            } else if (!query.isEmpty()) {
                String.format("%s_%s", serviceUri, query)
            } else {
                serviceUri
            }
        }

        fun getUniqueUri(id: String): String {
            return ItemList::class.java.toString() + "/beerSearch/" + id
        }
    }
}

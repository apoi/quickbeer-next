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
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reark.reark.pojo.NetworkRequestStatus
import quickbeer.android.data.pojos.BeerMetadata
import quickbeer.android.data.stores.BeerMetadataStore
import quickbeer.android.data.stores.BeerStore
import quickbeer.android.network.NetworkApi
import quickbeer.android.network.fetchers.CommonFetcher
import quickbeer.android.network.utils.NetworkUtils
import timber.log.Timber

class BeerFetcher(
    private val networkApi: NetworkApi,
    private val networkUtils: NetworkUtils,
    networkRequestStatus: Consumer<NetworkRequestStatus>,
    private val beerStore: BeerStore,
    private val metadataStore: BeerMetadataStore
) : CommonFetcher(networkRequestStatus, NAME) {

    override fun requiredParams() = listOf(BEER_ID)

    override fun fetch(intent: Intent, listenerId: Int) {
        if (!validateParams(intent)) return

        val beerId = intent.getIntExtra(BEER_ID, 0)
        val uri = getUniqueUri(beerId)

        addListener(beerId, listenerId)

        if (isOngoingRequest(beerId)) {
            Timber.d("Found an ongoing request for beer %s", beerId)
            return
        }

        Timber.d("fetchBeer(requestId=%s, hashCode=%s, uri=%s", beerId, uri.hashCode(), uri)
        addRequest(beerId, createRequest(beerId, uri))
    }

    private fun createRequest(beerId: Int, uri: String): Disposable {
        val requestParams = networkUtils.createRequestParams("bd", beerId.toString())

        return networkApi.getBeer(requestParams)
            .flatMap { beerStore.put(it) }
            .compose(addFetcherTracking(beerId, uri))
            .subscribe({ metadataStore.put(BeerMetadata.newUpdate(beerId)) },
                { Timber.w(it, "Error fetching beer %s", beerId) })
    }

    companion object : FetcherCompanion {
        override val NAME = "__beer"
        const val BEER_ID = "beerId"
    }
}

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
import io.reactivex.Single
import io.reactivex.functions.Consumer
import io.reark.reark.pojo.NetworkRequestStatus
import quickbeer.android.data.pojos.Beer
import quickbeer.android.data.stores.BeerListStore
import quickbeer.android.data.stores.BeerStore
import quickbeer.android.data.stores.UserStore
import quickbeer.android.network.NetworkApi
import quickbeer.android.network.fetchers.actions.LoginAndRetry
import quickbeer.android.network.utils.NetworkUtils
import kotlin.collections.set

class TicksFetcher(
    networkApi: NetworkApi,
    networkUtils: NetworkUtils,
    networkRequestStatus: Consumer<NetworkRequestStatus>,
    beerStore: BeerStore,
    beerListStore: BeerListStore,
    private val userStore: UserStore
) : BeerSearchFetcher(networkApi, networkUtils, networkRequestStatus, beerStore, beerListStore, NAME) {

    override fun required(): List<String> {
        return listOf(USER_ID)
    }

    override fun fetch(intent: Intent, listenerId: Int) {
        if (!validateParams(intent)) return

        val userId = intent.getStringExtra(USER_ID)
        fetchBeerSearch(userId, listenerId)
    }

    override fun sort(list: List<Beer>): List<Beer> {
        return BeerSearchFetcher.sortByTickDate(list)
    }

    override fun createNetworkObservable(userId: String): Single<List<Beer>> {
        val params = networkUtils.createRequestParams("m", "1")
        params["u"] = userId

        return networkApi.getTicks(params)
            .retryWhen(LoginAndRetry(networkApi, userStore))
    }

    companion object {
        const val NAME = "__ticks"
        const val USER_ID = "userId"
    }
}

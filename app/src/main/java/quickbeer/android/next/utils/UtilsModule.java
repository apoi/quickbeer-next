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
package quickbeer.android.next.utils;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import quickbeer.android.next.injections.ForApplication;
import quickbeer.android.next.network.utils.ApiKey;
import quickbeer.android.next.network.utils.NetworkUtils;

@Module
public final class UtilsModule {
    @Provides
    @Singleton
    public NetworkUtils providesNetworkUtils(@ForApplication Context context) {
        return new NetworkUtils(new ApiKey().getApiKey(context));
    }
}

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
package quickbeer.android.next.data.store;

import android.content.ContentResolver;

import com.google.gson.Gson;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public final class StoreModule {
    @Provides
    @Singleton
    public UserSettingsStore provideUserSettingsStore(ContentResolver contentResolver) {
        return new UserSettingsStore(contentResolver);
    }

    @Provides
    @Singleton
    public NetworkRequestStatusStore provideNetworkRequestStatusStore(ContentResolver contentResolver, Gson gson) {
        return new NetworkRequestStatusStore(contentResolver, gson);
    }

    @Provides
    @Singleton
    public BeerStore provideBeerStore(ContentResolver contentResolver, Gson gson) {
        return new BeerStore(contentResolver, gson);
    }

    @Provides
    @Singleton
    public BeerSearchStore provideBeerSearchStore(ContentResolver contentResolver, Gson gson) {
        return new BeerSearchStore(contentResolver, gson);
    }

    @Provides
    @Singleton
    public ReviewStore provideReviewStore(ContentResolver contentResolver, Gson gson) {
        return new ReviewStore(contentResolver, gson);
    }

    @Provides
    @Singleton
    public ReviewListStore provideReviewListStore(ContentResolver contentResolver, Gson gson) {
        return new ReviewListStore(contentResolver, gson);
    }
}

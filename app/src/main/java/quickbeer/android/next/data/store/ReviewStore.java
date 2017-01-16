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
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import polanski.option.Option;
import quickbeer.android.next.data.store.cores.ReviewStoreCore;
import quickbeer.android.next.pojo.Review;

public class ReviewStore  extends StoreBase<Integer, Review, Option<Review>> {

    public ReviewStore(@NonNull final ContentResolver contentResolver, @NonNull final Gson gson) {
        super(new ReviewStoreCore(contentResolver, gson),
              Review::id,
              Option::ofObj,
              Option::none);
    }
}

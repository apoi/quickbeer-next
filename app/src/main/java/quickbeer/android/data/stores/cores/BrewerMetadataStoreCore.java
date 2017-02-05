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
package quickbeer.android.data.stores.cores;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import io.reark.reark.data.stores.StoreItem;
import quickbeer.android.data.columns.BrewerMetadataColumns;
import quickbeer.android.data.pojos.BrewerMetadata;
import quickbeer.android.data.providers.RateBeerProvider;
import quickbeer.android.utils.DateUtils;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static io.reark.reark.utils.Preconditions.get;

public class BrewerMetadataStoreCore extends StoreCoreBase<Integer, BrewerMetadata> {

    public BrewerMetadataStoreCore(@NonNull final ContentResolver contentResolver, @NonNull final Gson gson) {
        super(contentResolver, gson);
    }

    @NonNull
    public Observable<List<Integer>> getAccessedIds(@NonNull final String idColumn, @NonNull final String accessColumn) {
        return Observable
                .fromCallable(() -> {
                    String[] projection = { idColumn };
                    String selection = String.format("%s > 0", accessColumn); // Has access date
                    String orderBy = String.format("%s DESC", accessColumn); // Sort by date

                    List<Integer> idList = new ArrayList<>(10);

                    final Cursor cursor = getContentResolver().query(getContentUri(), projection, selection, null, orderBy);

                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            do {
                                idList.add(cursor.getInt(cursor.getColumnIndex(idColumn)));
                            } while (cursor.moveToNext());
                        }
                        cursor.close();
                    }

                    return idList;
                })
                .subscribeOn(Schedulers.io());
    }

    @NonNull
    public Observable<Integer> getNewlyAccessedItems(@NonNull final DateTime date) {
        return getStream()
                .map(StoreItem::item)
                .filter(item -> item.accessed().isAfter(0))
                .distinctUntilChanged(new Func1<BrewerMetadata, Object>() {
                    // Access date as key object indicating distinction
                    private DateTime latestAccess = date;

                    @Override
                    public DateTime call(BrewerMetadata item) {
                        if (item.accessed().isAfter(latestAccess)) {
                            latestAccess = item.accessed();
                        }
                        return latestAccess;
                    }
                })
                .map(BrewerMetadata::brewerId);
    }

    @NonNull
    @Override
    protected Uri getUriForId(@NonNull final Integer id) {
        return RateBeerProvider.BrewerMetadata.withId(get(id));
    }

    @NonNull
    @Override
    protected Integer getIdForUri(@NonNull final Uri uri) {
        return RateBeerProvider.BrewerMetadata.fromUri(get(uri));
    }

    @NonNull
    @Override
    protected Uri getContentUri() {
        return RateBeerProvider.BrewerMetadata.BREWER_METADATA;
    }

    @NonNull
    @Override
    protected String[] getProjection() {
        return new String[] {
                BrewerMetadataColumns.ID,
                BrewerMetadataColumns.UPDATED,
                BrewerMetadataColumns.ACCESSED
        };
    }

    @NonNull
    @Override
    protected BrewerMetadata read(@NonNull final Cursor cursor) {
        final int brewerId = cursor.getInt(cursor.getColumnIndex(BrewerMetadataColumns.ID));
        final DateTime updated = DateUtils.fromDbValue(cursor.getInt(cursor.getColumnIndex(BrewerMetadataColumns.UPDATED)));
        final DateTime accessed = DateUtils.fromDbValue(cursor.getInt(cursor.getColumnIndex(BrewerMetadataColumns.ACCESSED)));

        return BrewerMetadata.builder()
                .brewerId(brewerId)
                .updated(updated)
                .accessed(accessed)
                .build();
    }

    @NonNull
    @Override
    protected ContentValues getContentValuesForItem(@NonNull final BrewerMetadata item) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(BrewerMetadataColumns.ID, item.brewerId());
        contentValues.put(BrewerMetadataColumns.UPDATED, DateUtils.toDbValue(item.updated()));
        contentValues.put(BrewerMetadataColumns.ACCESSED, DateUtils.toDbValue(item.accessed()));

        return contentValues;
    }

    @NonNull
    @Override
    protected BrewerMetadata mergeValues(@NonNull final BrewerMetadata v1, @NonNull final BrewerMetadata v2) {
        return BrewerMetadata.merge(v1, v2);
    }
}
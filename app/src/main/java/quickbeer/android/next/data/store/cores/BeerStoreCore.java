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
package quickbeer.android.next.data.store.cores;

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
import io.reark.reark.utils.Log;
import quickbeer.android.next.data.schematicprovider.BeerColumns;
import quickbeer.android.next.data.schematicprovider.RateBeerProvider;
import quickbeer.android.next.pojo.Beer;
import quickbeer.android.next.utils.DateUtils;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static io.reark.reark.utils.Preconditions.get;

public class BeerStoreCore extends StoreCoreBase<Integer, Beer> {
    private static final String TAG = BeerStoreCore.class.getSimpleName();

    public BeerStoreCore(@NonNull final ContentResolver contentResolver, @NonNull final Gson gson) {
        super(contentResolver, gson);
    }

    @NonNull
    public Observable<List<Integer>> queryTicks() {
        return Observable
                .fromCallable(() -> {
                    String[] projection = { BeerColumns.ID };
                    String selection = String.format("%s > 0", BeerColumns.TICK_VALUE); // Has tick value
                    String orderBy = String.format("%s DESC", BeerColumns.TICK_DATE); // Sort by latest ticked

                    List<Integer> idList = new ArrayList<>(10);

                    final Cursor cursor = getContentResolver().query(getContentUri(), projection, selection, null, orderBy);

                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            do {
                                idList.add(cursor.getInt(cursor.getColumnIndex(BeerColumns.ID)));
                            } while (cursor.moveToNext());
                        }
                        cursor.close();
                    }

                    return idList;
                })
                .doOnNext(idList -> Log.d(TAG, "Ticked beers: " + idList.size()))
                .subscribeOn(Schedulers.io());
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
    public Observable<Beer> getNewlyAccessedItems(@NonNull final DateTime date) {
        return getStream()
                .map(StoreItem::item)
                .filter(item -> DateUtils.isValidDate(item.metadata().accessed()))
                .distinctUntilChanged(new Func1<Beer, DateTime>() {
                    // Access date as key object indicating distinction
                    private DateTime latestAccess = date;

                    @Override
                    public DateTime call(Beer item) {
                        if (item.metadata().accessed().isAfter(latestAccess)) {
                            latestAccess = item.metadata().accessed();
                        }
                        return latestAccess;
                    }
                });
    }

    @NonNull
    @Override
    protected Uri getUriForId(@NonNull final Integer id) {
        return RateBeerProvider.Beers.withId(get(id));
    }

    @NonNull
    @Override
    protected Integer getIdForUri(@NonNull final Uri uri) {
        return RateBeerProvider.Beers.fromUri(get(uri));
    }

    @NonNull
    @Override
    protected Uri getContentUri() {
        return RateBeerProvider.Beers.BEERS;
    }

    @NonNull
    @Override
    protected String[] getProjection() {
        return new String[] {
                BeerColumns.ID,
                BeerColumns.JSON,
                BeerColumns.NAME,
                BeerColumns.TICK_VALUE,
                BeerColumns.TICK_DATE,
                BeerColumns.REVIEW,
                BeerColumns.MODIFIED,
                BeerColumns.UPDATED,
                BeerColumns.ACCESSED
        };
    }

    @NonNull
    @Override
    protected Beer read(@NonNull final Cursor cursor) {
        final String json = cursor.getString(cursor.getColumnIndex(BeerColumns.JSON));
        final int tickValue = cursor.getInt(cursor.getColumnIndex(BeerColumns.TICK_VALUE));
        final DateTime tickDate = DateUtils.fromDbValue(cursor.getInt(cursor.getColumnIndex(BeerColumns.TICK_DATE)));
        final int reviewId = cursor.getInt(cursor.getColumnIndex(BeerColumns.REVIEW));
        final boolean isModified = cursor.getInt(cursor.getColumnIndex(BeerColumns.MODIFIED)) > 0;
        final DateTime updated = DateUtils.fromDbValue(cursor.getInt(cursor.getColumnIndex(BeerColumns.UPDATED)));
        final DateTime accessed = DateUtils.fromDbValue(cursor.getInt(cursor.getColumnIndex(BeerColumns.ACCESSED)));

        return Beer.builder(getGson().fromJson(json, Beer.class))
                .tickValue(tickValue)
                .tickDate(tickDate)
                //.reviewId(reviewId)
                //.isModified(isModified)
                //.updateDate(updated)
                //.accessDate(accessed)
                .build();
    }

    @NonNull
    @Override
    protected ContentValues getContentValuesForItem(@NonNull final Beer item) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(BeerColumns.ID, item.id());
        contentValues.put(BeerColumns.JSON, getGson().toJson(item));
        contentValues.put(BeerColumns.NAME, item.name());
        contentValues.put(BeerColumns.TICK_VALUE, item.tickValue());
        contentValues.put(BeerColumns.TICK_DATE, DateUtils.toDbValue(item.tickDate()));
        //contentValues.put(BeerColumns.REVIEW, item.reviewId());
        //contentValues.put(BeerColumns.MODIFIED, item.isModified() ? 1 : 0);
        contentValues.put(BeerColumns.UPDATED, DateUtils.toDbValue(item.metadata().updated()));
        contentValues.put(BeerColumns.ACCESSED, DateUtils.toDbValue(item.metadata().accessed()));

        return contentValues;
    }

    @NonNull
    @Override
    protected Beer mergeValues(@NonNull final Beer v1, @NonNull final Beer v2) {
        return Beer.merge(v1, v2);
    }
}

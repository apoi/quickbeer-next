package quickbeer.android.next.data.store;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import io.reark.reark.utils.Preconditions;
import quickbeer.android.next.QuickBeer;
import quickbeer.android.next.data.schematicprovider.RateBeerProvider;
import quickbeer.android.next.data.schematicprovider.ReviewColumns;
import quickbeer.android.next.pojo.Review;

public class ReviewStore extends StoreBase<Review, Integer> {
    private static final String TAG = ReviewStore.class.getSimpleName();

    public ReviewStore(@NonNull ContentResolver contentResolver) {
        super(contentResolver);

        QuickBeer.getInstance().getGraph().inject(this);
    }

    @NonNull
    @Override
    protected Integer getIdFor(@NonNull Review item) {
        Preconditions.checkNotNull(item, "Review cannot be null.");

        return item.getId();
    }

    @NonNull
    @Override
    public Uri getContentUri() {
        return RateBeerProvider.Reviews.REVIEWS;
    }

    @NonNull
    @Override
    protected String[] getProjection() {
        return new String[] { ReviewColumns.ID, ReviewColumns.JSON };
    }

    @NonNull
    @Override
    protected ContentValues getContentValuesForItem(Review item) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReviewColumns.ID, item.getId());
        contentValues.put(ReviewColumns.JSON, getGson().toJson(item));
        return contentValues;
    }

    @NonNull
    @Override
    protected Review read(Cursor cursor) {
        final String json = cursor.getString(cursor.getColumnIndex(ReviewColumns.JSON));
        return getGson().fromJson(json, Review.class);
    }

    @NonNull
    @Override
    protected ContentValues readRaw(Cursor cursor) {
        final String json = cursor.getString(cursor.getColumnIndex(ReviewColumns.JSON));
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReviewColumns.JSON, json);
        return contentValues;
    }

    @NonNull
    @Override
    public Uri getUriForId(@NonNull Integer id) {
        Preconditions.checkNotNull(id, "Id cannot be null.");

        return RateBeerProvider.Reviews.withId(id);
    }

    @Override
    protected boolean contentValuesEqual(ContentValues v1, ContentValues v2) {
        return v1.getAsString(ReviewColumns.JSON).equals(v2.getAsString(ReviewColumns.JSON));
    }

    @NonNull
    @Override
    protected ContentValues mergeValues(ContentValues v1, ContentValues v2) {
        Review b1 = getGson().fromJson(v1.getAsString(ReviewColumns.JSON), Review.class);
        Review b2 = getGson().fromJson(v2.getAsString(ReviewColumns.JSON), Review.class);
        return getContentValuesForItem(b1.overwrite(b2));
    }
}

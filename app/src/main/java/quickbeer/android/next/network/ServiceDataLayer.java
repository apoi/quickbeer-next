package quickbeer.android.next.network;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Arrays;
import java.util.Collection;

import quickbeer.android.next.data.DataLayerBase;
import quickbeer.android.next.data.store.BeerSearchStore;
import quickbeer.android.next.data.store.BeerStore;
import quickbeer.android.next.data.store.NetworkRequestStatusStore;
import quickbeer.android.next.network.fetchers.base.Fetcher;
import quickbeer.android.next.utils.Preconditions;

/**
 * Created by ttuo on 16/04/15.
 */
public class ServiceDataLayer extends DataLayerBase {
    private static final String TAG = ServiceDataLayer.class.getSimpleName();

    @NonNull
    final private Collection<Fetcher> fetchers;

    public ServiceDataLayer(@NonNull NetworkRequestStatusStore networkRequestStatusStore,
                            @NonNull BeerStore beerStore,
                            @NonNull BeerSearchStore beerSearchStore) {
        super(networkRequestStatusStore, beerStore, beerSearchStore);

        fetchers = Arrays.asList();
    }

    public void processIntent(@NonNull Intent intent) {
        Preconditions.checkNotNull(intent, "Intent cannot be null.");

        final String fetcherIdentifier = intent.getStringExtra("fetcherIdentifier");
        final String contentUriString = intent.getStringExtra("contentUriString");

        if (fetcherIdentifier != null) {
            Fetcher matchingFetcher = findFetcherByIdentifier(fetcherIdentifier);
            if (matchingFetcher != null) {
                Log.v(TAG, "Fetcher found for " + fetcherIdentifier);
                matchingFetcher.fetch(intent);
            } else {
                Log.e(TAG, "Unknown identifier " + fetcherIdentifier);
            }
        } else if (contentUriString != null) {
            final Uri contentUri = Uri.parse(contentUriString);
            Fetcher matchingFetcher = findFetcherByContentUri(contentUri);
            if (matchingFetcher != null) {
                Log.v(TAG, "Fetcher found for " + contentUri);
                matchingFetcher.fetch(intent);
            } else {
                Log.e(TAG, "Unknown Uri " + contentUri);
            }
        } else {
            Log.e(TAG, "No fetcher found");
        }
    }

    @Nullable
    private Fetcher findFetcherByIdentifier(@NonNull String identifier) {
        Preconditions.checkNotNull(identifier, "Identifier cannot nbe null.");

        for (Fetcher fetcher : fetchers) {
            if (fetcher.getIdentifier().equals(identifier)) {
                return fetcher;
            }
        }
        return null;
    }

    @Nullable
    private Fetcher findFetcherByContentUri(@NonNull Uri contentUri) {
        Preconditions.checkNotNull(contentUri, "Content URL cannot be null.");

        for (Fetcher fetcher : fetchers) {
            if (fetcher.getContentUri().equals(contentUri)) {
                return fetcher;
            }
        }
        return null;
    }
}

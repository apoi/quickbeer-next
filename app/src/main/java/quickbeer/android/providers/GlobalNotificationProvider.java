/**
 * This file is part of QuickBeer.
 * Copyright (C) 2017 Antti Poikela <antti.poikela@iki.fi>
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
package quickbeer.android.providers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import io.reark.reark.data.DataStreamNotification;
import quickbeer.android.utils.DataModelUtils;
import rx.Observable;
import rx.Subscription;
import timber.log.Timber;

import static io.reark.reark.utils.Preconditions.get;

public class GlobalNotificationProvider {

    private int counter = 0;

    @NonNull
    private final Context context;

    @NonNull
    private final Map<Integer, Subscription> subscriptionList = new HashMap<>(10);

    public GlobalNotificationProvider(@NonNull Context context) {
        this.context = get(context);
    }

    public void addNetworkSuccessListener(@NonNull Observable<DataStreamNotification<Void>> observable,
                                          @NonNull String successToast,
                                          @NonNull String failureToast) {
        int index = ++counter;

        Subscription subscription = observable
                .takeUntil(DataModelUtils::isRequestFinishedNotification)
                .doOnCompleted(() -> subscriptionList.remove(index))
                .subscribe(notification -> {
                    switch (notification.getType()) {
                        case FETCHING_COMPLETED_WITH_VALUE:
                        case FETCHING_COMPLETED_WITHOUT_VALUE:
                            showToast(successToast);
                            break;
                        case FETCHING_COMPLETED_WITH_ERROR:
                            showToast(failureToast);
                            break;
                        case FETCHING_START:
                        case ON_NEXT:
                            break;
                    }
                }, error -> Timber.w(error, "Global listener error"));

        subscriptionList.put(index, subscription);
    }

    private void showToast(@NonNull String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

}

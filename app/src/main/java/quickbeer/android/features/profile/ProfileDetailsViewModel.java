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
package quickbeer.android.features.profile;

import android.support.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;

import io.reark.reark.data.DataStreamNotification;
import quickbeer.android.core.viewmodel.SimpleViewModel;
import quickbeer.android.data.DataLayer;
import quickbeer.android.data.pojos.ItemList;
import quickbeer.android.data.pojos.User;
import quickbeer.android.providers.ResourceProvider;
import quickbeer.android.rx.RxUtils;
import rx.Observable;

import static io.reark.reark.utils.Preconditions.get;

public class ProfileDetailsViewModel extends SimpleViewModel {

    @NonNull
    private final DataLayer.GetUser getUser;

    @NonNull
    private final DataLayer.GetTickedBeers getTickedBeers;

    @NonNull
    private final ResourceProvider resourceProvider;

    @Inject
    public ProfileDetailsViewModel(@NonNull DataLayer.GetUser getUser,
                                   @NonNull DataLayer.GetTickedBeers getTickedBeers,
                                   @NonNull ResourceProvider resourceProvider) {
        this.getUser = get(getUser);
        this.getTickedBeers = get(getTickedBeers);
        this.resourceProvider = get(resourceProvider);
    }

    @NonNull
    public Observable<User> getUser() {
        return getUser.call()
                .compose(RxUtils::pickValue);
    }

    @NonNull
    public Observable<List<Integer>> getTicks(@NonNull Integer userId) {
        return getTickedBeers.call(String.valueOf(userId))
                .filter(DataStreamNotification::isOnNext)
                .map(DataStreamNotification::getValue)
                .map(ItemList::getItems);
    }

}
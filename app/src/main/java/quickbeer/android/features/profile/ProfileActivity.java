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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import quickbeer.android.R;
import quickbeer.android.core.activity.BindingDrawerActivity;
import quickbeer.android.core.viewmodel.DataBinder;
import quickbeer.android.core.viewmodel.SimpleDataBinder;
import quickbeer.android.providers.NavigationProvider;
import quickbeer.android.providers.NavigationProvider.Page;
import quickbeer.android.rx.RxUtils;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static io.reark.reark.utils.Preconditions.checkNotNull;
import static io.reark.reark.utils.Preconditions.get;

public class ProfileActivity extends BindingDrawerActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Nullable
    @Inject
    NavigationProvider navigationProvider;

    @Nullable
    @Inject
    ProfileLoginViewModel viewModel;

    @NonNull
    private final DataBinder dataBinder = new SimpleDataBinder() {
        @Override
        public void bind(@NonNull CompositeSubscription subscription) {
            // Change fragment on login success
            subscription.add(viewModel()
                    .loginCompletedStream()
                    .filter(RxUtils::isTrue)
                    .map(__ -> Page.PROFILE_VIEW)
                    .subscribe(get(navigationProvider)::replacePage, Timber::e));

            // Trigger refresh of ticked beers after user has logged in
            subscription.add(viewModel()
                    .loginCompletedStream()
                    .filter(RxUtils::isTrue)
                    .switchMap(__ -> viewModel().getUser())
                    .compose(RxUtils::pickValue)
                    .subscribe(user -> viewModel().fetchTicks(user.id()), Timber::e));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkNotNull(navigationProvider);

        setContentView(R.layout.basic_activity);

        ButterKnife.bind(this);

        setupDrawerLayout(false);

        setBackNavigationEnabled(true);

        toolbar.setTitle(getString(R.string.profile));

        navigationProvider.addPage(Page.PROFILE_LOGIN);
    }

    @Override
    protected void onResume() {
        super.onResume();

        viewModel().autoLogin();
    }

    @Override
    protected void inject() {
        getComponent().inject(this);
    }

    @NonNull
    @Override
    protected ProfileLoginViewModel viewModel() {
        return get(viewModel);
    }

    @NonNull
    @Override
    protected DataBinder dataBinder() {
        return dataBinder;
    }

    @Override
    protected void navigateTo(@NonNull MenuItem menuItem) {
        get(navigationProvider).navigateWithNewActivity(menuItem);
    }
}

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
package quickbeer.android.providers;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import javax.inject.Inject;
import javax.inject.Named;

import quickbeer.android.R;
import quickbeer.android.features.about.AboutActivity;
import quickbeer.android.features.about.AboutDetailsFragment;
import quickbeer.android.features.home.HomeActivity;
import quickbeer.android.features.home.HomeFragment;
import quickbeer.android.features.list.ListActivity;
import quickbeer.android.features.list.fragments.BarcodeSearchFragment;
import quickbeer.android.features.list.fragments.BeerSearchFragment;
import quickbeer.android.features.list.fragments.BeersInCountryFragment;
import quickbeer.android.features.list.fragments.BeersInStyleFragment;
import quickbeer.android.features.list.fragments.CountryListFragment;
import quickbeer.android.features.list.fragments.ReviewedBeersFragment;
import quickbeer.android.features.list.fragments.StyleListFragment;
import quickbeer.android.features.list.fragments.TickedBeersFragment;
import quickbeer.android.features.list.fragments.TopBeersFragment;
import quickbeer.android.features.profile.ProfileDetailsFragment;
import quickbeer.android.features.profile.ProfileLoginFragment;
import timber.log.Timber;

import static io.reark.reark.utils.Preconditions.checkNotNull;
import static io.reark.reark.utils.Preconditions.get;

public final class NavigationProvider {

    public static final String NAVIGATION_KEY = "menuNavigationId";

    public static final String PAGE_KEY = "pageNavigationId";

    public enum Page {
        HOME,
        TICKS,
        REVIEWS,
        BEER_SEARCH,
        BARCODE_SEARCH,
        TOP_BEERS,
        COUNTRY_LIST,
        COUNTRY,
        STYLE_LIST,
        STYLE,
        PROFILE_LOGIN,
        PROFILE_VIEW,
        ABOUT;

        static Page from(int index) {
            return Page.values()[index];
        }
    }

    @NonNull
    private final AppCompatActivity activity;

    @NonNull
    private final Integer container;

    @Inject
    NavigationProvider(@NonNull AppCompatActivity activity,
                       @NonNull @Named("fragmentContainer") Integer container) {
        this.activity = get(activity);
        this.container = container;
    }

    public void addPage(int menuNavigationId) {
        addPage(toPage(menuNavigationId), null);
    }

    public void addPage(int menuNavigationId, @Nullable Bundle arguments) {
        addPage(toPage(menuNavigationId), arguments);
    }

    public void addPage(@NonNull Page page) {
        transaction(page, null, true);
    }

    public void addPage(@NonNull Page page, @Nullable Bundle arguments) {
        transaction(page, arguments, true);
    }

    public void replacePage(@NonNull Page page) {
        transaction(page, null, false);
    }

    public void clearToPage(int menuNavigationId) {
        activity.getSupportFragmentManager()
                .popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        addPage(menuNavigationId);
    }

    public static boolean intentHasNavigationTarget(@NonNull Intent intent) {
        return intent.hasExtra(NAVIGATION_KEY) || intent.hasExtra(PAGE_KEY);
    }

    public void navigateWithIntent(@NonNull Intent intent) {
        Page page = intent.hasExtra(NAVIGATION_KEY)
                ? toPage(intent.getIntExtra(NAVIGATION_KEY, 0))
                : Page.from(intent.getIntExtra(PAGE_KEY, Page.HOME.ordinal()));

        Bundle arguments = getArguments(intent);

        addPage(page, arguments);
    }

    @NonNull
    private static Bundle getArguments(@NonNull Intent intent) {
        Bundle bundle = new Bundle();
        setBundleValue(intent, bundle, "country");
        setBundleValue(intent, bundle, "style");

        return bundle;
    }

    private static void setBundleValue(@NonNull Intent intent, @NonNull Bundle bundle, @NonNull String key) {
        if (intent.hasExtra(key)) {
            bundle.putString(key, intent.getStringExtra(key));
        }
    }

    private void transaction(@NonNull Page page, @Nullable Bundle arguments, boolean addToBackStack) {
        checkNotNull(page);

        final Fragment fragment = toFragment(page);
        fragment.setArguments(arguments);

        FragmentTransaction transition = activity
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(container, fragment, page.toString())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

        if (addToBackStack && hasFragment()) {
            transition = transition.addToBackStack(page.toString());
        }

        transition.commit();
    }

    private boolean hasFragment() {
        return activity.getSupportFragmentManager().findFragmentById(container) != null;
    }

    public boolean canNavigateBack() {
        return activity.getSupportFragmentManager().getBackStackEntryCount() > 0;
    }

    public void navigateBack() {
        activity.getSupportFragmentManager().popBackStack();
    }

    public void navigateAllBack() {
        activity.getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    public void triggerSearch(@NonNull String query) {
        Timber.d("query(" + query + ")");

        Bundle bundle = new Bundle();
        bundle.putString("query", query);

        addPage(Page.BEER_SEARCH, bundle);
    }

    public void navigateWithNewActivity(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.nav_about) {
            launchActivity(AboutActivity.class);
            return;
        }

        Intent intent;

        if (menuItem.getItemId() == R.id.nav_home) {
            intent = new Intent(activity, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else {
            intent = new Intent(activity, ListActivity.class);
        }

        intent.putExtra("menuNavigationId", menuItem.getItemId());
        activity.startActivity(intent);
    }

    public void navigateWithCurrentActivity(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.nav_home) {
            Intent intent = new Intent(activity, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
        } else {
            clearToPage(menuItem.getItemId());
        }
    }

    public void launchActivity(Class<?> activityClass) {
        Intent intent = new Intent(activity, activityClass);
        activity.startActivity(intent);
    }

    @NonNull
    private static Page toPage(int menuNavigationId) {
        switch (menuNavigationId) {
            case R.id.nav_home:
                return Page.HOME;
            case R.id.nav_ticks:
                return Page.TICKS;
            case R.id.nav_reviews:
                return Page.REVIEWS;
            case R.id.nav_best:
                return Page.TOP_BEERS;
            case R.id.nav_countries:
                return Page.COUNTRY_LIST;
            case R.id.nav_styles:
                return Page.STYLE_LIST;
            case R.id.nav_about:
                return Page.ABOUT;
            default:
                return Page.HOME;
        }
    }

    private static Fragment toFragment(@NonNull Page page) {
        switch (page) {
            case HOME:
                return new HomeFragment();
            case TICKS:
                return new TickedBeersFragment();
            case REVIEWS:
                return new ReviewedBeersFragment();
            case BEER_SEARCH:
                return new BeerSearchFragment();
            case BARCODE_SEARCH:
                return new BarcodeSearchFragment();
            case TOP_BEERS:
                return new TopBeersFragment();
            case COUNTRY_LIST:
                return new CountryListFragment();
            case COUNTRY:
                return new BeersInCountryFragment();
            case STYLE_LIST:
                return new StyleListFragment();
            case STYLE:
                return new BeersInStyleFragment();
            case PROFILE_LOGIN:
                return new ProfileLoginFragment();
            case PROFILE_VIEW:
                return new ProfileDetailsFragment();
            case ABOUT:
                return new AboutDetailsFragment();
        }

        return new HomeFragment();
    }
}

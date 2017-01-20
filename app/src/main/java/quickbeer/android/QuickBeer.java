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
package quickbeer.android;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.danlew.android.joda.JodaTimeAndroid;

import quickbeer.android.injections.ApplicationGraph;
import timber.log.Timber;

import static io.reark.reark.utils.Preconditions.get;

@SuppressWarnings({"StaticVariableUsedBeforeInitialization", "AssignmentToStaticFieldFromInstanceMethod"})
public class QuickBeer extends Application {

    @Nullable
    private static QuickBeer instance;

    @Nullable
    private ApplicationGraph applicationGraph;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        JodaTimeAndroid.init(this);

        applicationGraph = ApplicationGraph.Initializer.init(this);
        applicationGraph.inject(this);
    }

    @NonNull
    public static QuickBeer getInstance() {
        return get(instance);
    }

    @NonNull
    public ApplicationGraph getGraph() {
        return get(applicationGraph);
    }
}

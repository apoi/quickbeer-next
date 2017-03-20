/*
 * The MIT License
 *
 * Copyright (c) 2013-2016 reark project contributors
 *
 * https://github.com/reark/reark/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package quickbeer.android.injections;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;

import com.squareup.picasso.Picasso;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import quickbeer.android.providers.GlobalNotificationProvider;
import quickbeer.android.providers.ResourceProvider;
import quickbeer.android.providers.ToastProvider;

@Module
public final class ApplicationModule {

    private final Application application;

    public ApplicationModule(Application application) {
        this.application = application;
    }

    @Provides
    @Singleton
    @ForApplication
    Context provideApplicationContext() {
        return application;
    }

    @Provides
    @Singleton
    Application provideApplication() {
        return application;
    }

    @Provides
    @Singleton
    static ResourceProvider provideUserProvider(@ForApplication Context context) {
        return new ResourceProvider(context);
    }

    @Provides
    @Singleton
    static ToastProvider provideToastProvider(@ForApplication Context context) {
        return new ToastProvider(context);
    }

    @Provides
    @Singleton
    static GlobalNotificationProvider provideGlobalNotificationProvider(
            @ForApplication Context context,
            ToastProvider toastProvider) {
        return new GlobalNotificationProvider(context, toastProvider);
    }

    @Provides
    @Singleton
    static ContentResolver provideContentResolver(@ForApplication Context context) {
        return context.getContentResolver();
    }

    @Provides
    @Singleton
    static Picasso providePicasso(@ForApplication Context context) {
        return Picasso.with(context);
    }
}

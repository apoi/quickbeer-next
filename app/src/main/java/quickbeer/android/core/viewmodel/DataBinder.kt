/*
 * Copyright 2017 Futurice GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package quickbeer.android.core.viewmodel

import rx.subscriptions.CompositeSubscription

/**
 * Provides the facility to bind/unbind to arbitrary data sources.
 */
interface DataBinder {

    /**
     * Bind to the data source.
     *
     * @param subscription a [CompositeSubscription] to hold the bindings.
     */
    fun bind(subscription: CompositeSubscription)

    /**
     * Unbind from the data source.
     */
    fun unbind()
}
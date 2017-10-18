/**
 * This file is part of QuickBeer.
 * Copyright (C) 2017 Antti Poikela <antti.poikela@iki.fi>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package quickbeer.android.viewmodels

import io.reark.reark.data.DataStreamNotification
import io.reark.reark.utils.Preconditions.get
import quickbeer.android.data.pojos.ItemList
import rx.Observable
import rx.functions.Func1
import rx.subjects.BehaviorSubject

abstract class NetworkViewModel<T> : quickbeer.android.core.viewmodel.BaseViewModel() {

    enum class ProgressStatus {
        LOADING,
        ERROR,
        VALUE,
        EMPTY
    }

    private val progressStatus = BehaviorSubject.create<ProgressStatus>()

    fun getProgressStatus(): Observable<ProgressStatus> {
        return progressStatus.asObservable()
    }

    fun setProgressStatus(status: ProgressStatus) {
        progressStatus.onNext(get(status))
    }

    override fun unbind() {
        // No implementation
    }

    internal fun toProgressStatus(): Func1<DataStreamNotification<*>, ProgressStatus> {
        return Func1 { notification ->
            if (notification.isOngoing || notification.isCompletedWithValue) {
                ProgressStatus.LOADING
            } else if (notification.isCompletedWithError) {
                ProgressStatus.ERROR
            } else if (notification.isOnNext && hasValue(notification.value)) {
                ProgressStatus.VALUE
            } else {
                ProgressStatus.EMPTY
            }
        }
    }

    private fun hasValue(item: Any?): Boolean {
        if (item == null) {
            return false
        } else if (item is Collection<*>) {
            return !item.isEmpty()
        } else if (item is ItemList<*>) {
            return !item.items.isEmpty()
        } else {
            return true
        }
    }
}
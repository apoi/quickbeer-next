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
package quickbeer.android.features.brewerdetails

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.collapsing_toolbar_activity.*
import quickbeer.android.R
import quickbeer.android.analytics.Analytics
import quickbeer.android.analytics.Events.Entry
import quickbeer.android.core.activity.BindingDrawerActivity
import quickbeer.android.core.viewmodel.DataBinder
import quickbeer.android.core.viewmodel.SimpleDataBinder
import quickbeer.android.core.viewmodel.ViewModel
import quickbeer.android.data.HasBrewerDetailsData
import quickbeer.android.data.actions.BrewerActions
import quickbeer.android.data.pojos.Brewer
import quickbeer.android.features.photoview.PhotoViewActivity
import quickbeer.android.providers.NavigationProvider
import quickbeer.android.providers.ProgressStatusProvider
import quickbeer.android.providers.ToastProvider
import quickbeer.android.transformations.BlurTransformation
import quickbeer.android.utils.kotlin.isNumeric
import quickbeer.android.viewmodels.SearchViewViewModel
import timber.log.Timber
import javax.inject.Inject

class BrewerDetailsActivity : BindingDrawerActivity() {

    @Inject
    internal lateinit var brewerActions: BrewerActions

    @Inject
    internal lateinit var navigationProvider: NavigationProvider

    @Inject
    internal lateinit var toastProvider: ToastProvider

    @Inject
    internal lateinit var progressStatusProvider: ProgressStatusProvider

    @Inject
    internal lateinit var searchViewViewModel: SearchViewViewModel

    @Inject
    internal lateinit var picasso: Picasso

    @Inject
    internal lateinit var analytics: Analytics

    private var brewerId: Int = 0

    private val dataBinder = object : SimpleDataBinder() {
        override fun bind(disposable: CompositeDisposable) {
            val sourceObservable = brewerActions.get(brewerId, HasBrewerDetailsData())
                .filter { it.isOnNext }
                .map { it.value }
                .take(1)
                .publish()

            // Update brewer access date
            disposable.add(sourceObservable
                .map { it.id }
                .subscribe({ brewerActions.access(it) }, { Timber.e(it) }))

            // Set toolbar title
            disposable.add(sourceObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ setToolbarDetails(it!!) }, { Timber.e(it) }))

            disposable.add(progressStatusProvider
                .progressStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ progress_indicator_bar.setProgress(it) }, { Timber.e(it) }))

            disposable.add(
                sourceObservable
                    .connect())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.collapsing_toolbar_activity)

        setupDrawerLayout(false)

        setBackNavigationEnabled(true)

        collapsing_toolbar_background.setOnClickListener { toastProvider.showToast(R.string.brewer_details_no_photo) }

        if (savedInstanceState != null) {
            brewerId = savedInstanceState.getInt("brewerId")
        } else {
            if (Intent.ACTION_VIEW == intent.action) {
                val idSegment = intent.data.pathSegments.find { it.isNumeric() }
                if (idSegment != null) {
                    brewerId = idSegment.toInt()
                    analytics.createEvent(Entry.LINK_BREWER)
                }
            }

            if (brewerId <= 0) {
                brewerId = intent.getIntExtra("brewerId", 0)
            }

            supportFragmentManager.beginTransaction()
                .add(R.id.container, BrewerDetailsPagerFragment.newInstance(brewerId))
                .commit()
        }
    }

    private fun setToolbarDetails(brewer: Brewer) {
        collapsing_toolbar.title = brewer.name

        picasso.load(brewer.getImageUri())
            .transform(BlurTransformation(applicationContext, 15))
            .into(collapsing_toolbar_background, object : Callback.EmptyCallback() {
                override fun onSuccess() {
                    toolbar_overlay_gradient.visibility = View.VISIBLE
                    collapsing_toolbar_background.setOnClickListener { openPhotoView(brewer.getImageUri()) }
                }
            })
    }

    private fun openPhotoView(uri: String) {
        val intent = Intent(this, PhotoViewActivity::class.java)
        intent.putExtra("source", uri)
        startActivity(intent)
    }

    override fun inject() {
        getComponent().inject(this)
    }

    override fun viewModel(): ViewModel {
        return searchViewViewModel
    }

    override fun dataBinder(): DataBinder {
        return dataBinder
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("brewerId", brewerId)
        super.onSaveInstanceState(outState)
    }

    override fun navigateTo(menuItem: MenuItem) {
        navigationProvider.navigateWithNewActivity(menuItem)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        Timber.d("onBackPressed")

        if (navigationProvider.canNavigateBack()) {
            navigationProvider.navigateBack()
        } else {
            super.onBackPressed()
        }
    }
}

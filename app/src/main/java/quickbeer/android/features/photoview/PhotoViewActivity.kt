/*
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
package quickbeer.android.features.photoview

import android.os.Bundle
import android.view.Window
import android.view.WindowManager.LayoutParams
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.photo_view_activity.*
import quickbeer.android.R
import quickbeer.android.core.activity.InjectingBaseActivity
import javax.inject.Inject

class PhotoViewActivity : InjectingBaseActivity() {

    @Inject
    internal lateinit var picasso: Picasso

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN)
        component.inject(this)

        setContentView(R.layout.photo_view_activity)
        initImageView(intent.getStringExtra("source") ?: "")
    }

    override fun inject() {
        component.inject(this)
    }

    private fun initImageView(source: String) {
        picasso.load(source)
                .centerInside()
                .fit()
                .into(photo_view)
    }
}
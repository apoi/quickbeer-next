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

public interface Constants {

    int DEFAULT_USER_ID = 0;

    int NAV_ARROW_ANIMATION_DURATION = 350;

    String API_URL = "https://www.ratebeer.com";

    String API_KEY_NAME = "apiKey";

    String BEER_IMAGE_PATH = "https://res.cloudinary.com/ratebeer/image/upload/w_250,c_limit/beer_%d.jpg";

    String BREWER_IMAGE_PATH = "http://res.cloudinary.com/ratebeer/image/upload/w_250,c_limit/brew_%d.jpg";

    String FACEBOOK_PATH = "https://m.facebook.com/%s";

    String TWITTER_PATH = "https://www.twitter.com/%s";

    String LOGIN_DEFAULT_SAVE_INFO = "on";

    String META_QUERY_PREFIX = "__";

    interface Fetchers {

        String LOGIN = "loginFetcher";

        String BEER = "beerFetcher";

        String BEER_SEARCH = "beerSearchFetcher";

        String BARCODE_SEARCH = "barcodeSearchFetcher";

        String TOP_BEERS = "topBeersFetcher";

        String BEERS_IN_COUNTRY = "beersInCountryFetcher";

        String BEERS_IN_STYLE = "beersInStyleFetcher";

        String BREWER = "brewerFetcher";

        String BREWER_BEERS = "brewerBeersFetcher";

        String REVIEW = "reviewFetcher";

        String OWN_REVIEWS = "ownReviewsFetcher";

        String TICKS = "ticksFetcher";

        String TICK = "tickBeerFetcher";

    }

}

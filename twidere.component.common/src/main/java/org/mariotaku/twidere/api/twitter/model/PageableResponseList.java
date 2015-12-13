/*
 *                 Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.api.twitter.model;

import com.bluelinelabs.logansquare.annotation.JsonObject;

import org.mariotaku.restfu.http.RestHttpResponse;
import org.mariotaku.twidere.api.twitter.util.InternalParseUtil;

import java.util.ArrayList;

/**
 * Created by mariotaku on 15/5/7.
 */
@JsonObject
public class PageableResponseList<T> extends ArrayList<T> implements TwitterResponse, CursorSupport {

    private int accessLevel;
    private RateLimitStatus rateLimitStatus;


    @Override
    public final void processResponseHeader(RestHttpResponse resp) {
        rateLimitStatus = RateLimitStatus.createFromResponseHeader(resp);
        accessLevel = InternalParseUtil.toAccessLevel(resp);
    }

    @Override
    public final int getAccessLevel() {
        return accessLevel;
    }

    @Override
    public final RateLimitStatus getRateLimitStatus() {
        return rateLimitStatus;
    }

    @Override
    public long getNextCursor() {
        return 0;
    }

    @Override
    public long getPreviousCursor() {
        return 0;
    }

    @Override
    public boolean hasNext() {
        return getNextCursor() != 0;
    }

    @Override
    public boolean hasPrevious() {
        return getPreviousCursor() != 0;
    }
}

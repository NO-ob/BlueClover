/*
 * Clover - 4chan browser https://github.com/Floens/Clover/
 * Copyright (C) 2014  Floens
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.floens.chan.core.site.http;


import androidx.annotation.Nullable;

import org.floens.chan.core.di.UserAgentProvider;
import org.floens.chan.core.site.Site;
import org.floens.chan.core.site.SiteRequestModifier;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Manages the {@link HttpCall} executions.
 */
@Singleton
public class HttpCallManager {
    private static final int TIMEOUT = 30000;

    private UserAgentProvider userAgentProvider;
    private OkHttpClient client;

    @Inject
    public HttpCallManager(UserAgentProvider userAgentProvider) {
        this.userAgentProvider = userAgentProvider;
        client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
    }

    public void makeHttpCall(
            HttpCall httpCall,
            HttpCall.HttpCallback<? extends HttpCall> callback
    ) {
        makeHttpCall(httpCall, callback, null);
    }

    public void makeHttpCall(
            HttpCall httpCall,
            HttpCall.HttpCallback<? extends HttpCall> callback,
            @Nullable ProgressRequestBody.ProgressRequestListener progressListener
    ) {
        httpCall.setCallback(callback);

        Request.Builder requestBuilder = new Request.Builder();

        Site site = httpCall.site;
        httpCall.setup(requestBuilder, progressListener);

        if (site != null) {
            final SiteRequestModifier siteRequestModifier = site.requestModifier();
            if (siteRequestModifier != null) {
                siteRequestModifier.modifyHttpCall(httpCall, requestBuilder);
            }
        }

        requestBuilder.header("User-Agent", userAgentProvider.getUserAgent());
        Request request = requestBuilder.build();

        client.newCall(request).enqueue(httpCall);
    }
}

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
package org.floens.chan;

import android.os.Build;

import org.floens.chan.utils.TLSSocketFactory;

import javax.net.ssl.HttpsURLConnection;

/**
 * The ChanApplication wrapping our Chan application.
 * For historical reasons the main application class needs to be 'org.floens.chan.ChanApplication'.
 */
public class ChanApplication extends Chan {
    @Override
    public void onCreate() {
        super.onCreate();

        initialize();

        if (Build.VERSION.SDK_INT < 22) {
            try {
                HttpsURLConnection.setDefaultSSLSocketFactory(TLSSocketFactory.getInstance());
            } catch (Exception ignored) { }
        }
    }
}

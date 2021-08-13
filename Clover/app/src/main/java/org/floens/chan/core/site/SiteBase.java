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
package org.floens.chan.core.site;


import static org.floens.chan.Chan.injector;

import com.android.volley.RequestQueue;

import org.codejargon.feather.Feather;
import org.floens.chan.core.database.LoadableProvider;
import org.floens.chan.core.manager.BoardManager;
import org.floens.chan.core.model.json.site.SiteConfig;
import org.floens.chan.core.model.orm.Board;
import org.floens.chan.core.settings.SettingProvider;
import org.floens.chan.core.settings.json.JsonSettings;
import org.floens.chan.core.settings.json.JsonSettingsProvider;
import org.floens.chan.core.site.http.HttpCallManager;
import org.floens.chan.utils.Time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SiteBase implements Site {
    protected int id;
    protected SiteConfig config;

    protected HttpCallManager httpCallManager;
    protected RequestQueue requestQueue;
    protected BoardManager boardManager;
    protected LoadableProvider loadableProvider;

    private JsonSettings userSettings;
    protected SettingProvider settingsProvider;

    private boolean initialized = false;

    @Override
    public void initialize(int id, SiteConfig config, JsonSettings userSettings) {
        this.id = id;
        this.config = config;
        this.userSettings = userSettings;

        if (initialized) {
            throw new IllegalStateException();
        }
        initialized = true;
    }

    @Override
    public void postInitialize() {
        long start = Time.startTiming();

        Feather injector = injector();
        httpCallManager = injector.instance(HttpCallManager.class);
        requestQueue = injector.instance(RequestQueue.class);
        boardManager = injector.instance(BoardManager.class);
        loadableProvider = injector.instance(LoadableProvider.class);
        SiteService siteService = injector.instance(SiteService.class);

        settingsProvider = new JsonSettingsProvider(userSettings, () -> {
            siteService.updateUserSettings(this, userSettings);
        });

        initializeSettings();

        if (boardsType().canList) {
            actions().boards(boards -> boardManager.updateAvailableBoardsForSite(this, boards.boards));
        }

        Time.endTiming("initialized " + name(), start);
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public Board board(String code) {
        return boardManager.getBoard(this, code);
    }

    @Override
    public List<SiteSetting> settings() {
        return new ArrayList<>();
    }

    public void initializeSettings() {
    }

    @Override
    public Board createBoard(String name, String code) {
        Board existing = board(code);
        if (existing != null) {
            return existing;
        }

        Board board = Board.fromSiteNameCode(this, name, code);
        boardManager.updateAvailableBoardsForSite(this, Collections.singletonList(board));
        return board;
    }
}

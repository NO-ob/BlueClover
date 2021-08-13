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

import org.floens.chan.core.model.Post;
import org.floens.chan.core.model.json.site.SiteConfig;
import org.floens.chan.core.model.orm.Board;
import org.floens.chan.core.model.orm.Loadable;
import org.floens.chan.core.settings.json.JsonSettings;
import org.floens.chan.core.site.http.DeleteRequest;
import org.floens.chan.core.site.http.Reply;
import org.floens.chan.core.site.parser.ChanReader;

import java.util.List;

public interface Site {
    enum Feature {
        /**
         * This site supports posting. (Or rather, we've implemented support for it.)
         *
         * @see SiteActions#post(Reply, SiteActions.PostListener)
         * @see SiteEndpoints#reply(Loadable)
         */
        POSTING,

        /**
         * This site supports deleting posts.
         *
         * @see SiteActions#delete(DeleteRequest, SiteActions.DeleteListener)
         * @see SiteEndpoints#delete(Post)
         */
        POST_DELETE,

        /**
         * This site supports reporting posts.
         *
         * @see SiteEndpoints#report(Post)
         */
        POST_REPORT
    }

    /**
     * Features available to check when {@link Feature#POSTING} is {@code true}.
     */
    enum BoardFeature {
        /**
         * This board supports posting with images.
         */
        // TODO(multisite) use this
        POSTING_IMAGE,

        /**
         * This board supports posting with a checkbox to mark the posted image as a spoiler.
         */
        // TODO(multisite) use this
        POSTING_SPOILER,

        /**
         * This board support loading the archive, a list of threads that are locked after expiring.
         */
        ARCHIVE,
    }

    /**
     * How the boards are organized for this site.
     */
    enum BoardsType {
        /**
         * The site's boards are static, there is no extra info for a board in the api.
         */
        STATIC(true),

        /**
         * The site's boards are dynamic, a boards.json like endpoint is available to get the available boards.
         */
        DYNAMIC(true),

        /**
         * The site's boards are dynamic and infinite, existence of boards should be checked per board.
         */
        INFINITE(false);

        /**
         * Can the boards be listed, in other words, can
         * {@link SiteActions#boards(SiteActions.BoardsListener)} be used, and is
         * {@link #board(String)} available.
         */
        public boolean canList;

        BoardsType(boolean canList) {
            this.canList = canList;
        }
    }

    /**
     * Initialize the site with the given id, config, and userSettings.
     * <p><b>Note: do not use any managers at this point, because they rely on the sites being initialized.
     * Instead, use {@link #postInitialize()}</b>
     *
     * @param id           the site id
     * @param config       the site config
     * @param userSettings the site user settings
     */
    void initialize(int id, SiteConfig config, JsonSettings userSettings);

    void postInitialize();

    /**
     * Global positive (>0) integer that uniquely identifies this site.<br>
     * Use the id received from {@link #initialize(int, SiteConfig, JsonSettings)}.
     *
     * @return a positive (>0) integer that uniquely identifies this site.
     */
    int id();

    String name();

    SiteIcon icon();

    BoardsType boardsType();

    SiteUrlHandler resolvable();

    boolean feature(Feature feature);

    boolean boardFeature(BoardFeature boardFeature, Board board);

    List<SiteSetting> settings();

    SiteEndpoints endpoints();

    SiteRequestModifier requestModifier();

    ChanReader chanReader();

    SiteActions actions();

    /**
     * Return the board for this site with the given {@code code}.
     * <p>This does not need to create the board if it doesn't exist. This is important for
     * sites that have the board type INFINITE. Returning from the database is
     * enough.</p>
     *
     * @param code the board code
     * @return a board with the board code, or {@code null}.
     */
    Board board(String code);

    /**
     * Create a new board with the specified {@code code} and {@code name}.
     * <p>This is only applicable to sites with a board type INFINITE.</p>
     *
     * @param name the name of the board.
     * @param code the code to create the board with.
     * @return the created board.
     */
    Board createBoard(String name, String code);
}

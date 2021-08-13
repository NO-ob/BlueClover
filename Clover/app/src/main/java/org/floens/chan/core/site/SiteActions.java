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

import org.floens.chan.core.model.Archive;
import org.floens.chan.core.model.orm.Board;
import org.floens.chan.core.site.http.DeleteRequest;
import org.floens.chan.core.site.http.DeleteResponse;
import org.floens.chan.core.site.http.HttpCall;
import org.floens.chan.core.site.http.Reply;
import org.floens.chan.core.site.http.ReplyResponse;

public interface SiteActions {
    void boards(BoardsListener boardsListener);

    interface BoardsListener {
        void onBoardsReceived(Boards boards);
    }

    void post(Reply reply, PostListener postListener);

    interface PostListener {

        void onPostComplete(HttpCall httpCall, ReplyResponse replyResponse);

        void onUploadingProgress(int percent);

        void onPostError(HttpCall httpCall, Exception exception);
    }

    boolean postRequiresAuthentication();

    /**
     * If {@link ReplyResponse#requireAuthentication} was {@code true}, or if
     * {@link #postRequiresAuthentication()} is {@code true}, get the authentication
     * required to post.
     * <p>
     * <p>Some sites know beforehand if you need to authenticate, some sites only report it
     * after posting. That's why there are two methods.</p>
     *
     * @return an {@link SiteAuthentication} model that describes the way to authenticate.
     */
    SiteAuthentication postAuthenticate();

    void delete(DeleteRequest deleteRequest, DeleteListener deleteListener);

    interface DeleteListener {
        void onDeleteComplete(HttpCall httpCall, DeleteResponse deleteResponse);

        void onDeleteError(HttpCall httpCall);
    }

    void archive(Board board, ArchiveListener archiveListener);

    interface ArchiveListener {
        void onArchive(Archive archive);

        void onArchiveError();
    }
}

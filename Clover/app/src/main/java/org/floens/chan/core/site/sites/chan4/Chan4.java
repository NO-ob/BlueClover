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
package org.floens.chan.core.site.sites.chan4;

import android.webkit.CookieManager;
import android.webkit.WebView;

import androidx.annotation.Nullable;

import org.floens.chan.core.model.Post;
import org.floens.chan.core.model.orm.Board;
import org.floens.chan.core.model.orm.Loadable;
import org.floens.chan.core.settings.OptionSettingItem;
import org.floens.chan.core.settings.OptionsSetting;
import org.floens.chan.core.site.Boards;
import org.floens.chan.core.site.Site;
import org.floens.chan.core.site.SiteActions;
import org.floens.chan.core.site.SiteAuthentication;
import org.floens.chan.core.site.SiteBase;
import org.floens.chan.core.site.SiteEndpoints;
import org.floens.chan.core.site.SiteIcon;
import org.floens.chan.core.site.SiteRequestModifier;
import org.floens.chan.core.site.SiteSetting;
import org.floens.chan.core.site.SiteUrlHandler;
import org.floens.chan.core.site.common.CommonReplyHttpCall;
import org.floens.chan.core.site.common.DefaultPostParser;
import org.floens.chan.core.site.common.FutabaChanReader;
import org.floens.chan.core.site.http.DeleteRequest;
import org.floens.chan.core.site.http.HttpCall;
import org.floens.chan.core.site.http.Reply;
import org.floens.chan.core.site.parser.ChanReader;
import org.floens.chan.core.site.parser.CommentParser;
import org.floens.chan.utils.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import okhttp3.HttpUrl;
import okhttp3.Request;

public class Chan4 extends SiteBase {
    public static final SiteUrlHandler URL_HANDLER = new SiteUrlHandler() {
        @Override
        public Class<? extends Site> getSiteClass() {
            return Chan4.class;
        }

        @Override
        public boolean matchesName(String value) {
            return value.equals("4chan");
        }

        @Override
        public boolean respondsTo(HttpUrl url) {
            return url.host().equals("4chan.org") ||
                    url.host().equals("www.4chan.org") ||
                    url.host().equals("boards.4chan.org") ||
                    url.host().equals("www.4channel.org") ||
                    url.host().equals("boards.4channel.org");
        }

        @Override
        public String desktopUrl(Loadable loadable, @Nullable Post post) {
            if (loadable.isCatalogMode()) {
                String thread = "";
                if (post != null && post.isOP) {
                    thread = "thread/" + post.no;
                }

                return "https://boards.4chan.org/" + loadable.board.code + "/" + thread;
            } else if (loadable.isThreadMode()) {
                String url = "https://boards.4chan.org/" + loadable.board.code + "/thread/" + loadable.no;
                if (post != null) {
                    url += "#p" + post.no;
                }
                return url;
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public Loadable resolveLoadable(Site site, HttpUrl url) {
            List<String> parts = url.pathSegments();

            if (!parts.isEmpty()) {
                String boardCode = parts.get(0);
                Board board = site.board(boardCode);
                if (board != null) {
                    if (parts.size() < 3) {
                        // Board mode
                        return Loadable.forCatalog(board);
                    } else if (parts.size() >= 3) {
                        // Thread mode
                        int no = -1;
                        try {
                            no = Integer.parseInt(parts.get(2));
                        } catch (NumberFormatException ignored) {
                        }

                        int post = -1;
                        String fragment = url.fragment();
                        if (fragment != null) {
                            int index = fragment.indexOf("p");
                            if (index >= 0) {
                                try {
                                    post = Integer.parseInt(fragment.substring(index + 1));
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }

                        if (no >= 0) {
                            Loadable loadable = Loadable.forThread(site, board, no);
                            if (post >= 0) {
                                loadable.markedNo = post;
                            }

                            return loadable;
                        }
                    }
                }
            }

            return null;
        }
    };

    private static final String TAG = "Chan4";

    private static final String CAPTCHA_KEY = "6Ldp2bsSAAAAAAJ5uyx_lx34lJeEpTLVkP5k04qc";

    private static final Random random = new Random();

    private final SiteEndpoints endpoints = new SiteEndpoints() {
        private final HttpUrl a = new HttpUrl.Builder()
                .scheme("https")
                .host("a.4cdn.org")
                .build();

        private final HttpUrl i = new HttpUrl.Builder()
                .scheme("https")
                .host("i.4cdn.org")
                .build();

        private final HttpUrl t = new HttpUrl.Builder()
                .scheme("https")
                .host("i.4cdn.org")
                .build();

        private final HttpUrl s = new HttpUrl.Builder()
                .scheme("https")
                .host("s.4cdn.org")
                .build();

        private final HttpUrl sys = new HttpUrl.Builder()
                .scheme("https")
                .host("sys.4chan.org")
                .build();

        private final HttpUrl b = new HttpUrl.Builder()
                .scheme("https")
                .host("boards.4chan.org")
                .build();

        @Override
        public HttpUrl catalog(Board board) {
            return a.newBuilder()
                    .addPathSegment(board.code)
                    .addPathSegment("catalog.json")
                    .addQueryParameter("t", String.valueOf(System.currentTimeMillis()))
                    .build();
        }

        @Override
        public HttpUrl thread(Board board, Loadable loadable) {
            return a.newBuilder()
                    .addPathSegment(board.code)
                    .addPathSegment("thread")
                    .addPathSegment(loadable.no + ".json")
                    .addQueryParameter("t", String.valueOf(System.currentTimeMillis()))
                    .build();
        }

        @Override
        public HttpUrl imageUrl(Post.Builder post, Map<String, String> arg) {
            return i.newBuilder()
                    .addPathSegment(post.board.code)
                    .addPathSegment(arg.get("tim") + "." + arg.get("ext"))
                    .build();
        }

        @Override
        public HttpUrl thumbnailUrl(Post.Builder post, boolean spoiler, Map<String, String> arg) {
            if (spoiler) {
                HttpUrl.Builder image = s.newBuilder()
                        .addPathSegment("image");
                if (post.board.customSpoilers >= 0) {
                    int i = random.nextInt(post.board.customSpoilers) + 1;
                    image.addPathSegment("spoiler-" + post.board.code + i + ".png");
                } else {
                    image.addPathSegment("spoiler.png");
                }
                return image.build();
            } else {
                return t.newBuilder()
                        .addPathSegment(post.board.code)
                        .addPathSegment(arg.get("tim") + "s.jpg")
                        .build();
            }
        }

        @Override
        public HttpUrl icon(Post.Builder post, String icon, Map<String, String> arg) {
            HttpUrl.Builder b = s.newBuilder().addPathSegment("image");

            switch (icon) {
                case "country":
                    b.addPathSegment("country");
                    b.addPathSegment(arg.get("country_code").toLowerCase(Locale.ENGLISH) + ".gif");
                    break;
                case "board_flag":
                    b.addPathSegment("flags");
                    b.addPathSegment(post.board.code);
                    b.addPathSegment(arg.get("board_flag_code").toLowerCase(Locale.ENGLISH) + ".gif");
                    break;
                case "since4pass":
                    b.addPathSegment("minileaf.gif");
                    break;
            }

            return b.build();
        }

        @Override
        public HttpUrl boards() {
            return a.newBuilder()
                    .addPathSegment("boards.json")
                    .build();
        }

        @Override
        public HttpUrl archive(Board board) {
            return b.newBuilder()
                    .addPathSegment(board.code)
                    .addPathSegment("archive")
                    .build();
        }

        @Override
        public HttpUrl reply(Loadable loadable) {
            return sys.newBuilder()
                    .addPathSegment(loadable.board.code)
                    .addPathSegment("post")
                    .build();
        }

        @Override
        public HttpUrl delete(Post post) {
            return sys.newBuilder()
                    .addPathSegment(post.board.code)
                    .addPathSegment("imgboard.php")
                    .build();
        }

        @Override
        public HttpUrl report(Post post) {
            return sys.newBuilder()
                    .addPathSegment(post.board.code)
                    .addPathSegment("imgboard.php")
                    .addQueryParameter("mode", "report")
                    .addQueryParameter("no", String.valueOf(post.no))
                    .build();
        }
    };

    private SiteRequestModifier siteRequestModifier = new SiteRequestModifier() {
        @Override
        public void modifyHttpCall(HttpCall httpCall, Request.Builder requestBuilder) { }

        @SuppressWarnings("deprecation")
        @Override
        public void modifyWebView(WebView webView) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
        }
    };

    private SiteActions actions = new SiteActions() {
        @Override
        public void boards(final BoardsListener listener) {
            requestQueue.add(new Chan4BoardsRequest(Chan4.this, response -> {
                listener.onBoardsReceived(new Boards(response));
            }, (error) -> {
                Logger.e(TAG, "Failed to get boards from server", error);

                // API fail, provide some default boards
                List<Board> list = new ArrayList<>();
                list.add(new Board(Chan4.this, "Technology", "g", true, true));
                list.add(new Board(Chan4.this, "Food & Cooking", "ck", true, true));
                list.add(new Board(Chan4.this, "Do It Yourself", "diy", true, true));
                list.add(new Board(Chan4.this, "Animals & Nature", "an", true, true));
                Collections.shuffle(list);
                listener.onBoardsReceived(new Boards(list));
            }));
        }

        @Override
        public void archive(Board board, ArchiveListener archiveListener) {
            requestQueue.add(new Chan4ArchiveRequest(Chan4.this, board,
                    archiveListener::onArchive,
                    error -> archiveListener.onArchiveError()));
        }

        @Override
        public void post(Reply reply, final PostListener postListener) {
            httpCallManager.makeHttpCall(
                    new Chan4ReplyCall(Chan4.this, reply), new HttpCall.HttpCallback<CommonReplyHttpCall>() {
                        @Override
                        public void onHttpSuccess(CommonReplyHttpCall httpPost) {
                            postListener.onPostComplete(httpPost, httpPost.replyResponse);
                        }

                        @Override
                        public void onHttpFail(CommonReplyHttpCall httpPost, Exception e) {
                            postListener.onPostError(httpPost, e);
                        }
                    }, postListener::onUploadingProgress);
        }

        @Override
        public boolean postRequiresAuthentication() {
            return true;
        }

        @Override
        public SiteAuthentication postAuthenticate() {
            switch (captchaType.get()) {
                case V2JS:
                    return SiteAuthentication.fromCaptcha2(CAPTCHA_KEY, "https://boards.4chan.org");
                case V2NOJS:
                    return SiteAuthentication.fromCaptcha2nojs(CAPTCHA_KEY, "https://boards.4chan.org");
                case NEWCAPTCHA:
                    return SiteAuthentication.fromNewCaptcha("https://sys.4chan.org");
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public void delete(DeleteRequest deleteRequest, final DeleteListener deleteListener) {
            httpCallManager.makeHttpCall(new Chan4DeleteHttpCall(Chan4.this, deleteRequest), new HttpCall.HttpCallback<Chan4DeleteHttpCall>() {
                @Override
                public void onHttpSuccess(Chan4DeleteHttpCall httpPost) {
                    deleteListener.onDeleteComplete(httpPost, httpPost.deleteResponse);
                }

                @Override
                public void onHttpFail(Chan4DeleteHttpCall httpPost, Exception e) {
                    deleteListener.onDeleteError(httpPost);
                }
            });
        }
    };

    public enum CaptchaType implements OptionSettingItem {
        V2JS("v2js"),
        V2NOJS("v2nojs"),
        NEWCAPTCHA("newcaptcha");

        String name;

        CaptchaType(String name) {
            this.name = name;
        }

        @Override
        public String getKey() {
            return name;
        }
    }

    private OptionsSetting<CaptchaType> captchaType;

    @Override
    public void initializeSettings() {
        super.initializeSettings();

        captchaType = new OptionsSetting<>(settingsProvider,
                "preference_captcha_type",
                CaptchaType.class, CaptchaType.NEWCAPTCHA);
    }

    @Override
    public List<SiteSetting> settings() {
        return Arrays.asList(
                SiteSetting.forOption(
                        captchaType,
                        "Captcha type",
                        Arrays.asList("Javascript", "Noscript", "Slider captcha"))
        );
    }

    @Override
    public String name() {
        return "4chan";
    }

    @Override
    public SiteIcon icon() {
        return SiteIcon.fromAssets("icons/4chan.png");
    }

    @Override
    public SiteUrlHandler resolvable() {
        return URL_HANDLER;
    }

    @Override
    public boolean feature(Feature feature) {
        switch (feature) {
            case POSTING:
                // yes, we support posting.
                return true;
            case POST_DELETE:
                // yes, with the password saved when posting.
                return true;
            case POST_REPORT:
                // yes, with a custom url
                return true;
            default:
                return false;
        }
    }

    @Override
    public BoardsType boardsType() {
        // yes, boards.json
        return BoardsType.DYNAMIC;
    }

    @Override
    public boolean boardFeature(BoardFeature boardFeature, Board board) {
        switch (boardFeature) {
            case POSTING_IMAGE:
                // yes, we support image posting.
                return true;
            case POSTING_SPOILER:
                // depends if the board supports it.
                return board.spoilers;
            case ARCHIVE:
                return board.archive;
            default:
                return false;
        }
    }

    @Override
    public SiteEndpoints endpoints() {
        return endpoints;
    }

    @Override
    public SiteRequestModifier requestModifier() {
        return siteRequestModifier;
    }

    @Override
    public ChanReader chanReader() {
        CommentParser commentParser = new CommentParser();
        commentParser.addDefaultRules();
        commentParser.addInternalDomain("4chan.org");
        commentParser.addInternalDomain("www.4chan.org");
        commentParser.addInternalDomain("boards.4chan.org");
        commentParser.addInternalDomain("4channel.org");
        commentParser.addInternalDomain("www.4channel.org");
        commentParser.addInternalDomain("boards.4channel.org");
        return new FutabaChanReader(new DefaultPostParser(commentParser));
    }

    @Override
    public SiteActions actions() {
        return actions;
    }
}

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
package org.floens.chan.ui.controller;

import static org.floens.chan.ui.theme.ThemeHelper.theme;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.floens.chan.R;
import org.floens.chan.controller.Controller;
import org.floens.chan.core.model.Post;
import org.floens.chan.core.model.PostImage;
import org.floens.chan.core.presenter.ThreadPresenter;
import org.floens.chan.core.settings.ChanSettings;
import org.floens.chan.ui.cell.PostCellInterface;
import org.floens.chan.ui.helper.PostPopupHelper;
import org.floens.chan.ui.view.LoadView;
import org.floens.chan.ui.view.ThumbnailView;
import org.floens.chan.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostRepliesController extends Controller {
    private static final int TRANSITION_DURATION = 200;

    private PostPopupHelper postPopupHelper;
    private ThreadPresenter presenter;

    private int statusBarColorPrevious;
    private boolean first = true;

    private LoadView loadView;
    private ListView listView;
    private PostPopupHelper.RepliesData displayingData;

    public PostRepliesController(Context context, PostPopupHelper postPopupHelper, ThreadPresenter presenter) {
        super(context);
        this.postPopupHelper = postPopupHelper;
        this.presenter = presenter;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        view = inflateRes(R.layout.layout_post_replies_container);

        // Clicking outside the popup view
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postPopupHelper.pop();
            }
        });

        loadView = view.findViewById(R.id.loadview);

        if (Build.VERSION.SDK_INT >= 21) {
            statusBarColorPrevious = getWindow().getStatusBarColor();
            if (statusBarColorPrevious != 0) {
                AndroidUtils.animateStatusBar(getWindow(), true, statusBarColorPrevious, TRANSITION_DURATION);
            }
        }
    }

    @Override
    public void stopPresenting() {
        super.stopPresenting();
        if (Build.VERSION.SDK_INT >= 21) {
            if (statusBarColorPrevious != 0) {
                AndroidUtils.animateStatusBar(getWindow(), true, statusBarColorPrevious, TRANSITION_DURATION);
            }
        }
    }

    public ThumbnailView getThumbnail(PostImage postImage) {
        if (listView == null) {
            return null;
        } else {
            ThumbnailView thumbnail = null;
            for (int i = 0; i < listView.getChildCount(); i++) {
                View view = listView.getChildAt(i);
                if (view instanceof PostCellInterface) {
                    PostCellInterface postView = (PostCellInterface) view;
                    Post post = postView.getPost();

                    if (!post.images.isEmpty()) {
                        for (int j = 0; j < post.images.size(); j++) {
                            if (post.images.get(j).equalUrl(postImage)) {
                                thumbnail = postView.getThumbnailView(postImage);
                            }
                        }
                    }
                }
            }
            return thumbnail;
        }
    }

    public List<ThumbnailView> getThumbnails() {
        if (listView == null) {
            return Collections.emptyList();
        } else {
            List<ThumbnailView> thumbnails = new ArrayList<>(7);
            for (int i = 0; i < listView.getChildCount(); i++) {
                View view = listView.getChildAt(i);
                if (view instanceof PostCellInterface) {
                    PostCellInterface postView = (PostCellInterface) view;
                    Post post = postView.getPost();

                    if (!post.images.isEmpty()) {
                        for (int j = 0; j < post.images.size(); j++) {
                            thumbnails.add(postView.getThumbnailView(post.images.get(j)));
                        }
                    }
                }
            }
            return thumbnails;
        }
    }

    public void setPostRepliesData(PostPopupHelper.RepliesData data) {
        displayData(data);
    }

    public List<Post> getPostRepliesData() {
        return displayingData.posts;
    }

    public void scrollTo(int displayPosition, boolean smooth) {
        listView.smoothScrollToPosition(displayPosition);
    }

    private void displayData(final PostPopupHelper.RepliesData data) {
        displayingData = data;

        View dataView;
        if (ChanSettings.repliesButtonsBottom.get()) {
            dataView = inflateRes(R.layout.layout_post_replies_bottombuttons);
        } else {
            dataView = inflateRes(R.layout.layout_post_replies);
        }

        listView = dataView.findViewById(R.id.post_list);
        listView.setDivider(null);
        listView.setDividerHeight(0);

        View repliesBack = dataView.findViewById(R.id.replies_back);
        repliesBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postPopupHelper.pop();
            }
        });

        View repliesClose = dataView.findViewById(R.id.replies_close);
        repliesClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postPopupHelper.popAll();
            }
        });

        Drawable backDrawable = theme().backDrawable.makeDrawable(context);
        Drawable doneDrawable = theme().doneDrawable.makeDrawable(context);

        TextView repliesBackText = dataView.findViewById(R.id.replies_back_icon);
        TextView repliesCloseText = dataView.findViewById(R.id.replies_close_icon);
        repliesBackText.setCompoundDrawablesWithIntrinsicBounds(backDrawable, null, null, null);
        repliesCloseText.setCompoundDrawablesWithIntrinsicBounds(doneDrawable, null, null, null);
        if (theme().isLightTheme) {
            repliesBackText.setTextColor(0x8a000000);
            repliesCloseText.setTextColor(0x8a000000);
        } else {
            repliesBackText.setTextColor(0xffffffff);
            repliesCloseText.setTextColor(0xffffffff);
        }

        ArrayAdapter<Post> adapter = new ArrayAdapter<Post>(context, 0) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                PostCellInterface postCell;
                if (convertView instanceof PostCellInterface) {
                    postCell = (PostCellInterface) convertView;
                } else {
                    postCell = (PostCellInterface) LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_post, parent, false);
                }

                final Post p = getItem(position);
                boolean showDivider = position < getCount() - 1;
                postCell.setPost(null,
                        p,
                        presenter,
                        false,
                        false,
                        false,
                        data.forPost.no,
                        showDivider,
                        ChanSettings.PostViewMode.LIST,
                        false);

                return (View) postCell;
            }
        };

        adapter.addAll(data.posts);
        listView.setAdapter(adapter);

        listView.setSelectionFromTop(data.listViewIndex, data.listViewTop);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                data.listViewIndex = view.getFirstVisiblePosition();
                View v = view.getChildAt(0);
                data.listViewTop = (v == null) ? 0 : v.getTop();
            }
        });

        loadView.setFadeDuration(first ? 0 : 150);
        first = false;
        loadView.setView(dataView);
    }

    @Override
    public boolean onBack() {
        postPopupHelper.pop();
        return true;
    }

    private Window getWindow() {
        return ((Activity) context).getWindow();
    }
}

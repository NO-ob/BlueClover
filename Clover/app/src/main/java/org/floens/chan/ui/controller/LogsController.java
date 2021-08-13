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

import static org.floens.chan.utils.AndroidUtils.getAttrColor;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.floens.chan.R;
import org.floens.chan.controller.Controller;
import org.floens.chan.ui.toolbar.ToolbarMenuSubItem;
import org.floens.chan.utils.AndroidUtils;
import org.floens.chan.utils.IOUtils;
import org.floens.chan.utils.Logger;

import java.io.IOException;
import java.io.InputStream;

public class LogsController extends Controller {
    private static final String TAG = "LogsController";

    private TextView logTextView;

    private String logText;

    public LogsController(Context context) {
        super(context);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        navigation.setTitle(org.floens.chan.R.string.settings_logs_screen);

        navigation.buildMenu().withOverflow()
                .withSubItem(R.string.settings_logs_copy, this::copyLogsClicked)
                .build().build();

        ScrollView container = new ScrollView(context);
        container.setBackgroundColor(getAttrColor(context, org.floens.chan.R.attr.backcolor));
        logTextView = new TextView(context);
        container.addView(logTextView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        view = container;

        loadLogs();
    }

    private void copyLogsClicked(ToolbarMenuSubItem item) {
        ClipboardManager clipboard = (ClipboardManager) AndroidUtils.getAppContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Logs", logText);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, R.string.settings_logs_copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    private void loadLogs() {
        Process process;
        try {
            process = new ProcessBuilder()
                    .command("logcat", "-d", "-v", "tag")
                    .start();
        } catch (IOException e) {
            Logger.e(TAG, "Error starting logcat", e);
            return;
        }

        InputStream outputStream = process.getInputStream();
        logText = IOUtils.readString(outputStream);
        logTextView.setText(logText);
    }
}

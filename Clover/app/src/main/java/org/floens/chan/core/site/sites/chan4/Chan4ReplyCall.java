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

import android.text.TextUtils;

import org.floens.chan.core.site.Site;
import org.floens.chan.core.site.common.CommonReplyHttpCall;
import org.floens.chan.core.site.http.Reply;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class Chan4ReplyCall extends CommonReplyHttpCall {
    public Chan4ReplyCall(Site site, Reply reply) {
        super(site, reply);
    }

    @Override
    public void addParameters(MultipartBody.Builder formBuilder) {
        // TODO this should be a separate option
        if (reply.options.contains("[")) {
            String flag = reply.options.split("\\[")[1].split("\\]")[0].toUpperCase();
            formBuilder.addFormDataPart("flag", flag);
            reply.options = reply.options.replaceAll("\\[[^\\]]+\\]", "");
        }

        formBuilder.addFormDataPart("mode", "regist");
        formBuilder.addFormDataPart("pwd", replyResponse.password);

        if (reply.loadable.isThreadMode()) {
            formBuilder.addFormDataPart("resto", String.valueOf(reply.loadable.no));
        }

        formBuilder.addFormDataPart("name", reply.name);
        formBuilder.addFormDataPart("email", reply.options);

        if (!reply.loadable.isThreadMode() && !TextUtils.isEmpty(reply.subject)) {
            formBuilder.addFormDataPart("sub", reply.subject);
        }

        formBuilder.addFormDataPart("com", reply.comment);

        if (reply.captchaResponse != null) {
            if (reply.captchaChallenge != null) {
                //formBuilder.addFormDataPart("recaptcha_challenge_field", reply.captchaChallenge);
                //formBuilder.addFormDataPart("recaptcha_response_field", reply.captchaResponse);
                formBuilder.addFormDataPart("t-challenge", reply.captchaChallenge);
                formBuilder.addFormDataPart("t-response", reply.captchaResponse);
            } else {
                formBuilder.addFormDataPart("g-recaptcha-response", reply.captchaResponse);
            }
        }

        if (reply.file != null) {
            formBuilder.addFormDataPart("upfile", reply.fileName, RequestBody.create(
                    MediaType.parse("application/octet-stream"), reply.file
            ));
        }

        if (reply.spoilerImage) {
            formBuilder.addFormDataPart("spoiler", "on");
        }
    }
}

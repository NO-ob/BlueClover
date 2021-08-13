package org.floens.chan.core.site.sites.dvach;

import static org.floens.chan.core.site.SiteEndpoints.makeArgument;

import android.util.JsonReader;

import org.floens.chan.core.model.Post;
import org.floens.chan.core.model.PostImage;
import org.floens.chan.core.site.SiteEndpoints;
import org.floens.chan.core.site.common.CommonSite;
import org.floens.chan.core.site.parser.ChanReaderProcessingQueue;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DvachApi extends CommonSite.CommonApi {
    DvachApi(CommonSite commonSite) {
        super(commonSite);
    }

    @Override
    public void loadThread(JsonReader reader, ChanReaderProcessingQueue queue) throws Exception {
        reader.beginObject(); // Main object

        while (reader.hasNext()) {

            if (reader.nextName().equals("threads")) {
                reader.beginArray(); // Threads array

                while (reader.hasNext()) {
                    reader.beginObject(); // Posts object
                    if (reader.nextName().equals("posts")) {
                        reader.beginArray(); // Posts array
                        while (reader.hasNext()) {
                            readPostObject(reader, queue);
                        }
                        reader.endArray();
                    }
                    reader.endObject();
                }

                reader.endArray();
            } else {
                reader.skipValue();
            }
        }

        reader.endObject();
    }

    @Override
    public void loadCatalog(JsonReader reader, ChanReaderProcessingQueue queue) throws Exception {
        reader.beginObject(); // Main object

        while (reader.hasNext()) {
            if (reader.nextName().equals("threads")) {
                reader.beginArray(); // Threads array

                while (reader.hasNext()) {
                    readPostObject(reader, queue);
                }

                reader.endArray();
            } else {
                reader.skipValue();
            }
        }

        reader.endObject();
    }

    @Override
    public void readPostObject(JsonReader reader, ChanReaderProcessingQueue queue) throws Exception {
        Post.Builder builder = new Post.Builder();
        builder.board(queue.getLoadable().board);

        SiteEndpoints endpoints = queue.getLoadable().getSite().endpoints();

        List<PostImage> files = new ArrayList<>();

        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();

            switch (key) {
                case "name":
                    builder.name(reader.nextString());
                    break;
                case "comment":
                    builder.comment(reader.nextString());
                    break;
                case "timestamp":
                    builder.setUnixTimestampSeconds(reader.nextLong());
                    break;
                case "trip":
                    builder.tripcode(reader.nextString());
                    break;
                case "op":
                    int opId = reader.nextInt();
                    builder.op(opId == 0);
                    builder.opId(opId);
                    break;
                case "sticky":
                    builder.sticky(reader.nextInt() == 1);
                    break;
                case "closed":
                    builder.closed(reader.nextInt() == 1);
                    break;
                case "archived":
                    builder.archived(reader.nextInt() == 1);
                    break;
                case "posts_count":
                    builder.replies(reader.nextInt() - 1);
                    break;
                case "files_count":
                    builder.images(reader.nextInt());
                    break;
                case "lasthit":
                    builder.lastModified(reader.nextLong());
                    break;
                case "num":
                    String num = reader.nextString();
                    builder.id(Integer.parseInt(num));
                    break;
                case "files":
                    reader.beginArray();

                    while (reader.hasNext()) {
                        PostImage postImage = readPostImage(reader, builder, endpoints);
                        if (postImage != null) {
                            files.add(postImage);
                        }
                    }

                    reader.endArray();
                    break;
                default:
                    // Unknown/ignored key
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();

        builder.images(files);

        if (builder.op) {
            // Update OP fields later on the main thread
            Post.Builder op = new Post.Builder();
            op.closed(builder.closed);
            op.archived(builder.archived);
            op.sticky(builder.sticky);
            op.replies(builder.replies);
            op.images(builder.imagesCount);
            op.uniqueIps(builder.uniqueIps);
            op.lastModified(builder.lastModified);
            queue.setOp(op);
        }

        Post cached = queue.getCachedPost(builder.id);
        if (cached != null) {
            // Id is known, use the cached post object.
            queue.addForReuse(cached);
            return;
        }

        queue.addForParse(builder);
    }

    private PostImage readPostImage(JsonReader reader, Post.Builder builder,
                                    SiteEndpoints endpoints) throws IOException {
        reader.beginObject();

        String path = null;
        long fileSize = 0;
        String fileExt = null;
        int fileWidth = 0;
        int fileHeight = 0;
        String fileName = null;
        String thumbnail = null;

        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "path":
                    path = reader.nextString();
                    break;
                case "name":
                    fileName = reader.nextString();
                    break;
                case "size":
                    fileSize = reader.nextLong();
                    break;
                case "width":
                    fileWidth = reader.nextInt();
                    break;
                case "height":
                    fileHeight = reader.nextInt();
                    break;
                case "thumbnail":
                    thumbnail = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();

        if (fileName != null) {
            fileExt = fileName.substring(fileName.lastIndexOf('.') + 1);
            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        }

        if (path != null && fileName != null) {
            Map<String, String> args = makeArgument("path", path, "thumbnail", thumbnail);
            return new PostImage.Builder()
                    .originalName(fileName)
                    .thumbnailUrl(endpoints.thumbnailUrl(builder, false, args))
                    .spoilerThumbnailUrl(endpoints.thumbnailUrl(builder, true, args))
                    .imageUrl(endpoints.imageUrl(builder, args))
                    .filename(Parser.unescapeEntities(fileName, false))
                    .extension(fileExt)
                    .imageWidth(fileWidth)
                    .imageHeight(fileHeight)
                    .size(fileSize)
                    .build();
        }
        return null;
    }
}

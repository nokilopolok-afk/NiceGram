/*
 * This is the source code of NiceGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.messages;

import android.os.Environment;
import android.text.TextUtils;
import com.google.android.exoplayer2.util.Log;
import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.database.AyuData;
import com.radolyn.ayugram.database.dao.DeletedMessageDao;
import com.radolyn.ayugram.database.entities.DeletedMessageFull;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AyuMessagesController {
    public static final String attachmentsSubfolder = "Saved Attachments";
    public static final File attachmentsPath = new File(
            new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), AyuConstants.APP_NAME),
            attachmentsSubfolder
    );
    private static AyuMessagesController instance;
    private final DeletedMessageDao deletedMessageDao;

    private AyuMessagesController() {
        initializeAttachmentsFolder();

        deletedMessageDao = AyuData.getDeletedMessageDao();
    }

    private static void initializeAttachmentsFolder() {
        if (!attachmentsPath.exists()) {
            attachmentsPath.mkdirs();
            try {
                new File(attachmentsPath, ".nomedia").createNewFile();
            } catch (IOException e) {
                // ignored, I hate java
            }
        }
    }

    public static AyuMessagesController getInstance() {
        if (instance == null) {
            instance = new AyuMessagesController();
        }
        return instance;
    }

    public DeletedMessageFull getMessage(long userId, long dialogId, int messageId) {
        return deletedMessageDao.getMessage(userId, dialogId, messageId);
    }

    public List<DeletedMessageFull> getMessages(long userId, long dialogId, long topicId, int startId, int endId, int limit) {
        return deletedMessageDao.getMessages(userId, dialogId, topicId, startId, endId, limit);
    }

    public List<DeletedMessageFull> getMessagesGrouped(long userId, long dialogId, long groupedId) {
        return deletedMessageDao.getMessagesGrouped(userId, dialogId, groupedId);
    }

    public List<DeletedMessageFull> getRecentDeletedMessages(int limit, int offset) {
        return deletedMessageDao.getRecentMessages(limit, offset);
    }

    public void delete(long userId, long dialogId, int messageId) {
        var msg = getMessage(userId, dialogId, messageId);
        if (msg == null) {
            return;
        }

        deletedMessageDao.delete(userId, dialogId, messageId);

        if (!TextUtils.isEmpty(msg.message.mediaPath)) {
            var p = new File(msg.message.mediaPath);
            if (p.exists()) {
                try {
                    p.delete();
                } catch (Exception e) {
                    Log.e("NiceGram", "failed to delete file " + msg.message.mediaPath, e);
                }
            }
        }
    }

    public void clean() {
        AyuData.clean();
        AyuData.create();

        // force to recreate a database to avoid crash
        instance = null;
    }
}

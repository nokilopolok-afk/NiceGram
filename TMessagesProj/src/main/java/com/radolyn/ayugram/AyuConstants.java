/*
 * This is the source code of NiceGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;

public class AyuConstants {
    public static final long[] OFFICIAL_CHANNELS = {
            1905581924, // @ayugramchat
            1794457129, // @ayugram1338
            1434550607, // @radolyn
    };
    public static final long[] DEVS = {
            139303278, // @alexeyzavar
            778327202, // @sharapagorg
            963494570, // @Zanko_no_tachi
            238292700, // @MaxPlays
            1795176335, // @radolyn_services
    };

    public static final int DOCUMENT_TYPE_NONE = 0;
    public static final int DOCUMENT_TYPE_PHOTO = 1;
    public static final int DOCUMENT_TYPE_STICKER = 2;
    public static final int DOCUMENT_TYPE_FILE = 3;

    public static final int OPTION_HISTORY = 1338_01;
    public static final int OPTION_TTL = 1338_02;
    public static final int OPTION_READ_UNTIL = 1338_03;

    public static final int DRAWER_TOGGLE_GHOST = 1000;
    public static final int DRAWER_KILL_APP = 1001;

    public static final int MESSAGE_EDITED_NOTIFICATION = 6968;
    public static final int MESSAGES_DELETED_NOTIFICATION = 6969;
    public static final int AYUSYNC_STATE_CHANGED = 6970;
    public static final int AYUSYNC_LAST_SENT_CHANGED = 6971;
    public static final int AYUSYNC_LAST_RECEIVED_CHANGED = 6972;
    public static final int AYUSYNC_REGISTER_STATUS_CODE_CHANGED = 6973;

    public static String DEFAULT_DELETED_MARK = "🧹";
    public static String DEFAULT_AYUSYNC_SERVER = BuildVars.isBetaApp() ? "ayusync-dev.radolyn.com:5000" : "ayusync.cloud";

    public static String AYU_DATABASE = "ayu-data";

    public static String APP_GITHUB = "NiceGram/NiceGram4A";
    public static String APP_NAME = "NiceGram";

    public static String BUILD_STORE_PACKAGE = "com.android.vending";
    public static String BUILD_ORIGINAL_PACKAGE = "org.telegram.messenger";

    private static final String ADMIN_PREFS = "nexora_admin";

    public static String getChannelUsername() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) return "ayugram1338";
        SharedPreferences prefs = context.getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE);
        String value = prefs.getString("channel_username", "ayugram1338");
        return value == null || value.trim().isEmpty() ? "ayugram1338" : value.trim().replaceFirst("^@", "");
    }

    public static String getChatUsername() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) return "ayugramchat";
        SharedPreferences prefs = context.getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE);
        String value = prefs.getString("chat_username", "ayugramchat");
        return value == null || value.trim().isEmpty() ? "ayugramchat" : value.trim().replaceFirst("^@", "");
    }
}

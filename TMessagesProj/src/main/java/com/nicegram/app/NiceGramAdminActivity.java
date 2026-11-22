package com.nicegram.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

public final class NiceGramAdminActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseAnalytics.getInstance(this).logEvent("nicegram_admin_open", null);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("NiceGram Admin");
        title.setTextSize(24);
        content.addView(title);

        SharedPreferences prefs = getSharedPreferences("nicegram_admin", MODE_PRIVATE);

        TextView channelLabel = new TextView(this);
        channelLabel.setText("Username канала");
        content.addView(channelLabel);
        EditText channelInput = new EditText(this);
        channelInput.setText(prefs.getString("channel_username", "ayugram1338"));
        content.addView(channelInput);

        TextView chatLabel = new TextView(this);
        chatLabel.setText("Username чата");
        content.addView(chatLabel);
        EditText chatInput = new EditText(this);
        chatInput.setText(prefs.getString("chat_username", "ayugramchat"));
        content.addView(chatInput);

        Button saveButton = new Button(this);
        saveButton.setText("Сохранить");
        saveButton.setOnClickListener(v -> {
            String channel = channelInput.getText().toString().trim().replaceFirst("^@", "");
            String chat = chatInput.getText().toString().trim().replaceFirst("^@", "");
            prefs.edit()
                .putString("channel_username", channel.isEmpty() ? "ayugram1338" : channel)
                .putString("chat_username", chat.isEmpty() ? "ayugramchat" : chat)
                .apply();
            Toast.makeText(this, "Изменения сохранены", Toast.LENGTH_SHORT).show();
        });
        content.addView(saveButton);

        TextView stats = new TextView(this);
        stats.setText(
            "Statistics\n\n" +
            "Build: " + org.telegram.messenger.BuildConfig.VERSION_NAME + "\n" +
            "Package: " + getPackageName() + "\n" +
            "Analytics: Firebase enabled\n" +
            "Crash reporting: Firebase enabled\n" +
            "TDLib: connected at runtime\n\n" +
            "Production totals are delivered by Firebase Analytics and Crashlytics."
        );
        stats.setTextSize(16);
        content.addView(stats);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(content);
        setContentView(scrollView);
    }
}

/*
 * This is the source code of NiceGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.database.entities.DeletedMessage;
import com.radolyn.ayugram.database.entities.DeletedMessageFull;
import com.radolyn.ayugram.messages.AyuMessagesController;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DeletedMessagesHistoryActivity extends BaseFragment {
    private static final int PAGE_SIZE = 50;

    private List<DeletedMessageFull> messages = new ArrayList<>();
    private RecyclerListView listView;
    private EmptyTextProgressView emptyView;
    private ListAdapter adapter;
    private boolean loading;
    private boolean reachedEnd;

    private void loadMessages(boolean reset) {
        if (loading) {
            return;
        }
        if (reset) {
            messages = new ArrayList<>();
            reachedEnd = false;
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
        if (reachedEnd) {
            return;
        }

        loading = true;
        List<DeletedMessageFull> page = AyuMessagesController.getInstance()
                .getRecentDeletedMessages(PAGE_SIZE, messages.size());
        int start = messages.size();
        messages.addAll(page);
        reachedEnd = page.size() < PAGE_SIZE;
        loading = false;
        if (adapter != null) {
            adapter.notifyItemRangeInserted(start, page.size());
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.DeletedMessagesHistory));
        actionBar.setActionBarMenuOnItemClick(new org.telegram.ui.ActionBar.ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setVerticalScrollBarEnabled(true);
        adapter = new ListAdapter(context);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((view, position) -> showMessage(position));
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && !reachedEnd && !loading
                        && layoutManagerCanLoadMore(recyclerView)) {
                    loadMessages(false);
                }
            }
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        emptyView = new EmptyTextProgressView(context);
        emptyView.setText(LocaleController.getString(R.string.DeletedMessagesHistoryEmpty));
        emptyView.showTextView();
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setEmptyView(emptyView);

        fragmentView = frameLayout;
        loadMessages(false);
        return fragmentView;
    }

    private boolean layoutManagerCanLoadMore(RecyclerView recyclerView) {
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (!(manager instanceof LinearLayoutManager)) {
            return false;
        }
        LinearLayoutManager layoutManager = (LinearLayoutManager) manager;
        return layoutManager.findLastVisibleItemPosition() >= messages.size() - 5;
    }

    private void showMessage(int position) {
        if (position < 0 || position >= messages.size() || getParentActivity() == null) {
            return;
        }

        DeletedMessageFull full = messages.get(position);
        if (full == null || full.message == null) {
            return;
        }

        File attachment = full.message.mediaPath == null ? null : new File(full.message.mediaPath);
        String text = full.message.text;
        if (text == null || text.trim().isEmpty()) {
            if (attachment == null) {
                text = LocaleController.getString(R.string.DeletedMessageNoText);
            } else if (attachment.exists()) {
                text = LocaleController.getString(R.string.DeletedMessageAttachment);
            } else {
                text = LocaleController.getString(R.string.DeletedMessageAttachmentMissing);
            }
        }
        String details = LocaleController.formatDateChat(full.message.entityCreateDate)
                + "\n#"
                + full.message.messageId
                + "\n\n"
                + text;

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity())
                .setTitle(getChatTitle(full.message))
                .setMessage(details)
                .setPositiveButton(LocaleController.getString(R.string.OK), null);
        if (attachment != null && attachment.exists()) {
            builder.setNeutralButton(LocaleController.getString("Open", R.string.Open),
                    (dialog, which) -> {
                        if (!AndroidUtilities.openForView(
                                attachment,
                                attachment.getName(),
                                full.message.mimeType,
                                getParentActivity(),
                                null
                        )) {
                            Toast.makeText(
                                    getParentActivity(),
                                    LocaleController.getString(R.string.DeletedMessageAttachmentMissing),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
        }
        builder.show();
    }

    private String getChatTitle(DeletedMessage message) {
        long dialogId = message.dialogId;
        for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
            if (UserConfig.getInstance(account).getClientUserId() != message.userId) {
                continue;
            }
            var peer = MessagesController.getInstance(account).getUserOrChat(dialogId);
            if (peer instanceof org.telegram.tgnet.TLRPC.User) {
                String name = UserObject.getUserName((org.telegram.tgnet.TLRPC.User) peer);
                if (!name.isEmpty()) {
                    return name;
                }
            } else if (peer instanceof org.telegram.tgnet.TLRPC.Chat) {
                String title = ((org.telegram.tgnet.TLRPC.Chat) peer).title;
                if (title != null && !title.isEmpty()) {
                    return title;
                }
            }
        }
        return String.valueOf(dialogId);
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        return new ArrayList<>();
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context context;

        ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextCell cell = new TextCell(context);
            cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            DeletedMessageFull full = messages.get(position);
            if (full == null || full.message == null) {
                return;
            }

            String chatTitle = getChatTitle(full.message);
            String messageTitle = chatTitle + " · #" + full.message.messageId;
            String preview = full.message.text;
            if (preview == null || preview.trim().isEmpty()) {
                File attachment = full.message.mediaPath == null ? null : new File(full.message.mediaPath);
                if (attachment == null) {
                    preview = LocaleController.getString(R.string.DeletedMessageNoText);
                } else if (attachment.exists()) {
                    preview = LocaleController.getString(R.string.DeletedMessageAttachment);
                } else {
                    preview = LocaleController.getString(R.string.DeletedMessageAttachmentMissing);
                }
            }
            String subtitle = LocaleController.formatDateChat(full.message.entityCreateDate) + " · " + preview;

            TextCell cell = (TextCell) holder.itemView;
            cell.setText(messageTitle);
            cell.setSubtitle(subtitle);
            cell.setNeedDivider(position < messages.size() - 1);
        }
    }
}

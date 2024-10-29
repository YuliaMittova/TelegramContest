package org.telegram.ui.Components;

import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import androidx.collection.LongSparseArray;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.BottomSheet;

public class InviteLinkQRCodeBottomSheet extends BottomSheet {

    private final TextView linkTextView;
    private boolean isLinkGenerating;
    private String lastUrl;

    public InviteLinkQRCodeBottomSheet(Context context, long chatId) {
        super(context, false);
        fixNavigationBar();
        final TLRPC.Chat currentChat = MessagesController.getInstance(currentAccount).getChat(chatId);
        final TLRPC.ChatFull chatInfo = MessagesController.getInstance(currentAccount).getChatFull(chatId);

        lastUrl = getLink(currentChat, chatInfo);
        if (lastUrl == null) {
            generateLinkAndRunAction(chatId, chatInfo, null);
        }

        final LayoutInflater inflater = LayoutInflater.from(context);
        final View customView = inflater.inflate(R.layout.invite_link_bottom_sheet, null);
        setTextViewTint(context, customView);
        setCustomView(customView);

        linkTextView = customView.findViewById(R.id.inviteLinkBottomSheet_linkText);
        linkTextView.setText(lastUrl);

        final View qrView = customView.findViewById(R.id.inviteLinkBottomSheet_qrIcon);
        qrView.setOnClickListener(v -> {
            QRCodeBottomSheet qrCodeBottomSheet = new QRCodeBottomSheet(
                    getContext(),
                    LocaleController.getString(R.string.InviteByQRCode),
                    lastUrl,
                    LocaleController.getString(R.string.QRCodeLinkHelpChannel),
                    false
            );
            qrCodeBottomSheet.setCenterAnimation(R.raw.qr_code_logo);
            qrCodeBottomSheet.show();
        });

        final View copyButton = customView.findViewById(R.id.inviteLinkBottomSheet_copyButtonContainer);
        copyButton.setOnClickListener(v -> {
            lastUrl = getLink(currentChat, chatInfo);
            if (lastUrl == null) {
                generateLinkAndRunAction(chatId, chatInfo, InviteLinkAction.COPY);
            } else {
                onCopyLinkClicked();
            }
        });

        final View shareButton = customView.findViewById(R.id.inviteLinkBottomSheet_shareButtonContainer);
        shareButton.setOnClickListener(v -> {
            lastUrl = getLink(currentChat, chatInfo);
            if (lastUrl == null) {
                generateLinkAndRunAction(chatId, chatInfo, InviteLinkAction.SHARE);
            } else {
                onShareLinkClicked();
            }
        });

        final View manageLinksButton = customView.findViewById(R.id.inviteLinkBottomSheet_manageInviteLinks);
        manageLinksButton.setOnClickListener(v -> {
            lastUrl = getLink(currentChat, chatInfo);
            if (lastUrl == null) {
                generateLinkAndRunAction(chatId, chatInfo, InviteLinkAction.MANAGE_LINKS);
            } else {
                onManageLinksClicked(chatId, chatInfo);
            }
        });
    }

    private String getLink(final TLRPC.Chat chat, final TLRPC.ChatFull chatInfo) {
        final String username;
        if (chat != null && !TextUtils.isEmpty(username = ChatObject.getPublicUsername(chat))) {
            return "https://" + MessagesController.getInstance(currentAccount).linkPrefix + "/" + username;
        } else if (chatInfo != null && chatInfo.exported_invite != null) {
            return chatInfo.exported_invite.link;
        } else {
            return null;
        }
    }

    private void updateLinkTextView() {
        linkTextView.setText(lastUrl);
    }

    private void onCopyLinkClicked() {
        if (lastUrl == null) return;

        final ClipboardManager clipboard = (ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clip = ClipData.newPlainText("label", lastUrl);
        clipboard.setPrimaryClip(clip);
        dismiss();
        BulletinFactory.createCopyLinkBulletin(attachedFragment).show();
    }

    private void onManageLinksClicked(final long chatId, final TLRPC.ChatFull chatInfo) {
//        Need to clarify if this is a correct thing to call
//            ManageLinksActivity fragment = new ManageLinksActivity(chatId, 0, 0);
//            fragment.setInfo(chatInfo, chatInfo.exported_invite);
//            attachedFragment.presentFragment(fragment);
    }

    private void onShareLinkClicked() {
        try {
            if (lastUrl == null) return;
            attachedFragment.showDialog(new ShareAlert(getContext(), null, lastUrl, false, lastUrl, false, attachedFragment.getResourceProvider()) {
                @Override
                protected void onSend(LongSparseArray<TLRPC.Dialog> dids, int count, TLRPC.TL_forumTopic topic) {
                    final String str;
                    if (dids != null && dids.size() == 1) {
                        long did = dids.valueAt(0).id;
                        if (did == 0 || did == UserConfig.getInstance(currentAccount).getClientUserId()) {
                            str = getString(R.string.InvLinkToSavedMessages);
                        } else {
                            str = formatString(R.string.InvLinkToUser, MessagesController.getInstance(currentAccount).getPeerName(did, true));
                        }
                    } else {
                        str = formatString(R.string.InvLinkToChats, LocaleController.formatPluralString("Chats", count));
                    }
                    showBulletin(R.raw.forward, AndroidUtilities.replaceTags(str));
                }
            });
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void setTextViewTint(final Context context, final View customView) {
        final Drawable qrCodeDrawable = ContextCompat.getDrawable(context, R.drawable.msg_qrcode);
        if (qrCodeDrawable != null) {
            final int color = ContextCompat.getColor(context, R.color.invite_link_qr_tint);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                DrawableCompat.setTint(qrCodeDrawable, color);
            } else {
                qrCodeDrawable.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
            ((ImageView) customView.findViewById(R.id.inviteLinkBottomSheet_qrIcon)).setImageDrawable(qrCodeDrawable);
        }
    }

    private void generateLinkAndRunAction(long chatId, final TLRPC.ChatFull chatInfo, InviteLinkAction action) {
        if (isLinkGenerating) return;
        isLinkGenerating = true;
        final TLRPC.TL_messages_exportChatInvite req = new TLRPC.TL_messages_exportChatInvite();
        req.legacy_revoke_permanent = true;
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(-chatId);
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                TLRPC.TL_chatInviteExported invite = (TLRPC.TL_chatInviteExported) response;
                if (chatInfo != null) {
                    chatInfo.exported_invite = invite;
                }

                if (invite.link == null) return;
                lastUrl = invite.link;
                updateLinkTextView();
                if (action == null) return;
                switch (action) {
                    case COPY: {
                        onCopyLinkClicked();
                        break;
                    }
                    case SHARE: {
                        onShareLinkClicked();
                        break;
                    }
                    case MANAGE_LINKS: {
                        onManageLinksClicked(chatId, chatInfo);
                        break;
                    }
                }
            }
            isLinkGenerating = false;
        }));
    }

    private void showBulletin(int resId, CharSequence str) {
        Bulletin b = BulletinFactory.of(attachedFragment).createSimpleBulletin(resId, str);
        b.hideAfterBottomSheet = false;
        b.show(true);
    }

    private enum InviteLinkAction {
        COPY,
        SHARE,
        MANAGE_LINKS
    }
}

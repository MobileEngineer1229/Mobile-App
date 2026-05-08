package com.smarthome.iot.ui.dialogs;

import android.app.Activity;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.smarthome.iot.R;
import com.smarthome.iot.ui.adapters.ShareContactAdapter;
import com.smarthome.iot.ui.adapters.ShareSocialAdapter;

import java.util.ArrayList;
import java.util.List;

public class ShareDialog {
    private BottomSheetDialog dialog;
    private String invitationCode;
    private String role;

    public ShareDialog(Activity activity, String invitationCode, String role) {
        this.invitationCode = invitationCode;
        this.role = role;
        
        View bottomSheetView = activity.getLayoutInflater().inflate(R.layout.bottom_sheet_share, null);
        dialog = new BottomSheetDialog(activity);
        dialog.setContentView(bottomSheetView);
        
        RecyclerView recyclerViewRecentPeople = bottomSheetView.findViewById(R.id.recyclerViewRecentPeople);
        RecyclerView recyclerViewSocialMedia = bottomSheetView.findViewById(R.id.recyclerViewSocialMedia);
        
        // Setup recent people
        List<ShareContactAdapter.ContactItem> recentContacts = new ArrayList<>();
        recentContacts.add(new ShareContactAdapter.ContactItem("Jenny Wilson", R.drawable.ic_whatsapp));
        recentContacts.add(new ShareContactAdapter.ContactItem("Kristin Watson", R.drawable.ic_facebook));
        recentContacts.add(new ShareContactAdapter.ContactItem("Clinton McClure", R.drawable.ic_instagram));
        recentContacts.add(new ShareContactAdapter.ContactItem("Sarah Wilona", R.drawable.ic_whatsapp));
        
        ShareContactAdapter contactAdapter = new ShareContactAdapter(recentContacts, contact -> {
            // TODO: Share via contact
            dialog.dismiss();
        });
        recyclerViewRecentPeople.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewRecentPeople.setAdapter(contactAdapter);
        
        // Setup social media
        List<ShareSocialAdapter.SocialItem> socialApps = new ArrayList<>();
        socialApps.add(new ShareSocialAdapter.SocialItem("WhatsApp", R.drawable.ic_whatsapp));
        socialApps.add(new ShareSocialAdapter.SocialItem("Facebook", R.drawable.ic_facebook));
        socialApps.add(new ShareSocialAdapter.SocialItem("Instagram", R.drawable.ic_instagram));
        socialApps.add(new ShareSocialAdapter.SocialItem("Telegram", R.drawable.ic_telegram));
        socialApps.add(new ShareSocialAdapter.SocialItem("Twitter", R.drawable.ic_twitter));
        
        ShareSocialAdapter socialAdapter = new ShareSocialAdapter(socialApps, app -> {
            // TODO: Share via social media
            dialog.dismiss();
        });
        recyclerViewSocialMedia.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewSocialMedia.setAdapter(socialAdapter);
    }

    public void show() {
        if (dialog != null) {
            dialog.show();
        }
    }
}

package com.example.premiereversion;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NotificationAdapter extends ArrayAdapter<NotificationItem> {

    private final Context context;
    private final ArrayList<NotificationItem> notifications;

    public NotificationAdapter(Context context, ArrayList<NotificationItem> notifications) {
        super(context, 0, notifications);
        this.context = context;
        this.notifications = notifications;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        NotificationItem notif = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.notification_text);
        Button deleteBtn = convertView.findViewById(R.id.btn_delete_notification);

        textView.setText(notif.contenu);

        deleteBtn.setOnClickListener(v -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://backend-g7pr.onrender.com/notifications/delete/" + notif.id)
                    .delete()
                    .build();

            new Thread(() -> {
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        ((Activity) context).runOnUiThread(() -> {
                            notifications.remove(position);
                            notifyDataSetChanged();
                            Toast.makeText(context, "Notification supprimée ✅", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        ((Activity) context).runOnUiThread(() ->
                                Toast.makeText(context, "Erreur suppression", Toast.LENGTH_SHORT).show());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    ((Activity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Erreur réseau", Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        return convertView;
    }
}

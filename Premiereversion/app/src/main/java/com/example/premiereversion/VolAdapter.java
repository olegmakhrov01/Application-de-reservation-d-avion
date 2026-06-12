package com.example.premiereversion;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class VolAdapter extends ArrayAdapter<Vol> {

    private final Context context;
    private final ArrayList<Vol> vols;
    private final String userId;

    public VolAdapter(Context context, ArrayList<Vol> vols, String userId) {
        super(context, 0, vols);
        this.context = context;
        this.vols = vols;
        this.userId = userId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Récupère le vol à cette position
        Vol vol = getItem(position);

        // Réutilise une vue ou en crée une nouvelle si nécessaire
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_vol, parent, false);
        }

        // Remplit les TextViews avec les infos du vol
        TextView textDate = convertView.findViewById(R.id.textDate);
        TextView textDepart = convertView.findViewById(R.id.textDepart);
        TextView textDestination = convertView.findViewById(R.id.textDestination);
        TextView textPrix = convertView.findViewById(R.id.textPrix);
        Button reserverButton = convertView.findViewById(R.id.buttonReserver);

        try {
            String rawDate = vol.getDate(); // Exemple : 2025-04-10T04:00:00.000Z
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            Date date = inputFormat.parse(rawDate);
            String formattedDate = outputFormat.format(date);
            ((TextView) convertView.findViewById(R.id.textDate)).setText("Date : " + formattedDate);
        } catch (Exception e) {
            ((TextView) convertView.findViewById(R.id.textDate)).setText("Date : " + vol.getDate()); // fallback
        }
        textDepart.setText("Départ : " + vol.getDepart());
        textDestination.setText("Destination : " + vol.getDestination());
        textPrix.setText("Prix : " + vol.getPrix() + " $");

        // Gère le clic sur le bouton Réserver
        reserverButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, Reservation.class);
            intent.putExtra("vol_id", vol.getId());
            intent.putExtra("id_utilisateurs", userId);
            context.startActivity(intent);
        });

        return convertView;
    }
}

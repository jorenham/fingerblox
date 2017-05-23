package org.fingerblox.fingerblox;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class MatchAdapter extends ArrayAdapter<Pair<String, Integer>> {
    public MatchAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public MatchAdapter(Context context, int resource, List<Pair<String, Integer>> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.match_row_item, null);
        }

        Pair<String, Integer> p = getItem(position);

        if (p != null) {
            TextView matchNameTextView = (TextView) v.findViewById(R.id.match_name);
            TextView matchPercentageTextView = (TextView) v.findViewById(R.id.match_percentage);

            if (matchNameTextView != null) {
                matchNameTextView.setText(p.first);
            }

            if (matchPercentageTextView != null) {
                matchPercentageTextView.setText(p.second + "%");
            }
        }

        return v;
    }

}
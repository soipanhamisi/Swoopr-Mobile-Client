package com.example.carpoolclient;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AutocompleteAdapter extends ArrayAdapter<AutocompletePrediction> implements Filterable {

    private static final String TAG = "AutocompleteAdapter";
    private final PlacesClient placesClient;
    private List<AutocompletePrediction> resultList = new ArrayList<>();

    public AutocompleteAdapter(Context context, PlacesClient placesClient) {
        super(context, R.layout.item_suggestion);
        this.placesClient = placesClient;
    }

    @Override
    public int getCount() {
        return Math.min(resultList.size(), 2); 
    }

    @Nullable
    @Override
    public AutocompletePrediction getItem(int position) {
        if (position >= 0 && position < resultList.size()) {
            return resultList.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_suggestion, parent, false);
        }
        AutocompletePrediction item = getItem(position);
        if (item != null) {
            TextView textView = convertView.findViewById(R.id.tv_suggestion_name);
            textView.setText(item.getFullText(null));
            
            View divider = convertView.findViewById(R.id.suggestion_divider);
            // Show divider only if it's the first item and there's a second one
            if (position == 0 && resultList.size() > 1) {
                divider.setVisibility(View.VISIBLE);
            } else {
                divider.setVisibility(View.GONE);
            }
        }
        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                if (constraint != null && constraint.length() > 0) {
                    List<AutocompletePrediction> predictions = getPredictions(constraint);
                    results.values = predictions;
                    results.count = predictions.size();
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    resultList = (List<AutocompletePrediction>) results.values;
                    notifyDataSetChanged();
                } else {
                    resultList = new ArrayList<>();
                    notifyDataSetInvalidated();
                }
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                if (resultValue instanceof AutocompletePrediction) {
                    return ((AutocompletePrediction) resultValue).getFullText(null);
                }
                return super.convertResultToString(resultValue);
            }
        };
    }

    private List<AutocompletePrediction> getPredictions(CharSequence constraint) {
        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(constraint.toString())
                .setCountries("KE")
                .build();

        Task<com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse> task = placesClient.findAutocompletePredictions(request);

        try {
            Tasks.await(task, 5, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            Log.e(TAG, "Error fetching predictions", e);
            if (e.getCause() instanceof ApiException) {
                ApiException apiException = (ApiException) e.getCause();
                Log.e(TAG, "Places API Error code: " + apiException.getStatusCode());
            }
        }

        if (task.isSuccessful() && task.getResult() != null) {
            return task.getResult().getAutocompletePredictions();
        } else {
            return new ArrayList<>();
        }
    }
}

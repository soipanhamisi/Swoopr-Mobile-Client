package com.example.carpoolclient;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carpoolclient.dtos.VehicleDto;

import java.util.List;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {

    private final List<VehicleDto> vehicles;
    private final OnVehicleSelectedListener listener;
    private int selectedPosition = -1;

    public interface OnVehicleSelectedListener {
        void onVehicleSelected(VehicleDto vehicle);
    }

    public VehicleAdapter(List<VehicleDto> vehicles, OnVehicleSelectedListener listener) {
        this.vehicles = vehicles;
        this.listener = listener;
        // Initially no vehicle is selected to keep them both dark
        selectedPosition = -1;
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_registered_car, parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        VehicleDto vehicle = vehicles.get(position);
        holder.tvName.setText(vehicle.getDesc());
        holder.tvPlate.setText(vehicle.getRegNo());

        // Dark for unselected, bright for selected
        if (selectedPosition == position) {
            holder.itemView.setBackgroundResource(R.drawable.vehicle_card_background); // Bright green
            holder.tvName.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
            holder.tvPlate.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.black));
            holder.itemView.setElevation(8f);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.vehicle_card_unselected); // Dark surface
            holder.tvName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.earthy_dark_text_primary));
            holder.tvPlate.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.earthy_dark_text_secondary));
            holder.itemView.setElevation(2f);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            listener.onVehicleSelected(vehicle);
        });
    }

    @Override
    public int getItemCount() {
        return vehicles.size();
    }

    public void setSelectedVehicle(VehicleDto vehicle) {
        for (int i = 0; i < vehicles.size(); i++) {
            if (vehicles.get(i).getRegNo().equals(vehicle.getRegNo())) {
                int prev = selectedPosition;
                selectedPosition = i;
                notifyItemChanged(prev);
                notifyItemChanged(selectedPosition);
                break;
            }
        }
    }

    static class VehicleViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPlate;

        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_vehicle_name);
            tvPlate = itemView.findViewById(R.id.tv_vehicle_plate);
        }
    }
}

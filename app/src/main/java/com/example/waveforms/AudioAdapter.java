package com.example.waveforms;

import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;

import java.util.List;

// Adapter class for audio files
public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.FileViewHolder> {
    // This class is used to display the audio files in a list
    private List<AudioModel> fileList;
    // Interface for handling click events
    private static ClickListener onClick;

    public AudioAdapter(List<AudioModel> fileList, ClickListener clickListener) {
        this.fileList = fileList;
        this.onClick = clickListener;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.audio_item, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        AudioModel file = fileList.get(position);
        holder.fileName.setText("Recording " + (position + 1));
        holder.audioLength.setText(file.getDuration());
        holder.audioDate.setText(file.getDate());
        holder.audioExtension.setText(file.getExtension());
        if (!(file.getExtension().equals("mp3") || file.getExtension().equals("wav"))) {
            holder.playIcon.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public void handlePlayIcon(View view, boolean isPlaying) {
        // This method is used to change the play icon to pause icon and vice versa
        ImageView playIcon = view.findViewById(R.id.PlayIcon);
        if(isPlaying) {
            playIcon.setImageResource(R.drawable.pause_icon);
        } else {
            playIcon.setImageResource(R.drawable.play_icon);
        }
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{
        public TextView fileName;
        public TextView audioExtension;
        public TextView audioDate;
        public TextView audioLength;
        public ImageView playIcon;

        public FileViewHolder(View view) {
            super(view);
            fileName = view.findViewById(R.id.FileName);
            audioExtension = view.findViewById(R.id.AudioExtension);
            audioDate = view.findViewById(R.id.AudioDate);
            audioLength = view.findViewById(R.id.AudioDuration);
            playIcon = view.findViewById(R.id.PlayIcon);
            playIcon.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        public void changeIcon(int id) {
            playIcon.setImageResource(id);
        }

        @Override
        public void onClick(View v) {
            int position = this.getAdapterPosition();
            if (position >= 0) {
                onClick.onItemClick(position, v);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            int position = this.getAdapterPosition();
            if (position >= 0) {
                onClick.onItemLongClick(position, v);
                return true;
            }
            return false;
        }
    }

    public interface ClickListener {
        void onItemClick(int position, View v);
        void onItemLongClick(int position, View v);
    }
}

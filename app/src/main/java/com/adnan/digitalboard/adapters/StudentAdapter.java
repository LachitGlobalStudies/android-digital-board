package com.adnan.digitalboard.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.adnan.digitalboard.R;
import com.adnan.digitalboard.models.StudentModel;

import org.webrtc.EglBase;
import org.webrtc.SurfaceViewRenderer;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private List<StudentModel> studentList;
    private EglBase.Context eglContext;
    private OnStudentClickListener listener;

    public interface OnStudentClickListener {
        void onMuteToggle(StudentModel student, boolean shouldMute);
        void onVideoClick(StudentModel student);
    }

    public StudentAdapter(List<StudentModel> studentList, EglBase.Context eglContext, OnStudentClickListener listener) {
        this.studentList = studentList;
        this.eglContext = eglContext;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        StudentModel student = studentList.get(position);
        holder.txtStudentName.setText(student.getStudentName());

        // Initialize WebRTC Small Video Renderer
        holder.studentSmallVideo.init(eglContext, null);
        if (student.getVideoTrack() != null) {
            student.getVideoTrack().addSink(holder.studentSmallVideo);
        }

        // Mute / Unmute Button Logic
        holder.btnMuteAudio.setOnClickListener(v -> {
            boolean newMuteState = !student.isMuted();
            student.setMuted(newMuteState);
            holder.btnMuteAudio.setImageResource(
                    newMuteState ? android.R.drawable.ic_lock_silent_mode_off : android.R.drawable.ic_btn_speak_now
            );
            listener.onMuteToggle(student, newMuteState);
        });

        // Click on video to expand to 90% screen
        holder.studentSmallVideo.setOnClickListener(v -> listener.onVideoClick(student));
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView txtStudentName;
        SurfaceViewRenderer studentSmallVideo;
        ImageButton btnMuteAudio;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            txtStudentName = itemView.findViewById(R.id.txtStudentName);
            studentSmallVideo = itemView.findViewById(R.id.studentSmallVideo);
            btnMuteAudio = itemView.findViewById(R.id.btnMuteAudio);
        }
    }
}
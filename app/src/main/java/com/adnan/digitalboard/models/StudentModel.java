package com.adnan.digitalboard.models;

import org.webrtc.VideoTrack;

public class StudentModel {
    private String studentId;
    private String studentName;
    private boolean isMuted;
    private VideoTrack videoTrack;

    public StudentModel(String studentId, String studentName, VideoTrack videoTrack) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.videoTrack = videoTrack;
        this.isMuted = true; // Default muted
    }

    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public boolean isMuted() { return isMuted; }
    public void setMuted(boolean muted) { isMuted = muted; }
    public VideoTrack getVideoTrack() { return videoTrack; }

    // ✅ VideoTrack আপডেট করার জন্য এই মেথডটি যুক্ত করুন
    public void setVideoTrack(VideoTrack videoTrack) {
        this.videoTrack = videoTrack;
    }
}
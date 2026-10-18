package com.adnan.digitalboard.utils;

import org.webrtc.VideoTrack;
import java.util.HashMap;
import java.util.Map;

public class StudentTrackManager {
    private static final Map<String, VideoTrack> trackMap = new HashMap<>();

    public static void addTrack(String studentId, VideoTrack track) {
        trackMap.put(studentId, track);
    }

    public static VideoTrack getTrack(String studentId) {
        return trackMap.get(studentId);
    }
}
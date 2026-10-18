package com.adnan.digitalboard.view;

import android.graphics.Paint;
import android.graphics.Path;

public class Stroke {
    Path path;
    Paint paint;

    public Stroke(Path path, Paint paint) {
        this.path = path;
        this.paint = paint;
    }
}

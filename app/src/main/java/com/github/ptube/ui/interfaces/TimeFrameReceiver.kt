package com.github.ptube.ui.interfaces

import android.graphics.Bitmap

abstract class TimeFrameReceiver {
    abstract suspend fun getFrameAtTime(position: Long): Bitmap?
}

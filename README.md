# SyncCamera
Android 29+ API joint color and depth camera implementation. Requires a back facing camera on the device with depth capabilities. Both streams (depth and color) are returned through ARGB_8888 bitmaps.

The depth image and color image streams should be aligned through camera calibration parameters. Camera parameters are written as metadata to internal storage.

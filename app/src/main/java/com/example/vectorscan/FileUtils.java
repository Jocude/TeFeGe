package com.example.vectorscan;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.widget.*;
import androidx.activity.result.*;
import androidx.core.content.*;
import java.io.*;
import java.text.*;
import java.util.*;
import okhttp3.*;
public class FileUtils {

    public static String getPath(Context context, Uri uri) {

        String[] projection = {MediaStore.Images.Media.DATA};

        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {

            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

            cursor.moveToFirst();

            String path = cursor.getString(columnIndex);

            cursor.close();

            return path;
        }

        return null;
    }
}

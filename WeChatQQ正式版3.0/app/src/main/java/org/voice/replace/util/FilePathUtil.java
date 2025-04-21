package org.voice.replace.util;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

public class FilePathUtil {

    private static final String TAG = "FilePathUtil";

	private Context context;
	
	public FilePathUtil(Context context) {
        this.context = context;
    }
    /**
     * 获取真实文件路径，兼容所有安卓版本
     *
     * @param context 上下文
     * @param uri     文件 Uri
     * @return 文件绝对路径（null 表示获取失败）
     */
    @SuppressLint("NewApi")
    public static String getPath(Context context, Uri uri) {
        try {
            // Android 4.4 (KITKAT) 及以上，URI 通过 DocumentProvider
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
                if (isExternalStorageDocument(uri)) {
                    // ExternalStorageProvider: 对外部存储文件
                    final String[] docId = DocumentsContract.getDocumentId(uri).split(":");
                    final String type = docId[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + docId[1];
                    } else {
                        return "/storage/" + type + "/" + docId[1]; // 非主存储
                    }
                } else if (isDownloadsDocument(uri)) {
                    // DownloadsProvider: 对下载类文件
                    final String id = DocumentsContract.getDocumentId(uri);
                    if (id.startsWith("raw:")) { // 如果直接返回路径
                        return id.replaceFirst("raw:", "");
                    }

                    try {
                        Uri contentUri = ContentUris.withAppendedId(
							Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
                        return getDataColumn(context, contentUri, null, null);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "DownloadsProvider format error: " + e.getMessage());
                    }
                } else if (isMediaDocument(uri)) {
                    // MediaProvider: 对图片、视频等媒体类文件
                    final String[] docId = DocumentsContract.getDocumentId(uri).split(":");
                    final String type = docId[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{docId[1]};
                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                // content:// URIs, 应用内容提供者
                return getDataColumn(context, uri, null, null);
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                // file:// URIs，直接返回路径
                return uri.getPath();
            }
        } catch (Exception e) {
            Log.e(TAG, "getPath error: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 通过内容提供者获取数据列
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndex(column);
                // 如果 '_data' 字段为空，表示无法直接解析路径
                if (index != -1) {
                    return cursor.getString(index);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getDataColumn error: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * 判断是否为 ExternalStorageProvider
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * 判断是否为 DownloadsProvider
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * 判断是否为 MediaProvider
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}


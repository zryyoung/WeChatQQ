"ui";
let fileType = "*/*";
let requestCode = 5;
var intent = new Intent();
intent.setType(fileType);
intent.setAction(Intent.ACTION_GET_CONTENT);
activity.startActivityForResult(intent, requestCode);
activity.getEventEmitter().on("activity_result", (requestCode, resultCode, data) => {
    if (activity) {
        ui.run(() => ui.finish());
    }
    if (resultCode != -1) {
        toastLog("取消");
        return false;
    } else {
        shell("su -c am start -n com.tencent.mm/.ui.LauncherUI", true);
        var uri = data.getData();
        log("uri: %s", uri.toString());
        let filePath = uriToFile(uri);
        log(filePath);
        if (isAudioFile(filePath)) {
            log("选择的音频文件路径: " + filePath);
            // 确保在主线程中执行 UI 操作
            setTimeout(function() {
                main(filePath);
            }, 100);
        } else {
            toast("请选择正确的音频文件！");
        }
    }

});

function isAudioFile(filePath) {
    return filePath && /\.(mp3|ogg|flac|wav|m4a|aac|amr|slk)$/i.test(filePath);
}


function uriToFile(uri) {
    var r = null,
        cursor,
        column_index,
        selection = null,
        selectionArgs = null,
        isKitKat = android.os.Build.VERSION.SDK_INT >= 19,
        docs;
    if (uri.getScheme().equalsIgnoreCase("content")) {
        if (isKitKat && android.provider.DocumentsContract.isDocumentUri(activity, uri)) {
            if (String(uri.getAuthority()) == "com.android.externalstorage.documents") {
                docs = String(android.provider.DocumentsContract.getDocumentId(uri)).split(":");
                if (docs[0] == "primary") {
                    return android.os.Environment.getExternalStorageDirectory() + "/" + docs[1];
                }
            } else if (String(uri.getAuthority()) == "com.android.providers.downloads.documents") {
                uri = android.content.ContentUris.withAppendedId(
                    android.net.Uri.parse("content://downloads/public_downloads"),
                    parseInt(android.provider.DocumentsContract.getDocumentId(uri))
                );
            } else if (String(uri.getAuthority()) == "com.android.providers.media.documents") {
                docs = String(android.provider.DocumentsContract.getDocumentId(uri)).split(":");
                if (docs[0] == "image") {
                    uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if (docs[0] == "video") {
                    uri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if (docs[0] == "audio") {
                    uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = [docs[1]];
            }
        }
        try {
            cursor = activity.getContentResolver().query(uri, ["_data"], selection, selectionArgs, null);
            if (cursor && cursor.moveToFirst()) {
                r = String(cursor.getString(cursor.getColumnIndexOrThrow("_data")));
            }
        } catch (e) {
            log(e);
        }
        if (cursor) cursor.close();
        return r;
    } else if (uri.getScheme().equalsIgnoreCase("file")) {
        return String(uri.getPath());
    }
    return null;
}


function main(musicFilePath) {
    let storage = storages.create("config");
    let savedWxFolder = storage.get("wxFolder", "");
    //let musicFilePath = "/storage/emulated/0/Music/1732439194786.mp3";
    //let musicFilePath = "/storage/emulated/0/Music/Ed Sheeran - Shape of You.flac";

    //let cmd = "/data/data/com.termux/files/usr/bin/bash " +
    //    files.cwd() + "/Python/replace_voice.sh " +
    //    savedQQ + " " + musicFilePath;

    let cmd = "/data/data/com.termux/files/usr/bin/bash " +
        files.cwd() + "/Python/replace_wechat_voice.sh " +
        savedWxFolder +
        " \"" + musicFilePath + "\"";
    //toastLog("执行命令: " + cmd);
    // 执行命令
    let result = shell("su -c 'nohup "+ cmd +"'", true);
    //toastLog(result);
    // 判断返回的 `code`
    if (result.code === 0) {
        //ui.post(function() {
        ui.run(function() {
            toastLog("✅ 语音替换成功！");
        });
        //});
    } else {
        ui.run(function() {
            toastLog("❌ 语音替换失败 (code=" + result.code + "): " + result.error);
        });
    }
    shell("su -c am start -n com.tencent.mm/.ui.LauncherUI", true);
}
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
            //setTimeout(function() {
            //    main(filePath);
            //}, 200);
            let scriptCode =
                "var filePath = \"" + filePath.replace(/\\/g, "\\\\").replace(/"/g, "\\\"") + "\";\n" +
                main.toString() + "\n" +
                "main(filePath);";

            engines.execScript("微信语音替换执行引擎", scriptCode);
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
    if (!savedWxFolder) {
        toastLog("❌ 未配置微信目录");
        return;
    }

    let prefix = "/data/data/com.termux/files/usr/bin";
    let ffmpeg = prefix + "/ffmpeg";

    let amrFilePath = musicFilePath.endsWith(".amr") ?
        musicFilePath :
        musicFilePath.replace(/\.\w+$/, ".amr");

    if (!files.exists(amrFilePath)) {
        toastLog("▶️ 正在转换为 AMR ...");

        let convertCmd = "su -c '" + ffmpeg + " -y -i \"" + musicFilePath + "\" -ar 8000 -ac 1 -b:a 12.2k \"" + amrFilePath + "\" > /dev/null 2>&1'"; //> /dev/null 2>&1'"
        log(convertCmd)

        threads.start(function() {
            shell(convertCmd, true);
        })
        // 设置超时时间（以秒为单位）
        let timeout = 10; // 设置超时为 30 秒
        let interval = 2000; // 每次检查的间隔时间为 1 秒
        let elapsed = 0;
        while (elapsed < timeout * 1000) { // 将超时转换为毫秒
            if (files.exists(amrFilePath)) {
                //toastLog("✅ 转换成功！");
                break;
            }
            sleep(interval); // 等待 1 秒后重试
            elapsed += interval; // 更新已耗时的总时间
        }

        if (elapsed >= timeout * 1000) {
            toastLog("❌ AMR 转换失败，超时未找到文件！");
        }

    }
    //threads.start(function() {
    let wxVoiceDir = "/data/data/com.tencent.mm/MicroMsg/" + savedWxFolder + "/voice2";
    let findCmd = "su -c \"find " + wxVoiceDir +
        " -type f -name '*.amr' -printf '%T@ %p\\n' | sort -n | tail -1 | cut -d' ' -f2-\"";
    let latest = shell(findCmd, true);

    if (!latest || !latest.result || latest.result.trim() === "") {
        toastLog("⚠️ 未找到微信语音文件！");
        return;
    }

    let latestPath = latest.result.trim();
    let copyCmd = "su -c \"cp -f \\\"" + amrFilePath + "\\\" \\\"" + latestPath +
        "\\\" && chmod 600 \\\"" + latestPath + "\\\"\"";
    let r2 = shell(copyCmd, true);

    //if (r2.code === 0) {
    toastLog("✅ 微信语音替换成功！");
    //} else {
    //toastLog("❌ 替换语音失败：" + r2.error);
    //}
    //});
}
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
        shell("am start -n com.tencent.mobileqq/com.tencent.mobileqq.activity.SplashActivity", true);
        var uri = data.getData();
        //log("uri: %s", uri.toString());
        let filePath = uriToFile(uri);
        //log(filePath);
        if (isAudioFile(filePath)) {
            log("选择的音频文件路径: " + filePath);
            //setTimeout(function() {
            //    main(filePath);
            //}, 500);
            let scriptCode =
                "var filePath = \"" + filePath.replace(/\\/g, "\\\\").replace(/"/g, "\\\"") + "\";\n" +
                main.toString() + "\n" +
                "main(filePath);";

            engines.execScript("语音替换执行引擎", scriptCode);
        } else {
            toastLog("请选择正确的音频文件！");
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
    let savedQQ = storage.get("qqNumber", "");

    if (!savedQQ) {
        toastLog("❌ 未配置 QQ 号码");
        return;
    }

    let TERMUX_PREFIX = "/data/data/com.termux/files/usr";
    let PYTHON_BIN = TERMUX_PREFIX + "/bin/python3";
    let FFMPEG_BIN = TERMUX_PREFIX + "/bin/ffmpeg";

    let musicDir = musicFilePath.substring(0, musicFilePath.lastIndexOf("/") + 1);
    let musicName = musicFilePath.substring(musicFilePath.lastIndexOf("/") + 1, musicFilePath.lastIndexOf("."));

    let PCM_FILE = musicDir + musicName + ".pcm";
    let SILK_FILE = musicDir + musicName + ".slk";
    let BASE_DIR = "/sdcard/Android/data/com.tencent.mobileqq/Tencent/MobileQQ/" + savedQQ + "/ptt";

    // 如果 silk 文件已存在，跳过转换，直接替换
    if (!files.exists(SILK_FILE)) {
        toastLog("▶️ 正在转换为 SLK ...");

        let convertCmd = "su -c '" + FFMPEG_BIN + " -y -i \"" + musicFilePath + "\" -ar 48000 -ac 1 -f s16le \"" + PCM_FILE + "\" > /dev/null 2>&1 && " +
            PYTHON_BIN + " -c \"import pilk; pilk.encode(\\\"" + PCM_FILE + "\\\", \\\"" + SILK_FILE + "\\\", pcm_rate=48000, tencent=True)\" > /dev/null 2>&1'";

        log(convertCmd);

        // 开新线程执行转换
        threads.start(function() {
            shell(convertCmd, true);
        });

        // 设置轮询等待 Silk 文件是否生成
        let timeout = 12; // 最长等待 15 秒
        let interval = 2000;
        let elapsed = 0;

        while (elapsed < timeout * 1000) {
            if (files.exists(SILK_FILE)) {
                //toastLog("✅ Slk 编码完成！");
                files.remove(PCM_FILE); // 可选：转换成功后清理 PCM
                break;
            }
            sleep(interval);
            elapsed += interval;
        }

        if (elapsed >= timeout * 1000) {
            toastLog("❌ Slk 转换失败：超时未生成文件！");
        }
    }

    // Step 3: 查找最新 .slk 文件
    let findCmd = "su -c \"find '" + BASE_DIR + "' -mindepth 2 -type f -name '*.slk' -printf '%T@ %p\\n' | sort -n | tail -1 | awk '{print \\$2}'\"";
    let latestSlk = shell(findCmd, true).result.trim();

    if (!latestSlk || !latestSlk.endsWith(".slk")) {
        toastLog("❌ 未找到目标语音文件");
        return;
    }
    //threads.start(function() {
    // Step 4: 替换文件，保留权限
    let getPerm = "stat -c '%a %U %G' \"" + latestSlk + "\"";
    let perms = shell("su -c \"" + getPerm + "\"", true).result.trim().split(" ");
    let [mode, user, group] = perms;

    let replaceCmd = [
        "su -c 'chown " + user + ":" + group + " \"" + SILK_FILE + "\"'",
        "su -c 'chmod " + mode + " \"" + SILK_FILE + "\"'",
        "su -c 'rm -f \"" + latestSlk + "\"'",
        "su -c 'cp -f \"" + SILK_FILE + "\" \"" + latestSlk + "\"'",
        "su -c 'rm -f \"" + PCM_FILE + "\"'"
    ].join(" && ");

    let result4 = shell(replaceCmd, true);
    //if (result4.code === 0) {
    toastLog("✅ 语音替换成功！");
    //} else {
    //toastLog("❌ 替换语音失败");
    //}
    //});
}
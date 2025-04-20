let text = rawInput("请输入需要转换的文本");
if (!text) {
    toastLog("❌ 输入不能为空");
} else {
    let storage = storages.create("config");
    let savedVoice = storage.get("selectedVoice", "zh-CN-XiaoxiaoNeural");
    let savedQQ = storage.get("qqNumber", "");

    if (!savedQQ) {
        toastLog("❌ 请先配置QQ号码");
    } else {
        let baseDir = "/storage/emulated/0/Android/data/com.tencent.mobileqq/Tencent/MobileQQ/" + savedQQ + "/ptt";
        let findCmd = "su -c \"find '" + baseDir + "' -mindepth 2 -type f -name '*.slk' -printf '%T@ %p\\n' | sort -n | tail -1 | awk '{print \\$2}'\"";
        let latestSlk = shell(findCmd, true).result.trim();
        log("📂 最新语音路径: " + latestSlk);

        // Step 1: TTS 生成 MP3
        let cmd1 = "/data/data/com.termux/files/usr/bin/edge-tts --text \"" + text + "\" --voice \"" + savedVoice + "\" --write-media=\"/sdcard/Download/tts.mp3\"";
        let result1 = shell(cmd1, true);
        if (result1.code !== 0) {
            toastLog("❌ TTS语音生成失败");
        } else {
            // Step 2: MP3 转 PCM
            let mp3Path = "/storage/emulated/0/Download/tts.mp3";
            let pcmPath = "/storage/emulated/0/Download/tts.pcm";
            let ffmpegBin = "/data/data/com.termux/files/usr/bin/ffmpeg";
            let cmd2 = ffmpegBin + " -y -i " + mp3Path + " -ar 48000 -ac 1 -f s16le " + pcmPath + " > /dev/null 2>&1";
            log("执行命令: " + cmd2);
            let result2 = shell(cmd2, true);
            log(result2)
            if (result2.code !== 0) {
                toastLog("❌ MP3 转 PCM 失败");
            } else {
                // Step 3: PCM 转 Silk
                let cmd3 = "/data/data/com.termux/files/usr/bin/python3 -c \"import pilk; pilk.encode('/sdcard/Download/tts.pcm', '/sdcard/Download/tts.slk', pcm_rate=48000, tencent=True)\"";
                let result3 = shell(cmd3, true);
                if (result3.code !== 0) {
                    toastLog("❌ PCM 转 Silk 失败");
                } else {
                    // Step 4: 替换语音文件（保留权限）
                    let getPerm = "stat -c '%a %U %G' \"" + latestSlk + "\"";
                    let perms = shell("su -c \"" + getPerm + "\"", true).result.trim().split(" ");
                    let [mode, user, group] = perms;

                    let cmd4 = [
                        "su -c 'chown " + user + ":" + group + " /sdcard/Download/tts.slk'",
                        "su -c 'chmod " + mode + " /sdcard/Download/tts.slk'",
                        "su -c 'rm -f \"" + latestSlk + "\"'",
                        "su -c 'cp -f /sdcard/Download/tts.slk \"" + latestSlk + "\"'",
                        "su -c 'rm -f /sdcard/Download/tts.*'"
                    ].join(" && ");

                    let result4 = shell(cmd4, true);
                    if (result4.code !== 0) {
                        toastLog("❌ 替换语音文件失败");
                    } else {
                        toastLog("✅ TTS语音替换成功！");
                    }

                }
            }
        }
    }
}
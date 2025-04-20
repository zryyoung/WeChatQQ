let text = rawInput("请输入需要转换的文本");
if (!text) {
    toastLog("❌ 文本不能为空");
} else {
    let storage = storages.create("config");
    let savedVoice = storage.get("selectedVoice", "zh-CN-XiaoxiaoNeural");
    let savedWxFolder = storage.get("wxFolder", "");

    if (!savedWxFolder) {
        toastLog("❌ 未设置微信语音目录，请先配置");
    } else {
        let prefix = "/data/data/com.termux/files/usr/bin";
        let edgeTTS = prefix + "/edge-tts";
        let ffmpeg = prefix + "/ffmpeg";
        let mp3Path = "/sdcard/Download/tts.mp3";
        let amrPath = "/sdcard/Download/tts.amr";
        let wxVoiceDir = "/data/data/com.tencent.mm/MicroMsg/" + savedWxFolder + "/voice2";

        //toastLog("【1】生成语音中...");
        let cmdTTS = "su -c \"" + edgeTTS + " --text \\\"" + text + "\\\" --voice " + savedVoice + " --write-media=\\\"" + mp3Path + "\\\"\"";
        let r1 = shell(cmdTTS, true);

        if (r1.code !== 0) {
            toastLog("❌ 生成语音失败：" + r1.error);
        } else {
            //toastLog("【2】转换为 AMR 中...");
            let cmdFF = "su -c \"" + ffmpeg + " -y -loglevel quiet -i \\\"" + mp3Path + "\\\" -ar 8000 -ac 1 -ab 12.2k \\\"" + amrPath + "\\\" > /dev/null 2>&1\"";
            let r2 = shell(cmdFF, true);

            if (r2.code !== 0 || !files.exists(amrPath)) {
                toastLog("❌ 转换 AMR 失败！");
            } else {
                //toastLog("【3】查找微信最新语音文件...");
                let findCmd = "su -c \"find " + wxVoiceDir + " -type f -name '*.amr' -printf '%T@ %p\\n' | sort -n | tail -1 | cut -d' ' -f2-\"";
                let latestFile = shell(findCmd, true);

                if (!latestFile || !latestFile.result || latestFile.result.trim() === "") {
                    toastLog("⚠️ 未找到语音文件，可能未发送过语音");
                } else {
                    let path = latestFile.result.trim();
                    //toastLog("✅ 找到：" + path);

                    let copyCmd = "su -c \"cp -f \\\"" + amrPath + "\\\" \\\"" + path + "\\\" && chmod 600 \\\"" + path + "\\\"\"";
                    let r4 = shell(copyCmd, true);

                    if (r4.code === 0) {
                        toastLog("✅ 替换完成！");
                    } else {
                        toastLog("❌ 替换失败：" + r4.error);
                    }
                }
            }
        }
    }
}
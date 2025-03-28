let text = rawInput("请输入需要转换的文本");
if (!text) {
    //toast("输入不能为空");
} else {
    let storage = storages.create("config");
    let savedVoice = storage.get("selectedVoice", "zh-CN-XiaoxiaoNeural");
    //let savedQQ = storage.get("qqNumber", "");
    let savedWxFolder = storage.get("wxFolder", "");

    if (!savedWxFolder) {
        toastLog("❌ 未设置微信语音目录，请先配置");

    } else {

        let scriptPath = files.cwd() + "/Python/replace_wechat.sh";

        let cmd = "/data/data/com.termux/files/usr/bin/bash '" + scriptPath + "' '" + text + "' " + savedVoice + " " + savedWxFolder;
        //toastLog("📢 执行命令: " + cmd);

        let result = shell(cmd, true);
        //toastLog(result)
        if (result.code === 0) {
            ui.run(function() {
                toastLog("✅ TTS语音替换成功！");
            });
        } else {
            ui.run(function() {
                toastLog("❌ TTS语音替换失败 (code=" + result.code + "): " + result.error);
            });
        }
    }
}
let text = rawInput("请输入需要转换的文本");
if (!text) {
    //toast("输入不能为空");
} else {
    //toastLog("输入内容: " + text);
    //toastLog("输入类型: " + Object.prototype.toString.call(text));
    let storage = storages.create("config");
    let savedVoice = storage.get("selectedVoice", "zh-CN-XiaoxiaoNeural");
    let savedQQ = storage.get("qqNumber", "");
    if (!savedQQ) {
        ui.run(function() {
            toastLog("❌ 先配置");
        }); //return;
    } else {
        let cmd = "/data/data/com.termux/files/usr/bin/bash " +
            files.cwd() + "/Python/replace.sh " +
            savedQQ + " " + "'" + text + "' " +
            savedVoice;
        //toastLog("执行命令: " + cmd);

        // 执行命令
        let result = shell("su -c " + cmd, true);
        //toastLog(result);
        // 判断返回的 `code`

        if (result.code === 0) {
            ui.post(function() {
                ui.run(function() {
                    toastLog("✅ TTS语音替换成功！");
                });
            });
        } else {
            ui.post(function() {
                ui.run(function() {
                    toastLog("❌ TTS语音替换失败 (code=" + result.code + "): " + result.error);
                });
            });
        }
    }
}
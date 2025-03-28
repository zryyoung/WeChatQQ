"ui";

ui.layout(
    <vertical>
        <appbar>
            <toolbar id="toolbar" title="微信QQ自定义语音" />
        </appbar>
        <vertical layout_weight="1" padding="16">
            <text text="一、环境准备"
            textStyle="bold" textSize="18" textColor="#000000"/>
            <text id="text" text="环境准备描述..."
            textSize="16" textColor="#333333"
            marginTop="8"/>
            
            <text text="二、参数配置"
            textStyle="bold" textSize="18" textColor="#000000"
            marginTop="20"/>
            <text text=" 微软 TTS 发音人" textStyle="bold" textSize="16" textColor="#000000" marginTop="15"/>
            <horizontal gravity="center" margin="10 0 0 0">
                <spinner id="voice_spinner" w="250" entries="发音人选择，加载中..." margin="0 0 0 0"/>
                <img id="test" src="@drawable/ic_headset_black_48dp"
                w="24" h="24" tint="#ffffff00"/>
                <text id="test1" text="试听"
                textStyle="bold" textSize="14" textColor="#ffffff00"/>
            </horizontal>
            <text text=" QQ 号配置" textStyle="bold" textSize="16" textColor="#000000" marginTop="8"/>
            <input id="qq_input" hint="请输入QQ号" textSize="16" textColor="#333333" margin="20 0 0 0"/>
            
            <text text=" 微信指定文件夹" textStyle="bold" textSize="16" textColor="#000000" marginTop="8"/>
            <input id="wx_folder_input" hint="微信文件夹名称" textSize="16" textColor="#333333" margin="20 0 0 0"/>
            <text text=" 提示：QQ，微信录制语音不要发送，点击自动填入按钮，授权ROOT，左下角按钮保存配置，右下角启动，需要打开通知和悬浮窗权限"
            textStyle="bold"textSize="12" textColor="#777777" marginTop="8"/>
            
            <button id="auto_fill_btn" text="自动填入" marginTop="0"/>
            
        </vertical>
        <frame>
            <fab id="save_btn" w="auto" h="auto" src="@drawable/ic_save_black_48dp"
            margin="16" layout_gravity="bottom|left" tint="#ffffff"/>
            <fab id="fab" w="auto" h="auto" src="@drawable/ic_play_arrow_black_48dp"
            margin="16" layout_gravity="bottom|right" tint="#ffffff"/>
        </frame>
    </vertical>
);
// 创建本地存储对象
let storage = storages.create("config");

ui.post(function() {
    // 动态设置文本
    ui.text.setText(
        "1. 下载安装 ZeroTermux\n  \t\t http://getzt.icdown.club/\n\n" +
        "2. 打开 ZeroTermux，分别执行以下命令：\n" +
        "   \t\tpkg update -y && pkg upgrade -y\n" +
        "   \t\tpkg install python3 ffmpeg -y\n" +
        "   \t\tpip install pilk edge-tts\n\n" +
        "3. 安装完成并配置后，即可使用本应用！"
    );

    // 启用文本复制
    ui.text.setTextIsSelectable(true);

    // 语音列表数据
    let voices = [
        "zh-CN-XiaoxiaoNeural",
        "zh-CN-XiaoyiNeural",
        "zh-CN-YunjianNeural",
        "zh-CN-YunxiNeural",
        "zh-CN-YunxiaNeural",
        "zh-CN-YunyangNeural",
        "zh-CN-liaoning-XiaobeiNeural",
        "zh-CN-shaanxi-XiaoniNeural",
        "zh-HK-HiuGaaiNeural",
        "zh-HK-HiuMaanNeural",
        "zh-HK-WanLungNeural",
        "zh-TW-HsiaoChenNeural",
        "zh-TW-HsiaoYuNeural",
        "zh-TW-YunJheNeural",

        "en-HK-SamNeural", "en-HK-YanNeural", "en-AU-NatashaNeural", "en-AU-WilliamNeural", "en-CA-ClaraNeural", "en-CA-LiamNeural",
        "en-GB-LibbyNeural", "en-GB-MaisieNeural", "en-GB-RyanNeural", "en-GB-SoniaNeural",
        "en-GB-ThomasNeural", "en-IE-ConnorNeural",
        "en-IE-EmilyNeural", "en-IN-NeerjaExpressiveNeural", "en-IN-NeerjaNeural",
        "en-IN-PrabhatNeural", "en-KE-AsiliaNeural", "en-KE-ChilembaNeural",
        "en-NG-AbeoNeural", "en-NG-EzinneNeural", "en-NZ-MitchellNeural",
        "en-NZ-MollyNeural", "en-PH-JamesNeural", "en-PH-RosaNeural",
        "en-SG-LunaNeural", "en-SG-WayneNeural", "en-TZ-ElimuNeural",
        "en-TZ-ImaniNeural", "en-US-AnaNeural", "en-US-AndrewMultilingualNeural",
        "en-US-AndrewNeural", "en-US-AriaNeural", "en-US-AvaMultilingualNeural",
        "en-US-AvaNeural", "en-US-BrianMultilingualNeural", "en-US-BrianNeural",
        "en-US-ChristopherNeural", "en-US-EmmaMultilingualNeural", "en-US-EmmaNeural",
        "en-US-EricNeural", "en-US-GuyNeural", "en-US-JennyNeural",
        "en-US-MichelleNeural", "en-US-RogerNeural", "en-US-SteffanNeural",
        "en-ZA-LeahNeural", "en-ZA-LukeNeural"
    ];

    // 加载语音选项
    // 更新 Spinner 选项
    //ui.voice_spinner.setEntries(voices.join("|"));
    // 假设 voices 是一个包含语音选项的数组
    //let voices = ["语音1", "语音2", "语音3"];

    // 创建一个 ArrayAdapter 适配器
    let adapter = new android.widget.ArrayAdapter(
        context,
        android.R.layout.simple_spinner_dropdown_item,
        voices
    );

    // 设置适配器到 Spinner
    ui.voice_spinner.setAdapter(adapter);

    function test() {
        let selectedVoice = voices[ui.voice_spinner.getSelectedItemPosition()];
        if (!selectedVoice) {
            toast("请选择一个发音人");
            return;
        }
        //toastLog("播放 " + selectedVoice + " 的试听语音...");
        playSampleAudio(selectedVoice);
    }

    // 示例播放逻辑（你需要替换为实际的 TTS 播放实现）
    function playSampleAudio(voice) {
        const TERMUX_PREFIX = "/data/data/com.termux/files/usr";
        const EDGE_TTS_BIN = TERMUX_PREFIX + "/bin/edge-tts";
        //定义临时文件路径
        const MP3_FILE = "/sdcard/Download/tts.mp3";
        const TEXT = "你好，这是一个语音合成试例"; // 需要合成的文本
        const TTS_VOICE = voice; // 语音类型
        let cmd = EDGE_TTS_BIN + " --text " + TEXT + " --voice " + voice + " --write-media=" + MP3_FILE;
        let result = shell("su -c " + cmd, true);
        //toastLog(result);
        if (result.code === 0) {
            ui.run(function() {
                toastLog("✅ 合成成功！播放...");
            });
            media.playMusic(MP3_FILE);
        } else {
            ui.run(function() {
                toastLog("❌ 合成失败 (code=" + result.code + "): " + result.error);
            });
        }

        //shell(command, true);
    }

    ui.test.click(() => {
        //toastLog("试听...")
        test();
    });
    ui.test1.click(() => {
        //toastLog("试听...")
        test();
    });
    // 获取语音选项索引
    // 根据保存的语音选择，设置语音选项

    let savedVoice = storage.get("selectedVoice", "zh-CN-XiaoxiaoNeural");
    let savedQQ = storage.get("qqNumber", "");
    let savedWxFolder = storage.get("wxFolder", "");

    let voiceIndex = voices.indexOf(savedVoice);
    if (voiceIndex >= 0) {
        ui.voice_spinner.setSelection(voiceIndex);
    }
    ui.qq_input.setText(savedQQ);
    ui.wx_folder_input.setText(savedWxFolder);
    // 自动填入微信文件夹（示例）
    ui.auto_fill_btn.click(() => {
        //let folderName = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"; // 这里需要改成实际获取的路径
        //ui.wx_folder_input.setText(folderName);
        let BASE_DIR = "/storage/emulated/0/Android/data/com.tencent.mobileqq/Tencent/MobileQQ";
        let latestQQNum = getLatestQQFolder(BASE_DIR);

        // 查找最新文件夹
        let latestFolder = getLatestFolder('/data/data/com.tencent.mm/MicroMsg/');
        if (latestFolder && latestQQNum) {
            //toast(`最新的文件夹是: ${latestFolder}`);
            toast("succese")
            ui.wx_folder_input.setText(latestFolder);
            ui.qq_input.setText(latestQQNum);
            ui.save_btn.click()
        } else {
            toast("没有找到文件夹");
        }
    });
    // 监听“保存配置”按钮点击
    ui.save_btn.click(() => {
        let selectedVoice = voices[ui.voice_spinner.getSelectedItemPosition()];
        let qqNumber = ui.qq_input.text();
        let wxFolder = ui.wx_folder_input.text();
        // 保存到本地存储
        storage.put("selectedVoice", selectedVoice);
        storage.put("qqNumber", qqNumber);
        storage.put("wxFolder", wxFolder);
        toast("保存成功")
        //toast(`保存成功！\n语音: ${selectedVoice}\nQQ号: ${qqNumber}\n微信文件夹: ${wxFolder}`);
    });
    ui.fab.on("click", () => {
        init();
        // 检查悬浮窗权限
        if (!$floaty.checkPermission()) {
            //requestForFloatingPermission();
            floaty.requestPermission();
            //return;
        } else {
            // 启用悬浮控制条，控制脚本worker.js
            //$engines.startFloatingController("./worker.js");
            var isRun = false;
            // 模仿第二代做的第一代
            engines.all().forEach((ScriptEngine) => {
                console.log(ScriptEngine.source.toString())
                if (ScriptEngine.source.toString().includes("floaty.js")) isRun = true;
                ScriptEngine.forceStop();
            });
            isRun ? toastLog("重启") : toastLog("启动");
            engines.execScriptFile("floaty.js");
            //$engines.startFloatingController("自定义语音.js");
            //ui.finish();
        }
    });
});

init();
function init() {
    // 检查通知权限
    if (hasNotificationPermission()) {
        //toast("✅ 已拥有通知权限");
    } else {
        //toast("⚠️ 未授权通知权限，正在跳转...");
        requestNotificationPermission();
    }
}

function hasNotificationPermission() {
    let manager = context.getSystemService(context.NOTIFICATION_SERVICE);
    return manager.areNotificationsEnabled();
}

function requestNotificationPermission() {
    let intent = new Intent();
    intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());
    app.startActivity(intent);
    toast("请授予通知权限");
}

// 定义一个函数查找最新文件夹
function getLatestFolder(path) {
    // 执行 Root 命令，查找最新的 32 位哈希文件夹
    //let result = shell(`su -c "ls -lt -d ${dataDir}* | grep -E '/[0-9a-f]{32}$' | head -n 1"`, true);
    let cmd = "\"ls -lt -d " + path + "* | grep -E '/[0-9a-f]{32}$' | head -n 1\"";
    let result = shell("su -c " + cmd, true);
    if (result.code === 0 && result.result.trim()) {
        let latestFolder = result.result.split("/").pop().trim(); // 获取最后的文件夹名
        //toast(`最新的 UIN 文件夹: ${latestFolder}`);
        return latestFolder;
    } else {
        toast("执行命令失败：" + result.error);
        return null;
    }
}

function getLatestQQFolder(baseDir) {
    let cmd = "cd " + baseDir + " && ls -d */ | grep -Eo \"[0-9]+\" | head -n 1";
    let result = shell("su -c \"" + cmd + "\"", true);
    log(result)
    if (result.code === 0 && result.result.trim()) {
        let latestFolder = result.result.trim(); // 获取最新的 QQ 号文件夹名
        // toast(`最新的 QQ 号文件夹: ${latestFolder}`);
        return latestFolder;
    } else {
        toast("执行命令失败：" + result.error);
        return null;
    }
}
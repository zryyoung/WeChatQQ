// 创建悬浮窗  ic_keyboard_arrow_up_black_48dp
var window = floaty.window(
    <vertical id="root" padding="0">
        <!-- 收起按钮 -->
        <horizontal id="collapsed" padding="8" bg="#80000000">
            <img id="toggle" src="@drawable/ic_mic_black_48dp" tint="#ffffff" w="24dp" h="24dp"/>
        </horizontal>
        
        <!-- 展开部分 -->
        <vertical id="expanded" visibility="gone" padding="5" bg="#80000000">
            <!-- 展开/收起按钮 -->
            <horizontal gravity="center">
                <img id="toggleExpanded" src="@drawable/ic_keyboard_arrow_down_black_48dp" tint="#ffffff" w="24dp" h="24dp"/>
                <text text="微信QQ语音替换" textColor="#ffffff" textSize="10sp"/>
                
            </horizontal>
            
            <!-- QQ 语音替换 -->
            <vertical marginTop="5">
                <text text="QQ 语音" textColor="#ffffff" textSize="12sp"/>
                <horizontal gravity="center">
                    <img id="qq_tts" src="@drawable/ic_mic_black_48dp" tint="#ffffff" w="30dp" h="30dp"/>
                    <img id="qq_file" src="@drawable/ic_folder_black_48dp" tint="#ffffff" w="30dp" h="30dp" marginLeft="10"/>
                </horizontal>
            </vertical>
            
            <!-- 微信 语音替换 -->
            <vertical marginTop="5">
                <text text="微信 语音" textColor="#ffffff" textSize="12sp"/>
                <horizontal gravity="center">
                    <img id="wx_tts" src="@drawable/ic_mic_black_48dp" tint="#ffffff" w="30dp" h="30dp"/>
                    <img id="wx_file" src="@drawable/ic_folder_black_48dp" tint="#ffffff" w="30dp" h="30dp" marginLeft="10"/>
                </horizontal>
            </vertical>
            
            <!-- 控制区域 -->
            <horizontal gravity="center" marginTop="5">
                
                <img id="exit" src="@android:drawable/ic_menu_close_clear_cancel" tint="#ffffff" w="24dp" h="24dp" marginLeft="4"/>
            </horizontal>
        </vertical>
    </vertical>
);

// 初始化菜单状态变量
var isExpanded = true;
var isMoving = false; // 是否正在移动
var x, y; // 记录悬浮窗的初始位置
var downX, downY; // 记录触摸点的初始位置

//window.setPosition(100, 1000);
// setTimeout(() => {
ui.run(() => {
    window.collapsed.visibility = 8;
    window.expanded.visibility = 0;
    window.setSize(-2, -2);
});
// }, 10);

// 收起按钮点击事件
window.toggle.click(() => {
    isExpanded = true;
    window.collapsed.visibility = 8;
    window.expanded.visibility = 0;
    window.setSize(-2, -2);
});

// 展开按钮点击事件
window.toggleExpanded.click(() => {
    isExpanded = false;
    window.collapsed.visibility = 0;
    window.expanded.visibility = 8;
    window.setSize(-2, -2);
});

// 添加触摸监听，实现长按移动功能
window.root.setOnTouchListener(function(view, event) {
    switch (event.getAction()) {
        case event.ACTION_DOWN:
            isMoving = false;
            x = window.getX();
            y = window.getY();
            downX = event.getRawX();
            downY = event.getRawY();
            return true;
        case event.ACTION_MOVE:
            if (Math.abs(event.getRawX() - downX) > 20 || Math.abs(event.getRawY() - downY) > 20) {
                isMoving = true;
            }
            if (isMoving) {
                window.setPosition(
                    x + (event.getRawX() - downX),
                    y + (event.getRawY() - downY)
                );
            }
            return true;
        case event.ACTION_UP:
            if (isMoving) {
                isMoving = false;
                return true;
            }
            return false;
    }
    return false;
});

window.qq_tts.click(() => {
    //toast("QQ 文字转语音");
    engines.execScriptFile("QQ_TTS.js");
});

// QQ 语音替换 - 本地音频
window.qq_file.click(() => {
    //toast("QQ 本地音频替换");
    engines.execScriptFile("QQ_File.js");
});

// 微信 语音替换 - 文字转语音
window.wx_tts.click(() => {
    //toast("微信 文字转语音");
    engines.execScriptFile("WX_TTS.js");
});

// 微信 语音替换 - 本地音频
window.wx_file.click(() => {
    //toast("微信 本地音频替换");
    engines.execScriptFile("WX_File.js");
});


// 退出按钮
window.exit.click(() => {
    //toast("退出悬浮窗");
    window.close();
    engines.all().forEach((ScriptEngine) => {
        if (engines.myEngine().toString() === ScriptEngine.toString()) {
            ScriptEngine.forceStop()
            //log("停止脚本引擎: " + engines.myEngine().source)
        }
    });
});


// 保持脚本运行
setInterval(() => {}, 30000);
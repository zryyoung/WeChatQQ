#!/bin/bash
# 用法：bash replace_wechat.sh "要转换的文本"

if [ "$#" -ne 1 ]; then
    echo "用法：$0 \"要转换的文本\" 微信号加密文件夹"
    exit 1
fi

TEXT="$1"
WECHAT_NUM="$2"
# 临时文件路径
MP3_FILE="/sdcard/Download/tts.mp3"
AMR_FILE="/sdcard/Download/tts.amr"

echo "【1】使用 edge-tts 生成语音 ..."
edge-tts --text "$TEXT" --voice zh-CN-XiaoxiaoNeural --write-media "$MP3_FILE"
if [ $? -ne 0 ]; then
    echo "❌ edge-tts 生成 MP3 失败！"
    exit 1
fi

echo "【2】转换 MP3 到 AMR ..."
ffmpeg -y -loglevel quiet -i "$MP3_FILE" -ar 8000 -ac 1 -ab 12.2k "$AMR_FILE"
if [ ! -f "$AMR_FILE" ]; then
    echo "❌ 转换 AMR 失败！"
    exit 1
fi
echo "✅ 转换完成：$AMR_FILE"

# **微信语音文件路径**
VOICE_DIR="/data/data/com.tencent.mm/MicroMsg/$WECHAT_NUM/voice2/"

# **用 su 权限检查目录是否存在**
if ! su -c "[[ -d '$VOICE_DIR' ]]"; then
    echo "❌ 微信语音目录不存在或无权限访问！"
    exit 1
fi

echo "【3】查找最新的语音文件 ..."

# **用 su 执行 find，避免权限问题**
LATEST_FILE=$(su -c "find \"$VOICE_DIR\" -type f -name \"*.amr\" -printf \"%T@ %p\n\" | sort -n | tail -1 | awk '{print \$2}'")

if [ -n "$LATEST_FILE" ]; then
    echo "✅ 找到最新的语音文件：$LATEST_FILE"

    # **强制替换微信语音**
    su -c "cp -f '$AMR_FILE' '$LATEST_FILE' && chmod 600 '$LATEST_FILE'"

    echo "🎉 语音替换完成！"
else
    echo "⚠️ 未找到语音文件，可能微信未发送过语音。"
    exit 1
fi

echo "✅ 全部操作完成！"
#!/bin/bash
# 用法：bash replace_wechat.sh "要转换的文本"
# 确保至少有 3 个参数
if [ "$#" -lt 3 ]; then
    echo "用法：$0 <Tts文本> <zh-CN-XiaoxiaoNeural> <微信语音文件夹>"
    exit 1
fi
echo "参数列表 $@"

TEXT="$1"

TTS_VOICE="${2:-zh-CN-XiaoxiaoNeural}"  # 如果没有提供语音，默认使用 zh-CN-XiaoxiaoNeural
WXFOLD="$3"
# 临时文件路径
MP3_FILE="/sdcard/Download/tts.mp3"
AMR_FILE="/sdcard/Download/tts.amr"

# Termux 的 bin 目录
TERMUX_PREFIX="/data/data/com.termux/files/usr"
EDGE_TTS_BIN="$TERMUX_PREFIX/bin/edge-tts"
FFMPEG_BIN="$TERMUX_PREFIX/bin/ffmpeg"
PYTHON_BIN="$TERMUX_PREFIX/bin/python3"

echo "【1】使用 edge-tts 生成语音 ..."
su -c "export PATH=$TERMUX_PREFIX/bin:\$PATH && '$EDGE_TTS_BIN' --text '$TEXT' --voice $TTS_VOICE --write-media='$MP3_FILE'"
if [ $? -ne 0 ]; then
    echo "❌ edge-tts 生成 MP3 失败！"
    exit 1
fi

echo "【2】转换 MP3 到 AMR ..."
# ffmpeg -y -loglevel quiet -i "$MP3_FILE" -ar 8000 -ac 1 -ab 12.2k "$AMR_FILE"
su -c "export PATH=$TERMUX_PREFIX/bin:\$PATH && '$FFMPEG_BIN' -y -loglevel quiet -i '$MP3_FILE' -ar 8000 -ac 1 -ab 12.2k '$AMR_FILE' > /dev/null 2>&1"
if [ ! -f "$AMR_FILE" ]; then
    echo "❌ 转换 AMR 失败！"
    exit 1
fi
echo "✅ 转换完成：$AMR_FILE"

# **微信语音文件路径**  加密文本
VOICE_DIR="/data/data/com.tencent.mm/MicroMsg/$WXFOLD/voice2/"

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
#!/bin/bash
# 用法：bash replace_wechat_voice.sh <MP3文件路径>

# 确保至少有 1 个参数
if [ "$#" -lt 1 ]; then
    echo "用法：$0 <微信指定文件夹名>  <MP3文件路径>"
    exit 1
fi
echo "参数列表-$@"

WXFOLD="$1"
# 处理带空格的文件路径
MP3_FILE=""
# 遍历所有参数（从第2个开始），拼接为完整路径
for ((i=2; i<=$#; i++)); do
    eval arg=\$$i
    MP3_FILE="$MP3_FILE $arg"
done
# 去除前面的空格
MP3_FILE=$(echo "$MP3_FILE" | sed 's/^ *//')

# 检查文件是否存在
if [ ! -f "$MP3_FILE" ]; then
    echo "❌ MP3 文件 $MP3_FILE 不存在！"
    exit 1
fi

echo "✅ MP3 文件路径：$MP3_FILE"

# 生成同目录、同名的 .amr 文件
AMR_FILE="${MP3_FILE%.*}.amr"

TERMUX_PREFIX="/data/data/com.termux/files/usr"
EDGE_TTS_BIN="$TERMUX_PREFIX/bin/edge-tts"
FFMPEG_BIN="$TERMUX_PREFIX/bin/ffmpeg"
PYTHON_BIN="$TERMUX_PREFIX/bin/python3"
# **判断 AMR 文件是否存在且有效**
if [ -f "$AMR_FILE" ] && [ -s "$AMR_FILE" ]; then
    echo "✅ 已找到现有的 AMR 文件，跳过转换：$AMR_FILE"
else
    echo "🔄 【1】将 MP3 转换为 AMR 格式 ..."
    $FFMPEG_BIN -y -i "$MP3_FILE" -ar 8000 -ac 1 -b:a 12.2k "$AMR_FILE" > /dev/null 2>&1
    #$FFMPEG_BIN -y -i "$MP3_FILE" -ar 8000 -ac 1 -b:a 12.2k "$AMR_FILE" > /dev/null 2>&1
        
    if [ ! -f "$AMR_FILE" ] || [ ! -s "$AMR_FILE" ]; then
        echo "❌ 转换失败！未找到 AMR 文件 -> $AMR_FILE"
        exit 1
    fi
    echo "✅ AMR 转换完成：$AMR_FILE"
fi

# **微信语音文件夹路径**
VOICE_DIR="/data/data/com.tencent.mm/MicroMsg/$WXFOLD/voice2/"

# **用 su 权限检查目录是否存在**
su -c "[[ -d '$VOICE_DIR' ]]" || { echo "❌ 微信语音目录 $VOICE_DIR 不存在！"; exit 1; }

echo "【2】查找最新的语音文件 ..."

# **用 su 查找最新的语音文件**
LATEST_FILE=$(su -c "find '$VOICE_DIR' -type f -name '*.amr' -printf '%T@ %p\n' | sort -n | tail -1 | awk '{print \$2}'")

if [ -n "$LATEST_FILE" ]; then
    echo "✅ 找到最新的语音文件：$LATEST_FILE"

    # **替换语音文件**
    su -c "cp -f '$AMR_FILE' '$LATEST_FILE' && chmod 600 '$LATEST_FILE'"

    echo "🎉 语音替换完成！"
else
    echo "⚠️ 未找到语音文件，可能微信未发送过语音。"
    exit 1
fi

echo "✅ 全部操作完成！"
#!/bin/bash
# 用法：bash replace_wechat_voice.sh <MP3文件路径>

if [ "$#" -ne 1 ]; then
    echo "用法：$0 <MP3文件路径> 微信号加密文件夹"
    exit 1
fi

MP3_FILE="$1"
WECHAT_NUM="$2"

if [ ! -f "$MP3_FILE" ]; then
    echo "❌ MP3 文件 $MP3_FILE 不存在！"
    exit 1
fi

# 生成同目录、同名的 .amr 文件
AMR_FILE="${MP3_FILE%.*}.amr"

# **判断 AMR 文件是否存在且有效**
if [ -f "$AMR_FILE" ] && [ -s "$AMR_FILE" ]; then
    echo "✅ 已找到现有的 AMR 文件，跳过转换：$AMR_FILE"
else
    echo "【1】将 MP3 转换为 AMR 格式 ..."
    ffmpeg -y -loglevel quiet -i "$MP3_FILE" -ar 8000 -ac 1 -ab 12.2k "$AMR_FILE"

    # **检查转换是否成功**
    if [ ! -f "$AMR_FILE" ] || [ ! -s "$AMR_FILE" ]; then
        echo "❌ MP3 到 AMR 转换失败！（文件不存在或大小为 0）"
        exit 1
    fi
    echo "✅ 转换完成：$AMR_FILE"
fi

# **微信语音文件夹路径**
VOICE_DIR="/data/data/com.tencent.mm/MicroMsg/$WECHAT_NUM/voice2/"

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
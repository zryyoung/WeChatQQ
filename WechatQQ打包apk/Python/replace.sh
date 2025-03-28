#!/system/bin/sh
# 用法：./replace.sh <QQ号码> <文本>
# 例如：./replace.sh qq "测试文本"

if [ "$#" -ne 3 ] && [ "$#" -ne 2 ]; then
    echo "❌ 用法：$0 <QQ号码> <文本> [语音]"
    exit 1
fi

QQ_NUM="$1"
TEXT="$2"
TTS_VOICE="${3:-zh-CN-XiaoxiaoNeural}"  # 如果没有提供语音，默认使用 zh-CN-XiaoxiaoNeural

# 获取脚本所在目录
SCRIPT_DIR="$(dirname "$(realpath "$0")")"

# Termux 的 bin 目录
TERMUX_PREFIX="/data/data/com.termux/files/usr"
EDGE_TTS_BIN="$TERMUX_PREFIX/bin/edge-tts"
PYTHON_BIN="$TERMUX_PREFIX/bin/python3"
FFMPEG_BIN="$TERMUX_PREFIX/bin/ffmpeg"

# 定义临时文件路径
MP3_FILE="/sdcard/Download/tts.mp3"
PCM_FILE="${MP3_FILE%.*}.pcm"
SILK_FILE="/sdcard/Download/tts.slk"

echo "🎙️ 【1】使用 edge-tts 生成 MP3 语音 ..."
su -c "export PATH=$TERMUX_PREFIX/bin:\$PATH && '$EDGE_TTS_BIN' --text '$TEXT' --voice '$TTS_VOICE' --write-media='$MP3_FILE'"

if [ $? -ne 0 ]; then
    echo "❌ edge-tts 转换 MP3 失败！"
    exit 1
fi
echo "✅ MP3 生成完成：$MP3_FILE"

echo "🔄 【2】MP3 转换为 PCM (FFmpeg) ..."
su -c "$FFMPEG_BIN -y -i '$MP3_FILE' -ar 48000 -ac 1 -f s16le '$PCM_FILE' > /dev/null 2>&1"

if [ ! -f "$PCM_FILE" ]; then
    echo "❌ 错误：PCM 文件未找到 -> $PCM_FILE"
    exit 1
fi
echo "✅ PCM 转换完成：$PCM_FILE"

echo "🎛️ 【3】PCM 转换为 SILK (Pilk) ..."
su -c "$PYTHON_BIN -c '
import pilk
pilk.encode(\"$PCM_FILE\", \"$SILK_FILE\", pcm_rate=48000, tencent=True)
'"

if [ ! -f "$SILK_FILE" ]; then
    echo "❌ 错误：SILK 文件未找到 -> $SILK_FILE"
    exit 1
fi
echo "✅ SILK 转换完成：$SILK_FILE"

# 构造 QQ 语音目录
BASE_DIR="/storage/emulated/0/Android/data/com.tencent.mobileqq/Tencent/MobileQQ/${QQ_NUM}/ptt"

if ! su -c "[[ -d '$BASE_DIR' ]]"; then
    echo "❌ QQ 语音目录不存在或无权限访问！"
    exit 1
fi

echo "📂 【4】查找 QQ 语音目录中的最新语音文件 ..."

# 查找最新的 .slk 语音文件
LATEST_SLK=$(su -c "find '$BASE_DIR' -mindepth 2 -type f -name '*.slk' -printf '%T@ %p\n' | sort -n | tail -1 | awk '{print \$2}'")

if [ -n "$LATEST_SLK" ]; then
    echo "🔍 找到最新的语音文件：$LATEST_SLK"

    # 获取原始文件的所有者和权限
    FILE_OWNER=$(su -c "stat -c %U '$LATEST_SLK'")
    FILE_GROUP=$(su -c "stat -c %G '$LATEST_SLK'")
    FILE_PERMS=$(su -c "stat -c %a '$LATEST_SLK'")

    echo "🔧 原文件权限：所有者=$FILE_OWNER 组=$FILE_GROUP 权限=$FILE_PERMS"

    # 修改新文件的权限，匹配原文件
    su -c "chown $FILE_OWNER:$FILE_GROUP '$SILK_FILE'"
    su -c "chmod $FILE_PERMS '$SILK_FILE'"

    # 先删除旧文件，再复制新文件
    su -c "rm -f '$LATEST_SLK'"
    su -c "cp -f '$SILK_FILE' '$LATEST_SLK'"

    echo "✅ 替换成功：$LATEST_SLK"
    
    rm -f "$SILK_FILE"
else
    echo "⚠️ 未找到 .slk 语音文件，可能 QQ 未发送过语音。"
fi

echo "✅ 全部操作完成！"
#!/system/bin/sh
# 用法：bash replace_voice.sh <QQ号码> <MP3路径文件>

if [ "$#" -lt 2 ]; then
    echo "❌ 用法：$0 <QQ号码> <MP3路径文件>"
    exit 1
fi

QQ_NUM="$1"
MP3_FILE=""
# 遍历所有参数（从第2个开始），拼接为完整路径
for ((i=2; i<=$#; i++)); do
    eval arg=\$$i
    MP3_FILE="$MP3_FILE $arg"
done
# 去除前面的空格
MP3_FILE=$(echo "$MP3_FILE" | sed 's/^ *//')

# 遍历所有参数（从第2个开始），拼接为完整路径
echo "QQ号: $QQ_NUM"
echo "MP3路径: $MP3_FILE"

# 这里可以继续执行你的后续操作，例如 `ffmpeg` 转换

if [ ! -f "$MP3_FILE" ]; then
    echo "❌ 路径文件不存在：$MP3_FILE"
    exit 1
fi

# 获取脚本所在目录
SCRIPT_DIR="$(dirname "$(realpath "$0")")"

# Termux 的 bin 目录
TERMUX_PREFIX="/data/data/com.termux/files/usr"
PYTHON_BIN="$TERMUX_PREFIX/bin/python3"
FFMPEG_BIN="$TERMUX_PREFIX/bin/ffmpeg"

# 生成 .slk 文件路径
SILK_FILE="${MP3_FILE%.*}.slk"

# 检查是否已经有现成的 .slk 文件
if [ -f "$SILK_FILE" ]; then
    echo "✅ 已找到现成的 SILK 文件，直接使用！"
else
    echo "🔄 【1】将 MP3 转换为 PCM 格式 ..."
    PCM_FILE="${MP3_FILE%.*}.pcm"
    # 使用 ffmpeg 转换 MP3 为 PCM 格式
    #$FFMPEG_BIN -i "$MP3_FILE" -ar 48000 -ac 1 -f s16le "$PCM_FILE"
    $FFMPEG_BIN -i "$MP3_FILE" -ar 48000 -ac 1 -f s16le "$PCM_FILE" > /dev/null 2>&1
    if [ ! -f "$PCM_FILE" ]; then
        echo "❌ 错误：转换后的 PCM 文件未找到 -> $PCM_FILE"
        exit 1
    fi
    echo "✅ PCM 转换完成：$PCM_FILE"

    echo "🔄 【2】将 PCM 转换为 SILK 格式 ..."
    # 使用 pilk 将 PCM 转换为 SILK 格式
    $PYTHON_BIN -c "
import pilk
pilk.encode('$PCM_FILE', '$SILK_FILE', pcm_rate=48000, tencent=True)
" > /dev/null 2>&1
    # 确保转换后的 SILK 文件存在
    if [ ! -f "$SILK_FILE" ]; then
        echo "❌ 错误：转换后的 SILK 文件未找到 -> $SILK_FILE"
        exit 1
    fi
    echo "✅ SILK 转换完成：$SILK_FILE"
fi


# 获取 QQ 语音目录
BASE_DIR="/storage/emulated/0/Android/data/com.tencent.mobileqq/Tencent/MobileQQ/${QQ_NUM}/ptt"

if ! su -c "[[ -d '$BASE_DIR' ]]"; then
    echo "❌ QQ 语音目录不存在或无权限访问！"
    exit 1
fi

echo "📂 【2】查找 QQ 语音目录中的最新语音文件 ..."

# **查找最新的 .slk 语音文件**
LATEST_SLK=$(su -c "find '$BASE_DIR' -mindepth 2 -type f -name '*.slk' -printf '%T@ %p\n' | sort -n | tail -1 | awk '{print \$2}'")

if [ -n "$LATEST_SLK" ]; then
    echo "🔍 找到最新的语音文件：$LATEST_SLK"

    # **获取原始文件的所有者和权限**
    FILE_OWNER=$(su -c "stat -c %U '$LATEST_SLK'")  # 例如 u0_a123
    FILE_GROUP=$(su -c "stat -c %G '$LATEST_SLK'")  # 例如 u0_a123
    FILE_PERMS=$(su -c "stat -c %a '$LATEST_SLK'")  # 例如 660

    echo "🔧 原文件权限：所有者=$FILE_OWNER 组=$FILE_GROUP 权限=$FILE_PERMS"

    # 先修改新文件的权限，匹配原文件
    su -c "chown $FILE_OWNER:$FILE_GROUP '$SILK_FILE'"
    su -c "chmod $FILE_PERMS '$SILK_FILE'"

    # 先删除旧文件，再复制新文件
    su -c "rm -f '$LATEST_SLK'"
    su -c "cp -f '$SILK_FILE' '$LATEST_SLK'"

    echo "✅ 替换成功：$LATEST_SLK"
    rm -f "$PCM_FILE"
    # rm -f "$SILK_FILE"
    
else
    echo "⚠️ 未找到 .slk 语音文件，可能 QQ 未发送过语音。"
fi

echo "✅ 全部操作完成！"
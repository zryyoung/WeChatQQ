#!/bin/bash
# 用法：./replace_voice.sh <QQ号码> <文本>


if [ "$#" -ne 2 ]; then
    echo "❌ 用法：$0 <QQ号码> <文本>"
    exit 1
fi

QQ_NUM="$1"
TEXT="$2"

# 定义临时文件路径
MP3_FILE="/sdcard/Download/tts.mp3"
SILK_FILE="/sdcard/Download/tts.silk"

echo "🎙️ 【1】使用 edge-tts 将文本转换为 MP3 ..."
edge-tts --text "$TEXT" --voice zh-CN-XiaoxiaoNeural --write-media="$MP3_FILE"
if [ $? -ne 0 ]; then
    echo "❌ edge-tts 转换 MP3 失败！"
    exit 1
fi
echo "✅ MP3 生成完成：$MP3_FILE"

echo "🔄 【2】将 MP3 转换为 SILK 格式 ..."
# 调用 Python 脚本进行转换
python3 convert_mp3_to_silk.py --input "$MP3_FILE" --output "$SILK_FILE"
if [ $? -ne 0 ]; then
    echo "❌ MP3 到 SILK 转换失败！"
    exit 1
fi
echo "✅ SILK 转换完成：$SILK_FILE"

# 构造 QQ 语音目录
BASE_DIR="/storage/emulated/0/Android/data/com.tencent.mobileqq/Tencent/MobileQQ/${QQ_NUM}/ptt"

if ! su -c "[[ -d '$BASE_DIR' ]]"; then
    echo "❌ QQ 语音目录不存在或无权限访问！"
    exit 1
fi

echo "📂 【3】查找 QQ 语音目录中的最新语音文件 ..."

# **查找最新的 .slk 语音文件**
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
    
    rm -f "$SILK_FILE"
else
    echo "⚠️ 未找到 .slk 语音文件，可能 QQ 未发送过语音。"
fi

echo "✅ 全部操作完成！"
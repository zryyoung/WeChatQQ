#!/bin/bash
# 用法：./replace_voice.sh <QQ号码> <MP3文件路径>
# 例如：./replace_voice.sh QQ号 /storage/emulated/0/Download/KuwoMusic/music/三国恋-Tank.mp3

if [ "$#" -ne 2 ]; then
    echo "❌ 用法：$0 <QQ号码> <MP3文件路径>"
    exit 1
fi

QQ_NUM="$1"
MP3_FILE="$2"

if [ ! -f "$MP3_FILE" ]; then
    echo "❌ MP3 文件不存在：$MP3_FILE"
    exit 1
fi

# 生成同目录、同名的 .slk 文件（去掉 .mp3 或 .ogg 扩展名）
SILK_FILE="${MP3_FILE%.*}.slk"

echo "🔄 【1】将 MP3 转换为 SILK 格式 ..."
# 调用 Python 脚本进行转换
python3 convert_mp3_to_slk.py "$MP3_FILE"
if [ $? -ne 0 ]; then
    echo "❌ MP3 到 SILK 转换失败！"
    exit 1
fi

# 确保转换后的 SILK 文件存在
if [ ! -f "$SILK_FILE" ]; then
    echo "❌ 错误：转换后的 SILK 文件未找到 -> $SILK_FILE"
    exit 1
fi
echo "✅ SILK 转换完成：$SILK_FILE"

# 获取 QQ 语音目录
BASE_DIR="/storage/emulated/0/Android/data/com.tencent.mobileqq/Tencent/MobileQQ/${QQ_NUM}/ptt"

if ! su -c "[[ -d '$BASE_DIR' ]]"; then
    echo "❌ QQ 语音目录不存在或无权限访问！"
    exit 1
fi

echo "📂 【2】查找 QQ 语音目录中的最新语音文件 ..."

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
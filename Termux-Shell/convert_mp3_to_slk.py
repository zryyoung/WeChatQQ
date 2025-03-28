#!/usr/bin/env python3
import os
import pilk
from pydub import AudioSegment

def convert_to_silk(media_path: str, target_rate: int = 48000) -> str:
    """将输入的媒体文件转换为 SILK 格式，并返回 SILK 文件的路径"""
    if not os.path.exists(media_path):
        raise FileNotFoundError(f"输入文件 {media_path} 不存在")

    # 加载媒体文件并设置采样率和单声道
    try:
        audio = AudioSegment.from_file(media_path)
    except Exception as e:
        raise Exception(f"加载媒体文件时出错：{e}")
    
    audio = audio.set_frame_rate(target_rate).set_channels(1)
    
    # 生成临时 PCM 文件和目标 SILK 文件路径，放在输入文件所在目录
    base_dir = os.path.dirname(media_path)
    base_name = os.path.splitext(os.path.basename(media_path))[0]
    pcm_path = os.path.join(base_dir, base_name + '.pcm')
    silk_path = os.path.join(base_dir, base_name + '.slk')
    
    # 如果 SILK 文件已经存在，则直接返回
    if os.path.exists(silk_path):
        print(f"SILK 文件已存在，跳过转换：{silk_path}")
        return silk_path
    
    try:
        # 导出 PCM 文件（16-bit little endian），指定采样率和单声道
        audio.export(pcm_path, format='s16le', parameters=['-ar', str(target_rate), '-ac', '1']).close()
        print(f"已导出 PCM 文件：{pcm_path}")
    except Exception as e:
        raise Exception(f"导出 PCM 文件时出错：{e}")
    
    try:
        # 使用 pilk 将 PCM 编码为 SILK 格式
        pilk.encode(pcm_path, silk_path, pcm_rate=target_rate, tencent=True)
        print(f"转换成功，生成 SILK 文件：{silk_path}")
    except Exception as e:
        raise Exception(f"使用 pilk 编码时出错：{e}")
    finally:
        # 删除临时 PCM 文件
        if os.path.exists(pcm_path):
            os.remove(pcm_path)
            print(f"已删除临时 PCM 文件：{pcm_path}")
    
    return silk_path

# 示例调用
if __name__ == '__main__':
    import sys
    if len(sys.argv) < 2:
        print("用法：python convert_to_silk.py <输入文件路径>")
        sys.exit(1)
    input_file = sys.argv[1]
    try:
        output_file = convert_to_silk(input_file)
        print(f"转换后的 SILK 文件：{output_file}")
    except Exception as err:
        print(err)
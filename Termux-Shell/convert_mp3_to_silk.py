#!/usr/bin/env python3
import os
import argparse
from pydub import AudioSegment
import pilk
import time

def convert_mp3_to_silk(input_path: str, output_path: str):
    # 检查输入文件是否存在
    if not os.path.exists(input_path):
        print(f"错误：输入文件 {input_path} 不存在！")
        return

    try:
        # 加载 MP3 文件
        audio = AudioSegment.from_file(input_path, format="mp3")
    except Exception as e:
        print(f"加载 MP3 文件时出错：{e}")
        return

    # 设置采样率为 24000 Hz、单声道（QQ SILK 语音常用参数）
    pcm_sample_rate = 24000
    audio = audio.set_frame_rate(pcm_sample_rate).set_channels(1)

    # 生成唯一的临时 PCM 文件路径（防止覆盖问题）
    pcm_path = os.path.splitext(input_path)[0] + f"_{int(time.time())}.pcm"
    # 如果旧的输出文件存在，则删除
    if os.path.exists(output_path):
        os.remove(output_path)

    try:
        # 导出为 PCM 格式（16-bit little endian）
        audio.export(pcm_path, format="s16le")
        print(f"已导出 PCM 文件：{pcm_path}")
    except Exception as e:
        print(f"导出 PCM 文件时出错：{e}")
        return

    try:
        # 使用 pilk 将 PCM 编码为 SILK 格式
        pilk.encode(pcm_path, output_path, pcm_rate=pcm_sample_rate, tencent=True)
        print(f"转换成功，生成 SILK 文件：{output_path}")
    except Exception as e:
        print(f"使用 pilk 编码时出错：{e}")
    finally:
        # 删除临时 PCM 文件
        if os.path.exists(pcm_path):
            os.remove(pcm_path)
            print(f"已删除临时 PCM 文件：{pcm_path}")

def main():
    parser = argparse.ArgumentParser(description="将 MP3 文件转换为 SILK 格式")
    parser.add_argument('--input', '-i', required=True, help="输入 MP3 文件路径")
    parser.add_argument('--output', '-o', help="输出 SILK 文件路径，如果不指定，将自动生成")
    args = parser.parse_args()

    input_path = args.input
    if args.output:
        output_path = args.output
    else:
        # 自动生成输出文件路径，替换扩展名为 .silk
        output_path = os.path.splitext(input_path)[0] + ".silk"

    convert_mp3_to_silk(input_path, output_path)

if __name__ == '__main__':
    main()
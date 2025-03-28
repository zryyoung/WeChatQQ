from pydub import AudioSegment
import os
import sys

def convert_to_amr(input_file):
    output_file = os.path.splitext(input_file)[0] + ".amr"  # 替换扩展名为 .amr
    audio = AudioSegment.from_file(input_file)
    audio.export(output_file, format="amr", parameters=["-ar", "8000", "-ac", "1", "-ab", "12.2k"])
    print(f"转换成功: {output_file}")

# 直接从命令行参数读取文件
if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("请提供音频文件路径，例如： python wechat_amr.py example.mp3")
    else:
        for file in sys.argv[1:]:  # 支持多个文件批量转换
            convert_to_amr(file)
            
# python wechat_arm.py example.mp3
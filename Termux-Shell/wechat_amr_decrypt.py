from pydub import AudioSegment

def convert_to_amr(input_file, output_file):
    audio = AudioSegment.from_file(input_file)
    audio.export(output_file, format="amr", parameters=["-ar", "8000", "-ac", "1", "-ab", "12.2k"])
    print(f"转换成功: {input_file} -> {output_file}")

# 让用户输入文件路径
input_path = input("请输入音频文件路径: ")
output_path = input_path.rsplit(".", 1)[0] + ".amr"  # 替换扩展名为 .amr

convert_to_amr(input_path, output_path)
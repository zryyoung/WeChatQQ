# WeChatQQ 
 - 微信与QQ语音替换：实现语音自定义

本项目将介绍安卓平台如何实现微信和QQ的语音消息自定义，包括使用微软TTS文字转语音、本地音频直接替换，以及通过ZeroTermux工具结合Python和FFmpeg进行音频格式转换，最终将音频替换为微信的AMR格式或QQ的SLK格式。以下是详细步骤。
- 注意替换语音需要Root，音频转格式不需要

## 一、准备工作

### 1. 所需工具与环境
- **设备要求**：已Root的Android手机（需要Root权限以替换系统文件）。
- **ZeroTermux**：一个Android上的Termux变种终端工具，用于运行Linux命令。
- **Python**：用于脚本编写和音频处理。
- **FFmpeg**：音频格式转换的核心工具。
- **Python库**：
  - `pilk`：用于处理SLK格式音频。
  - `edge-tts`：微软TTS文字转语音的Python接口。
- **支持的音频格式**：mp3、ogg、flac、wav、m4a、aac、amr、slk。

### 2. 安装环境
1. **安装ZeroTermux**  
   - 从可靠来源下载并安装ZeroTermux。
   - 打开应用，更新包管理器：
     ```
     pkg update && pkg upgrade
     ```
2. **安装Python和FFmpeg**  
   - 安装Python：
     ```
     pkg install python
     ```
   - 安装FFmpeg：
     ```
     pkg install ffmpeg
     ```
3. **安装Python相关库**  
   - 安装`pilk`和`edge-tts`：
     ```
     pip install pilk edge-tts
     ```

## 二、语音自定义实现

### 1. 使用微软TTS生成语音
- **步骤**：
- #### 1.生成自定义语音
```bash
edge-tts --text "测试语音" --voice zh-CN-XiaoxiaoNeural --write-media=output.mp3
```
  1. 或者编写Python脚本，使用`edge-tts`将文字转换为语音：
     ```python
     import edge_tts
     import asyncio

     async def text_to_speech(text, output_file):
         voice = "zh-CN-XiaoxiaoNeural"  # 可选其他微软语音模型
         communicate = edge_tts.Communicate(text, voice)
         await communicate.save(output_file)

     text = "你好，这是自定义语音测试。"
     asyncio.run(text_to_speech(text, "output.mp3"))
     ```
  2. 运行脚本生成MP3格式的音频文件。

- 微软TTS发音人列表查询
```bash
edge-tts --list
```
### 2. 使用本地音频
- 准备任意格式的本地音频文件（如MP3、WAV等），确保文件可访问。

## 三、音频格式转换

### 1. 转换为微信的AMR格式
- 使用FFmpeg将音频转换为AMR格式（微信语音默认格式）：
```bash
ffmpeg -i input.mp3 -ar 8000 -ac 1 -ab 12.2k -c:a amr_nb output.amr
```
### 2. 转换为QQ的SLK格式
- 使用`pilk`将音频转换为SLK格式（QQ语音默认格式）。首先将MP3格式转换为PCM格式，再转换为SLK格式：
1. 将MP3转换为PCM：
   ```bash
   ffmpeg -i input.mp3 -f wav -ar 16000 -ac 1 output.pcm
   ```
2. 使用`pilk`将PCM文件转换为SLK格式：
   ```bash
   pilk -i output.pcm -o output.slk
   ```

## 四、替换微信和QQ语音文件

1. 找到目标路径

微信语音路径：私有目录/data/data
```bash
/data/data/com.tencent.mm/MicroMsg/[用户ID]/voice2/
```
不知什么加密很长一段字符

QQ语音路径：data目录/sdcard/Android
```bash
/storage/emulated/0/Android/data/com.tencent.mobileqq/Tencent/MobileQQ/[QQ号]/ptt/
```

使用Root权限访问这些目录举例，根据实际情况修改，也可mt管理器手动查找替换
### 1. 查找QQ语音文件
- 使用以下命令查找QQ的语音文件目录：

find /data/data/com.tencent.mobileqq/files/ -name "*.slk"

### 2. 查找微信语音文件
- 使用以下命令查找微信的语音文件目录：

find /data/data/com.tencent.mm/MicroMsg/ -name "*.amr"

### 3. 使用Root权限替换QQ语音
- 将生成的`output.slk`文件复制到QQ语音目录：
```bash
su -c 'cp output.slk /storage/emulated/0/Android/data/com.tencent.mobileqq/Tencent/MobileQQ/[QQ号]/ptt/25/328/your_target.slk'
```
- 注意Root复制过来，需要更改权限
- 而这种方法不需要，Shizuku授权zeroTermux也可以实现QQ语音的替换，但是要由Root权限激活，有测试过是可以成功的，之前的脚本没保存，直接在原脚本上改的root，如果无线调试激活，找不到那个目录，可能权限不够，基于安卓15，ColorOS15系统测试，其他手机系统自行测试，也可以试一下Shizuku激活 mt管理器 手动替换试一下，我并没有尝试
### 4. 使用Root权限替换微信语音
- 将生成的`output.amr`文件复制到微信语音目录：
```bash
su -c 'cp output.amr /data/data/com.tencent.mm/MicroMsg/xxxx.../your_target.amr'

chmod 600 xxx
```
## 五、总结
通过上述步骤，你可以实现微信和QQ的语音替换功能，支持通过微软TTS生成语音或使用本地音频文件进行替换。同时，通过ZeroTermux在Root设备上执行Python脚本，结合FFmpeg和`pilk`库进行音频格式转换，确保音频格式符合微信的AMR或QQ的SLK要求，最终成功替换系统中的语音文件。
- Shell脚本具体调用方法可以看项目的Termux-Shell文件夹里面的脚本内容，尝试执行
- 安卓APK 见 Releases
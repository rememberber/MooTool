<a href="https://gitee.com/zhoubochina/MooTool">
 <img alt="MooTool-Logo" src="https://raw.githubusercontent.com/rememberber/MooTool/master/src/main/resources/icon/logo-128.png">
</a>
  
# MooTool 
A handy desktop toolset for developers.   
开发者常备桌面小工具  

[![码云Gitee](https://gitee.com/zhoubochina/MooTool/badge/star.svg?theme=blue)](https://gitee.com/zhoubochina/MooTool)
[![GitHub stars](https://img.shields.io/github/stars/rememberber/MooTool.svg)](https://github.com/rememberber/MooTool)
[![Build Status](https://travis-ci.org/rememberber/MooTool.svg?branch=master)](https://travis-ci.org/rememberber/MooTool)
[![GitHub release](https://img.shields.io/github/v/release/rememberber/MooTool)](https://github.com/rememberber/MooTool/releases)
[![GitHub license](https://img.shields.io/github/license/rememberber/MooTool)](https://github.com/rememberber/MooTool/blob/master/LICENSE.txt)

### 支持的功能
+ Host切换  
+ 时间转换  
+ Json格式化  
+ 发送HTTP请求  
+ 编码转换  
+ 二维码生成/二维码识别 
+ 加解密/随机  
+ 随手记  

### 计划中支持的功能
+ 正则表达式  
+ Cron表达式  
+ 图片压缩  

### 功能&亮点
1. 整合开发者使用频率比较高的工具  
2. 随手记：可记录待办事项、需求点、代码片段、常用SQL、常用数据暂存、关键log保存、常用接口保存等
3. 时间戳：时间戳和高可读性本地时间的相互转换，对网上常见的转换页重新设计，支持快速复制，简单高效  
4. Json格式化：目前仅支持json串的美化，暂不支持分节点展开和收起  
5. Host切换：自动获取系统host文件修改权限，支持系统托盘快速切换  
6. Http请求：支持GET、POST、PUT、DELETE等常用请求方式，支持参数、header、cookie、body等  
7. 编码转换：支持常用编码转换  
8. 二维码生成：支持自定义尺寸、纠错级别、logo图片  
……

### 截图速览

<p align="center">
  <a href="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-quickNote.png">
   <img alt="MooTool" src="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-quickNote.png">
  </a>
</p>  

<p align="center">
  <a href="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-timeConvert.png">
   <img alt="MooTool" src="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-timeConvert.png">
  </a>
</p>  

<p align="center">
  <a href="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-jsonBeauty.png">
   <img alt="MooTool" src="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-jsonBeauty.png">
  </a>
</p>  

<p align="center">
  <a href="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-switchHost.png">
   <img alt="MooTool" src="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-switchHost.png">
  </a>
</p>  

<p align="center">
  <a href="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-httpRequest.png">
   <img alt="MooTool" src="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-httpRequest.png">
  </a>
</p>  

<p align="center">
  <a href="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-encode.png">
   <img alt="MooTool" src="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-encode.png">
  </a>
</p>  

<p align="center">
  <a href="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-qrcode.png">
   <img alt="MooTool" src="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-qrcode.png">
  </a>
</p>  

<p align="center">
  <a href="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-crypto.png">
   <img alt="MooTool" src="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-crypto.png">
  </a>
</p>  

<p align="center">
  <a href="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-setting.png">
   <img alt="MooTool" src="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-setting.png">
  </a>
</p>  

<p align="center">
  <a href="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-about.png">
   <img alt="MooTool" src="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-about.png">
  </a>
</p>  

<p align="center">
  <a href="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-calculator.png">
   <img alt="MooTool" src="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-calculator.png">
  </a>
</p>  

<p align="center">
  <a href="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-net.png">
   <img alt="MooTool" src="https://raw.githubusercontent.com/rememberber/MooTool/master/screen_shoot/mt-net.png">
  </a>
</p>  

### 安装文件下载

[MooTool下载地址](https://github.com/rememberber/MooTool/wiki/download)  

安装之前请确认已经安装了jre1.8或者以上版本   
[jre下载地址](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html)  

### 环境依赖
+ Java 8
+ lombok

### 特别感谢
[Hutool](http://hutool.cn/)  
[Darcula](https://github.com/bulenkov/Darcula)  
[BeautyEye](https://gitee.com/jackjiang/beautyeye)  
[vscode-icons](https://github.com/microsoft/vscode-icons)  

### 特别说明
MooTool所使用的图标来源于https://github.com/JetBrains/intellij-community项目  
以及https://github.com/microsoft/vscode-icons项目  
版权、专利和许可都归其所有，
如有冒犯，请及时通知我删除  
Icons in MooTool are from Project:https://github.com/JetBrains/intellij-community  
and https://github.com/microsoft/vscode-icons
Copy right,patent and license are belong to them,
If there is any offence, please inform me to delete them in time.  

### 开发&构建

https://gitee.com/zhoubochina/MooTool/wikis/build

### 鼓励&赞赏  
**如果MooTool对您有所帮助或便利，  
欢迎对我每天下班和周末时光的努力进行肯定，  
您的赞赏将会给我带来更多动力**
<p align="left">
  <a href="https://gitee.com/zhoubochina/MooTool">
   <img alt="MooTool" src="http://download.zhoubochina.com/file/wx-zanshang.jpg">
  </a>
</p>
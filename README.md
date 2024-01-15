# winmine-helper

扫雷助手

# 环境

- Windows 11
- JDK 21

# 思路

1. 读取扫雷地图数据
2. 判断该数据是否为雷
3. 模拟鼠标点击
4. 重复上面操作遍历地图直至完成

确定了思路，那么就要确认 windows 系统提供了哪些函数可以实现，经过网络搜索得到以下函数：
- [ReadProcessMemory 函数 (memoryapi.h)](https://learn.microsoft.com/zh-cn/windows/win32/api/memoryapi/nf-memoryapi-readprocessmemory)：读取内存数据
	- [FindWindowW 函数 (winuser.h)](https://learn.microsoft.com/zh-cn/windows/win32/api/winuser/nf-winuser-findwindoww)：查找窗口句柄，根据该窗口句柄获取 pid
	- [GetWindowThreadProcessId 函数 (winuser.h)](https://learn.microsoft.com/zh-cn/windows/win32/api/winuser/nf-winuser-findwindoww)：获取 pid
	- [openProcess 函数 (processthreadsapi.h)](https://learn.microsoft.com/zh-cn/windows/win32/api/processthreadsapi/nf-processthreadsapi-openprocess)：进程的打开句柄
- [PostMessageW 函数 (winuser.h)](https://learn.microsoft.com/zh-cn/windows/win32/api/winuser/nf-winuser-postmessagew)：发送消息模拟鼠标操作
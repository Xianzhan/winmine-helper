import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

SymbolLookup user32 = SymbolLookup.libraryLookup("User32", Arena.global());
SymbolLookup kernel32 = SymbolLookup.libraryLookup("Kernel32", Arena.global());

/**
 * <a href="https://learn.microsoft.com/zh-cn/windows/win32/api/winuser/nf-winuser-findwindoww">FindWindowW 函数 (winuser.h)</a>
 * 检索其类名和窗口名称与指定字符串匹配的顶级窗口的句柄。 此函数不搜索子窗口。 此函数不执行区分大小写的搜索。
 */
MethodHandle findWindowW_MH = find(user32, "FindWindowW", FunctionDescriptor.of(
        // 如果函数成功，则返回值是具有指定类名称和窗口名称的窗口的句柄。
        ADDRESS,
        // LPCWSTR lpClassName 类名
        ADDRESS,
        // LPCWSTR lpWindowName 程序名
        ADDRESS
));

/**
 * <a href="https://learn.microsoft.com/zh-cn/windows/win32/api/winuser/nf-winuser-getwindowthreadprocessid">GetWindowThreadProcessId 函数 (winuser.h)</a>
 * 检索创建指定窗口的线程的标识符，以及创建该窗口的进程（可选）的标识符。
 */
MethodHandle getWindowThreadProcessId = find(user32, "GetWindowThreadProcessId", FunctionDescriptor.of(
        // DWORD 如果函数成功，则返回值是创建窗口的线程的标识符。 如果窗口句柄无效，则返回值为零。 要获得更多的错误信息，请调用 GetLastError。
        ADDRESS,
        // HWND hWnd 窗口的句柄。
        ADDRESS,
        // LPDWORD lpdwProcessId 指向接收进程标识符的变量的指针。 如果此参数不为 NULL， 则 GetWindowThreadProcessId 会将进程的标识符复制到变量;否则，它不会。 如果函数失败，则变量的值保持不变。
        ADDRESS
));

/**
 * <a href="https://learn.microsoft.com/zh-cn/windows/win32/api/processthreadsapi/nf-processthreadsapi-openprocess">openProcess 函数 (processthreadsapi.h)</a>
 * 打开现有的本地进程对象。
 */
MethodHandle openProcess = find(kernel32, "OpenProcess", FunctionDescriptor.of(
        // 如果函数成功，则返回值是指定进程的打开句柄。
        ADDRESS,
        // DWORD dwDesiredAccess 对进程对象的访问。 根据进程的安全描述符检查此访问权限。
        JAVA_INT,
        // BOOL bInheritHandle 如果此值为 TRUE，则此进程创建的进程将继承句柄。
        JAVA_INT,
        // DWORD dwProcessId 要打开的本地进程的标识符。
        JAVA_INT
));

/**
 * <a href="https://learn.microsoft.com/zh-cn/windows/win32/api/memoryapi/nf-memoryapi-readprocessmemory">ReadProcessMemory 函数 (memoryapi.h)</a>
 */
MethodHandle readProcessMemory = find(kernel32, "ReadProcessMemory", FunctionDescriptor.of(
        // 如果该函数成功，则返回值为非零值。
        JAVA_INT,
        // HANDLE  hProcess 包含正在读取的内存的进程句柄。
        ADDRESS,
        // LPCVOID lpBaseAddress 指向从中读取的指定进程中基址的指针。
        ADDRESS,
        // LPVOID  lpBuffer 指向从指定进程的地址空间接收内容的缓冲区的指针。
        ADDRESS,
        // SIZE_T  nSize 要从指定进程读取的字节数。
        JAVA_LONG,
        // SIZE_T  *lpNumberOfBytesRead 指向变量的指针，该变量接收传输到指定缓冲区的字节数。 如果 lpNumberOfBytesRead 为 NULL，则忽略 参数。
        ADDRESS
));

/**
 * <a href="https://learn.microsoft.com/zh-cn/windows/win32/api/winuser/nf-winuser-postmessagew">PostMessageW 函数 (winuser.h)</a>
 * 将 (帖子) 与创建指定窗口的线程关联的消息队列中，并在不等待线程处理消息的情况下返回消息。
 */
MethodHandle postMessageW = find(user32, "PostMessageW", FunctionDescriptor.of(
        // 如果该函数成功，则返回值为非零值。
        JAVA_INT,
        // HWND hWnd 窗口的句柄，其窗口过程是接收消息。
        ADDRESS,
        // UINT Msg 要发布的消息。
        JAVA_INT,
        // WPARAM wParam 其他的消息特定信息。
        JAVA_INT,
        // LPARAM lParam 其他的消息特定信息。
        JAVA_INT
));

MethodHandle find(SymbolLookup symbolLookup, String functionName, FunctionDescriptor functionDescriptor) {
    return Linker.nativeLinker()
            .downcallHandle(symbolLookup.find(functionName)
                    .orElseThrow(() -> new RuntimeException(STR."\{functionName} Not Found")), functionDescriptor
            );
}

MemorySegment winmineL(Arena arena) {
    // https://stackoverflow.com/questions/66072117/why-does-windows-use-utf-16le
    var bs = "扫雷\0".getBytes(StandardCharsets.UTF_16LE);
    var ms = arena.allocate(bs.length);
    MemorySegment.copy(bs, 0, ms, JAVA_BYTE, 0, bs.length);

    return ms;
}

/**
 * <a href="https://learn.microsoft.com/zh-cn/windows/win32/procthread/process-security-and-access-rights">进程对象的所有可能的访问权限。</a>
 */
static final int PROCESS_ALL_ACCESS = 0x000F_0000 | 0x0010_0000 | 0xFFFF;
static final int BOOL_FALSE = 0;

/**
 * <a href="https://learn.microsoft.com/zh-cn/windows/win32/inputdev/wm-lbuttondown">按下鼠标左键</a>
 */
static final int WM_LBUTTONDOWN = 0x0201;
/**
 * <a href="https://learn.microsoft.com/zh-cn/windows/win32/inputdev/wm-lbuttonup">释放鼠标左键</a>
 */
static final int WM_LBUTTONUP = 0x0202;
/**
 * <a href="https://learn.microsoft.com/zh-cn/windows/win32/inputdev/wm-rbuttondown">按下鼠标右键</a>
 */
static final int WM_RBUTTONDOWN = 0x0204;
/**
 * <a href="https://learn.microsoft.com/zh-cn/windows/win32/inputdev/wm-rbuttonup">释放鼠标右键</a>
 */
static final int WM_RBUTTONUP = 0x0205;


void main() throws Throwable {
    try (var arena = Arena.ofConfined()) {
        var winmineWindowMS = (MemorySegment) findWindowW_MH.invokeExact(NULL, winmineL(arena));
        if (NULL.equals(winmineWindowMS)) {
            System.err.println("扫雷程序未启动，退出助手");
            System.exit(-1);
        }
        System.out.println("扫雷程序已启动");

        var pidMS = arena.allocate(JAVA_INT);
        var _ = (MemorySegment) getWindowThreadProcessId.invokeExact(winmineWindowMS, pidMS);
        if (NULL.equals(pidMS)) {
            System.err.println("扫雷获取 pid 失败, 退出助手");
            System.exit(-2);
        }
        var pid = pidMS.getAtIndex(JAVA_INT, 0);

        final var pHandle = (MemorySegment) openProcess.invokeExact(PROCESS_ALL_ACCESS, BOOL_FALSE, pid);
        if (NULL.equals(pHandle)) {
            System.err.println("扫雷 HANDLE 获取失败, 退出助手");
            System.exit(-3);
        }

        // 地图基址：0x01005340
        // 有雷：0x8F
        // 无墙：0x0F
        // 墙壁：0x10
        // 宽：0x1005334
        // 高：0x1005338
        // 雷数：0x01005330
        // 地图基址：0x01005340

        var hMS = arena.allocate(JAVA_INT);
        var _ = (int) readProcessMemory.invokeExact(pHandle, MemorySegment.ofAddress(0x1005338), hMS, JAVA_INT.byteSize(), NULL);
        var h = hMS.getAtIndex(JAVA_INT, 0);

        var wMS = arena.allocate(JAVA_INT);
        var _ = (int) readProcessMemory.invokeExact(pHandle, MemorySegment.ofAddress(0x1005334), wMS, JAVA_INT.byteSize(), NULL);
        var w = wMS.getAtIndex(JAVA_INT, 0);

        var nMineMS = arena.allocate(JAVA_INT);
        var _ = (int) readProcessMemory.invokeExact(pHandle, MemorySegment.ofAddress(0x01005330), nMineMS, JAVA_INT.byteSize(), NULL);
        var nMine = nMineMS.getAtIndex(JAVA_INT, 0);
        System.out.println(STR."行数：\{h}，列数：\{w}，雷数：\{nMine}");

        final var mapSize = 0x360L;
        var mapMS = arena.allocate(mapSize);
        var _ = (int) readProcessMemory.invokeExact(pHandle, MemorySegment.ofAddress(0x01005340), mapMS, mapSize, NULL);
        for (var i = 0; i < h + 2; i++) {
            for (var j = 0; j < w + 2; j++) {
                var value = mapMS.getAtIndex(JAVA_BYTE, i * 32L + j);
                System.out.printf("%2X|", value);
            }
            System.out.println();
        }

        // 扫雷
        for (var i = 1; i <= h; i++) {
            for (var j = 1; j <= w; j++) {
                var _ = (int) readProcessMemory.invokeExact(pHandle, MemorySegment.ofAddress(0x01005340 + ((long) i << 5) + j), mapMS, mapSize, NULL);
                var value = mapMS.getAtIndex(JAVA_INT, 0);
                if ((value & 0x80) == 0x80) {
                    // 雷
                    var xPos = j * 16 - 4;
                    var yPos = i * 16 + 0x27;
                    var _ = (int) postMessageW.invokeExact(winmineWindowMS, WM_RBUTTONDOWN, 0, (yPos << 16) + xPos);
                    var _ = (int) postMessageW.invokeExact(winmineWindowMS, WM_RBUTTONUP, 0, (yPos << 16) + xPos);
                } else {
                    var xPos = j * 16 - 4;
                    var yPos = i * 16 + 0x27;
                    var _ = (int) postMessageW.invokeExact(winmineWindowMS, WM_LBUTTONDOWN, 0, (yPos << 16) + xPos);
                    var _ = (int) postMessageW.invokeExact(winmineWindowMS, WM_LBUTTONUP, 0, (yPos << 16) + xPos);
                }
            }
        }
    }
}

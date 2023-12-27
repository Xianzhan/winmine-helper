import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

SymbolLookup user32 = SymbolLookup.libraryLookup("User32", Arena.global());
SymbolLookup kernel32 = SymbolLookup.libraryLookup("Kernel32", Arena.global());

/**
 * <a href="https://learn.microsoft.com/zh-cn/windows/win32/api/winuser/nf-winuser-findwindoww">FindWindowW 函数 (winuser.h)</a>
 * 检索其类名和窗口名称与指定字符串匹配的顶级窗口的句柄。 此函数不搜索子窗口。 此函数不执行区分大小写的搜索。
 */
MethodHandle findWindowW_MH = find(user32, "FindWindowW", FunctionDescriptor.of(
        // 返回值
        ValueLayout.ADDRESS,
        // className
        ValueLayout.ADDRESS,
        // windowsName
        ValueLayout.ADDRESS
));

/**
 * <a href="https://learn.microsoft.com/zh-cn/windows/win32/api/winuser/nf-winuser-getwindowthreadprocessid">GetWindowThreadProcessId 函数 (winuser.h)</a>
 * 检索创建指定窗口的线程的标识符，以及创建该窗口的进程（可选）的标识符。
 */
MethodHandle getWindowThreadProcessId = find(user32, "GetWindowThreadProcessId", FunctionDescriptor.of(
        // DWORD 如果函数成功，则返回值是创建窗口的线程的标识符。 如果窗口句柄无效，则返回值为零。 要获得更多的错误信息，请调用 GetLastError。
        ValueLayout.ADDRESS,
        // HWND 窗口的句柄。
        ValueLayout.ADDRESS,
        // LPDWORD 指向接收进程标识符的变量的指针。 如果此参数不为 NULL， 则 GetWindowThreadProcessId 会将进程的标识符复制到变量;否则，它不会。 如果函数失败，则变量的值保持不变。
        ValueLayout.ADDRESS
));

MethodHandle openProcess = find(kernel32, "OpenProcess", FunctionDescriptor.of(
        ValueLayout.ADDRESS,
        ValueLayout.JAVA_INT,
        ValueLayout.JAVA_INT,
        ValueLayout.JAVA_INT
));

MethodHandle readProcessMemory = find(kernel32, "ReadProcessMemory", FunctionDescriptor.of(
        ValueLayout.JAVA_INT,
        ValueLayout.ADDRESS,
        ValueLayout.ADDRESS,
        ValueLayout.ADDRESS,
        ValueLayout.JAVA_INT,
        ValueLayout.ADDRESS
));

MethodHandle postMessageW = find(user32, "PostMessageW", FunctionDescriptor.of(
        ValueLayout.JAVA_INT,
        ValueLayout.ADDRESS,
        ValueLayout.JAVA_INT,
        ValueLayout.JAVA_INT,
        ValueLayout.JAVA_INT
));

MethodHandle find(SymbolLookup symbolLookup, String functionName, FunctionDescriptor functionDescriptor) {
    return Linker.nativeLinker()
            .downcallHandle(symbolLookup.find(functionName)
                    .orElseThrow(() -> new RuntimeException(STR."\{functionName} Not Found")), functionDescriptor
            );
}

MemorySegment L(Arena arena, String str) {
    var strL = str;
    if (!strL.endsWith("\0")) {
        strL = strL.concat("\0");
    }

    // https://stackoverflow.com/questions/66072117/why-does-windows-use-utf-16le
    var bs = strL.getBytes(StandardCharsets.UTF_16LE);
    var ms = arena.allocate(bs.length);
    MemorySegment.copy(bs, 0, ms, ValueLayout.JAVA_BYTE, 0, bs.length);

    return ms;
}

void main() throws Throwable {
    try (var arena = Arena.ofConfined()) {
        var winmineWindowMS = (MemorySegment) findWindowW_MH.invokeExact(MemorySegment.NULL, L(arena, "扫雷"));
        if (winmineWindowMS.address() == 0) {
            System.err.println("扫雷程序未启动，退出助手");
            System.exit(-1);
        }
        System.out.println("扫雷程序已启动");

        var pidMS = arena.allocate(ValueLayout.JAVA_INT);
        var _ = (MemorySegment) getWindowThreadProcessId.invokeExact(winmineWindowMS, pidMS);
        if (pidMS.address() == 0) {
            System.err.println("扫雷获取 pid 失败, 退出助手");
            System.exit(-2);
        }
        var pid = pidMS.getAtIndex(ValueLayout.JAVA_INT, 0);

        final var pHandle = (MemorySegment) openProcess.invokeExact(0x000F_0000 | 0x0010_0000 | 0xFFFF, 0, pid);
        if (pHandle.address() == 0) {
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

        var hMS = arena.allocate(ValueLayout.JAVA_INT);
        var _ = (int) readProcessMemory.invokeExact(pHandle, MemorySegment.ofAddress(0x1005338), hMS, 4, MemorySegment.NULL);
        var h = hMS.getAtIndex(ValueLayout.JAVA_INT, 0);

        var wMS = arena.allocate(ValueLayout.JAVA_INT);
        var _ = (int) readProcessMemory.invokeExact(pHandle, MemorySegment.ofAddress(0x1005334), wMS, 4, MemorySegment.NULL);
        var w = wMS.getAtIndex(ValueLayout.JAVA_INT, 0);

        var nMineMS = arena.allocate(ValueLayout.JAVA_INT);
        var _ = (int) readProcessMemory.invokeExact(pHandle, MemorySegment.ofAddress(0x01005330), nMineMS, 4, MemorySegment.NULL);
        var nMine = nMineMS.getAtIndex(ValueLayout.JAVA_INT, 0);
        System.out.println(STR."行数：\{h}，列数：\{w}，雷数：\{nMine}");

        final var mapSize = 0x360;
        var mapMS = arena.allocate(mapSize);
        var _ = (int) readProcessMemory.invokeExact(pHandle, MemorySegment.ofAddress(0x01005340), mapMS, mapSize, MemorySegment.NULL);
        for (var i = 0; i < h + 2; i++) {
            for (var j = 0; j < w + 2; j++) {
                var value = mapMS.getAtIndex(ValueLayout.JAVA_BYTE, i * 32L + j);
                System.out.printf("%2X|", value);
            }
            System.out.println();
        }

        // https://learn.microsoft.com/zh-cn/windows/win32/inputdev/mouse-input-notifications
        var WM_LBUTTONDOWN = 0x0201;
        var WM_LBUTTONUP = 0x0202;
        var WM_RBUTTONDOWN = 0x0204;
        var WM_RBUTTONUP = 0x0205;

        // 扫雷
        for (var i = 1; i <= h; i++) {
            for (var j = 1; j <= w; j++) {
                var _ = (int) readProcessMemory.invokeExact(pHandle, MemorySegment.ofAddress(0x01005340 + ((long) i << 5) + j), mapMS, mapSize, MemorySegment.NULL);
                var value = mapMS.getAtIndex(ValueLayout.JAVA_INT, 0);
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

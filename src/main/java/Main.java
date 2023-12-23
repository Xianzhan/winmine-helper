import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

SymbolLookup user32 = SymbolLookup.libraryLookup("User32", Arena.global());

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

MethodHandle find(SymbolLookup symbolLookup, String functionName, FunctionDescriptor functionDescriptor) {
    return Linker.nativeLinker()
            .downcallHandle(symbolLookup.find(functionName)
                    .orElseThrow(() -> new RuntimeException(STR."\{functionName} Not Found")), functionDescriptor
            );
}

MemorySegment L(Arena arena, String str) {
    // https://stackoverflow.com/questions/66072117/why-does-windows-use-utf-16le
    var bs = str.getBytes(StandardCharsets.UTF_16LE);
    var ms = arena.allocate(bs.length + Short.BYTES);
    MemorySegment.copy(bs, 0, ms, ValueLayout.JAVA_BYTE, 0, bs.length);
    // fix: 乱码 UTF_16LE 双字节
    ms.set(ValueLayout.JAVA_SHORT, bs.length, (short) '\0');

    return ms;
}

void main() throws Throwable {
    try (var arena = Arena.ofConfined()) {
        var winmineWindow = (MemorySegment) findWindowW_MH.invokeExact(MemorySegment.NULL, L(arena, "扫雷"));
        if (winmineWindow.address() == 0) {
            System.out.println("扫雷程序未启动");
            return;
        } else {
            System.out.println("扫雷程序已启动");
        }

        var pid = arena.allocate(ValueLayout.JAVA_INT);
        var tid = (MemorySegment) getWindowThreadProcessId.invokeExact(winmineWindow, pid);
        System.out.println(tid.get(ValueLayout.JAVA_INT, 0));
        System.out.println(pid.get(ValueLayout.JAVA_INT, 0));
    }
}

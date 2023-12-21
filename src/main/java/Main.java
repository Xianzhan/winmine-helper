import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

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
        var user32 = SymbolLookup.libraryLookup("User32", arena);

        // https://learn.microsoft.com/zh-cn/windows/win32/api/winuser/nf-winuser-findwindoww
        var findWindowW_MH = find(user32, "FindWindowW", FunctionDescriptor.of(
                // 返回值
                ValueLayout.ADDRESS,
                // className
                ValueLayout.ADDRESS,
                // windowsName
                ValueLayout.ADDRESS
        ));
        var winmineWindow = (MemorySegment) findWindowW_MH.invokeExact(MemorySegment.NULL, L(arena, "扫雷"));
        if (winmineWindow.address() == 0) {
            System.out.println("扫雷程序未启动");
        } else {
            System.out.println("扫雷程序已启动");
        }
    }
}

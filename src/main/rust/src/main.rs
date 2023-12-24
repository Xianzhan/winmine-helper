use windows::{
    core::PCWSTR,
    Win32::{
        Foundation::BOOL,
        System::Threading::{OpenProcess, PROCESS_ACCESS_RIGHTS},
        UI::WindowsAndMessaging::{FindWindowW, GetWindowThreadProcessId},
    },
};

fn main() {
    unsafe {
        let winmine = FindWindowW(
            PCWSTR::null(),
            PCWSTR::from_raw("扫雷\0".encode_utf16().collect::<Vec<u16>>().as_ptr()),
        );
        println!("winmine HWND: {:?}", winmine);

        let mut pid = 0u32;
        let thread_id = GetWindowThreadProcessId(winmine, Some(&mut pid));
        println!("pid: {pid}, thread_id: {thread_id}");
        if pid == 0 {
            println!("扫雷未打开，退出程序.");
            return;
        }

        let p_handle = OpenProcess(
            PROCESS_ACCESS_RIGHTS(0x000F_0000 | 0x0010_0000 | 0xFFFF),
            BOOL(0),
            pid,
        );

        match p_handle {
            Err(e) => println!("打开进程错误. {}", e),
            Ok(handle) => {
                println!("打开进程成功. {:?}", handle)
            }
        }
    }
}

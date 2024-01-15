use std::process;

use windows::{
    core::PCWSTR,
    Win32::{
        Foundation::BOOL,
        System::Threading::{OpenProcess, PROCESS_ALL_ACCESS},
        UI::WindowsAndMessaging::{FindWindowW, GetWindowThreadProcessId},
    },
};

fn main() {
    unsafe {
        let winmine = FindWindowW(
            PCWSTR::null(),
            PCWSTR::from_raw("扫雷\0".encode_utf16().collect::<Vec<u16>>().as_ptr()),
        );

        let mut pid = 0u32;
        let _ = GetWindowThreadProcessId(winmine, Some(&mut pid));
        if pid == 0 {
            eprintln!("扫雷获取 pid 失败, 退出助手");
            process::exit(-1);
        }
        println!("扫雷程序已启动");

        let p_handle = OpenProcess(PROCESS_ALL_ACCESS, BOOL(0), pid);
        if let Err(e) = p_handle {
            eprintln!("扫雷 HANDLE 获取失败, 退出助手. {}", e);
            process::exit(-2);
        }
    }
}

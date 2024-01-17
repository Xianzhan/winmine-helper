use std::{ffi::c_void, mem::size_of, process, ptr};

use windows::{
    core::PCWSTR,
    Win32::{
        Foundation::BOOL,
        System::{
            Diagnostics::Debug::ReadProcessMemory,
            Threading::{OpenProcess, PROCESS_ALL_ACCESS},
        },
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
        let p_handle = p_handle.expect("p_handle 获取失败");

        let hight_addr = 0x1005338;
        let mut hight = 0;
        let _ = ReadProcessMemory(
            p_handle,
            ptr::addr_of!(hight_addr) as *const c_void,
            ptr::addr_of_mut!(hight) as *mut c_void,
            size_of::<i32> as usize,
            None,
        );
        // TODO hight = 0
        println!("p_handle: {:?}, hight: {hight}", p_handle);
    }
}

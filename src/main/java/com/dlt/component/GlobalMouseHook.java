package com.dlt.component;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HINSTANCE;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.LowLevelMouseProc;
import com.sun.jna.platform.win32.WinUser.MSLLHOOKSTRUCT;
import com.sun.jna.Pointer;

/**
 * 使用 JNA 安装 Windows 低级鼠标钩子（WH_MOUSE_LL）
 * 全局捕获鼠标事件，不拦截任何窗口的点击
 */
public class GlobalMouseHook {

    // 鼠标侧键常量
    public static final int WM_XBUTTONDOWN = 0x020B;
    public static final int XBUTTON1 = 0x0001; // 后退侧键
    public static final int XBUTTON2 = 0x0002; // 前进侧键

    private volatile HHOOK hhk;
    private LowLevelMouseProc mouseProc;
    private MouseSideButtonListener listener;
    private Thread hookThread;

    /**
     * 侧键监听回调接口
     */
    public interface MouseSideButtonListener {
        /**
         * 当鼠标侧键被按下时调用
         * @param button 4=后退侧键(XBUTTON1), 5=前进侧键(XBUTTON2)
         */
        void onSideButtonPressed(int button);
    }

    public GlobalMouseHook(MouseSideButtonListener listener) {
        this.listener = listener;
    }

    /**
     * 安装全局鼠标钩子（在新线程中运行）
     */
    public void install() {
        hookThread = new Thread(() -> {
            final User32 lib = User32.INSTANCE;
            HINSTANCE hInst = Kernel32.INSTANCE.GetModuleHandle(null);

            mouseProc = new LowLevelMouseProc() {
                @Override
                public LRESULT callback(int nCode, WPARAM wParam, MSLLHOOKSTRUCT info) {
                    if (nCode >= 0 && wParam.intValue() == WM_XBUTTONDOWN) {
                        // 从 mouseData 中提取侧键信息
                        // MSLLHOOKSTRUCT 的 mouseData 高16位包含 XBUTTON 信息
                        int mouseData = info.mouseData;
                        int xButton = (mouseData >> 16) & 0xFFFF;

                        int button;
                        if (xButton == XBUTTON1) {
                            button = 4; // 后退侧键
                        } else if (xButton == XBUTTON2) {
                            button = 5; // 前进侧键
                        } else {
                            button = xButton;
                        }

                        if (listener != null) {
                            listener.onSideButtonPressed(button);
                        }
                    }
                    // 必须调用 CallNextHookEx，不拦截事件，传递给下一个钩子
                    return lib.CallNextHookEx(hhk, nCode, wParam, new LPARAM(Pointer.nativeValue(info.getPointer())));
                }
            };

            hhk = lib.SetWindowsHookEx(
                WinUser.WH_MOUSE_LL,
                mouseProc,
                hInst,
                0 // 全局钩子，dwThreadId 为 0
            );

            if (hhk == null) {
                System.err.println("安装鼠标钩子失败，错误码: " + Native.getLastError());
                return;
            }

            System.out.println("全局鼠标钩子已安装");

            // 消息循环（必须运行在钩子安装的线程上）
            WinUser.MSG msg = new WinUser.MSG();
            while (lib.GetMessage(msg, null, 0, 0) > 0) {
                lib.TranslateMessage(msg);
                lib.DispatchMessage(msg);
            }
        }, "GlobalMouseHook");

        hookThread.setDaemon(true);
        hookThread.start();
    }

    /**
     * 卸载全局鼠标钩子
     */
    public void uninstall() {
        if (hhk != null) {
            User32.INSTANCE.UnhookWindowsHookEx(hhk);
            hhk = null;
            System.out.println("全局鼠标钩子已卸载");
        }
        if (hookThread != null) {
            hookThread.interrupt();
        }
    }
}

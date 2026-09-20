package com.example.security.notification

import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.Notification
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

actual object PushNotificationManager {

    var sharedTrayIcon: TrayIcon? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    actual fun getPushToken(): String? {
        return null
    }

    actual fun showLocalNotification(title: String, body: String, messageId: String?) {
        println("[PushNotificationManager] showLocalNotification chamado. title: $title, body: $body")
        scope.launch {
            showSystemTrayNotification(title, body, messageId)
        }
    }

    actual fun hasPermission(): Boolean {
        return true
    }

    private val shownNotifications = mutableSetOf<String>()

    actual fun getClickedNotificationMessageId(): String? {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        if (!isWindows) return null

        try {
            val appData = System.getenv("APPDATA") ?: return null
            val signalFile = java.io.File(appData, "Pmsg/toast_signal.txt")
            if (signalFile.exists()) {
                val messageId = signalFile.readText().trim()
                signalFile.delete()
                if (messageId.isNotEmpty()) {
                    return messageId
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun showSystemTrayNotification(title: String, body: String, messageId: String?) {
        println("[PushNotificationManager] showSystemTrayNotification invocado. title=$title")
        
        // Dedup: só exibe se o messageId for nulo ou ainda não foi exibido.
        if (messageId != null && !shownNotifications.add(messageId)) {
            println("[PushNotificationManager] Notificação ignorada (já exibida): $messageId")
            return
        }
        
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        if (isWindows) {
            try {
                val safeTitle = escapePowerShell(title)
                val safeBody = escapePowerShell(body)
                val safeMessageId = escapePowerShell(messageId ?: "")
                
                val appData = System.getenv("APPDATA") ?: ""
                if (appData.isNotEmpty()) {
                    val pmsgDir = java.io.File(appData, "Pmsg")
                    pmsgDir.mkdirs()
                    val handlerFile = java.io.File(pmsgDir, "handler.ps1")
                    val vbsFile = java.io.File(pmsgDir, "handler.vbs")
                    
                    if (!handlerFile.exists() || !vbsFile.exists()) {
                        handlerFile.writeText("""
                            param([string]${'$'}url)
                            ${'$'}id = ${'$'}url -replace 'raix://','' -replace '/',''
                            Set-Content -Path "$appData\Pmsg\toast_signal.txt" -Value ${'$'}id
                        """.trimIndent())
                        
                        vbsFile.writeText(
                            "Set objShell = CreateObject(\"WScript.Shell\")\n" +
                            "cmd = \"powershell -WindowStyle Hidden -ExecutionPolicy Bypass -File \"\"\" & \"${handlerFile.absolutePath}\" & \"\"\" \"\"\" & WScript.Arguments(0) & \"\"\"\"\n" +
                            "objShell.Run cmd, 0, False\n"
                        )
                        val psReg = """
                            ${'$'}hkcu = [Microsoft.Win32.Registry]::CurrentUser
                            ${'$'}classes = ${'$'}hkcu.OpenSubKey('Software\Classes', ${'$'}true)
                            ${'$'}raix = ${'$'}classes.CreateSubKey('raix')
                            ${'$'}raix.SetValue('', 'URL:raix Protocol')
                            ${'$'}raix.SetValue('URL Protocol', '')
                            ${'$'}cmd = ${'$'}raix.CreateSubKey('shell\open\command')
                            ${'$'}cmd.SetValue('', 'wscript.exe "${vbsFile.absolutePath}" "%1"')
                        """.trimIndent().replace('\n', ';')
                        ProcessBuilder("powershell", "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-Command", psReg).start().waitFor()
                    }

                    // AUMID Registry
                    val aumidPs1 = java.io.File(pmsgDir, "setup_aumid.ps1")
                    if (!aumidPs1.exists()) {
                        aumidPs1.writeText("""
                            ${'$'}Code = @'
                            using System;
                            using System.Runtime.InteropServices;
                            public class Shortcut {
                                [ComImport, Guid("00021401-0000-0000-C000-000000000046"), ClassInterface(ClassInterfaceType.None)]
                                private class ShellLink {}
                                [ComImport, Guid("000214F9-0000-0000-C000-000000000046"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
                                private interface IShellLinkW {
                                    void GetPath([Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder pszFile, int cchMaxPath, IntPtr pfd, uint fFlags);
                                    void GetIDList(out IntPtr ppidl);
                                    void SetIDList(IntPtr pidl);
                                    void GetDescription([Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder pszFile, int cchMaxName);
                                    void SetDescription([MarshalAs(UnmanagedType.LPWStr)] string pszName);
                                    void GetWorkingDirectory([Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder pszDir, int cchMaxPath);
                                    void SetWorkingDirectory([MarshalAs(UnmanagedType.LPWStr)] string pszDir);
                                    void GetArguments([Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder pszArgs, int cchMaxPath);
                                    void SetArguments([MarshalAs(UnmanagedType.LPWStr)] string pszArgs);
                                    void GetHotkey(out short pwHotkey);
                                    void SetHotkey(short pwHotkey);
                                    void GetShowCmd(out uint piShowCmd);
                                    void SetShowCmd(uint piShowCmd);
                                    void GetIconLocation([Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder pszIconPath, int cchIconPath, out int piIcon);
                                    void SetIconLocation([MarshalAs(UnmanagedType.LPWStr)] string pszIconPath, int iIcon);
                                    void SetRelativePath([MarshalAs(UnmanagedType.LPWStr)] string pszPathRel, uint dwReserved);
                                    void Resolve(IntPtr hwnd, uint fFlags);
                                    void SetPath([MarshalAs(UnmanagedType.LPWStr)] string pszFile);
                                }
                                [ComImport, Guid("886D8EEB-8CF2-4446-8D02-CDBA1DBDCF99"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
                                private interface IPropertyStore {
                                    void GetCount(out uint cProps);
                                    void GetAt(uint iProp, out PROPERTYKEY pkey);
                                    void GetValue(ref PROPERTYKEY key, out PROPVARIANT pv);
                                    void SetValue(ref PROPERTYKEY key, ref PROPVARIANT propvar);
                                    void Commit();
                                }
                                [ComImport, Guid("0000010b-0000-0000-C000-000000000046"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
                                private interface IPersistFile {
                                    void GetClassID(out Guid pClassID);
                                    void IsDirty();
                                    void Load([MarshalAs(UnmanagedType.LPWStr)] string pszFileName, uint dwMode);
                                    void Save([MarshalAs(UnmanagedType.LPWStr)] string pszFileName, bool fRemember);
                                    void SaveCompleted([MarshalAs(UnmanagedType.LPWStr)] string pszFileName);
                                    void GetCurFile([Out, MarshalAs(UnmanagedType.LPWStr)] System.Text.StringBuilder pszFileName);
                                }
                                [StructLayout(LayoutKind.Sequential)]
                                private struct PROPERTYKEY {
                                    public Guid fmtid;
                                    public uint pid;
                                }
                                [StructLayout(LayoutKind.Sequential)]
                                private struct PROPVARIANT {
                                    public ushort vt;
                                    public ushort wReserved1;
                                    public ushort wReserved2;
                                    public ushort wReserved3;
                                    public IntPtr unionmember;
                                }
                                public static void Create(string shortcutPath, string targetPath, string aumid) {
                                    IShellLinkW link = (IShellLinkW)new ShellLink();
                                    link.SetPath(targetPath);
                                    IPropertyStore store = (IPropertyStore)link;
                                    PROPERTYKEY pkey = new PROPERTYKEY();
                                    pkey.fmtid = new Guid("9F4C2855-9F79-4B39-A8D0-E1D42DE1D5F3");
                                    pkey.pid = 5;
                                    PROPVARIANT var = new PROPVARIANT();
                                    var.vt = 31; // VT_LPWSTR
                                    var.unionmember = Marshal.StringToCoTaskMemUni(aumid);
                                    store.SetValue(ref pkey, ref var);
                                    store.Commit();
                                    IPersistFile file = (IPersistFile)link;
                                    file.Save(shortcutPath, false);
                                }
                            }
                            '@
                            Add-Type -TypeDefinition ${'$'}Code
                            [Shortcut]::Create("${'$'}env:APPDATA\Microsoft\Windows\Start Menu\Programs\Raix.lnk", (Get-Process -Id ${'$'}PID).Path, "Raix")
                        """.trimIndent())
                        ProcessBuilder("powershell", "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-File", aumidPs1.absolutePath).start().waitFor()
                    }
                }
                
                val psCommand = """
                    [Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null;
                    ${'$'}template = [Windows.UI.Notifications.ToastTemplateType]::ToastText02;
                    ${'$'}xml = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent(${'$'}template);
                    
                    if ('$safeMessageId' -ne '') {
                        ${'$'}toastNode = ${'$'}xml.GetElementsByTagName('toast').Item(0);
                        ${'$'}toastNode.SetAttribute('launch', 'raix://$safeMessageId');
                        ${'$'}toastNode.SetAttribute('activationType', 'protocol');
                    }
                    
                    ${'$'}texts = ${'$'}xml.GetElementsByTagName('text');
                    ${'$'}texts.Item(0).AppendChild(${'$'}xml.CreateTextNode('$safeTitle')) | Out-Null;
                    ${'$'}texts.Item(1).AppendChild(${'$'}xml.CreateTextNode('$safeBody')) | Out-Null;
                    ${'$'}toast = [Windows.UI.Notifications.ToastNotification]::new(${'$'}xml);
                    
                    ${'$'}notifier = [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('Raix');
                    ${'$'}notifier.Show(${'$'}toast);
                """.trimIndent().replace('\n', ' ')
                
                val process = ProcessBuilder("powershell", "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-Command", psCommand).start()
                process.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        // Fallback: AWT TrayIcon (Para Linux/Mac se houver)
        try {
            if (!SystemTray.isSupported()) {
                println("[PushNotificationManager] SystemTray não é suportado!")
                return
            }
            val tray = SystemTray.getSystemTray()

            val iconToUse = sharedTrayIcon ?: run {
                val resource = PushNotificationManager::class.java.getResource("/icon.png")
                val image = if (resource != null) {
                    Toolkit.getDefaultToolkit().createImage(resource)
                } else {
                    val fallback = java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                    val g = fallback.createGraphics()
                    g.color = java.awt.Color.BLUE
                    g.fillRect(0, 0, 16, 16)
                    g.dispose()
                    fallback
                }
                TrayIcon(image, "Raix").apply {
                    isImageAutoSize = true
                    tray.add(this)
                }
            }

            iconToUse.displayMessage(title, body, TrayIcon.MessageType.INFO)
        } catch (_: Throwable) {
        }
    }

    private fun escapePowerShell(str: String): String {
        return str.replace("'", "''")
            .replace("`", "``")
            .replace("\$", "`\$")
            .replace("\n", "`n")
            .replace("\r", "")
    }
}

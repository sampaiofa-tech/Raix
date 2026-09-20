# Tech Debt

## Windows AUMID Shortcut
- **Date:** 2026-09-20
- **Component:** Windows Notifications (PushNotificationManager.desktop.kt)
- **Description:** The `setup_aumid.ps1` script creates a Start Menu shortcut using PowerShell so that toast notifications correctly bind to the AUMID "Raix". The target of the shortcut is set dynamically by inspecting the current Java process (`ProcessHandle.current().info().command()`). This fallback to `javaw.exe` works in most cases, but for MSI installations, it might be better to hardcode or securely discover the `Raix.exe` binary path.

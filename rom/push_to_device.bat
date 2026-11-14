@echo off
echo ==============================================================
echo Installing BestEdgingPanel as a System Privileged App (priv-app)
echo ==============================================================

adb root
timeout /t 1 >nul
adb remount
if errorlevel 1 (
    echo [*] Remounting via su shell...
    adb shell "su -c 'mount -o remount,rw /system_ext || mount -o remount,rw /system || mount -o remount,rw /'"
)

echo [*] Creating target directories...
adb shell "su -c 'mkdir -p /system_ext/priv-app/BestEdgingPanel /system_ext/etc/permissions'"

echo [*] Pushing BestEdgingPanel.apk...
adb push "%~dp0..\BestEdgingPanel.apk" /data/local/tmp/BestEdgingPanel.apk
adb shell "su -c 'cp /data/local/tmp/BestEdgingPanel.apk /system_ext/priv-app/BestEdgingPanel/BestEdgingPanel.apk && chmod 644 /system_ext/priv-app/BestEdgingPanel/BestEdgingPanel.apk && chown root:root /system_ext/priv-app/BestEdgingPanel/BestEdgingPanel.apk'"

echo [*] Pushing privapp-permissions-com.amintum.bestedgingpanel.xml...
adb push "%~dp0etc\permissions\privapp-permissions-com.amintum.bestedgingpanel.xml" /data/local/tmp/privapp.xml
adb shell "su -c 'cp /data/local/tmp/privapp.xml /system_ext/etc/permissions/privapp-permissions-com.amintum.bestedgingpanel.xml && chmod 644 /system_ext/etc/permissions/privapp-permissions-com.amintum.bestedgingpanel.xml && chown root:root /system_ext/etc/permissions/privapp-permissions-com.amintum.bestedgingpanel.xml'"

echo [*] Cleaning up tmp files...
adb shell "rm -f /data/local/tmp/BestEdgingPanel.apk /data/local/tmp/privapp.xml"

echo ==============================================================
echo SUCCESS: BestEdgingPanel is installed to /system_ext/priv-app/
echo Reboot device to activate system permissions.
echo ==============================================================

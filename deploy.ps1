Write-Host "Waiting for device..." -ForegroundColor Cyan
adb wait-for-device
Write-Host "Device detected!" -ForegroundColor Green

$apkPath = "app\build\outputs\apk\debug\BestEdgingPanel.apk"
if (-not (Test-Path $apkPath)) {
    Write-Error "APK not found at $apkPath"
    exit 1
}

Write-Host "Pushing APK to /data/local/tmp/..." -ForegroundColor Cyan
adb push $apkPath /data/local/tmp/BestEdgingPanel.apk

Write-Host "Copying to Magisk overlay /system/product/priv-app/..." -ForegroundColor Cyan
adb shell "su -c 'cp /data/local/tmp/BestEdgingPanel.apk /data/adb/modules/sysui_overlay/system/product/priv-app/BestEdgingPanel/BestEdgingPanel.apk; chmod 644 /data/adb/modules/sysui_overlay/system/product/priv-app/BestEdgingPanel/BestEdgingPanel.apk'"

Write-Host "Restarting FloatingPanelService..." -ForegroundColor Cyan
adb shell "su -c 'am force-stop com.amintum.bestedgingpanel; am start-foreground-service -n com.amintum.bestedgingpanel/.FloatingPanelService'"

Write-Host "Deployment complete!" -ForegroundColor Green

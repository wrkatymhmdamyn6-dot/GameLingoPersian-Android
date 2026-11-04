# GameLingo فارسی — نسخه 0.3.0

این بسته سورس پروژه است؛ APK پس از اجرای موفق GitHub Actions در بخش Artifacts در دسترس خواهد بود.

## تغییرات
- هم‌تراز شدن Java و Kotlin روی JVM 17.
- versionCode برابر 3 و versionName برابر 0.3.0.
- ثبت callback مربوط به MediaProjection پیش از ایجاد VirtualDisplay.

## ساخت APK
در Android Studio پروژه را باز کنید و Build > Build APK(s) را اجرا کنید؛ یا در محیط دارای Android SDK 35 و JDK 17 دستور `gradle --no-daemon assembleDebug` را اجرا کنید.

خروجی: `app/build/outputs/apk/debug/app-debug.apk`

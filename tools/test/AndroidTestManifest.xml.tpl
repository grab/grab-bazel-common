<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="{{.PACKAGE_NAME}}">

    <instrumentation
        android:label="Tests for {{.PACKAGE_NAME}}"
        android:name="{{.INSTRUMENTATION_RUNNER}}"
        android:targetPackage="{{.PACKAGE_NAME}}" />

    <queries>

        <package
            android:name="androidx.test.orchestrator" />

        <package
            android:name="androidx.test.services" />

        <package
            android:name="com.google.android.apps.common.testing.services" />
    </queries>

    <uses-permission
        android:name="android.permission.REORDER_TASKS" />

    <application
        android:debuggable="true">

        <uses-library
            android:name="android.test.runner" />

        <activity
            android:name="androidx.test.core.app.InstrumentationActivityInvoker$BootstrapActivity"
            android:exported="true">

            <intent-filter>

                <action
                    android:name="android.intent.action.MAIN" />
            </intent-filter>
        </activity>

        <activity
            android:name="androidx.test.core.app.InstrumentationActivityInvoker$EmptyActivity"
            android:exported="true">

            <intent-filter>

                <action
                    android:name="android.intent.action.MAIN" />
            </intent-filter>
        </activity>

        <activity
            android:name="androidx.test.core.app.InstrumentationActivityInvoker$EmptyFloatingActivity"
            android:exported="true">

            <intent-filter>
                <action
                    android:name="android.intent.action.MAIN" />
            </intent-filter>
        </activity>
    </application>
</manifest>

package com.yaahua.vcam;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.view.Surface;

import java.util.List;
import java.util.concurrent.Executor;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Azar-only camera diagnostics. This class observes the camera pipeline and
 * deliberately does not replace surfaces or alter Azar's camera behavior.
 */
public final class AzarDiagnostics {
    public static final String PACKAGE_NAME = "com.azarlive.android";

    private AzarDiagnostics() {}

    public static boolean isAzar(XC_LoadPackage.LoadPackageParam lpparam) {
        return lpparam != null && PACKAGE_NAME.equals(lpparam.packageName);
    }

    public static void init(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!isAzar(lpparam)) return;

        XposedBridge.log("【VCAM-AZAR】diagnostics enabled; sdk=" + Build.VERSION.SDK_INT);
        hookOpenCamera(lpparam);
        hookImageReader(lpparam);
        hookCaptureSession(lpparam);
        hookAddTarget(lpparam);
    }

    private static void hookOpenCamera(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(CameraManager.class, "openCamera",
                    String.class, CameraDevice.StateCallback.class, Handler.class,
                    new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam param) {
                            XposedBridge.log("【VCAM-AZAR】openCamera(handler) id=" + param.args[0]
                                    + " callback=" + className(param.args[1]));
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("【VCAM-AZAR】openCamera(handler) hook unavailable: " + t);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                XposedHelpers.findAndHookMethod(CameraManager.class, "openCamera",
                        String.class, Executor.class, CameraDevice.StateCallback.class,
                        new XC_MethodHook() {
                            @Override protected void beforeHookedMethod(MethodHookParam param) {
                                XposedBridge.log("【VCAM-AZAR】openCamera(executor) id=" + param.args[0]
                                        + " callback=" + className(param.args[2]));
                            }
                        });
            } catch (Throwable t) {
                XposedBridge.log("【VCAM-AZAR】openCamera(executor) hook unavailable: " + t);
            }
        }
    }

    private static void hookImageReader(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(ImageReader.class, "newInstance",
                    int.class, int.class, int.class, int.class, new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam param) {
                            XposedBridge.log("【VCAM-AZAR】ImageReader.newInstance "
                                    + param.args[0] + "x" + param.args[1]
                                    + " format=" + param.args[2] + " maxImages=" + param.args[3]);
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("【VCAM-AZAR】ImageReader hook unavailable: " + t);
        }
    }

    private static void hookCaptureSession(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(CameraDevice.class, "createCaptureSession",
                    List.class, CameraCaptureSession.StateCallback.class, Handler.class,
                    new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam param) {
                            XposedBridge.log("【VCAM-AZAR】createCaptureSession outputs="
                                    + describeSurfaces((List<?>) param.args[0])
                                    + " callback=" + className(param.args[1]));
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("【VCAM-AZAR】createCaptureSession hook unavailable: " + t);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                XposedHelpers.findAndHookMethod(CameraDevice.class, "createCaptureSession",
                        SessionConfiguration.class, new XC_MethodHook() {
                            @Override protected void beforeHookedMethod(MethodHookParam param) {
                                SessionConfiguration config = (SessionConfiguration) param.args[0];
                                String outputs = "null";
                                if (config != null) {
                                    StringBuilder value = new StringBuilder("[");
                                    for (android.hardware.camera2.params.OutputConfiguration output
                                            : config.getOutputConfigurations()) {
                                        value.append(output.getSurfaces()).append(',');
                                    }
                                    outputs = value.append(']').toString();
                                }
                                XposedBridge.log("【VCAM-AZAR】createCaptureSession(SessionConfiguration) outputs="
                                        + outputs);
                            }
                        });
            } catch (Throwable t) {
                XposedBridge.log("【VCAM-AZAR】SessionConfiguration hook unavailable: " + t);
            }
        }
    }

    private static void hookAddTarget(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(CaptureRequest.Builder.class, "addTarget",
                    Surface.class, new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam param) {
                            XposedBridge.log("【VCAM-AZAR】CaptureRequest.addTarget surface=" + param.args[0]);
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("【VCAM-AZAR】addTarget hook unavailable: " + t);
        }
    }

    private static String className(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }

    private static String describeSurfaces(List<?> outputs) {
        if (outputs == null) return "null";
        StringBuilder value = new StringBuilder("[");
        for (Object output : outputs) value.append(output).append(',');
        return value.append(']').toString();
    }
}

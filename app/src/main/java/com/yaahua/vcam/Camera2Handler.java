package com.yaahua.vcam;

import android.hardware.camera2.CameraDevice;
import android.media.MediaMetadataRetriever;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CameraCaptureSession;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.view.Surface;
import android.widget.Toast;

import java.io.File;
import java.util.concurrent.Executor;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Camera2Handler {

    public static void init(final XC_LoadPackage.LoadPackageParam lpparam) {
        hookOpenCamera_3arg(lpparam);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            hookOpenCamera_4arg(lpparam);
        }
        hookAddTarget(lpparam);
        hookRemoveTarget(lpparam);
        hookBuild(lpparam);
        hookImageReaderNewInstance(lpparam);
        hookCaptureFailed(lpparam);
    }

    // ======================== openCamera (3参数: String, StateCallback, Handler) ========================
    private static void hookOpenCamera_3arg(final XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraManager", lpparam.classLoader,
                "openCamera", String.class, CameraDevice.StateCallback.class, Handler.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[1] == null) return;
                if (param.args[1].equals(SharedState.c2_state_cb)) return;
                SharedState.c2_state_cb = (CameraDevice.StateCallback) param.args[1];
                SharedState.c2_state_callback = param.args[1].getClass();

                if (HookGuards.isDisabled()) return;
                File file = HookGuards.getVideoFile();
                SharedState.need_to_show_toast = HookGuards.shouldShowToast();
                if (!file.exists()) {
                    if (SharedState.toast_content != null && SharedState.need_to_show_toast) {
                        try {
                            Toast.makeText(SharedState.toast_content,
                                    "不存在替换视频\n" + lpparam.packageName + "当前路径：" + SharedState.video_path,
                                    Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    return;
                }
                XposedBridge.log("【VCAM】1位参数初始化相机，类：" + SharedState.c2_state_callback.toString());
                SharedState.is_first_hook_build = true;
                Camera2SessionHook.processCamera2Init(SharedState.c2_state_callback);
            }
        });
    }

    // ======================== openCamera (4参数: String, Executor, StateCallback) API 28+ ========================
    private static void hookOpenCamera_4arg(final XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraManager", lpparam.classLoader,
                "openCamera", String.class, Executor.class, CameraDevice.StateCallback.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[2] == null) return;
                if (param.args[2].equals(SharedState.c2_state_cb)) return;
                SharedState.c2_state_cb = (CameraDevice.StateCallback) param.args[2];

                if (HookGuards.isDisabled()) return;
                File file = HookGuards.getVideoFile();
                SharedState.need_to_show_toast = HookGuards.shouldShowToast();
                if (!file.exists()) {
                    if (SharedState.toast_content != null && SharedState.need_to_show_toast) {
                        try {
                            Toast.makeText(SharedState.toast_content,
                                    "不存在替换视频\n" + lpparam.packageName + "当前路径：" + SharedState.video_path,
                                    Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    return;
                }
                SharedState.c2_state_callback = param.args[2].getClass();
                XposedBridge.log("【VCAM】2位参数初始化相机，类：" + SharedState.c2_state_callback.toString());
                SharedState.is_first_hook_build = true;
                Camera2SessionHook.processCamera2Init(SharedState.c2_state_callback);
            }
        });
    }

    // ======================== CaptureRequest.Builder.addTarget ========================
    private static void hookAddTarget(final XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder",
                lpparam.classLoader, "addTarget", Surface.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] == null) return;
                if (param.thisObject == null) return;

                File file = HookGuards.getVideoFile();
                SharedState.need_to_show_toast = HookGuards.shouldShowToast();
                if (!file.exists()) {
                    if (SharedState.toast_content != null && SharedState.need_to_show_toast) {
                        try {
                            Toast.makeText(SharedState.toast_content,
                                    "不存在替换视频\n" + lpparam.packageName + "当前路径：" + SharedState.video_path,
                                    Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    return;
                }
                if (param.args[0].equals(SharedState.c2_virtual_surface)) return;
                if (HookGuards.isDisabled()) return;

                String surfaceInfo = param.args[0].toString();
                if (surfaceInfo.contains("Surface(name=null)")) {
                    if (SharedState.c2_reader_Surfcae == null) {
                        SharedState.c2_reader_Surfcae = (Surface) param.args[0];
                    } else {
                        if ((!SharedState.c2_reader_Surfcae.equals(param.args[0])) &&
                                SharedState.c2_reader_Surfcae_1 == null) {
                            SharedState.c2_reader_Surfcae_1 = (Surface) param.args[0];
                        }
                    }
                } else {
                    if (SharedState.c2_preview_Surfcae == null) {
                        SharedState.c2_preview_Surfcae = (Surface) param.args[0];
                    } else {
                        if ((!SharedState.c2_preview_Surfcae.equals(param.args[0])) &&
                                SharedState.c2_preview_Surfcae_1 == null) {
                            SharedState.c2_preview_Surfcae_1 = (Surface) param.args[0];
                        }
                    }
                }
                XposedBridge.log("【VCAM】添加目标：" + param.args[0].toString());
                param.args[0] = SharedState.c2_virtual_surface;
            }
        });
    }

    // ======================== CaptureRequest.Builder.removeTarget ========================
    private static void hookRemoveTarget(final XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder",
                lpparam.classLoader, "removeTarget", Surface.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] == null) return;
                if (param.thisObject == null) return;

                File file = HookGuards.getVideoFile();
                SharedState.need_to_show_toast = HookGuards.shouldShowToast();
                if (!file.exists()) {
                    if (SharedState.toast_content != null && SharedState.need_to_show_toast) {
                        try {
                            Toast.makeText(SharedState.toast_content,
                                    "不存在替换视频\n" + lpparam.packageName + "当前路径：" + SharedState.video_path,
                                    Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    return;
                }
                if (HookGuards.isDisabled()) return;

                Surface rm_surf = (Surface) param.args[0];
                if (rm_surf.equals(SharedState.c2_preview_Surfcae)) SharedState.c2_preview_Surfcae = null;
                if (rm_surf.equals(SharedState.c2_preview_Surfcae_1)) SharedState.c2_preview_Surfcae_1 = null;
                if (rm_surf.equals(SharedState.c2_reader_Surfcae_1)) SharedState.c2_reader_Surfcae_1 = null;
                if (rm_surf.equals(SharedState.c2_reader_Surfcae)) SharedState.c2_reader_Surfcae = null;

                XposedBridge.log("【VCAM】移除目标：" + param.args[0].toString());
            }
        });
    }

    // ======================== CaptureRequest.Builder.build ========================
    private static void hookBuild(final XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder",
                lpparam.classLoader, "build", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.thisObject == null) return;
                if (param.thisObject.equals(SharedState.c2_builder)) return;
                SharedState.c2_builder = (CaptureRequest.Builder) param.thisObject;

                File file = HookGuards.getVideoFile();
                SharedState.need_to_show_toast = HookGuards.shouldShowToast();
                if (!file.exists() && SharedState.need_to_show_toast) {
                    if (SharedState.toast_content != null) {
                        try {
                            Toast.makeText(SharedState.toast_content,
                                    "不存在替换视频\n" + lpparam.packageName + "当前路径：" + SharedState.video_path,
                                    Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    return;
                }
                if (HookGuards.isDisabled()) return;

                XposedBridge.log("【VCAM】开始build请求");
                Camera2SessionHook.processCamera2Play();
            }
        });
    }

    // ======================== ImageReader.newInstance ========================
    private static void hookImageReaderNewInstance(final XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod("android.media.ImageReader", lpparam.classLoader, "newInstance",
                int.class, int.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                int requestedWidth = (int) param.args[0];
                int requestedHeight = (int) param.args[1];
                int[] videoSize = getSelectedVideoSize();
                if (videoSize != null && requestedWidth > 0 && requestedHeight > 0) {
                    // Keep the app's requested width but derive height from the actual video's aspect ratio.
                    // This avoids forcing portrait clips into a fixed 4:3/16:9 ImageReader buffer.
                    int adjustedHeight = Math.max(1, Math.round(requestedWidth * (videoSize[1] / (float) videoSize[0])));
                    param.args[1] = adjustedHeight;
                    requestedHeight = adjustedHeight;
                    XposedBridge.log("【VCAM】ImageReader按视频比例调整: " + requestedWidth + "x" + requestedHeight
                            + " (源视频 " + videoSize[0] + "x" + videoSize[1] + ")");
                }
                SharedState.c2_ori_width = requestedWidth;
                SharedState.c2_ori_height = requestedHeight;
                XposedBridge.log("【VCAM】应用创建了渲染器：宽：" + requestedWidth + " 高：" + requestedHeight + "格式" + param.args[2]);
                SharedState.imageReaderFormat = (int) param.args[2];
                SharedState.need_to_show_toast = HookGuards.shouldShowToast();
                if (SharedState.toast_content != null && SharedState.need_to_show_toast) {
                    try {
                        Toast.makeText(SharedState.toast_content,
                                "应用创建了渲染器：\n宽：" + param.args[0] + "\n高：" + param.args[1] +
                                "\n一般只需要宽高比与视频相同", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        XposedBridge.log("【VCAM】[toast]" + e.toString());
                    }
                }
            }
        });
    }

    private static int[] getSelectedVideoSize() {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            File video = HookGuards.getVideoFile();
            if (video == null || !video.exists()) return null;
            retriever.setDataSource(video.getAbsolutePath());
            String widthValue = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String heightValue = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String rotationValue = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            if (widthValue == null || heightValue == null) return null;
            int width = Integer.parseInt(widthValue);
            int height = Integer.parseInt(heightValue);
            int rotation = rotationValue == null ? 0 : Integer.parseInt(rotationValue);
            if (rotation == 90 || rotation == 270) {
                int swap = width;
                width = height;
                height = swap;
            }
            return width > 0 && height > 0 ? new int[]{width, height} : null;
        } catch (Throwable error) {
            XposedBridge.log("【VCAM】读取视频尺寸失败: " + error);
            return null;
        } finally {
            try { retriever.release(); } catch (Throwable ignored) {}
        }
    }

    // ======================== CaptureCallback.onCaptureFailed ========================
    private static void hookCaptureFailed(final XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraCaptureSession.CaptureCallback",
                lpparam.classLoader, "onCaptureFailed", CameraCaptureSession.class,
                CaptureRequest.class, CaptureFailure.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                XposedBridge.log("【VCAM】onCaptureFailed" + "原因：" +
                        ((CaptureFailure) param.args[2]).getReason());
            }
        });
    }
}
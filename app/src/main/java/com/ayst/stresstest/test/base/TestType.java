/*
 * Copyright(c) 2018 Bob Shen <ayst.shen@foxmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ayst.stresstest.test.base;

/**
 * Created by Bob Shen on 2017/11/13.
 */

public enum TestType{
    TYPE_CPU_TEST,                  // CPU Test
    TYPE_MEMORY_TEST,               // Memory Test
    TYPE_DDR_TEST,                  // DDR Test
    TYPE_VIDEO_TEST,                // Video Test
    TYPE_AUDIO_TEST,                // Audio Test
    TYPE_WIFI_TEST,                 // Wifi Test
    TYPE_BT_TEST,                   // Bluetooth Test
    TYPE_AIRPLANE_MODE_TEST,        // Airplane mode Test
    TYPE_REBOOT_TEST,               // Reboot Test
    TYPE_SLEEP_TEST,                // Sleep Test
    TYPE_RECOVERY_TEST,             // Recovery Test
    TYPE_TIMING_BOOT_TEST,          // Timing boot Test
    TYPE_NETWORK_TEST,              // Network Test
    TYPE_CAMERA_TEST,               // Camera Test
    TYPE_UVCCAMERA_TEST             // UVCCamera Test
}

/*
 * Created by Bob Shen on 18-8-9 下午5:28
 * Copyright(c) 2018. All rights reserved.
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

#include "MemOpUtils.h"
#include "stdlib.h"
#include "stdio.h"
#include "string.h"
#include <unistd.h>
#include "android/log.h"

#define MEMOP_LOG_TAG "libjnimemop"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, MEMOP_LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, MEMOP_LOG_TAG, __VA_ARGS__)

#define MAX_SIZE 2000
static int *p[MAX_SIZE];
static int count = 0;

/*
 * Class:     com_ayst_stresstest_util_MemOpUtils
 * Method:    malloc
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_ayst_stresstest_util_MemOpUtils_malloc
        (JNIEnv *, jclass, jint size) {
    if (count < MAX_SIZE) {
        p[count] = (int *) malloc(256 * 1024 * size * sizeof(int));
        if (p[count]) {
            memset(p[count], 2, 256 * 1024 * size * sizeof(int));
            count++;
            LOGI("memfill, malloc memory size is %d", size);
            return size;
        } else {
            LOGI("memfill, malloc memory error");
        }
    } else {
        LOGE("memfill, count is MAX_SIZE");
    }
    return 0;
}

/*
 * Class:     com_ayst_stresstest_util_MemOpUtils
 * Method:    free
 * Signature: ()I
 */
JNIEXPORT void JNICALL Java_com_ayst_stresstest_util_MemOpUtils_free
        (JNIEnv *, jclass) {
    for (int i = 0; i < MAX_SIZE; i++) {
        if (NULL != p[i]) {
            free(p[i]);
            p[i] = NULL;
        }
    }
    count = 0;
    LOGI("memfree, free memory");
    return;
}


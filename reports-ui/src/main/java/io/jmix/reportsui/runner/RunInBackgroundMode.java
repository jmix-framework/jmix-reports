/*
 * Copyright 2021 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.reportsui.runner;

import io.jmix.reportsui.screen.ReportsClientProperties;

/**
 * Options to control the way to run a report in UI: synchronously or in the background.
 */
public enum RunInBackgroundMode {
    /**
     * Run a report in the background
     */
    YES,
    /**
     * Run a report synchronously
     */
    NO,
    /**
     * The setting {@link ReportsClientProperties#getUseBackgroundReportProcessing()}
     * defines the default way to run a report: if true, a report will be run in background, otherwise - synchronously.
     */
    DEFAULT
}

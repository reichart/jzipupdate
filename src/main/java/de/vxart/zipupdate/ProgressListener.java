/*
 * Copyright 2005 Philipp Reichart <philipp.reichart@vxart.de>
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
package de.vxart.zipupdate;

/**
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public interface ProgressListener {
    /**
     * Sets or resets this listener to indeterminate mode and labels it with the
     * given message.
     * <p>
     * Indeterminate mode is used for tasks of unknown length.
     */
    void init(String message);

    /**
     * Sets or resets this listener to determinate
     * mode using the specified message, minimum and
     * maximum values.
     */
    void init(String message, int min, int max);

    /**
     * Updates the progress to the specified value.
     * <p>
     * Implementations may rely on this happening in order to
     * provide advanced progress information, therefore the
     * caller should update continously and not "jump"around.
     *
     * @param progress the new progress value
     */
    void update(int progress);

    /**
     * Returns the latest value set by the update method.
     *
     * @return the current progress value
     */
    int getProgress();

    /**
     * Labels the current task with the specified string.
     *
     * @param label a label describing the current task
     */
    void label(String label);

    /**
     * Finishes the display of progress by a progress listener,
     * e.g. by closing a progress dialog.
     */
    void finish();
}

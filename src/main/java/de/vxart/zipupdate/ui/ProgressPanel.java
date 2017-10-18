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
package de.vxart.zipupdate.ui;

import de.vxart.zipupdate.ProgressListener;

import javax.swing.*;

/**
 * @author Philipp Reichart, philipp.reichart@vxart.de
 */
public class ProgressPanel extends Box implements ProgressListener {
    private JProgressBar progressBar;
    private JLabel message;
    private JLabel leftDetail, rightDetail;

    private boolean speedShown = true;
    private boolean timeShown = true;

    private long start;
    private long prevSecsLeft;
    private float prevSpeed;

    private int call = 0;


    public ProgressPanel() {
        super(BoxLayout.Y_AXIS);

        //setBorder(BorderFactory.createLineBorder(Color.RED, 4));

        message = new JLabel("¿?");

        Box box = new Box(BoxLayout.X_AXIS);
        box.add(message);
        box.add(Box.createHorizontalGlue());

        progressBar = new JProgressBar();
        progressBar.setStringPainted(false);

        leftDetail = new JLabel(" ");
        rightDetail = new JLabel(" ");

        Box details = new Box(BoxLayout.X_AXIS);
        details.add(leftDetail);
        details.add(Box.createHorizontalGlue());
        details.add(rightDetail);

        add(box);
        add(details);
        add(progressBar);
    }

    public void init(String _message) {
        this.message.setText(_message);

        leftDetail.setText(" ");
        rightDetail.setText(" ");

        progressBar.setStringPainted(false);
        progressBar.setIndeterminate(true);

        start = System.currentTimeMillis();
        call = 0;
    }

    public void init(String _message, int min, int max) {
        this.message.setText(_message);

        progressBar.setStringPainted(true);
        progressBar.setIndeterminate(false);

        progressBar.setMinimum(min);
        progressBar.setMaximum(max);
        progressBar.setValue(min);

        start = System.currentTimeMillis();
        call = 0;
    }

    public void update(int value) {
        long now = System.currentTimeMillis();

        float speed = value / (1 + (now - start));

        // smooth speed by interpolating with previous value
        speed = (speed + prevSpeed) / 2;

        String kBytePerSecond = String.valueOf(speed);
        kBytePerSecond = kBytePerSecond.substring(
                0, kBytePerSecond.indexOf('.') + 2);

        prevSpeed = speed;

		/*
         * Calculate percentage and times
		 */
        int percent = 100 * value / progressBar.getMaximum();

        long secsElapsed = (now - start) / 1000;
        long secsTotal = 100 * secsElapsed / (percent + 1);
        long secsLeft = 1 + secsTotal - secsElapsed;

        // smooth time by interpolating with previous value
        secsLeft = (secsLeft + prevSecsLeft) / 2;

        prevSecsLeft = secsLeft;


		/*
		 * Update UI elements
		 */
        if (speed > 0 && speedShown)
            leftDetail.setText(kBytePerSecond + " KB/s");
        else
            leftDetail.setText("");

        if (timeShown)
            rightDetail.setText("(" + getFormattedTime(secsLeft) + " left)");

        progressBar.setValue(value);

        call++;
    }

    public void label(String _message) {
        this.message.setText(_message);
    }

    private static String getFormattedTime(long seconds) {
        StringBuilder buffer = new StringBuilder();

        long minutes = seconds / 60;
        if (minutes > 0) {
            buffer.append(minutes);
            buffer.append("m ");
            seconds %= 60;
        }

        buffer.append(seconds);
        buffer.append("s");

        return buffer.toString();
    }

    public int getProgress() {
        return progressBar.getValue();
    }

    @Override
    public void finish() {}

    public boolean isSpeedShown() {
        return speedShown;
    }

    public void setSpeedShown(boolean speedShown) {
        this.speedShown = speedShown;

        if (!speedShown)
            leftDetail.setText(" ");
    }

    public boolean isTimeShown() {
        return timeShown;
    }

    public void setTimeShown(boolean timeShown) {
        this.timeShown = timeShown;

        if (!timeShown)
            rightDetail.setText(" ");
    }
}

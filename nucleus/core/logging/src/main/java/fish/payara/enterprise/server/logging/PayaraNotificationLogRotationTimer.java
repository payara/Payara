/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.enterprise.server.logging;

import com.sun.enterprise.server.logging.LogRotationTimerTask;
import java.util.Timer;

/**
 *
 * @author Susan Rai
 */
public class PayaraNotificationLogRotationTimer {

    private Timer rotationTimer;

    private LogRotationTimerTask rotationTimerTask;

    private static PayaraNotificationLogRotationTimer instance = new PayaraNotificationLogRotationTimer();

    private PayaraNotificationLogRotationTimer() {
        rotationTimer = new Timer("payara-log-rotation-timer");
    }

    public static PayaraNotificationLogRotationTimer getInstance() {
        return instance;
    }

    public void startTimer(LogRotationTimerTask timerTask) {
        rotationTimerTask = timerTask;
        rotationTimer.schedule(rotationTimerTask,
                timerTask.getRotationTimerValue());
    }

    public void stopTimer() {
        rotationTimer.cancel();
    }

    public void restartTimer() {
        // We will restart the timer only if the timerTask is set which
        // means user has set a value for LogRotation based on Time
        if (rotationTimerTask != null) {
            rotationTimerTask.cancel();
            rotationTimerTask = new LogRotationTimerTask(
                    // This is wierd, We need to have a fresh TimerTask object
                    // to reschedule the work.
                    rotationTimerTask.task,
                    rotationTimerTask.getRotationTimerValueInMinutes());
            rotationTimer.schedule(rotationTimerTask,
                    rotationTimerTask.getRotationTimerValue());
        }
    }

    public void restartTimerForDayBasedRotation() {
        // We will restart the timer only if the timerTask is set which
        // means user has set a value for LogRotation based on Time
        if (rotationTimerTask != null) {
            rotationTimerTask.cancel();
            rotationTimerTask = new  LogRotationTimerTask(
                    // This is wierd, We need to have a fresh TimerTask object
                    // to reschedule the work.
                    rotationTimerTask.task,
                    60 * 24);
            rotationTimer.schedule(rotationTimerTask,
                    1000 * 60 * 60 * 24);
        }
    }
}
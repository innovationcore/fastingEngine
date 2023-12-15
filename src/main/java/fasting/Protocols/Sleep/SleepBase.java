package fasting.Protocols.Sleep;
//%% NEW FILE SleepBase BEGINS HERE %%

/*PLEASE DO NOT EDIT THIS CODE*/
/*This code was generated using the UMPLE 1.32.1.6535.66c005ced modeling language!*/


import java.util.*;

/**
 * UML State diagram for Sleeping messages, represented in Umple
 */
// line 3 "model.ump"
// line 106 "model.ump"
public class SleepBase
{

    //------------------------
    // MEMBER VARIABLES
    //------------------------

    //SleepBase Attributes
    private int timeout24Hours;
    private int sleepWarnDeadline;
    private int wakeWarnDeadline;

    //SleepBase State Machines
    public enum State { initial, waitSleep, warnSleep, sleep, warnWake, wake, timeout24, endProtocol }
    private State state;

    //Helper Variables
    private TimedEventHandler timeoutwaitSleepTowarnSleepHandler;
    private TimedEventHandler timeoutwarnSleepTotimeout24Handler;
    private TimedEventHandler timeoutsleepTowarnWakeHandler;
    private TimedEventHandler timeoutwarnWakeTowaitSleepHandler;
    private TimedEventHandler timeoutwakeTowaitSleepHandler;

    //------------------------
    // CONSTRUCTOR
    //------------------------

    public SleepBase()
    {
        timeout24Hours = 0;
        sleepWarnDeadline = 0;
        wakeWarnDeadline = 0;
        setState(State.initial);
    }

    //------------------------
    // INTERFACE
    //------------------------

    public boolean setTimeout24Hours(int aTimeout24Hours)
    {
        boolean wasSet = false;
        timeout24Hours = aTimeout24Hours;
        wasSet = true;
        return wasSet;
    }

    public boolean setSleepWarnDeadline(int aSleepWarnDeadline)
    {
        boolean wasSet = false;
        sleepWarnDeadline = aSleepWarnDeadline;
        wasSet = true;
        return wasSet;
    }

    public boolean setWakeWarnDeadline(int aWakeWarnDeadline)
    {
        boolean wasSet = false;
        wakeWarnDeadline = aWakeWarnDeadline;
        wasSet = true;
        return wasSet;
    }

    public int getTimeout24Hours()
    {
        return timeout24Hours;
    }

    public int getSleepWarnDeadline()
    {
        return sleepWarnDeadline;
    }

    public int getWakeWarnDeadline()
    {
        return wakeWarnDeadline;
    }

    public String getStateFullName()
    {
        String answer = state.toString();
        return answer;
    }

    public State getState()
    {
        return state;
    }

    public boolean receivedWaitSleep()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case initial:
                setState(State.waitSleep);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    public boolean receivedWarnSleep()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case initial:
                setState(State.warnSleep);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    public boolean receivedSleep()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case initial:
                setState(State.sleep);
                wasEventProcessed = true;
                break;
            case waitSleep:
                exitState();
                setState(State.sleep);
                wasEventProcessed = true;
                break;
            case warnSleep:
                exitState();
                setState(State.sleep);
                wasEventProcessed = true;
                break;
            case sleep:
                exitState();
                setState(State.sleep);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    public boolean recievedWarnWake()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case initial:
                setState(State.warnWake);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    public boolean receivedEndProtocol()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case initial:
                setState(State.endProtocol);
                wasEventProcessed = true;
                break;
            case waitSleep:
                exitState();
                setState(State.endProtocol);
                wasEventProcessed = true;
                break;
            case warnSleep:
                exitState();
                setState(State.endProtocol);
                wasEventProcessed = true;
                break;
            case sleep:
                exitState();
                setState(State.endProtocol);
                wasEventProcessed = true;
                break;
            case warnWake:
                exitState();
                setState(State.endProtocol);
                wasEventProcessed = true;
                break;
            case wake:
                exitState();
                setState(State.endProtocol);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    public boolean timeoutwaitSleepTowarnSleep()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case waitSleep:
                exitState();
                setState(State.warnSleep);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    public boolean timeoutwarnSleepTotimeout24()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case warnSleep:
                exitState();
                setState(State.timeout24);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    public boolean receivedWake()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case sleep:
                exitState();
                setState(State.wake);
                wasEventProcessed = true;
                break;
            case warnWake:
                exitState();
                setState(State.wake);
                wasEventProcessed = true;
                break;
            case wake:
                exitState();
                setState(State.wake);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    public boolean timeoutsleepTowarnWake()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case sleep:
                exitState();
                setState(State.warnWake);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    public boolean timeoutwarnWakeTowaitSleep()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case warnWake:
                exitState();
                setState(State.waitSleep);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    public boolean timeoutwakeTowaitSleep()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case wake:
                exitState();
                setState(State.waitSleep);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    private boolean __autotransition243__()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case timeout24:
                setState(State.waitSleep);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    private void exitState()
    {
        switch(state)
        {
            case waitSleep:
                stopTimeoutwaitSleepTowarnSleepHandler();
                break;
            case warnSleep:
                stopTimeoutwarnSleepTotimeout24Handler();
                break;
            case sleep:
                stopTimeoutsleepTowarnWakeHandler();
                break;
            case warnWake:
                stopTimeoutwarnWakeTowaitSleepHandler();
                break;
            case wake:
                stopTimeoutwakeTowaitSleepHandler();
                break;
        }
    }

    private void setState(State aState)
    {
        state = aState;

        // entry actions and do activities
        switch(state)
        {
            case initial:
                // line 12 "model.ump"
                // entry point, don't do anything here
                stateNotify("initial");
                break;
            case waitSleep:
                // line 26 "model.ump"
                // wait here until a sleep text is received
                stateNotify("waitSleep");
                startTimeoutwaitSleepTowarnSleepHandler();
                break;
            case warnSleep:
                // line 37 "model.ump"
                // send a reminder message at a certain time
                stateNotify("warnSleep");
                startTimeoutwarnSleepTotimeout24Handler();
                break;
            case sleep:
                // line 48 "model.ump"
                // receive additional sleep messages and update
                // starting time, also receive wake
                stateNotify("sleep");
                startTimeoutsleepTowarnWakeHandler();
                break;
            case warnWake:
                // line 63 "model.ump"
                stateNotify("warnWake");
                startTimeoutwarnWakeTowaitSleepHandler();
                break;
            case wake:
                // line 72 "model.ump"
                // receive additional wake messages and update
                // end time
                stateNotify("wake");
                startTimeoutwakeTowaitSleepHandler();
                break;
            case timeout24:
                // line 83 "model.ump"
                // sends a message if sleep or wake are not
                // received for the day
                stateNotify("timeout24");
                __autotransition243__();
                break;
            case endProtocol:
                // line 93 "model.ump"
                stateNotify("endProtocol");
                break;
        }
    }

    private void startTimeoutwaitSleepTowarnSleepHandler()
    {
        timeoutwaitSleepTowarnSleepHandler = new TimedEventHandler(this,"timeoutwaitSleepTowarnSleep",sleepWarnDeadline);
    }

    private void stopTimeoutwaitSleepTowarnSleepHandler()
    {
        timeoutwaitSleepTowarnSleepHandler.stop();
    }

    private void startTimeoutwarnSleepTotimeout24Handler()
    {
        timeoutwarnSleepTotimeout24Handler = new TimedEventHandler(this,"timeoutwarnSleepTotimeout24",timeout24Hours);
    }

    private void stopTimeoutwarnSleepTotimeout24Handler()
    {
        timeoutwarnSleepTotimeout24Handler.stop();
    }

    private void startTimeoutsleepTowarnWakeHandler()
    {
        timeoutsleepTowarnWakeHandler = new TimedEventHandler(this,"timeoutsleepTowarnWake",wakeWarnDeadline);
    }

    private void stopTimeoutsleepTowarnWakeHandler()
    {
        timeoutsleepTowarnWakeHandler.stop();
    }

    private void startTimeoutwarnWakeTowaitSleepHandler()
    {
        timeoutwarnWakeTowaitSleepHandler = new TimedEventHandler(this,"timeoutwarnWakeTowaitSleep",timeout24Hours);
    }

    private void stopTimeoutwarnWakeTowaitSleepHandler()
    {
        timeoutwarnWakeTowaitSleepHandler.stop();
    }

    private void startTimeoutwakeTowaitSleepHandler()
    {
        timeoutwakeTowaitSleepHandler = new TimedEventHandler(this,"timeoutwakeTowaitSleep",timeout24Hours);
    }

    private void stopTimeoutwakeTowaitSleepHandler()
    {
        timeoutwakeTowaitSleepHandler.stop();
    }

    public static class TimedEventHandler extends TimerTask
    {
        private SleepBase controller;
        private String timeoutMethodName;
        private double howLongInSeconds;
        private Timer timer;

        public TimedEventHandler(SleepBase aController, String aTimeoutMethodName, double aHowLongInSeconds)
        {
            controller = aController;
            timeoutMethodName = aTimeoutMethodName;
            howLongInSeconds = aHowLongInSeconds;
            timer = new Timer();
            timer.schedule(this, (long)howLongInSeconds*1000);
        }

        public void stop()
        {
            timer.cancel();
        }

        public void run ()
        {
            if ("timeoutwaitSleepTowarnSleep".equals(timeoutMethodName))
            {
                boolean shouldRestart = !controller.timeoutwaitSleepTowarnSleep();
                if (shouldRestart)
                {
                    controller.startTimeoutwaitSleepTowarnSleepHandler();
                }
                return;
            }
            if ("timeoutwarnSleepTotimeout24".equals(timeoutMethodName))
            {
                boolean shouldRestart = !controller.timeoutwarnSleepTotimeout24();
                if (shouldRestart)
                {
                    controller.startTimeoutwarnSleepTotimeout24Handler();
                }
                return;
            }
            if ("timeoutsleepTowarnWake".equals(timeoutMethodName))
            {
                boolean shouldRestart = !controller.timeoutsleepTowarnWake();
                if (shouldRestart)
                {
                    controller.startTimeoutsleepTowarnWakeHandler();
                }
                return;
            }
            if ("timeoutwarnWakeTowaitSleep".equals(timeoutMethodName))
            {
                boolean shouldRestart = !controller.timeoutwarnWakeTowaitSleep();
                if (shouldRestart)
                {
                    controller.startTimeoutwarnWakeTowaitSleepHandler();
                }
                return;
            }
            if ("timeoutwakeTowaitSleep".equals(timeoutMethodName))
            {
                boolean shouldRestart = !controller.timeoutwakeTowaitSleep();
                if (shouldRestart)
                {
                    controller.startTimeoutwakeTowaitSleepHandler();
                }
                return;
            }
        }
    }

    public void delete()
    {}

    // line 100 "model.ump"
    public boolean stateNotify(String node){
        return true;
    }

    // line 101 "model.ump"
    public int currentTime(){
        return 1;
    }


    public String toString()
    {
        return super.toString() + "["+
                "timeout24Hours" + ":" + getTimeout24Hours()+ "," +
                "sleepWarnDeadline" + ":" + getSleepWarnDeadline()+ "," +
                "wakeWarnDeadline" + ":" + getWakeWarnDeadline()+ "]";
    }
}
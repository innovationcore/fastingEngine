package fasting.Protocols.CCW.CCW_WeeklyMessage;
//%% NEW FILE CCW_WeeklyMessageBase BEGINS HERE %%

/*PLEASE DO NOT EDIT THIS CODE*/
/*This code was generated using the UMPLE 1.32.1.6535.66c005ced modeling language!*/


import java.util.Timer;
import java.util.TimerTask;

/**
 * UML State diagram for a library loan, represented in Umple
 */
// line 3 "model.ump"
public class CCW_WeeklyMessageBase
{

    //------------------------
    // MEMBER VARIABLES
    //------------------------

    //CCW_WeeklyMessageBase Attributes
    private int timeout1Week;

    //CCW_WeeklyMessageBase State Machines
    public enum State { initial, waitWeek, sendWeeklyMessage, endProtocol }
    private State state;

    //Helper Variables
    private TimedEventHandler timeoutwaitWeekTosendWeeklyMessageHandler;

    //------------------------
    // CONSTRUCTOR
    //------------------------

    public CCW_WeeklyMessageBase()
    {
        timeout1Week = 0;
        setState(State.initial);
    }

    //------------------------
    // INTERFACE
    //------------------------

    public boolean setTimeout1Week(int aTimeout1Week)
    {
        boolean wasSet = false;
        timeout1Week = aTimeout1Week;
        wasSet = true;
        return wasSet;
    }

    public int getTimeout1Week()
    {
        return timeout1Week;
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

    public boolean receivedWaitWeek()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case initial:
                setState(State.waitWeek);
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
            case waitWeek:
                exitState();
                setState(State.endProtocol);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    public boolean timeoutwaitWeekTosendWeeklyMessage()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case waitWeek:
                exitState();
                setState(State.sendWeeklyMessage);
                wasEventProcessed = true;
                break;
            default:
                // Other states do respond to this event
        }

        return wasEventProcessed;
    }

    private boolean __autotransition13090__()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case sendWeeklyMessage:
                setState(State.waitWeek);
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
            case waitWeek:
                stopTimeoutwaitWeekTosendWeeklyMessageHandler();
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
                // line 11 "model.ump"
                // entrypoint
                stateNotify("initial");
                break;
            case waitWeek:
                // line 22 "model.ump"
                // wait here until a certain time of day on a
                // certain day of the week
                stateNotify("waitWeek");
                startTimeoutwaitWeekTosendWeeklyMessageHandler();
                break;
            case sendWeeklyMessage:
                // line 34 "model.ump"
                // send weekly message to participant
                stateNotify("sendWeeklyMessage");
                __autotransition13090__();
                break;
            case endProtocol:
                // line 43 "model.ump"
                stateNotify("endProtocol");
                break;
        }
    }

    private void startTimeoutwaitWeekTosendWeeklyMessageHandler()
    {
        timeoutwaitWeekTosendWeeklyMessageHandler = new TimedEventHandler(this,"timeoutwaitWeekTosendWeeklyMessage",timeout1Week);
    }

    private void stopTimeoutwaitWeekTosendWeeklyMessageHandler()
    {
        timeoutwaitWeekTosendWeeklyMessageHandler.stop();
    }

    public static class TimedEventHandler extends TimerTask
    {
        private CCW_WeeklyMessageBase controller;
        private String timeoutMethodName;
        private double howLongInSeconds;
        private Timer timer;

        public TimedEventHandler(CCW_WeeklyMessageBase aController, String aTimeoutMethodName, double aHowLongInSeconds)
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
            if ("timeoutwaitWeekTosendWeeklyMessage".equals(timeoutMethodName))
            {
                boolean shouldRestart = !controller.timeoutwaitWeekTosendWeeklyMessage();
                if (shouldRestart)
                {
                    controller.startTimeoutwaitWeekTosendWeeklyMessageHandler();
                }
                return;
            }
        }
    }

    public void delete()
    {}

    // line 50 "model.ump"
    public boolean stateNotify(String node){
        return true;
    }

    // line 51 "model.ump"
    public int currentTime(){
        return 1;
    }


    public String toString()
    {
        return super.toString() + "["+
                "timeout1Week" + ":" + getTimeout1Week()+ "]";
    }
}
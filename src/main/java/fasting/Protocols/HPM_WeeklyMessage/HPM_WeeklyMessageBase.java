package fasting.Protocols.HPM_WeeklyMessage;
//%% NEW FILE HPM_WeeklyMessageBase BEGINS HERE %%

/*PLEASE DO NOT EDIT THIS CODE*/
/*This code was generated using the UMPLE 1.32.1.6535.66c005ced modeling language!*/


import java.util.*;

/**
 * UML State diagram for a library loan, represented in Umple
 */
// line 3 "model.ump"
// line 50 "model.ump"
public class HPM_WeeklyMessageBase
{

    //------------------------
    // MEMBER VARIABLES
    //------------------------

    //HPM_WeeklyMessageBase Attributes
    private int timeout1Week;

    //HPM_WeeklyMessageBase State Machines
    public enum State { initial, waitWeek, sendHPM_WeeklyMessage, endProtocol }
    private State state;

    //Helper Variables
    private TimedEventHandler timeoutwaitWeekTosendHPM_WeeklyMessageHandler;

    //------------------------
    // CONSTRUCTOR
    //------------------------

    public HPM_WeeklyMessageBase()
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

    public boolean timeoutwaitWeekTosendHPM_WeeklyMessage()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case waitWeek:
                exitState();
                setState(State.sendHPM_WeeklyMessage);
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

    private boolean __autotransition100__()
    {
        boolean wasEventProcessed = false;

        State aState = state;
        switch (aState)
        {
            case sendHPM_WeeklyMessage:
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
                stopTimeoutwaitWeekTosendHPM_WeeklyMessageHandler();
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
                stateNotify("initial");
                break;
            case waitWeek:
                // line 20 "model.ump"
                stateNotify("waitWeek");
                startTimeoutwaitWeekTosendHPM_WeeklyMessageHandler();
                break;
            case sendHPM_WeeklyMessage:
                // line 30 "model.ump"
                stateNotify("sendHPM_WeeklyMessage");
                __autotransition100__();
                break;
            case endProtocol:
                // line 37 "model.ump"
                stateNotify("endProtocol");
                break;
        }
    }

    private void startTimeoutwaitWeekTosendHPM_WeeklyMessageHandler()
    {
        timeoutwaitWeekTosendHPM_WeeklyMessageHandler = new TimedEventHandler(this,"timeoutwaitWeekTosendHPM_WeeklyMessage",timeout1Week);
    }

    private void stopTimeoutwaitWeekTosendHPM_WeeklyMessageHandler()
    {
        timeoutwaitWeekTosendHPM_WeeklyMessageHandler.stop();
    }

    public static class TimedEventHandler extends TimerTask
    {
        private HPM_WeeklyMessageBase controller;
        private String timeoutMethodName;
        private double howLongInSeconds;
        private Timer timer;

        public TimedEventHandler(HPM_WeeklyMessageBase aController, String aTimeoutMethodName, double aHowLongInSeconds)
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
            if ("timeoutwaitWeekTosendHPM_WeeklyMessage".equals(timeoutMethodName))
            {
                boolean shouldRestart = !controller.timeoutwaitWeekTosendHPM_WeeklyMessage();
                if (shouldRestart)
                {
                    controller.startTimeoutwaitWeekTosendHPM_WeeklyMessageHandler();
                }
                return;
            }
        }
    }

    public void delete()
    {}

    // line 44 "model.ump"
    public boolean stateNotify(String node){
        return true;
    }

    // line 45 "model.ump"
    public int currentTime(){
        return 1;
    }


    public String toString()
    {
        return super.toString() + "["+
                "timeout1Week" + ":" + getTimeout1Week()+ "]";
    }
}